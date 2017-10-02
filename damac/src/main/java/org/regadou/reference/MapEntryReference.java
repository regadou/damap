package org.regadou.reference;

import java.util.Map;

public class MapEntryReference<T> extends TypedReference<T> {

   private Map.Entry<Object,T> entry;
   private String name;

   public MapEntryReference(Map.Entry<Object,T> entry) {
      super(entry.getClass(), "getValue");
      this.entry = entry;
      Object key = entry.getKey();
      name = (key == null) ? null : key.toString();
   }

   @Override
   public String getId() {
      return name;
   }

   @Override
   public T getValue() {
      return entry.getValue();
   }

   @Override
   public void setValue(T value) {
      entry.setValue(value);
   }
}
