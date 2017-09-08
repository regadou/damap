package org.regadou.resource;

import java.util.LinkedHashSet;
import org.regadou.damai.Namespace;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;

public class CollectionResource extends LinkedHashSet<Resource> implements Resource {

   private static final String[] PROPERTIES = {"size"};

   private String id;
   private ResourceManager resourceManager;
   private Namespace namespace;

   public CollectionResource(ResourceManager resourceManager, Resource...resources) {
      super();
      for (Resource r : resources)
         add(r);
      this.resourceManager = resourceManager;
      this.id = String.valueOf(hashCode());
      this.namespace = (Namespace)resourceManager.getResource("_:").getValue();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public Namespace getNamespace() {
      return namespace;
   }

   @Override
   public String[] getProperties() {
      return PROPERTIES;
   }

   @Override
   public Resource getProperty(Resource property) {
      //TODO: we must support @id, type, member_#
      if (property.toString().equals(PROPERTIES[0]))
         return new LiteralResource(size(), (Resource)resourceManager.getResource("xsd:int").getValue());
      return null;
   }

   @Override
   public void setProperty(Resource property, Resource value) {
      clear();
      add(value);
   }

   @Override
   public boolean addProperty(Resource property, Resource value) {
      return add(value);
   }
}
