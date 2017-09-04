package org.regadou.factory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.ScriptContextProperty;

public class DefaultResourceManager implements ResourceManager {

   private static final char[] FILE_CHARS = "./\\".toCharArray();

   private Map<String,ResourceFactory> factories = new HashMap<>();
   private Configuration configuration;

   @Inject
   public DefaultResourceManager(Configuration configuration) {
      this.configuration = configuration;
      registerFactory(new ServerResourceFactory(this, configuration));
      registerFactory(new UrlResourceFactory(this, configuration));
      registerFactory(new FileResourceFactory(this, configuration));
      //TODO: add class name factory
      //TODO: add javascript: and other script schemes with a ScriptEngineResourceFactory
   }

   @Override
   public Reference getResource(String name) {
      if (name == null || name.trim().isEmpty())
         return null;
      int index = name.indexOf(':');
      String scheme = (index < 0) ? null : name.substring(0, index);
      if (scheme != null || canBeFile(name)) {
         ResourceFactory factory = factories.get(scheme);
         if (factory != null)
            return factory.getResource(name);
      }

      return new ScriptContextProperty(configuration.getContextFactory(), name);
   }

   @Override
   public ResourceFactory getFactory(String scheme) {
      return factories.get(scheme);
   }

   @Override
   public ResourceFactory[] getFactories() {
      Set<ResourceFactory> set = new LinkedHashSet<>(factories.values());
      return set.toArray(new ResourceFactory[set.size()]);
   }

   @Override
   public String[] getSchemes() {
      return factories.keySet().toArray(new String[factories.size()]);
   }

   @Override
   public boolean registerFactory(ResourceFactory factory) {
      Map<String,ResourceFactory> newFactories = new HashMap<>();
      for (String scheme : factory.getSchemes()) {
         if (factories.containsKey(scheme) && factory != factories.get(scheme))
            return false;
         newFactories.put(scheme, factory);
      }
      factories.putAll(newFactories);
      return true;
   }

   private boolean canBeFile(String name) {
      for (char c : FILE_CHARS) {
         if (name.indexOf(c) >= 0)
            return true;
      }
      return false;
   }
}
