package org.regadou.system;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.regadou.damai.InstanceFactory;
import org.regadou.factory.DefaultInstanceFactory;
import org.regadou.reference.ReferenceHolder;

public class RestServlet implements Servlet {

   private static final String CONTEXT_PARAM = "org.regadou.system.Context";

   private ServletConfig servletConfig;
   private InstanceFactory instanceFactory;

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
      //TODO: look at parameter names if we have something to do
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

   public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession();
      Context cx = (Context)session.getAttribute(CONTEXT_PARAM);
      if (cx == null) {
         if (instanceFactory == null) {
            Injector injector = Guice.createInjector(new GuiceModule());
            instanceFactory = injector.getInstance(InstanceFactory.class);
            ((DefaultInstanceFactory)instanceFactory).setInjector(injector);
         }
         cx = Context.currentContext(true, new ReferenceHolder("instanceFactory", instanceFactory)
                                         , new ReferenceHolder("error", System.err));
         session.setAttribute(CONTEXT_PARAM, cx);
      }
      String method = request.getMethod().toLowerCase();
      String[] path = getPathParts(request.getPathInfo());
      response.setContentType("text/plain");
      response.getOutputStream().write(eval(method, path).getBytes());
   }

   private String[] getPathParts(String path) {
      if (path == null)
         return new String[0];
      while (path.startsWith("/"))
         path = path.substring(1);
      while (path.endsWith("/"))
         path = path.substring(0, path.length()-1);
      path = path.trim();
      return path.isEmpty() ? new String[0] : path.split("/");
   }

   private String eval(String method, String[] path) {
      //TODO: we need to do some real stuff here
      return method + " " + Arrays.asList(path);
   }
}
