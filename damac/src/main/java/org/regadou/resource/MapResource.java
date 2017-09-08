package org.regadou.resource;

import java.util.Iterator;
import java.util.Map;
import org.regadou.damai.Namespace;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;

public class MapResource implements Resource {

   private String id;
   private Namespace namespace;
   private Map<Resource,CollectionResource> properties;
   private transient String[] keys;
   private transient ResourceManager resourceManager;
   private transient Namespace blankNamespace;

   public MapResource(String id, Namespace namespace, ResourceManager resourceManager) {
      this.id = id;
      this.namespace = namespace;
      this.resourceManager = resourceManager;
      this.blankNamespace = (Namespace)resourceManager.getResource("_:").getValue();
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
   public Namespace getNamespace() {
      return namespace;
   }

   @Override
   public String getId() {
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
