package org.regadou.factory;

import java.io.PrintWriter;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;

public class HttpContextFactory implements ScriptContextFactory {

   private static final String CONTEXT_PARAM = ScriptContext.class.getName();

   private final ThreadLocal<ScriptContext> currentContext = new ThreadLocal() {
      @Override
      protected synchronized ScriptContext initialValue() { return null; }
   };

   @Override
   public ScriptContext getScriptContext(Reference... properties) {
      HttpSession session = null;
      if (properties != null) {
         for (Reference p : properties) {
            Object value = p.getValue();
            if (value instanceof HttpSession)
               session = (HttpSession)value;
            else if (value instanceof HttpServletRequest)
               session = ((HttpServletRequest)value).getSession();
         }
         if (session != null) {
            ScriptContext cx = (ScriptContext)session.getAttribute(CONTEXT_PARAM);
            if (cx != null) {
               currentContext.set(cx);
               return cx;
            }
         }
      }
      ScriptContext cx = currentContext.get();
      if (cx == null) {
         cx = new SimpleScriptContext();
         cx.setErrorWriter(new PrintWriter(System.err));
         currentContext.set(cx);
      }
      if (session != null)
         session.setAttribute(CONTEXT_PARAM, cx);
      return cx;
   }

   @Override
   public boolean closeScriptContext(ScriptContext context) {
      if (currentContext.get() == context) {
         currentContext.set(null);
         return true;
      }
      return false;
   }

}
