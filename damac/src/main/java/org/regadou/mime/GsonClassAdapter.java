package org.regadou.mime;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

public class GsonClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

   @Override
   public JsonElement serialize(Class<?> klazz, Type type, JsonSerializationContext jsc) {
      return new JsonPrimitive(klazz.getName());
   }

    @Override
    public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      try { return Class.forName(json.getAsString()); }
      catch (ClassNotFoundException e) { throw new RuntimeException(e); }
    }
}
