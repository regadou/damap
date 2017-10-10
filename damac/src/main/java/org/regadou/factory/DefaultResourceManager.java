package org.regadou.factory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import org.regadou.damai.Configuration;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.property.ScriptContextProperty;
import org.regadou.repository.RdfRepository;
import org.regadou.resource.DefaultNamespace;
import org.regadou.resource.GenericResource;

public class DefaultResourceManager implements ResourceManager {

   private static final String LOCALHOST = "http://localhost/";

   private Configuration configuration;
   private ResourceFactory nullSchemeFactory;
   private Map<String,ResourceFactory> factories = new TreeMap<>();
   private Map<String,Namespace> namespaces = new TreeMap<>();

   @Inject
   public DefaultResourceManager(Configuration configuration) {
      this.configuration = configuration;
      registerFactory(new NoSchemeResourceFactory(this, configuration));
      registerFactory(new UrlResourceFactory(this, configuration));
      registerFactory(new ServerResourceFactory(this, configuration));
      Repository repo = new RdfRepository(configuration, this);
      registerNamespace(new DefaultNamespace("_", LOCALHOST, repo));
      //TODO: add other script schemes with a ScriptEngineResourceFactory
   }

   @Override
   public Reference getResource(String id) {
      if (id == null || id.trim().isEmpty())
         return null;
      Namespace ns = namespaces.get(id);
      if (ns != null)
         return new GenericResource(ns.getPrefix()+":", ns, true, configuration);

      int index = id.indexOf(':');
      if (index < 0) {
         if (nullSchemeFactory != null) {
            Reference r = nullSchemeFactory.getResource(id);
            if (r != null)
               return r;
         }
      }
      else {
         ResourceFactory factory = factories.get(id.substring(0, index));
         if (factory != null)
            return factory.getResource(id);
      }

      return new ScriptContextProperty(configuration.getContextFactory(), id);
   }

   @Override
   public ResourceFactory getFactory(String scheme) {
      return (scheme == null) ? nullSchemeFactory : factories.get(scheme);
   }

   @Override
   public ResourceFactory[] getFactories() {
      Set<ResourceFactory> set = new LinkedHashSet<>(factories.values());
      if (nullSchemeFactory != null)
         set.add(nullSchemeFactory);
      return set.toArray(new ResourceFactory[set.size()]);
   }

   @Override
   public String[] getSchemes() {
      return factories.keySet().toArray(new String[factories.size()]);
   }

   @Override
   public boolean registerFactory(ResourceFactory factory) {
      Map<String,ResourceFactory> newFactories = new TreeMap<>();
      boolean nullFactory = false;
      for (String scheme : factory.getSchemes()) {
         if (scheme == null) {
            if (nullSchemeFactory != null && factory != nullSchemeFactory)
               return false;
            nullFactory = true;
            continue;
         }
         if (factories.containsKey(scheme) && factory != factories.get(scheme))
            return false;
         newFactories.put(scheme, factory);
      }
      factories.putAll(newFactories);
      if (nullFactory)
         nullSchemeFactory = factory;
      return nullFactory || !newFactories.isEmpty();
   }

   @Override
   public boolean registerNamespace(Namespace namespace) {
      String iri = namespace.getUri();
      if (namespaces.containsKey(iri))
         return false;
      if (!registerFactory(new NamespaceResourceFactory(namespace, this)))
         return false;
      namespaces.put(iri, namespace);
      return true;
   }
}
