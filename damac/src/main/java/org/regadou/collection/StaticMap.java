package org.regadou.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class StaticMap extends LinkedHashMap {

   public StaticMap(Object ... items) {
      super();
      Object key = null;
      boolean iskey = true;
      for (Object item : items) {
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

   public StaticMap(Object[] keys, Object[] values) {
      super();
      if (keys.length != values.length)
         throw new RuntimeException("Found "+keys.length+" keys but "+values.length+" values");
      for (int i = 0; i < keys.length; i++)
         put(keys[i], values[i]);
   }

   public StaticMap(Object[][] entries) {
      super();
      for (Object[] entry : entries) {
         if (entry == null)
            continue;
         switch (entry.length) {
            case 0:
               break;
            case 1:
               put(entry[0], null);
               break;
            case 2:
               put(entry[0], entry[1]);
               break;
            default:
               Object key = entry[0];
               List list = new ArrayList(Arrays.asList(entry));
               list.remove(0);
               put(key, list.toArray());
         }
      }
   }
}
