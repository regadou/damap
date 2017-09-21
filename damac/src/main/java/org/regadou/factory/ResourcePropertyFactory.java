package org.regadou.factory;

import org.regadou.damai.Configuration;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.property.GenericProperty;
import org.regadou.property.ResourceProperty;
import org.regadou.resource.DefaultResource;

public class ResourcePropertyFactory implements PropertyFactory<Resource> {

   public static final String ID_PROPERTY = "@id";

   private Configuration configuration;

   public ResourcePropertyFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public Property getProperty(Resource resource, String name) {
      if (ID_PROPERTY.equals(name))
         return new GenericProperty(resource, resource.getId(), true);
      if (hasResource(resource, name))
         return new ResourceProperty(resource, getResource(name));
      return null;
   }

   @Override
   public String[] getProperties(Resource resource) {
      return resource.getProperties();
   }

   @Override
   public Property addProperty(Resource resource, String name, Object value) {
      if (ID_PROPERTY.equals(name) || hasResource(resource, name))
         return null;
      Property p = new ResourceProperty(resource, getResource(name));
      return p;
   }

   @Override
   public boolean removeProperty(Resource resource, String name) {
      if (!ID_PROPERTY.equals(name) && hasResource(resource, name)) {
         resource.setProperty(resource, null);
         return true;
      }
      return false;
   }

   private boolean hasResource(Resource resource, String name) {
      for (String p : resource.getProperties()) {
         if (p.equals(name))
            return true;
      }
      return false;
   }

   private Resource getResource(String name) {
      Reference r = configuration.getResourceManager().getResource(name);
      if (r == null)
         return createResource(name);
      Object v = r.getValue();
      if (v == null)
         return createResource(name);
      if (v instanceof Resource)
         return (Resource)v;
      throw new RuntimeException(v.getClass().getName()+" is not a resource");
   }

   private Resource createResource(String name) {
      String prefix;
      int index = name.indexOf(':');
      if (index < 0)
         prefix = "";
      else {
         prefix = name.substring(0, index);
         name = name.substring(index+1);
      }

      ResourceManager resourceManager = configuration.getResourceManager();
      Reference ref = resourceManager.getResource(prefix+":");
      if (ref == null)
         throw new RuntimeException("Unknow namespace: "+prefix);
      Object v = ref.getValue();
      if (v instanceof Namespace) {
         Namespace ns = (Namespace)v;
         Repository repo = ns.getRepository();
         Resource res = new DefaultResource(name, ns, resourceManager, configuration.getConverter());
         repo.add(ns.getPrefix(), res);
         return res;
      }
      String type = (v == null) ? "null" : v.getClass().getName();
      throw new RuntimeException(type+" is not a namespace");
   }
}
