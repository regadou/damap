package org.regadou.factory;

import java.util.Iterator;
import java.util.Map;
import org.regadou.damai.Converter;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.reference.MapProperty;

public class MapPropertyFactory implements PropertyFactory<Map> {

   private Converter converter;

   public MapPropertyFactory(Converter converter) {
      this.converter = converter;
   }

   @Override
   public Property getProperty(Map map, String name) {
      return map.containsKey(name) ? new MapProperty(map, name) : null;
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
            if (name == null) {
               if (value instanceof Reference) {
                  Reference ref = (Reference)value;
                  name = ref.getName();
                  if (name == null)
                     ref.getType().getName();
               }
               else
                  name = (value == null) ? "value" : value.getClass().getName();
            }
            Property p = new MapProperty(map, name);
            p.setValue(converter.convert(value, p.getType()));
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
