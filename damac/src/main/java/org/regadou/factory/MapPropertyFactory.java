package org.regadou.factory;

import java.util.Iterator;
import java.util.Map;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.MapProperty;

public class MapPropertyFactory implements PropertyFactory<Map> {

   @Override
   public Property getProperty(Map map, String name) {
      return map.containsKey(name) ? new MapProperty(map, name, null) : null;
   }

   @Override
   public String[] getProperties(Map map) {
      String[] names = new String[map.size()];
      Iterator<String> it = map.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
         names[i] = it.next();
      return names;
   }

   @Override
   public Property addProperty(Map map, String name, Object value) {
      if (!map.containsKey(name)) {
         try {
            Property p = new MapProperty(map, name, null);
            p.setValue(value);
            return p;
         }
         catch (Exception e) {}
      }
      return null;
   }

   @Override
   public boolean removeProperty(Map map, String name) {
      if (map.containsKey(name)) {
         try {
            map.remove(name);
            return !map.containsKey(name);
         }
         catch (Exception e) {}
      }
      return false;
   }
}
