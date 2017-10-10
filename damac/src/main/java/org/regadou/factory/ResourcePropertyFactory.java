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
         return new ResourceProperty(resource, name);
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
      Property p = new ResourceProperty(resource, name);
      return p;
   }

   @Override
   public boolean removeProperty(Resource resource, String name) {
      if (!ID_PROPERTY.equals(name) && hasResource(resource, name)) {
         resource.setProperty(name, null);
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
}
