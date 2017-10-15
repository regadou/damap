package org.regadou.resource;

import org.regadou.damai.Configuration;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.reference.GenericReference;

public class GenericResource implements Resource {

   private String localName;
   private Object value;
   private Object owner;
   private boolean readonly;
   private Configuration configuration;

   public GenericResource(String localName, Object value, Object owner, boolean readonly, Configuration configuration) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      this.localName = localName;
      this.value = value;
      this.owner = owner;
      this.readonly = readonly;
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      return (localName == null) ? super.toString() : getId();
   }

   @Override
   public String getId() {
      if (localName == null)
         return null;
      if (owner instanceof Reference) {
         String ownerId = ((Reference)owner).getId();
         if (ownerId != null)
            return localName+"@"+ownerId;
      }
      return localName;
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
      return localName;
   }

   @Override
   public Reference getOwner() {
      if (owner == null)
         return null;
      if (owner instanceof Reference)
         return (Reference)owner;
      return new GenericReference(null, owner, true);
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
}
