package org.regadou.factory;

import javax.script.ScriptContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.script.HttpScriptContext;

public class HttpContextFactory implements ScriptContextFactory {


   private final ThreadLocal<ScriptContext> currentContext = new ThreadLocal() {
      @Override
      protected synchronized ScriptContext initialValue() { return null; }
   };

   private Configuration configuration;

   public HttpContextFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public ScriptContext getScriptContext(Reference... properties) {
      ScriptContext cx = currentContext.get();
      if (cx != null)
         return cx;
      HttpServletRequest request = null;
      HttpServletResponse response = null;
      if (properties != null) {
         for (Reference p : properties) {
            Object value = p.getValue();
            if (value instanceof HttpServletRequest)
               request = (HttpServletRequest)value;
            else if (value instanceof HttpServletResponse)
               response = (HttpServletResponse)value;
         }
      }
      cx = new HttpScriptContext(request, response, configuration);
      currentContext.set(cx);
      return cx;
   }

   @Override
   public void setScriptContext(ScriptContext context) {
      currentContext.set(context);
   }

}
