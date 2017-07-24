package org.regadou.factory;

import org.regadou.reference.MapProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.util.ClassIterator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Action;

public class DefaultPropertyFactory implements PropertyFactory {

   private Map<Class,Action<Map<String, Property>>> functions = new LinkedHashMap<>();
   private Action<Map<String, Property>> mapFunction = value -> {
      Map<String, Property> map = new LinkedHashMap<>();
      Object v0 = value[0];
      Map src = (v0 instanceof Map) ? (Map)v0 : new BeanMap(v0);
      for (Object key : src.keySet())
         map.put(String.valueOf(key), new MapProperty(src, key));
      return map;
   };

   public DefaultPropertyFactory() {
      //TODO: init known types: number, charseq, map, bean, collection, array, property, reference, class
   }

   @Override
   public Map<String, Property> getProperties(Object value) {
      if (value == null)
         return Collections.EMPTY_MAP;
      ClassIterator i = new ClassIterator(value);
      while (i.hasNext()) {
         Class c = i.next();
         Action<Map<String, Property>> f = functions.get(c);
         if (f != null)
            return f.execute(value);
      }
      return mapFunction.execute(value);
   }

   @Override
   public void setProperties(Class type, Action<Map<String, Property>> function) {
      functions.put(type, function);
   }

}
