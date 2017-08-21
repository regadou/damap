package org.regadou.factory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.script.ScriptContext;
import org.regadou.util.ClassIterator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;

public class DefaultPropertyManager implements PropertyManager {

   private Map<Class,PropertyFactory> factories = new LinkedHashMap<>();
   private PropertyFactory arrayFactory = new ArrayPropertyFactory();
   private PropertyFactory beanFactory = new BeanPropertyFactory();
   private PropertyFactory genericFactory = new GenericPropertyFactory(this);

   @Inject
   public DefaultPropertyManager() {
      factories.put(Map.class, new MapPropertyFactory());
      factories.put(Collection.class, new CollectionPropertyFactory());
      factories.put(ScriptContext.class, new ScriptContextPropertyFactory());
   }

   @Override
   public Property getProperty(Object value, String name) {
      Class type = (value == null) ? Void.class : value.getClass();
      PropertyFactory factory = getPropertyFactory(type);
      if (factory != null) {
         Property p = factory.getProperty(value, name);
         if (p != null)
            return p;
      }
      return genericFactory.getProperty(value, name);
   }

   @Override
   public <T> PropertyFactory<T> getPropertyFactory(Class<T> type) {
      ClassIterator it = new ClassIterator(type);
      while (it.hasNext()) {
         Class<T> c = (Class<T>)it.next();
         PropertyFactory<T> factory = factories.get(c);
         if (factory != null)
            return factory;
      }
      return type.isArray() ? arrayFactory : beanFactory;
   }

   @Override
   public <T> void registerPropertyFactory(Class<T> type, PropertyFactory<T> factory) {
      factories.put(type, factory);
   }
}
