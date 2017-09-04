package org.regadou.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;

public class ScriptContextGsonSerializer implements JsonSerializer<ScriptContext> {

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
}
