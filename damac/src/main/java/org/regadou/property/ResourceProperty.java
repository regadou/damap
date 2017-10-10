package org.regadou.property;

import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;

public class ResourceProperty implements Property<Resource,Reference> {

   private Resource resource;
   private String property;

   public ResourceProperty(Resource resource, String property) {
      this.resource = resource;
      this.property = property;
   }

   @Override
   public String toString() {
      return property+"@"+resource;
   }

   @Override
   public Resource getOwner() {
      return resource;
   }

   @Override
   public Class<Resource> getOwnerType() {
      return Resource.class;
   }

   @Override
   public String getId() {
      return property;
   }

   @Override
   public Reference getValue() {
      return resource.getProperty(property);
   }

   @Override
   public Class<Reference> getType() {
      return Reference.class;
   }

   @Override
   public void setValue(Reference value) {
      resource.setProperty(property, value);
   }
}
