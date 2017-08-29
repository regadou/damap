package org.regadou.factory;

import java.util.Iterator;
import java.util.Map;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.MapProperty;

public class BeanPropertyFactory implements PropertyFactory {

   @Override
   public Property getProperty(Object value, String name) {
      if (value == null)
         return null;
      BeanMap map = new BeanMap(value);
      return map.containsKey(name) ? new MapProperty(map, name, map.getType(name)) : null;
   }

   @Override
   public String[] getProperties(Object value) {
      if (value == null)
         return new String[0];
      Map map = new BeanMap(value);
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
