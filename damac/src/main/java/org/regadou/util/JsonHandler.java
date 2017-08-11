package org.regadou.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerInput;
import org.regadou.damai.MimeHandlerOutput;

public class JsonHandler implements MimeHandler {

   private static final String[] MIMETYPES = new String[]{"application/json", "text/json"};
   private Configuration configuration;
   private Gson gson;

   public JsonHandler(Configuration configuration) {
      this.configuration = configuration;
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
         Writer writer = new OutputStreamWriter(output, charset);
         writer.write(gson.toJson(value));
         writer.close();
      };
   }
}
