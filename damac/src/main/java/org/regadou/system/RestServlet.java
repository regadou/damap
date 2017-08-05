package org.regadou.system;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.script.ScriptContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.PathExpression;
import org.regadou.reference.ReferenceHolder;

public class RestServlet implements Servlet {

   private ServletConfig servletConfig;
   private Configuration configuration;
   private Map properties;

   @Override
   public String toString() {
      return "[Servlet "+servletConfig.getServletName()+"]";
   }

   @Override
   public String getServletInfo() {
      return "Damac REST Servlet Implementation";
   }

   @Override
   public void init(ServletConfig config) throws ServletException {
      servletConfig = config;
      properties = new LinkedHashMap();
      Enumeration e = config.getInitParameterNames();
      while (e.hasMoreElements()) {
         String name = e.nextElement().toString();
         properties.put(name, config.getInitParameter(name));
      }
      configuration = getInstance(Configuration.class);
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

   private <T> T getInstance(Class<T> type) throws ServletException {
      try {
         Object value = properties.containsKey(type) ? properties.get(type) : properties.get(type.getName());
         if (type.isInstance(value))
            return (T)value;
         boolean emptyValue = value == null || (value instanceof CharSequence && value.toString().trim().isEmpty());
         Class impl = (type.isInterface() && !emptyValue) ? Class.forName(value.toString()) : type;
         value = convert(value, impl);
         if (type.isInstance(value))
            properties.put(type, value);
         return (T)value;
      }
      catch (Exception e) {
         ServletException se = (e instanceof ServletException) ? (ServletException)e : new ServletException(e);
         throw se;
      }
   }

   private Object convert(Object src, Class type) throws Exception {
      if (type.isArray())
         return toArray(src, type.getComponentType());
      else {
         boolean emptyValue = src == null || (src instanceof CharSequence && src.toString().trim().isEmpty());
         for (Constructor c : type.getConstructors()) {
            Class[] types = c.getParameterTypes();
            switch (types.length) {
               case 0:
                  if (emptyValue)
                     return c.newInstance();
                  break;
               case 1:
                  if (types[0].isInstance(src))
                     return c.newInstance(src);
               default:
                  boolean notfound = false;
                  Object[] params = new Object[types.length];
                  for (int p = 0; p < params.length; p++) {
                     params[p] = getInstance(types[p]);
                     if (params[p] == null) {
                        notfound = true;
                        break;
                     }
                  }
                  if (!notfound)
                     return c.newInstance(params);
            }
         }
      }
      return null;
   }

   private Object toArray(Object src, Class subtype) {
      if (src instanceof Collection)
         src = ((Collection)src).toArray();
      else if (src instanceof CharSequence) {
         String txt = src.toString().trim();
         src = txt.isEmpty() ? new String[0] : txt.split(",");
      }
      else if (src == null)
         src = new Object[0];
      else if (!src.getClass().isArray())
         src = new Object[]{src};

      int length = Array.getLength(src);
      Object dst = Array.newInstance(subtype, length);
      for (int i = 0; i < length; i++) {
         try { Array.set(dst, i, convert(Array.get(src, i), subtype)); }
         catch (Exception e) {}
      }
      return dst;
   }

   private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
      ScriptContextFactory factory = configuration.getContextFactory();
      ScriptContext cx = factory.getScriptContext(new ReferenceHolder("session", request.getSession()));
      try {
         String method = request.getMethod().toLowerCase();
         String charset = request.getCharacterEncoding();
         String mimetype = request.getContentType();
         if (mimetype == null)
            mimetype = "application/json";
         Object value = getValue(configuration.getPropertyFactory(), cx, request.getPathInfo());
         response.setContentType(mimetype);
         configuration.getHandlerFactory()
                      .getHandler(mimetype)
                      .getOutputHandler(mimetype)
                      .save(response.getOutputStream(), charset, value);
      }
      finally { factory.closeScriptContext(cx); }
   }

   private Object getValue(PropertyFactory factory, ScriptContext cx, String path) {
      if (path == null)
         path = "";
      while (path.startsWith("/"))
         path = path.substring(1);
      while (path.endsWith("/"))
         path = path.substring(0, path.length()-1);
      path = path.trim();
      Object value = new PathExpression(factory, cx, path.isEmpty() ? new String[0] : path.split("/")).getValue(cx);
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      return value;
   }
}
