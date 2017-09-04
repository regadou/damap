package org.regadou.mime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.script.ScriptContext;
import org.regadou.damai.MimeHandler;
import org.regadou.script.DefaultScriptContext;
import org.regadou.script.HttpScriptContext;
import org.regadou.script.PropertiesScriptContext;
import org.regadou.script.ScriptContextGsonSerializer;

public class JsonHandler implements MimeHandler {

   private static final String[] MIMETYPES = new String[]{"application/json", "text/json"};
   private Gson gson;

   public JsonHandler() {
      ScriptContextGsonSerializer serializer = new ScriptContextGsonSerializer();
      this.gson  = new GsonBuilder().setPrettyPrinting()
                                    .setDateFormat("YYYY-MM-dd HH:mm:ss")
                                    .registerTypeAdapter(HttpScriptContext.class, serializer)
                                    .registerTypeAdapter(DefaultScriptContext.class, serializer)
                                    .registerTypeAdapter(PropertiesScriptContext.class, serializer)
                                    .registerTypeAdapter(ScriptContext.class, serializer)
                                    .create();
   }

   @Override
   public String[] getMimetypes() {
      return MIMETYPES;
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      return gson.fromJson(new InputStreamReader(input, charset), Object.class);
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      output.write(gson.toJson(value).getBytes(charset));
      output.flush();
   }
}
