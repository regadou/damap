package org.regadou.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class PersistableMap<K,V> extends LinkedHashMap<K,V> {

   private Predicate<Map> updateFunction;
   private boolean modified;

   public PersistableMap(Predicate<Map> updateFunction) {
      super();
      this.updateFunction = updateFunction;
   }

   public PersistableMap(Map<K,V> map, Predicate<Map> function) {
      super(map);
      this.updateFunction = function;
   }

   @Override
   public V put(K key, V value) {
      V old = super.put(key, value);
      modified = true;
      return old;
   }

   @Override
   public V remove(Object key) {
      V old = super.remove(key);
      modified = true;
      return old;
   }

   @Override
   public void clear() {
      super.clear();
      modified = true;
   }

   public boolean isModified() {
      return modified;
   }

   public boolean update() {
      if (!modified)
         return false;
      boolean success = updateFunction.test(this);
      if (success)
         modified = false;
      return success;
   }

   public Predicate<Map> getUpdateFunction() {
      return updateFunction;
   }
}
