package org.regadou.factory;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import org.regadou.reference.MapProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.util.ClassIterator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.CollectionProperty;
import org.regadou.reference.ReadOnlyProperty;
import org.regadou.util.MultiMap;

public class DefaultPropertyFactory implements PropertyFactory {

   private static final String TYPE_PROPERTY_NAME = "type";
   private static final List<Class> SIMPLE_TYPES = Arrays.asList(
      Void.class, Number.class, Boolean.class, Action.class,
      CharSequence.class, Property.class, Class.class
   );

   private Map<Class,Action<Map<String, Property>>> functions = new LinkedHashMap<>();

   private Action<Map<String, Property>> mapFunction = value -> {
      Object v0 = value[0];
      Map src = (v0 instanceof Map) ? (Map)v0 : new BeanMap(v0);
      Map<String, Property> map = new LinkedHashMap<>(setType(v0, Map.class));
      for (Object key : src.keySet())
         map.put(String.valueOf(key), new MapProperty(src, key));
      return map;
   };

   private Action<Map<String, Property>> collectionFunction = value -> {
      Object v0 = value[0];
      Collection src = (v0 instanceof Collection) ? (Collection)v0 : Arrays.asList(v0);
      int length = src.size();
      String name = CollectionProperty.lengthNames.get(0);
      Map<String, Property> map = new LinkedHashMap<>(setType(v0, Collection.class));
      map.put(name, new CollectionProperty(src, name));
      for (int i = 0; i < length; i++)
         map.put(String.valueOf(i), new CollectionProperty(src, i));
      return map;
   };

   private Action<Map<String, Property>> arrayFunction = value -> {
      return collectionFunction.execute(Arrays.asList((Object[])value[0]));
   };

   private Action<Map<String, Property>> referenceFunction = value -> {
      return getProperties(((Reference)value[0]).getValue());
   };

   private Action<Map<String, Property>> contextFunction = value -> {
      return getProperties(new MultiMap((ScriptContext)value[0]));
   };

   private Configuration configuration;

   @Inject
   public DefaultPropertyFactory(Configuration configuration) {
      this.configuration = configuration;
      functions.put(Collection.class, collectionFunction);
      functions.put(Object[].class, arrayFunction);
      functions.put(Reference.class, referenceFunction);
      functions.put(ScriptContext.class, contextFunction);
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
         if (SIMPLE_TYPES.contains(c))
            return Collections.EMPTY_MAP;
      }

      return mapFunction.execute(value);
   }

   @Override
   public void setProperties(Class type, Action<Map<String, Property>> function) {
      functions.put(type, function);
   }

   private Map<String, Property> setType(Object value, Class type) {
      return Collections.singletonMap(TYPE_PROPERTY_NAME, new ReadOnlyProperty(value, TYPE_PROPERTY_NAME, type));
   }
}
