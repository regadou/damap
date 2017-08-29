package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Property;

public class MapProperty implements Property {

   public static Class getMapValueType(Map map) {
      try { return map.getClass().getMethod("get", Object.class).getReturnType(); }
      catch (NoSuchMethodException|SecurityException e) { throw new RuntimeException(e); }
   }

   Map parent;
   Object key;
   Class type;

   public MapProperty(Map parent, Object key, Class type) {
      this.parent = parent;
      this.key = key;
      this.type = (type == null) ? getMapValueType(parent) : type;
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
      return type;
   }

   @Override
   public void setValue(Object value) {
      parent.put(key, value);
   }

}
