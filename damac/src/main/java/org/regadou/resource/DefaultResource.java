package org.regadou.resource;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.regadou.damai.Converter;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;

public class DefaultResource implements Resource {

   private String id;
   private Namespace namespace;
   private Map<String,CollectionResource> properties = new LinkedHashMap<>();
   protected transient ResourceManager resourceManager;
   protected transient Converter converter;

   public DefaultResource(String id, Namespace namespace, ResourceManager resourceManager, Converter converter) {
      this.id = id;
      this.namespace = namespace;
      this.resourceManager = resourceManager;
      this.converter = converter;
   }

   @Override
   public String toString() {
      return getId();
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
//TODO: transform it into proper java instance
      return properties;
   }

   @Override
   public Class getType() {
      CollectionResource values = properties.get("rdf:type");
      if (values != null && !values.isEmpty()) {
         for (Reference r : values) {
            Class type = findJavaType(r.getId());
            if (type != null)
               return type;
         }
      }
      return Object.class;
   }

   @Override
   public void setValue(Object value) {
//TODO: we need the algoritm to transform a java instance into a rdf resource
//      properties = value;
   }

   @Override
   public Reference getOwner() {
      return (namespace == null) ? null: new GenericReference(id, namespace, true);
   }

   @Override
   public String getLocalName() {
      return id;
   }

   @Override
   public String[] getProperties() {
      String[] keys = new String[properties.size()];
      Iterator<String> it = properties.keySet().iterator();
      for (int k = 0; it.hasNext(); k++)
         keys[k] = it.next();
      return keys;
   }

   @Override
   public Reference getProperty(String property) {
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
   public void setProperty(String property, Reference value) {
      if (value == null)
         properties.remove(property);
      else
         properties.put(property, new CollectionResource(resourceManager, value));
   }

   @Override
   public boolean addProperty(String property, Reference value) {
      if (value != null) {
         CollectionResource values = properties.get(property);
         if (values == null)
            properties.put(property, new CollectionResource(resourceManager, value));
         else
            values.add(value);
      }
      return false;
   }

   protected Class findJavaType(String typeId) {
      return findJavaType((Resource)resourceManager.getResource(typeId));
   }

   protected Class findJavaType(Resource restype) {
      if (restype != null) {
         Object v = restype.getValue();
         if (v instanceof Class)
            return (Class)v;
         else if (v != null) {
            try { return Class.forName(v.toString()); }
            catch (ClassNotFoundException ex) {}
         }
      }
      return null;
   }
}
