package org.regadou.factory;

import java.util.Map;
import java.util.TreeMap;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;
import org.regadou.reference.RdfResource;

public class RdfNamespace implements Namespace {

   private String prefix;
   private String iri;
   private ResourceManager resourceManager;
   private Map<String,Resource> resources = new TreeMap<>();

   public RdfNamespace(String prefix, String iri, ResourceManager resourceManager) {
      this.prefix = prefix;
      this.iri = iri;
      this.resourceManager = resourceManager;
      resourceManager.registerFactory(this);
   }

   @Override
   public String getIri() {
      return iri;
   }

   @Override
   public String getPrefix() {
      return prefix;
   }

   @Override
   public String[] getNames() {
      return resources.keySet().toArray(new String[resources.size()]);
   }

   @Override
   public Reference addResource(String uri) {
      String name = getLocalName(uri);
      if (name == null || name.trim().isEmpty())
         return null;
      if (resources.containsKey(name))
         return null;
      Resource r = new RdfResource(name, this, null);
      resources.put(name, r);
      return r;
   }

   @Override
   public boolean removeResource(String uri) {
      String name = getLocalName(uri);
      if (name == null || name.trim().isEmpty())
         return false;
      return resources.remove(name) != null;
   }

   @Override
   public Reference getResource(String uri) {
      String name = getLocalName(uri);
      if (name == null)
         return null;
      if (name.trim().isEmpty())
         return new GenericReference(prefix+":", this, true);
      return resources.get(name);
   }

   @Override
   public ResourceManager getResourceManager() {
      return resourceManager;
   }

   private String getLocalName(String uri) {
      int index = uri.indexOf(':');
      String prefix = (index < 0) ? "" : uri.substring(0, index);
      if (prefix.equals(this.prefix))
         return uri.substring(index+1);
      if (uri.startsWith(iri))
         return uri.substring(iri.length());
      return null;
   }
}
