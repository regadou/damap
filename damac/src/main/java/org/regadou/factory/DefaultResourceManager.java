package org.regadou.factory;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.regadou.damai.Configuration;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class DefaultResourceManager implements ResourceManager {

   private Map<String,ResourceFactory> factories = new HashMap<>();
   private Configuration configuration;

   @Inject
   public DefaultResourceManager(Configuration configuration) {
      this.configuration = configuration;
      registerFactory(new ServerResourceFactory(this, configuration));
      registerFactory(new UrlResourceFactory(this, configuration));
      registerFactory(new FileResourceFactory(this, configuration));
      //TODO: add javascript: and other script schemes with a ScriptEngineResourceFactory
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
   public void registerFactory(ResourceFactory factory) {
      for (String scheme : factory.getSchemes())
         factories.put(scheme, factory);
   }
}
