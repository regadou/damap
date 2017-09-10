package org.regadou.resource;

import java.util.LinkedHashSet;
import org.regadou.damai.Namespace;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;

public class CollectionResource extends LinkedHashSet<Resource> implements Resource<Resource[]> {

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
      this.namespace = (Namespace)resourceManager.getResource("_:");
   }

   @Override
   public String toString() {
      return super.toString();
   }

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public Resource[] getValue() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public Class<Resource[]> getType() {
      return Resource[].class;
   }

   @Override
   public void setValue(Resource[] value) {
   }

   @Override
   public String getLocalName() {
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
      //TODO: we must support @id, rdf:type, rdf:_#
      if (property.toString().equals(PROPERTIES[0]))
         return new LiteralResource(size(), resourceManager);
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
