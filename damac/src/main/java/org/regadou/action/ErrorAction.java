package org.regadou.action;

import java.io.IOException;
import java.io.Writer;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;

public class ErrorAction implements Action {

   private Configuration configuration;

   public ErrorAction (Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
         Writer error = configuration.getContextFactory().getScriptContext().getErrorWriter();
         if (error == null)
            return null;
         Converter converter = configuration.getConverter();
      try {
         for (Object param : parameters) {
            error.write(converter.convert(param, String.class));
            error.flush();
         }
         return null;
      } catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public String getName() {
      return "error";
   }

   @Override
   public Class getReturnType() {
      return Void.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return null;
   }

}
