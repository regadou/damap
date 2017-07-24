package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Reference;

public class MapEntryWrapper implements  Reference {

   private Map.Entry entry;
   private String name;

   public MapEntryWrapper(Map.Entry entry) {
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

   @Override
   public Class getType() {
      Object value = entry.getValue();
      return (value == null) ? Void.class : value.getClass();
   }

}
