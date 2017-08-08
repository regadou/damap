package org.regadou.factory;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.MapProperty;
import org.regadou.util.MapAdapter;

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

      return getContextProperty(name);
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

   private boolean canBeFile(String name) {
      for (char c : FILE_CHARS) {
         if (name.indexOf(c) >= 0)
            return true;
      }
      return false;
   }

   private Property getContextProperty(String name) {
      ScriptContext cx = configuration.getContextFactory().getScriptContext();
      int scope = cx.getAttributesScope(name);
      if (scope < 0)
         scope = ScriptContext.ENGINE_SCOPE;
      final int s = scope;
      Map<String,Object> map = new MapAdapter<>(
          key -> cx.getAttribute(key, s),
          (key, value) -> cx.setAttribute(key, value, s)
      );
      return new MapProperty(map, name);
   }
}
