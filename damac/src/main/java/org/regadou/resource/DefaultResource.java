package org.regadou.resource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;

public class DefaultResource implements Resource {

   private String id;
   private Namespace namespace;
   private Map<Resource,CollectionResource> properties = new LinkedHashMap<>();
   private transient String[] keys;
   private transient ResourceManager resourceManager;

   public DefaultResource(String id, Namespace namespace, ResourceManager resourceManager) {
      this.id = id;
      this.namespace = namespace;
      this.resourceManager = resourceManager;
   }

   @Override
   public String toString() {
      return namespace.getPrefix() + ":" + id;
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
   public Object getValue() {
      return properties;
   }

   @Override
   public Class getType() {
      Reference type = resourceManager.getResource("rdf:type");
      if (type instanceof Resource) {
         CollectionResource values = properties.get((Resource)type);
         if (values != null && !values.isEmpty()) {
            for (Resource r : values) {
               Object value = r.getValue();
               if (value instanceof Class)
                  return (Class)value;
               if (value != null) {
                  try { return Class.forName(value.toString()); }
                  catch (ClassNotFoundException e) {}
               }
            }
         }
      }
      return Object.class;
   }

   @Override
   public void setValue(Object value) {
//TODO: we need the algoritm to transform a java instance into a rdf resource
//      properties = value;
//      keys = null;
   }

   @Override
   public Namespace getNamespace() {
      return namespace;
   }

   @Override
   public String getLocalName() {
      return id;
   }

   @Override
   public String[] getProperties() {
      if (keys == null) {
         keys = new String[properties.size()];
         Iterator<Resource> it = properties.keySet().iterator();
         for (int k = 0; it.hasNext(); k++)
            keys[k] = it.next().toString();
      }
      return keys;
   }

   @Override
   public Resource getProperty(Resource property) {
      CollectionResource values = properties.get(property);
      if (values != null) {
         switch (values.size()) {
            case 0:
               break;
            case 1:
               return values.iterator().next();
            default:
               return values;
         }
      }
      return null;
   }

   @Override
   public void setProperty(Resource property, Resource value) {
      if (value == null)
         properties.remove(property);
      else
         properties.put(property, new CollectionResource(resourceManager, value));
      keys = null;
   }

   @Override
   public boolean addProperty(Resource property, Resource value) {
      if (value != null) {
         CollectionResource values = properties.get(property);
         if (values == null) {
            properties.put(property, new CollectionResource(resourceManager, value));
            keys = null;
         }
         else
            values.add(value);
      }
      return false;
   }
}
