package org.regadou.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.factory.RdfNamespace;
import org.regadou.reference.RdfResource;

public class RdfHandler implements MimeHandler {

   private static final Map<Class,String> JAVA_TO_RDF_TYPES = new LinkedHashMap<>();
   private static final Map<String,Class> RDF_TO_JAVA_TYPES = new TreeMap<>();
   private static class WritingContext {
      public Collection<Namespace> namespaces = new LinkedHashSet<>();
      public Collection<Statement> statements = new LinkedHashSet<>();
   }

   private RDFFormat syntax;
   private String[] mimetypes;
   private Configuration configuration;
   private WriterConfig config;

   public RdfHandler(RDFFormat syntax, Configuration configuration) {
      this.syntax = syntax;
      Collection<String> types = syntax.getMIMETypes();
      this.mimetypes = types.toArray(new String[types.size()]);
      this.configuration = configuration;
      if (syntax == RDFFormat.JSONLD) {
         config = new WriterConfig();
         config.set(JSONLDSettings.COMPACT_ARRAYS, true);
         config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
         config.set(JSONLDSettings.OPTIMIZE, true);
         config.set(JSONLDSettings.USE_NATIVE_TYPES, true);
         config.set(JSONLDSettings.USE_RDF_TYPE, true);
      }
   }

   @Override
   public String[] getMimetypes() {
      return mimetypes;
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      RDFParser parser = Rio.createParser(syntax);
      StatementCollector collector = new StatementCollector();
      parser.setRDFHandler(collector);
      parser.parse(input, "");
      Collection entities = new LinkedHashSet();
      Map<String,String> namespaces = collector.getNamespaces();
      ResourceManager resourceManager = configuration.getResourceManager();
      PropertyManager propertyManager = configuration.getPropertyManager();
      for (Statement st : collector.getStatements()) {
         Resource s = getResource(st.getSubject(), namespaces, resourceManager);
         entities.add(s);
         Resource p = getResource(st.getPredicate(), namespaces, resourceManager);
         Resource v = getResource(st.getObject(), namespaces, resourceManager);
         setProperty(s, p, v, propertyManager);
      }
      return entities;
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      RDFWriter writer = Rio.createWriter(syntax, output);
      if (config != null)
         writer.setWriterConfig(config);
      writer.startRDF();
      WritingContext cx = new WritingContext();
      for (Object node : configuration.getConverter().convert(value, Collection.class))
         extractData(cx, node);
      for (Namespace ns : cx.namespaces)
         writer.handleNamespace(ns.getPrefix(), ns.getIri());
      for (Statement st : cx.statements)
         writer.handleStatement(st);
      writer.endRDF();
   }

   private Resource getResource(Value v, Map<String,String> namespaces, ResourceManager resourceManager) {
      if (v instanceof Literal) {
         Literal lit = (Literal)v;
         String type = getType(lit, namespaces);
         switch (type) {
            case "xsd:string":
               return new RdfResource(null, null, lit.getLabel());
            case "xsd:boolean":
               return new RdfResource(null, null, lit.booleanValue());
            case "xsd:double":
               return new RdfResource(null, null, lit.doubleValue());
            case "xsd:float":
               return new RdfResource(null, null, lit.floatValue());
            case "xsd:long":
               return new RdfResource(null, null, lit.longValue());
            case "xsd:int":
               return new RdfResource(null, null, lit.intValue());
            case "xsd:short":
               return new RdfResource(null, null, lit.shortValue());
            case "xsd:byte":
               return new RdfResource(null, null, lit.byteValue());
            case "xsd:dateTime":
               return new RdfResource(null, null, lit.calendarValue().toGregorianCalendar().getTime());
            case "xsd:date":
               return new RdfResource(null, null, lit.calendarValue().toGregorianCalendar().getTime());
            case "xsd:time":
               return new RdfResource(null, null, lit.calendarValue().toGregorianCalendar().getTime());
            default:
               return new RdfResource(null, null, lit.stringValue());
         }
      }
      String uri = v.stringValue();
      Namespace ns = getNamespace(uri, namespaces, resourceManager);
      Reference ref = ns.getResource(uri);
      if (ref == null)
         ref = ns.addResource(uri);
      return (Resource)ref;
   }

   private Namespace getNamespace(String uri, Map<String,String> namespaces, ResourceManager resourceManager) {
      int index = uri.indexOf(':');
      String prefix = (index < 0) ? "" : uri.substring(0, index);
      ResourceFactory factory = resourceManager.getFactory(prefix);
      if (factory != null && factory instanceof Namespace)
         return (Namespace)factory;
      for (Map.Entry<String,String> entry : namespaces.entrySet()) {
         if (uri.startsWith(entry.getValue())) {
            Reference ref = resourceManager.getResource(entry.getKey()+":");
            if (ref != null) {
               Object value = ref.getValue();
               if (value instanceof Namespace)
                  return (Namespace)value;
            }
            return new RdfNamespace(entry.getKey(), entry.getValue(), resourceManager);
         }
      }

      index = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
      String nsUri = uri.substring(0, index);
      index = uri.lastIndexOf('/');
      prefix = nsUri.substring(index+1);
      return new RdfNamespace(prefix, nsUri, resourceManager);
   }

   private String getType(Literal literal, Map<String,String> namespaces) {
      IRI type = literal.getDatatype();
      String ns = type.getNamespace();
      for (Map.Entry<String,String> entry : namespaces.entrySet()) {
         if (ns.startsWith(entry.getValue()))
            return entry.getKey() + ":" + type.getLocalName();
      }
      return ns + type.getLocalName();
   }

   private void setProperty(Resource s, Resource p, Resource v, PropertyManager propertyManager) {
      Object data = s.getValue();
      while (data instanceof Reference)
         data = ((Reference)data).getValue();
      PropertyFactory factory = propertyManager.getPropertyFactory(data.getClass());
      String name = p.getNamespace().getPrefix() + ":" + p.getName();
      Property property = factory.getProperty(data, name);
      if (property == null)
         factory.addProperty(data, name, v);
      else
         p.setValue(v);
   }

   //TODO: how to convert any data to a RDF resource ?
   private void extractData(WritingContext cx, Object node) {/*
      IRI s = getIri(node, cx);
      Map<Node,Collection<Value>> properties = node.getProperties();
      for (Node property : properties.keySet()) {
         IRI p = getIri(property, cx);
         for (Value value : properties.get(property))
            cx.statements.add(rdfFactory.createStatement(s, p, getRdfValue(value, cx)));
      }*/
   }

   static {
      //TODO: init namespaces xsd, rdf, rdfs, damai
      for (Object[] mapping : new Object[][]{
         {CharSequence.class, "xsd:string"},
         {Boolean.class, "xsd:boolean"},
         {Double.class, "xsd:double"},
         {Float.class, "xsd:float"},
         {Long.class, "xsd:long"},
         {Integer.class, "xsd:int"},
         {Short.class, "xsd:short"},
         {Byte.class, "xsd:byte"},
         {Date.class, "xsd:dateTime"},
         {java.sql.Date.class, "xsd:date"},
         {java.sql.Time.class, "xsd:time"},
         {Map.class, "rdfs:Resource"},
         {Class.class, "rdfs:Class"},
         {Property.class, "rdf:Property"},
         {Collection.class, "rdfs:Container"},
         {Set.class, "rdf:Bag"},
         {List.class, "rdf:Seq"},
         {Action.class, "damai:Function"},
         {Expression.class, "damai:Expression"}
      }) {
         Class type = (Class)mapping[0];
         String iri = (String)mapping[1];
         JAVA_TO_RDF_TYPES.put(type, iri);
         RDF_TO_JAVA_TYPES.put(iri, type);
      }
   }
}
