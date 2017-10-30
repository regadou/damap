package org.regadou.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;

public class RdfCollection extends LinkedHashSet<Reference> implements Resource<Collection<Reference>> {

   private static final String[] PROPERTIES = "rdf:type,size".split(",");
   
   private String id;
   private Namespace namespace;

   public RdfCollection(ResourceManager resourceManager, Reference...values) {
      super();
      for (Reference r : values)
         add(r);
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
   public String getId() {
      return null;
   }

   @Override
   public Collection<Reference> getValue() {
      return Arrays.asList(toArray(new Reference[size()]));
   }

   @Override
   public Class getType() {
      return Collection.class;
   }

   @Override
   public void setValue(Collection<Reference> value) {
      clear();
      addAll(value);
   }

   @Override
   public String getLocalName() {
      return id;
   }

   @Override
   public Reference getOwner() {
      return (namespace == null) ? null: new GenericReference(id, namespace, true);
   }

   @Override
   public String[] getProperties() {
      return PROPERTIES;
   }

   @Override
   public Reference getProperty(String property) {
      //TODO: we must support @id, rdf:type, rdf:_#
      switch (property) {
         case "rdf:type":
            return wrap(Collection.class);
         case "size":
            return wrap(size());
         default:
            if (property.startsWith("rdf:_"))
               property = property.substring(5);
            try {
               int index = Integer.parseInt(property);
               if (index >= 0 && index < size())
                  return wrap(toArray()[index]);
            }
            catch (NumberFormatException e) {}
      }
      return null;
   }

   @Override
   public void setProperty(String property, Reference value) {
      clear();
      if (value instanceof RdfCollection)
         addAll(((RdfCollection)value).getValue());
      else
         add(value);
   }

   @Override
   public boolean addProperty(String property, Reference value) {
      return add(value);
   }

   private Reference wrap(Object value) {
      return (value instanceof Reference) ? (Reference)value : new GenericReference(null, value, true);
   }
}
