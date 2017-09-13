package org.regadou.mime;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import org.regadou.script.DefaultScriptContext;

public class GsonScriptContextAdapter implements JsonSerializer<ScriptContext>, JsonDeserializer<ScriptContext> {

   private ScriptEngineManager manager;

   public GsonScriptContextAdapter(ScriptEngineManager manager) {
      this.manager = manager;
   }

   @Override
   public JsonElement serialize(ScriptContext cx, Type type, JsonSerializationContext jsc) {
      Set<String> set = new TreeSet<>();
      for (Integer scope : cx.getScopes()) {
         Bindings bindings = cx.getBindings(scope);
         if (bindings != null) {
            for (String key : bindings.keySet())
               set.add(key);
         }
      }
      JsonArray attributes = new JsonArray();
      for (String e : set)
         attributes.add(e);
      return attributes;
   }

   @Override
   public ScriptContext deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      ScriptContext cx = new DefaultScriptContext(context.deserialize(json, typeOfT));
      if (!cx.getScopes().contains(ScriptContext.GLOBAL_SCOPE))
          cx.setBindings(manager.getBindings(), ScriptContext.GLOBAL_SCOPE);
      return cx;
   }
}
