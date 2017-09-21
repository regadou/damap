package org.regadou.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.regadou.damai.Converter;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.number.Time;

public class LiteralResource extends DefaultResource {

   private static final String[] PROPERTIES = {"rdf:type", "rdf:value"};

   private transient Object value;
   private transient Class type;

   public LiteralResource(Object value, ResourceManager resourceManager, Converter converter) {
      super(null, null, resourceManager, converter);
      setValue(value);
   }

   @Override
   public String toString() {
      return (value == null) ? "" : value.toString();
   }

   @Override
   public String[] getProperties() {
      return PROPERTIES;
   }

   @Override
   public Resource getProperty(Resource property) {
      switch (property.getId()) {
         case "rdf:type":
            return (Resource)resourceManager.getResource(findTypeId(value));
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
            this.type = findJavaType(value);
            this.value = converter.convert(this.value, type);
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
      return type;
   }

   @Override
   public void setValue(Object value) {
      String typeId;
      if (value instanceof Reference) {
         setValue(((Reference)value).getValue());
         return;
      }
      else if (value instanceof Literal) {
         Literal lit = (Literal)value;
         IRI iri = lit.getDatatype();
         typeId = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
         this.value = getLiteralValue(lit, typeId);
      }
      else if (value instanceof IRI) {
         IRI iri = (IRI)value;
         this.value = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
         typeId = "xsd:anyURI";
      }
      else if (value instanceof BNode) {
         this.value = "_:" + ((BNode)value).getID();
         typeId = "xsd:anyURI";
      }
      else {
         this.value = adjustType(value);
         typeId = findTypeId(value);
      }
      type = findJavaType(typeId);
   }

   private Object getLiteralValue(Literal lit, String typeId) {
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

   private Object adjustType(Object src) {
      if (src instanceof java.util.Date) {
         if (src instanceof Time || src instanceof Date || src instanceof Timestamp)
            return src;
         return new Timestamp(((java.util.Date)src).getTime());
      }
      if (src instanceof Boolean || src instanceof Number || src instanceof CharSequence || src instanceof URL)
         return src;
      if (src instanceof File) {
         try { return ((File)src).toURI().toURL(); }
         catch (MalformedURLException e) { return src.toString(); }
      }
      if (src instanceof URI) {
         try { return ((URI)src).toURL(); }
         catch (MalformedURLException e) { return src.toString(); }
      }
      return src.toString();
   }
}
