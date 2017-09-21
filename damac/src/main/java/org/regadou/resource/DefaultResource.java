package org.regadou.resource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.regadou.collection.ClassIterator;
import org.regadou.damai.Converter;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.number.Time;

public class DefaultResource implements Resource {

   private static final Map<Class,String> JAVA_RDF_MAP = new LinkedHashMap<>();
   private static final Map<String,Class> RDF_JAVA_MAP = new LinkedHashMap<>();
   private static final Map<Class,Class> PRIMITIVE_MAP = new LinkedHashMap<>();
   static {
      for (Class[] mapping : new Class[][]{
         {Byte.TYPE, Byte.class},
         {Short.TYPE, Short.class},
         {Integer.TYPE, Integer.class},
         {Long.TYPE, Long.class},
         {Float.TYPE, Float.class},
         {Double.TYPE, Double.class},
         {Boolean.TYPE, Boolean.class},
         {Character.TYPE, Character.class}
      }) {
         PRIMITIVE_MAP.put(mapping[0], mapping[1]);
      }
      for (Object[] mapping : new Object[][]{
         {Class.class, "rdfs:Datatype"},
         {Class.class, "rdfs:Class"},
         {Property.class, "rdf:Property"},
         {List.class, "rdf:List"},
         {List.class, "rdf:Seq"},
         {Set.class, "rdf:Bag"},
         {Collection.class, "rdfs:Container"},
         {CharSequence.class, "xsd:string"},
         {Boolean.class, "xsd:boolean"},
         {Double.class, "xsd:double"},
         {Float.class, "xsd:float"},
         {Long.class, "xsd:long"},
         {Integer.class, "xsd:int"},
         {Short.class, "xsd:short"},
         {Byte.class, "xsd:byte"},
         {java.util.Date.class, "xsd:dateTime"},
         {Timestamp.class, "xsd:dateTime"},
         {Date.class, "xsd:date"},
         {Time.class, "xsd:time"},
         {File.class, "xsd:anyURI"},
         {URI.class, "xsd:anyURI"},
         {URL.class, "xsd:anyURI"}
      }) {
         JAVA_RDF_MAP.put((Class)mapping[0], mapping[1].toString());
         RDF_JAVA_MAP.put(mapping[1].toString(), (Class)mapping[0]);
      }
   }

   private String id;
   private Namespace namespace;
   private Map<Resource,CollectionResource> properties = new LinkedHashMap<>();
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
//TODO: transform it into proper java instance
      return properties;
   }

   @Override
   public Class getType() {
      Reference restype = resourceManager.getResource("rdf:type");
      if (restype instanceof Resource) {
         CollectionResource values = properties.get((Resource)restype);
         if (values != null && !values.isEmpty()) {
            for (Resource r : values) {
               Class type = findJavaType(r);
               if (type != null)
                  return type;
            }
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
   public Namespace getNamespace() {
      return namespace;
   }

   @Override
   public String getLocalName() {
      return id;
   }

   @Override
   public String[] getProperties() {
      String[] keys = new String[properties.size()];
      Iterator<Resource> it = properties.keySet().iterator();
      for (int k = 0; it.hasNext(); k++)
         keys[k] = it.next().toString();
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
         properties.put(property, new CollectionResource(resourceManager, converter, value));
   }

   @Override
   public boolean addProperty(Resource property, Resource value) {
      if (value != null) {
         CollectionResource values = properties.get(property);
         if (values == null)
            properties.put(property, new CollectionResource(resourceManager, converter, value));
         else
            values.add(value);
      }
      return false;
   }

   protected String findTypeId(Object value) {
      ClassIterator it = new ClassIterator(value);
      while (it.hasNext()) {
         Class c = it.next();
         if (c.isPrimitive())
            c = PRIMITIVE_MAP.get(c);
         String typeId = JAVA_RDF_MAP.get(c);
         if (typeId != null)
            return typeId;
      }
      return "xsd:string";
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
