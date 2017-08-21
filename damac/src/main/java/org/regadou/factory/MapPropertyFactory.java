package org.regadou.factory;

import java.util.Iterator;
import java.util.Map;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.reference.MapProperty;

public class MapPropertyFactory implements PropertyFactory<Map> {

   @Override
   public Property getProperty(Map value, String name) {
      return value.containsKey(name) ? new MapProperty(value, name) : null;
   }

   @Override
   public String[] getProperties(Map value) {
      String[] names = new String[value.size()];
      Iterator<String> it = value.keySet().iterator();
      for (int i = 0; it.hasNext(); i++)
         names[i] = it.next();
      return names;
   }
}
