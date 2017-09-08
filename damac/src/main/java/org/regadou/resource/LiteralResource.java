package org.regadou.resource;

import org.regadou.damai.Namespace;
import org.regadou.damai.Resource;

public class LiteralResource implements Resource {

   private static final String[] PROPERTIES = {"rdf:type", "rdf:value"};

   private Object value;
   private Resource type;

   public LiteralResource(Object value, Resource type) {
      this.value = value;
      this.type = type;
   }

   @Override
   public String toString() {
      return (value == null) ? "" : value.toString();
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
   public String getId() {
      return null;
   }

   @Override
   public Namespace getNamespace() {
      return null;
   }

   @Override
   public String[] getProperties() {
      return PROPERTIES;
   }

   @Override
   public Resource getProperty(Resource property) {
      switch (property.toString()) {
         case "rdf:type":
            return type;
         case "rdf:value":
            return this;
         default:
            return null;
      }
   }

   @Override
   public void setProperty(Resource property, Resource value) {
      addProperty(property, value);
   }

   @Override
   public boolean addProperty(Resource property, Resource value) {
      switch (property.toString()) {
         case "rdf:type":
            this.type = value;
            return true;
         case "rdf:value":
            this.value = (value instanceof LiteralResource) ? ((LiteralResource)value).value : value.toString();
            return true;
         default:
            return false;
      }
   }

   public Object getValue() {
      return value;
   }

   public Resource getType() {
      return type;
   }
}
