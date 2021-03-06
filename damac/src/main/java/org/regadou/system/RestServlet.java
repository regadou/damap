package org.regadou.system;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
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
import org.regadou.action.ActionBuilder;
import org.regadou.damai.Bootstrap;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.InputStreamReference;
import org.regadou.expression.PathExpression;
import org.regadou.reference.GenericReference;
import org.regadou.resource.Url;
import org.regadou.script.DefaultCompiledScript;
import org.regadou.collection.EnumerationSet;
import org.regadou.action.GenericComparator;
import org.regadou.mime.HtmlHandler;
import org.regadou.script.DefaultScriptContext;
import org.regadou.collection.MapAdapter;
import org.regadou.collection.StaticMap;
import org.regadou.damai.Action;

public class RestServlet implements Servlet {

   private static final int NOT_FOUND = 404;
   private static final String DEFAULT_MIMETYPE = "text/html";
   private static final Map COMMAND_MAPPING = new StaticMap(new Object[][]{
      {"get",    Command.GET},
      {"put",    Command.SET},
      {"post",   Command.CREATE},
      {"patch",  Command.UPDATE},
      {"delete", Command.DESTROY}
   });

   private ServletConfig servletConfig;
   private Configuration configuration;
   private GenericComparator comparator;
   private CompiledScript initScript;
   private Map<String,Reference> keywords;

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
      comparator = configuration.getInstance(GenericComparator.class);
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
      try {
         URL url = configuration.getInitScript();
         if (url != null) {
            Url r = new Url(url, configuration);
            String mimetype = r.getMimetype();
            ScriptEngine engine = configuration.getEngineManager().getEngineByMimeType(mimetype);
            if (engine instanceof Compilable)
               initScript = ((Compilable)engine).compile(new InputStreamReader(r.getInputStream()));
            else if (engine != null)
               initScript = new DefaultCompiledScript(engine, new InputStreamReader(r.getInputStream()), configuration);
          }
      }
      catch (Exception ex) { ex.printStackTrace(); }

      keywords = new TreeMap<>();
      for (Object value : new Object[]{true, false, null})
         keywords.put(String.valueOf(value), new GenericReference(String.valueOf(value), value, true));
      List<Action> actions = new ActionBuilder(configuration)
              .setWantCommands(false)
              .setWantOperators(true)
              .setWantOptimized(false)
              .setWantSymbols(true)
              .setWantStandard(false)
              .buildAll();
      for (Action action : actions)
         keywords.put(action.getName(), new GenericReference(action.getName(), action, true));
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
      ScriptContext cx = factory.getScriptContext(new GenericReference("request", request),
                                                  new GenericReference("response", response));
      try {
         if (initScript != null) {
            try {
               Object result = initScript.eval(cx);
               while (result instanceof Reference)
                  result = ((Reference)result).getValue();
               if (result != null) {
                  ScriptContext cx2 = new DefaultScriptContext(result);
                  for (Integer scope : cx2.getScopes()) {
                     Bindings src = cx2.getBindings(scope);
                     if (src != null) {
                        Bindings dst = cx.getBindings(scope);
                        if (dst == null)
                           cx.setBindings(src, scope);
                        else
                           dst.putAll(src);
                     }
                  }
               }
            }
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
         Command command = (Command)COMMAND_MAPPING.get(request.getMethod().toLowerCase());
         Object data = null;
         String url = request.getRequestURL().toString();
         if (command.getParameterTypes().length > 1) {
            String id = url;
            String query = request.getQueryString();
            if (query != null && !query.isEmpty())
               id += "?" + query;
            data = new InputStreamReference(id, request.getInputStream(), mimetype, charset, configuration.getHandlerFactory());
         }
         Object value = new PathExpression(configuration, keywords, command, request.getPathInfo(), data).getValue(cx);
         if (value == null || (command == Command.DESTROY && value.equals(Boolean.FALSE)))
            response.setStatus(NOT_FOUND);
         else
            value = comparator.getValue(value);
         response.setContentType(mimetype);
         response.setCharacterEncoding(charset);
         MimeHandler handler = configuration.getHandlerFactory().getHandler(mimetype);
         if (handler == null) {
            response.setContentType("text/plain");
            response.getOutputStream().write(String.valueOf(value).getBytes(charset));
            return;
         }
         else if (handler instanceof HtmlHandler)
            value = new HtmlHandler.Link(url, value);
         handler.save(response.getOutputStream(), charset, value);
      }
      finally { factory.setScriptContext(null); }
   }
}
