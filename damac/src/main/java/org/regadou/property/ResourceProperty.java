package org.regadou.property;

import org.regadou.damai.Property;
import org.regadou.damai.Resource;

public class ResourceProperty implements Property<Resource,Resource> {

   private Resource resource;
   private Resource property;

   public ResourceProperty(Resource resource, Resource property) {
      this.resource = resource;
      this.property = property;
   }

   @Override
   public Resource getParent() {
      return resource;
   }

   @Override
   public Class<Resource> getParentType() {
      return Resource.class;
   }

   @Override
   public String getId() {
      return property.toString();
   }

   @Override
   public Resource getValue() {
      return resource.getProperty(property);
   }

   @Override
   public Class<Resource> getType() {
      return Resource.class;
   }

   @Override
   public void setValue(Resource value) {
      resource.setProperty(property, value);
   }
}
