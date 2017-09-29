package org.regadou.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;

public class InputAction implements Action {

   private Configuration configuration;

   public InputAction (Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
      ScriptContext cx = configuration.getContextFactory().getScriptContext();
      Reader reader = cx.getReader();
      if (reader == null)
         return null;
      try {
         switch (parameters.length) {
            case 0:
               return readLine(reader, cx);
            case 1:
               Object value = parameters[0];
               while (value instanceof Reference)
                  value = ((Reference)value).getValue();
               if (value == null)
                  return readLine(reader, cx);
               return readSize(reader, value);
            default:
               List buffers = new ArrayList();
               for (Object param : parameters)
                  buffers.add(readSize(reader, param));
               return buffers;
         }
      }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public String getName() {
      return "input";
   }

   @Override
   public Class getReturnType() {
      return Object.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return null;
   }

   private String readSize(Reader reader, Object value) throws IOException {
      Number n = configuration.getConverter().convert(value, Number.class);
      if (n == null)
         n = 0;
      char[] buffer = new char[n.intValue()];
      int got = reader.read(buffer);
      return new String(buffer, 0, got);
   }

   private String readLine(Reader reader, ScriptContext cx) throws IOException {
      BufferedReader br;
      if (reader instanceof BufferedReader)
         br = (BufferedReader)reader;
      else {
         br = new BufferedReader(reader);
         cx.setReader(br);
      }
      return br.readLine();
   }
}
