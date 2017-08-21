package org.regadou.system;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
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
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.PathExpression;
import org.regadou.reference.ReferenceHolder;
import org.regadou.reference.SimpleExpression;
import org.regadou.reference.UrlReference;
import org.regadou.script.DefaultCompiledScript;
import org.regadou.util.EnumerationSet;
import org.regadou.script.GenericComparator;
import org.regadou.util.HtmlHandler;
import org.regadou.util.MapAdapter;

public class RestServlet implements Servlet {

   private static final String DEFAULT_MIMETYPE = "text/html";

   private ServletConfig servletConfig;
   private Configuration configuration;
   private GenericComparator comparator;
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
      if (properties.containsKey("configuration"))
         configuration = new Bootstrap(properties.getProperty("configuration"));
      else
         configuration = new GuiceConfiguration();
      comparator = new GenericComparator(configuration);
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
            catch (ScriptException e) { throw new RuntimeException(e); }
         }
         String charset = request.getCharacterEncoding();
         if (charset == null)
            charset = Charset.defaultCharset().displayName();
         String mimetype = request.getContentType();
         if (mimetype == null || mimetype.indexOf('/') < 0) {
            mimetype = request.getHeader("accept");
            if (mimetype == null || mimetype.indexOf('/') < 0)
               mimetype = DEFAULT_MIMETYPE;
            else
               mimetype = mimetype.split(";")[0].split(",")[0];
         }
         Object value = getValue(request, cx);
         while (value instanceof Reference)
            value = ((Reference)value).getValue();
         response.setContentType(mimetype);
         response.setCharacterEncoding(charset);
         MimeHandler handler = configuration.getHandlerFactory().getHandler(mimetype);
         if (handler == null) {
            response.setContentType("text/plain");
            response.getOutputStream().write(String.valueOf(value).getBytes(charset));
            return;
         }
         else if (handler instanceof HtmlHandler)
            value = new HtmlHandler.Link(request.getRequestURL().toString(), value);
         handler.getOutputHandler(mimetype)
                .save(response.getOutputStream(), charset, value);
      }
      finally { factory.setScriptContext(null); }
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
      Reference result;
      switch (request.getMethod().toLowerCase()) {
         case "delete":
            if (path.isEmpty())
               return null;
            List<String> parent = new ArrayList<>(Arrays.asList(path.split("/")));
            String id = parent.remove(parent.size()-1);
            result = getReference(String.join("/", parent), cx);
            //TODO: remove the last part, use it as key to remove from map of result expression
         case "post":
            //TODO: should be a map or a collection to add DATA to
         case "put":
            //TODO: would call setValue(DATA) on result expression
         case "patch":
            //TODO: would merge result value with DATA and setValue the resulting object
         case "get":
         default:
            return getReference(path, cx);
      }
   }

   private Reference getReference(String path, ScriptContext cx) {
      if (path.isEmpty())
         return new ReferenceHolder(null, comparator.getSortedStringList(cx), true);
      List parts = new ArrayList();
      for (String part : path.split("/")) {
         String txt = part.trim();
         parts.add((!txt.startsWith("(") || !txt.endsWith(")")) ? txt
                   : new SimpleExpression(txt.substring(1, txt.length()-1), configuration));
      }
      return new PathExpression(configuration, cx, parts.toArray()).getValue(cx);
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
