package org.regadou.factory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.collection.ClassIterator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.repository.RepositoryItem;

public class DefaultPropertyManager implements PropertyManager {

   private Map<Class,PropertyFactory> factories = new LinkedHashMap<>();
   private PropertyFactory arrayFactory = new ArrayPropertyFactory();
   private PropertyFactory beanFactory = new BeanPropertyFactory();
   private PropertyFactory genericFactory = new GenericPropertyFactory(this);

   @Inject
   public DefaultPropertyManager(Configuration configuration) {
      factories.put(Map.class, new MapPropertyFactory(configuration.getConverter()));
      factories.put(Collection.class, new CollectionPropertyFactory(configuration.getConverter()));
      factories.put(Object[].class, arrayFactory);
      factories.put(ScriptContext.class, new ScriptContextPropertyFactory());
      factories.put(Repository.class, new RepositoryPropertyFactory());
      factories.put(RepositoryItem.class, new RepositoryItemPropertyFactory(configuration));
      factories.put(File.class, new FilePropertyFactory(configuration.getTypeMap()));
      factories.put(Resource.class, new ResourcePropertyFactory(configuration));
   }

   @Override
   public Property getProperty(Object value, String name) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
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
   public <T> boolean registerPropertyFactory(Class<T> type, PropertyFactory<T> factory) {
      if (factories.containsKey(type))
         return false;
      factories.put(type, factory);
      return true;
   }
}
