package org.regadou.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class DefaultResourceManager implements ResourceManager {

   private Map<String,ResourceFactory> factories = new HashMap<>();

   @Override
   public ResourceFactory getFactory(String scheme) {
      return factories.get(scheme);
   }

   @Override
   public ResourceFactory[] getFactories() {
      return factories.values().toArray(new ResourceFactory[factories.size()]);
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
