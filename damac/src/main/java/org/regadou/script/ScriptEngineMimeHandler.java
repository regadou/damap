package org.regadou.script;

import com.google.gson.Gson;
import java.util.List;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerInput;
import org.regadou.damai.MimeHandlerOutput;
import org.regadou.damai.Printable;
import org.regadou.util.StringInput;

public class ScriptEngineMimeHandler implements MimeHandler {

   private ScriptEngine scriptEngine;
   private Configuration configuration;
   private Gson gson;

   @Inject
   public ScriptEngineMimeHandler(ScriptEngine scriptEngine, Configuration configuration, Gson gson) {
      this.scriptEngine = scriptEngine;
      this.configuration = configuration;
      this.gson = gson;
   }

   @Override
   public String[] getMimetypes() {
      List<String> mimetypes = scriptEngine.getFactory().getMimeTypes();
      return mimetypes.toArray(new String[mimetypes.size()]);
   }

   @Override
   public MimeHandlerInput getInputHandler(String mimetype) {
      return (input, charset) -> {
         try { return scriptEngine.eval(new StringInput(input, charset).toString()); }
         catch (ScriptException e) { throw new RuntimeException(e); }
      };
   }

   @Override
   public MimeHandlerOutput getOutputHandler(String mimetype) {
      return (output, charset, value) -> {
         String txt;
         if (scriptEngine instanceof Printable)
            txt = ((Printable)scriptEngine).print(value);
         else if (scriptEngine.getFactory().getMimeTypes().contains("application/javascript"))
            txt = gson.toJson(value);
         else
            txt = configuration.getConverterManager().getConverter(Object.class, String.class).convert(value);
         output.write(txt.getBytes(charset));
      };
   }

}
