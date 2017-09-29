package org.regadou.action;

import java.io.IOException;
import java.io.Writer;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;

public class OutputAction implements Action {

   private Configuration configuration;

   public OutputAction (Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
         Writer writer = configuration.getContextFactory().getScriptContext().getWriter();
         if (writer == null)
            return null;
         Converter converter = configuration.getConverter();
      try {
         for (Object param : parameters) {
            writer.write(converter.convert(param, String.class));
            writer.flush();
         }
         return null;
      } catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public String getName() {
      return "output";
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
