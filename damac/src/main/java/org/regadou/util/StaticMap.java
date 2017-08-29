package org.regadou.util;

import java.util.LinkedHashMap;

public class StaticMap<T> extends LinkedHashMap<T,T> {

   public StaticMap(T ... items) {
      super();
      T key = null;
      boolean iskey = true;
      for (T item : items) {
         if (iskey) {
            key = item;
            iskey = false;
         }
         else {
            put(key, item);
            iskey = true;
         }
      }
   }
}
