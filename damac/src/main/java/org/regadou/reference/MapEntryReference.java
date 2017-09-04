package org.regadou.reference;

import java.util.Map;

public class MapEntryReference extends TypedReference {

   private Map.Entry entry;
   private String name;

   public MapEntryReference(Map.Entry entry) {
      super(entry.getClass(), "getValue");
      this.entry = entry;
      Object key = entry.getKey();
      name = (key == null) ? null : key.toString();
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      return entry.getValue();
   }

   @Override
   public void setValue(Object value) {
      entry.setValue(value);
   }
}
