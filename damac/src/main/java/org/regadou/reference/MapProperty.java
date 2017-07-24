package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Property;

public class MapProperty implements Property {

   Map parent;
   Object key;

   public MapProperty(Map parent, Object key) {
      this.parent = parent;
      this.key = key;
   }

   @Override
   public Object getParent() {
      return parent;
   }

   @Override
   public Class getParentType() {
      return Map.class;
   }

   @Override
   public String getName() {
      return String.valueOf(key);
   }

   @Override
   public Object getValue() {
      return parent.get(key);
   }

   @Override
   public Class getType() {
      Object value = parent.get(key);
      return (value == null) ? Void.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      parent.put(key, value);
   }

}
