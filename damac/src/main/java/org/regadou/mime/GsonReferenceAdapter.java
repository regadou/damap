package org.regadou.mime;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class GsonReferenceAdapter implements JsonSerializer<Reference>, JsonDeserializer<Reference> {

   @Override
   public JsonElement serialize(Reference ref, Type type, JsonSerializationContext cx) {
      return cx.serialize(ref.getValue());
   }

    @Override
    public Reference deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext cx) throws JsonParseException {
       return new GenericReference(null, cx.deserialize(json, typeOfT), true);
    }
}
