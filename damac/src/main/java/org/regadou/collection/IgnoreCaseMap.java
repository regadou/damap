package org.regadou.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class IgnoreCaseMap<T> extends LinkedHashMap<String,T> {

   private Function<Object,String> normalizeFunction;

   public IgnoreCaseMap() {
      this(null, null);
   }

   public IgnoreCaseMap(Map<String,T> src) {
      this(src, null);
   }

   public IgnoreCaseMap(Function<Object,String> normalizeFunction) {
      this(null, normalizeFunction);
   }

   public IgnoreCaseMap(Map<String,T> src, Function<Object,String> normalizeFunction) {
      super();
      this.normalizeFunction = (normalizeFunction == null) ? IgnoreCaseMap::normalizeKey : normalizeFunction;
      if (src != null) {
         for (Object key : src.keySet())
            put(normalizeFunction.apply(key), src.get(key));
      }
   }

   @Override
   public T get(Object key) {
      return super.get(normalizeFunction.apply(key));
   }

   @Override
   public T put(String key, T value) {
      return super.put(normalizeFunction.apply(key), value);
   }

   @Override
   public boolean containsKey(Object key) {
      return super.containsKey(normalizeFunction.apply(key));
   }

   @Override
   public T remove(Object key) {
      return super.remove(normalizeFunction.apply(key));
   }

   private static String normalizeKey(Object key) {
      return (key == null) ? "" : key.toString().trim().toLowerCase();
   }
}
