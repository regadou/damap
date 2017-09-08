package org.regadou.factory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.regadou.damai.Configuration;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;
import org.regadou.property.ScriptContextProperty;
import org.regadou.repository.RdfRepository;
import org.regadou.resource.DefaultNamespace;

public class DefaultResourceManager implements ResourceManager {

   private static final char[] FILE_CHARS = "./\\".toCharArray();
   private static final String LOCALHOST = "http://localhost/";

   private Map<String,ResourceFactory> factories = new HashMap<>();
   private Map<String,Reference> namespaces = new HashMap<>();
   private Configuration configuration;

   @Inject
   public DefaultResourceManager(Configuration configuration) {
      this.configuration = configuration;
      registerFactory(new ServerResourceFactory(this, configuration));
      registerFactory(new UrlResourceFactory(this, configuration));
      registerFactory(new FileResourceFactory(this, configuration));
      loadRdfSchema();
      //TODO: add class name factory
      //TODO: add javascript: and other script schemes with a ScriptEngineResourceFactory
   }

   @Override
   public Reference getResource(String name) {
      if (name == null || name.trim().isEmpty())
         return null;
      Reference r = namespaces.get(name);
      if (r != null)
         return r;
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

   @Override
   public boolean registerNamespace(Namespace namespace) {
      String iri = namespace.getIri();
      if (namespaces.containsKey(iri))
         return false;
      if (!registerFactory(new NamespaceResourceFactory(namespace, this)))
         return false;
      namespaces.put(iri, new GenericReference(namespace.getPrefix()+":", namespace, true));
      return true;
   }

   private boolean canBeFile(String name) {
      for (char c : FILE_CHARS) {
         if (name.indexOf(c) >= 0)
            return true;
      }
      return false;
   }

   private void loadRdfSchema() {
      Namespace ns = new DefaultNamespace("_", LOCALHOST, new RdfRepository(this));
      registerFactory(new NamespaceResourceFactory(ns, this));
      URL mimetypes = getClass().getResource("/mimetypes");
      for (File file : new File(mimetypes.getPath()).getParentFile().listFiles()) {
         //TODO: how to load files that gonna request resource manager that is not yet set ?
      }
   }
}
