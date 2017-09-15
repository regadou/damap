package org.regadou.resource;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.number.Time;
import org.regadou.collection.ClassIterator;

public class LiteralResource implements Resource {

   private static final String[] PROPERTIES = {"rdf:type", "rdf:value"};
   private static final Map<Class,String> TYPE_MAP = new LinkedHashMap<>();
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
         {Class.class, "rdfs:Class"},
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
         TYPE_MAP.put((Class)mapping[0], mapping[1].toString());
      }
   }

   private ResourceManager resourceManager;
   private Object value;
   private Resource type;
   private Class klass;

   public LiteralResource(Object value, ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
      setValue(value);
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
   public String getLocalName() {
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
      switch (property.getId()) {
         case "rdf:type":
            return findType();
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
      switch (property.getId()) {
         case "rdf:type":
            this.type = value;
            klass = null;
            //TODO: should we convert the value ?
            return true;
         case "rdf:value":
            setValue(value);
            return true;
         default:
            return false;
      }
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Class getType() {
      if (klass == null) {
         Object value = findType().getValue();
         if (value instanceof Class)
            klass = (Class)value;
         else if (value != null) {
            try { klass = Class.forName(value.toString()); }
            catch (ClassNotFoundException ex) { klass = Object.class; }
         }
         else
            klass = Object.class;
      }
      return klass;
   }

   @Override
   public void setValue(Object value) {
      if (value instanceof Resource)
         setValue(((Resource)value).getValue());
      else if (value instanceof Reference)
         setValue(((Reference)value).getValue());
      else if (value instanceof Literal)
         this.value = getLiteralValue((Literal)value);
      else if (value instanceof IRI) {
         IRI iri = (IRI)value;
         this.value = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
         this.type = (Resource)resourceManager.getResource("xsd:anyURI");
      }
      else if (value instanceof BNode) {
         this.value = "_:" + ((BNode)value).getID();
         this.type = (Resource)resourceManager.getResource("xsd:anyURI");
      }
      else {
         this.value = value;
         this.type = findType();
      }
   }

   private Resource findType() {
      if (type == null) {
         String typeId = null;
         ClassIterator it = new ClassIterator(value);
         while (it.hasNext()) {
            Class c = it.next();
            if (c.isPrimitive())
               c = PRIMITIVE_MAP.get(c);
            typeId = TYPE_MAP.get(c);
            if (typeId != null)
               break;
         }
         if (typeId == null)
            typeId = "xsd:string";
         if (value instanceof java.util.Date) {
            if (value instanceof Time || value instanceof Date || value instanceof Timestamp)
               ;
            else
               value = new Timestamp(((java.util.Date)value).getTime());
         }
         else if (value instanceof Number || value instanceof Boolean)
            ;
         else if (!(value instanceof CharSequence))
            value = value.toString();
         type = (Resource)resourceManager.getResource(typeId);
         klass = null;
      }
      return type;
   }

   private Object getLiteralValue(Literal lit) {
      IRI iri = lit.getDatatype();
      String typeId = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
      this.type = (Resource)resourceManager.getResource(typeId);
      switch (typeId) {
         case "xsd:boolean":
            return lit.booleanValue();
         case "xsd:double":
         case "xsd:decimal":
             return lit.doubleValue();
         case "xsd:float":
            return lit.floatValue();
         case "xsd:long":
         case "xsd:unsignedLong":
         case "xsd:integer":
         case "xsd:positiveInteger":
         case "xsd:nonPositiveInteger":
         case "xsd:negativeInteger":
         case "xsd:nonNegativeInteger":
            return lit.longValue();
         case "xsd:int":
        case "xsd:unsignedInt":
            return lit.intValue();
         case "xsd:short":
         case "xsd:unsignedShort":
            return lit.shortValue();
         case "xsd:byte":
         case "xsd:unsignedByte":
            return lit.byteValue();
         case "xsd:dateTime":
            return new Timestamp(lit.calendarValue().toGregorianCalendar().getTime().getTime());
         case "xsd:date":
            return new Date(lit.calendarValue().toGregorianCalendar().getTime().getTime());
         case "xsd:time":
            return new Time(lit.calendarValue().toGregorianCalendar());
         case "xsd:string":
         default:
            return lit.getLabel();
      }
   }
}
