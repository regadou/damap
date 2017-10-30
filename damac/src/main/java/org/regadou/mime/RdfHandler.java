package org.regadou.mime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.regadou.collection.ClassIterator;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Namespace;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.number.Time;
import org.regadou.reference.GenericReference;
import org.regadou.repository.RdfRepository;
import org.regadou.resource.DefaultNamespace;
import org.regadou.resource.RdfResource;

public class RdfHandler implements MimeHandler {

   private static class WritingContext {
      public Collection<Namespace> namespaces = new LinkedHashSet<>();
      public Collection<Statement> statements = new LinkedHashSet<>();
   }

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
         {Property.class, "rdf:Statement"},
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
         {Duration.class, "xsd:duration"},
         {File.class, "xsd:anyURI"},
         {URL.class, "xsd:anyURI"},
         {URI.class, "xsd:anyURI"}
      }) {
         JAVA_RDF_MAP.put((Class)mapping[0], mapping[1].toString());
         RDF_JAVA_MAP.put(mapping[1].toString(), (Class)mapping[0]);
      }
   }

   private RDFFormat syntax;
   private RDFParserFactory readerFactory;
   private RDFWriterFactory writerFactory;
   private String[] mimetypes;
   private Configuration configuration;
   private WriterConfig config;

   public RdfHandler(RDFFormat syntax, Configuration configuration) {
      this.syntax = syntax;
      this.readerFactory = getFactory(RDFParserFactory.class);
      this.writerFactory = getFactory(RDFWriterFactory.class);
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
      RDFParser parser = readerFactory.getParser();
      StatementCollector collector = new StatementCollector();
      parser.setRDFHandler(collector);
      parser.parse(input, "");
      ResourceManager resourceManager = configuration.getResourceManager();
      Converter converter = configuration.getConverter();
      Repository repo = new RdfRepository(configuration, configuration.getResourceManager());
      for (Map.Entry<String,String> ns : collector.getNamespaces().entrySet())
         resourceManager.registerNamespace(new DefaultNamespace(ns.getKey(), ns.getValue(), repo));
      for (Statement st : collector.getStatements()) {
         Reference s = getResource(st.getSubject(), resourceManager, converter);
         String p = getResource(st.getPredicate(), resourceManager, converter).getId();
         Reference v = getResource(st.getObject(), resourceManager, converter);
         if (s instanceof Resource)
            ((Resource)s).addProperty(p, v);
         else {
            PropertyFactory factory = configuration.getPropertyManager().getPropertyFactory(s.getType());
            Property property = factory.getProperty(s, p);
            if (property == null)
               factory.addProperty(s, p, v);
            else
               property.setValue(v);
         }
      }
      return repo;
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      RDFWriter writer = writerFactory.getWriter(output);
      if (config != null)
         writer.setWriterConfig(config);
      writer.startRDF();
      WritingContext cx = new WritingContext();
      for (Object node : configuration.getConverter().convert(value, Collection.class))
         extractData(cx, node);
      for (Namespace ns : cx.namespaces)
         writer.handleNamespace(ns.getPrefix(), ns.getUri());
      for (Statement st : cx.statements)
         writer.handleStatement(st);
      writer.endRDF();
   }

   private Reference getResource(Value v, ResourceManager resourceManager, Converter converter) {
      if (v instanceof Literal)
         return getLiteralReference(v, resourceManager, converter);
      Namespace ns;
      String id;
      if (v instanceof BNode) {
         ns = getNamespace("_", resourceManager);
         id = ((BNode)v).getID();
      }
      else if (v instanceof IRI) {
         IRI iri = (IRI)v;
         ns = getNamespace(iri.getNamespace(), resourceManager);
         if (ns == null)
            return createLiteralUri(iri.stringValue(), resourceManager, converter);
         id = iri.getLocalName();
      }
      else if (v instanceof org.eclipse.rdf4j.model.URI) {
         org.eclipse.rdf4j.model.URI uri = (org.eclipse.rdf4j.model.URI)v;
         ns = getNamespace(uri.getNamespace(), resourceManager);
         if (ns == null)
            return createLiteralUri(uri.stringValue(), resourceManager, converter);
         id = uri.getLocalName();
      }
      else
         throw new RuntimeException("Unknown value type: "+v.getClass().getName());

      Repository<Resource> repo = ns.getRepository();
      Resource r = repo.getOne(ns.getPrefix(), id);
      if (r == null) {
         r = new RdfResource(id, ns, resourceManager, converter);
         repo.add(ns.getPrefix(), r);
         return r;
      }
      return r;
   }

   private Namespace getNamespace(String uri, ResourceManager resourceManager) {
      Reference r;
      if (uri.endsWith(":"))
         r = resourceManager.getResource(uri);
      else if (uri.indexOf(':') < 0)
         r = resourceManager.getResource(uri+":");
      else
         r = resourceManager.getResource(uri);
      if (r == null)
         throw new RuntimeException("Unknown namespace: "+uri);
      return (r instanceof Namespace) ? (Namespace)r : null;
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

   private <T> T getFactory(Class<T> type) {
      String format = syntax.getName().replace("/", "").replace("-", "");
      String folder = format.equals("BinaryRDF") ? "binary" : format.toLowerCase();
      String action = type.getSimpleName().substring(3);
      String name = "org.eclipse.rdf4j.rio." + folder + "." + format + action;
      try { return (T) Class.forName(name).newInstance(); }
      catch (Exception e) { throw new RuntimeException(e); }
   }

   private Reference createLiteralUri(String url, ResourceManager resourceManager, Converter converter) {
      try { return getLiteralReference(new java.net.URI(url), resourceManager, converter); }
      catch (URISyntaxException e) { throw new RuntimeException(e); }
   }

   private Reference getLiteralReference(Object value, ResourceManager resourceManager, Converter converter) {
      if (value instanceof Reference)
         return getLiteralReference(((Reference)value).getValue(), resourceManager, converter);
      else if (value instanceof IRI) {
         IRI iri = (IRI)value;
         String id = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
         return resourceManager.getResource(id);
      }
      else if (value instanceof BNode)
         return resourceManager.getResource("_:" + ((BNode)value).getID());
      else if (value instanceof Literal) {
         Literal lit = (Literal)value;
         IRI iri = lit.getDatatype();
         String typeId = resourceManager.getResource(iri.getNamespace()).getId() + iri.getLocalName();
         value = getLiteralValue(lit, typeId);
      }
      else
         value = adjustType(value);

      return (value instanceof Reference) ? (Reference)value : new GenericReference(null, value, true);
   }

   private String findTypeId(Object value) {
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
         case "xsd:duration":
            return Duration.parse(lit.getLabel());
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
      if (src instanceof java.net.URI) {
         try { return ((java.net.URI)src).toURL(); }
         catch (MalformedURLException e) { return src.toString(); }
      }
      return src.toString();
   }
}
