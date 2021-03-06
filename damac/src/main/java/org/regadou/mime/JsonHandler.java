package org.regadou.mime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Reference;

public class JsonHandler implements MimeHandler {

   private static final String[] MIMETYPES = new String[]{"application/json", "text/json"};
   private Gson gson;

   public JsonHandler(Configuration configuration) {
      this.gson  = new GsonBuilder().setPrettyPrinting()
                                    .setDateFormat("YYYY-MM-dd HH:mm:ss")
                                    .registerTypeAdapter(Class.class, new GsonClassAdapter())
                                    .registerTypeHierarchyAdapter(ScriptContext.class, new GsonScriptContextAdapter(configuration.getEngineManager()))
                                    .registerTypeHierarchyAdapter(Reference.class, new GsonReferenceAdapter())
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
