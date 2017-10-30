package org.regadou.factory;

import java.util.Iterator;
import java.util.Map;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.property.MapProperty;

public class BeanPropertyFactory implements PropertyFactory {

   @Override
   public Property getProperty(Object value, String name) {
      if (value == null)
         return null;
      Map map = (value instanceof Map) ? (Map)value : new BeanMap(value);
      if (!map.containsKey(name))
         return null;
      else if (map instanceof BeanMap)
         return new MapProperty(map, name, ((BeanMap)map).getType(name));
      else
         return new MapProperty(map, name);
   }

   @Override
   public String[] getProperties(Object value) {
      if (value == null)
         return new String[0];
      Map map = (value instanceof Map) ? (Map)value : new BeanMap(value);
      String[] names = new String[map.size()];
      Iterator<String> it = map.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
         names[i] = it.next();
      return names;

   }

   @Override
   public Property addProperty(Object parent, String name, Object value) {
      return null;
   }

   @Override
   public boolean removeProperty(Object parent, String name) {
      return false;
   }
}
