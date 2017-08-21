package org.regadou.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStreamReader;
import javax.script.ScriptContext;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerInput;
import org.regadou.damai.MimeHandlerOutput;

public class JsonHandler implements MimeHandler {

   private static final String[] MIMETYPES = new String[]{"application/json", "text/json"};
   private Gson gson;

   public JsonHandler() {
      this.gson  = new GsonBuilder().setPrettyPrinting()
                                    .setDateFormat("YYYY-MM-dd HH:mm:ss")
                                    .registerTypeAdapter(ScriptContext.class, new ScriptContextGsonSerializer())
                                    .create();
   }

   @Override
   public String[] getMimetypes() {
      return MIMETYPES;
   }

   @Override
   public MimeHandlerInput getInputHandler(String mimetype) {
      return (input, charset) -> gson.fromJson(new InputStreamReader(input, charset), Object.class);
   }

   @Override
   public MimeHandlerOutput getOutputHandler(String mimetype) {
      return (output, charset, value) -> {
         output.write(gson.toJson(value).getBytes(charset));
         output.flush();
      };
   }
}
