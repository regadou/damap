package org.regadou.resource;

import org.regadou.damai.Configuration;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.reference.GenericReference;

public class GenericResource implements Resource {

   private String id;
   private Object value;
   private boolean readonly;
   private Configuration configuration;
   private String localName;
   private Namespace namespace;

   public GenericResource(String id, Object value, boolean readonly, Configuration configuration) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      this.id = id;
      this.value = value;
      this.readonly = readonly;
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return getId();
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Class getType() {
      return Object.class;
   }

   @Override
   public void setValue(Object value) {
      if (!readonly)
         this.value = value;
   }

   @Override
   public String getLocalName() {
      if (localName == null)
         detectLocalNameAndNamespace();
      return localName;
   }

   @Override
   public Reference getOwner() {
      if (namespace == null)
         detectLocalNameAndNamespace();
      return new GenericReference(namespace.getPrefix(), namespace, true);
   }

   @Override
   public String[] getProperties() {
      Class type = (value == null) ? Void.class : value.getClass();
      return configuration.getPropertyManager().getPropertyFactory(type).getProperties(value);
   }

   @Override
   public Reference getProperty(String property) {
      return configuration.getPropertyManager().getProperty(value, property);
   }

   @Override
   public void setProperty(String property, Reference value) {
      if (readonly)
         return;
      Property p = configuration.getPropertyManager().getProperty(this.value, property);
      if (p != null)
         p.setValue(value);
   }

   @Override
   public boolean addProperty(String property, Reference value) {
      if (readonly)
         return false;
      Class type = (this.value == null) ? Void.class : this.value.getClass();
      Property p = configuration.getPropertyManager().getPropertyFactory(type)
                                .addProperty(this.value, property, value.getValue());
      return p != null;
   }

   private void detectLocalNameAndNamespace() {

   }
}
