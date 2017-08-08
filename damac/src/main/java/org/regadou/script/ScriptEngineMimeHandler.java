package org.regadou.script;

import java.util.List;
import javax.inject.Inject;
import javax.script.ScriptContext;
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

   @Inject
   public ScriptEngineMimeHandler(ScriptEngine scriptEngine, Configuration configuration) {
      this.scriptEngine = scriptEngine;
      this.configuration = configuration;
   }

   @Override
   public String[] getMimetypes() {
      List<String> mimetypes = scriptEngine.getFactory().getMimeTypes();
      return mimetypes.toArray(new String[mimetypes.size()]);
   }

   @Override
   public MimeHandlerInput getInputHandler(String mimetype) {
      return (input, charset) -> {
         try {
            ScriptContext cx = configuration.getContextFactory().getScriptContext();
            return scriptEngine.eval(new StringInput(input, charset).toString(), cx);
         }
         catch (ScriptException e) { throw new RuntimeException(e); }
      };
   }

   @Override
   public MimeHandlerOutput getOutputHandler(String mimetype) {
      if (scriptEngine instanceof Printable)
         return (output, charset, value) ->  output.write(((Printable)scriptEngine).print(value).getBytes(charset));
      if (scriptEngine.getFactory().getMimeTypes().contains("application/javascript"))
         return configuration.getHandlerFactory().getHandler("application/json")
                                                 .getOutputHandler("application/json");
      return (output, charset, value) -> output.write(configuration.getConverter().convert(value, String.class).getBytes(charset));
   }
}
