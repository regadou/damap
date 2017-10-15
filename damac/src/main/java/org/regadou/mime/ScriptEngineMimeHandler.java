package org.regadou.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.inject.Inject;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Printable;
import org.regadou.system.StringInput;

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
   public Object load(InputStream input, String charset) throws IOException {
      try {
         ScriptContext cx = configuration.getContextFactory().getScriptContext();
         return scriptEngine.eval(new StringInput(input, charset).toString(), cx);
      }
      catch (ScriptException e) { throw new RuntimeException(e); }
   }

   @Override
   public void save (OutputStream output, String charset, Object value) throws IOException {
      if (scriptEngine instanceof Printable)
         output.write(((Printable)scriptEngine).print(value).getBytes(charset));
      else if (scriptEngine.getFactory().getMimeTypes().contains("application/javascript"))
         configuration.getHandlerFactory().getHandler("application/json")
                                          .save(output, charset, value);
      else
         output.write(configuration.getConverter().convert(value, String.class).getBytes(charset));
   }
}
