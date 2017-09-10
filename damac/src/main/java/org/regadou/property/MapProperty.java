package org.regadou.property;

import java.util.Map;

public class MapProperty extends TypedProperty<Map> {

   Object key;

   public MapProperty(Map parent, Object key) {
      super(parent, Map.class, "get", Object.class);
      this.key = key;
   }

   public MapProperty(Map parent, Object key, Class type) {
      super(parent, Map.class, type);
      this.key = key;
   }

   @Override
   public String getId() {
      return String.valueOf(key);
   }

   @Override
   public Object getValue() {
      return getParent().get(key);
   }

   @Override
   public void setValue(Object value) {
      getParent().put(key, value);
   }

}
