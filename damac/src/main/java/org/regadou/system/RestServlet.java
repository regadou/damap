package org.regadou.system;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Properties;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.regadou.damai.Bootstrap;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.PathExpression;
import org.regadou.reference.ReferenceHolder;
import org.regadou.reference.UrlReference;
import org.regadou.script.DefaultCompiledScript;
import org.regadou.util.EnumerationSet;
import org.regadou.util.MapAdapter;

public class RestServlet implements Servlet {

   private static final String DEFAULT_MIMETYPE = "application/json";

   private ServletConfig servletConfig;
   private Configuration configuration;
   private CompiledScript initScript;

   @Override
   public String toString() {
      return "[Servlet "+servletConfig.getServletName()+"]";
   }

   @Override
   public String getServletInfo() {
      return "Damai REST Servlet Implementation";
   }

   @Override
   public void init(ServletConfig config) throws ServletException {
      servletConfig = config;
      Properties properties = new Properties();
      Enumeration<String> e = config.getInitParameterNames();
      while (e.hasMoreElements()) {
         String name = e.nextElement();
         properties.put(name, config.getInitParameter(name));
      }
      ServletContext cx = config.getServletContext();
      configuration = new Bootstrap(properties.getProperty("configuration"));
      Bindings global = configuration.getGlobalScope();
      if (global == null) {
         global = new SimpleBindings(new MapAdapter<>(
            () -> new EnumerationSet(cx.getAttributeNames()),
            cx::getAttribute,
            cx::setAttribute,
            cx::removeAttribute)
         );
         configuration.getEngineManager().setBindings(global);
      }
      e = cx.getInitParameterNames();
      while (e.hasMoreElements()) {
         String name = e.nextElement();
         global.put(name, cx.getInitParameter(name));
      }
      global.put(Configuration.class.getName(), configuration);
      checkInitScript();
   }

   @Override
   public ServletConfig getServletConfig() {
      return servletConfig;
   }

   @Override
   public void destroy() {
   }

   @Override
   public void service(ServletRequest request, ServletResponse response) throws IOException {
      doRequest((HttpServletRequest)request, (HttpServletResponse)response);
   }

   private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
      ScriptContextFactory factory = configuration.getContextFactory();
      ScriptContext cx = factory.getScriptContext(new ReferenceHolder("request", request),
                                                  new ReferenceHolder("response", response));
      try {
         if (initScript != null) {
            try { initScript.eval(cx); }
            catch (ScriptException e) {}
         }
         String charset = request.getCharacterEncoding();
         if (charset == null)
            charset = Charset.defaultCharset().displayName();
         String mimetype = request.getContentType();
         if (mimetype == null)
            mimetype = DEFAULT_MIMETYPE;
         Object value = getValue(request, cx);
         response.setContentType(mimetype);
         response.setCharacterEncoding(charset);
         configuration.getHandlerFactory()
                      .getHandler(mimetype)
                      .getOutputHandler(mimetype)
                      .save(response.getOutputStream(), charset, value);
      }
      finally { factory.closeScriptContext(cx); }
   }

   private Object getValue(HttpServletRequest request, ScriptContext cx) {
      String path = request.getPathInfo();
      if (path == null)
         path = "";
      while (path.startsWith("/"))
         path = path.substring(1);
      while (path.endsWith("/"))
         path = path.substring(0, path.length()-1);
      path = path.trim();
      Reference r = new PathExpression(configuration.getPropertyFactory(), cx, path.isEmpty() ? new String[0] : path.split("/"));
      String method = request.getMethod().toLowerCase();
      //TODO: check for POST, PUT, PATCH, DELETE methods to do something about it
      Object value = r.getValue();
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      return value;
   }

   private void checkInitScript() {
      try {
         URL url = configuration.getInitScript();
         if (url != null) {
            UrlReference r = new UrlReference(url, configuration);
            String mimetype = r.getMimetype();
            ScriptEngine engine = configuration.getEngineManager().getEngineByMimeType(mimetype);
            if (engine instanceof Compilable)
               initScript = ((Compilable)engine).compile(new InputStreamReader(r.getInputStream()));
            else if (engine != null)
               initScript = new DefaultCompiledScript(engine, new InputStreamReader(r.getInputStream()), configuration);
          }
      }
      catch (Exception e) { e.printStackTrace(); }
   }
}
