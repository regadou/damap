package org.regadou.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.URI;
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
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Namespace;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceManager;
import org.regadou.repository.RdfRepository;
import org.regadou.resource.DefaultNamespace;
import org.regadou.resource.LiteralResource;
import org.regadou.resource.DefaultResource;

public class RdfHandler implements MimeHandler {

   private static class WritingContext {
      public Collection<Namespace> namespaces = new LinkedHashSet<>();
      public Collection<Statement> statements = new LinkedHashSet<>();
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
      Repository repo = new RdfRepository(configuration.getResourceManager(), configuration.getPropertyManager());
      for (Map.Entry<String,String> ns : collector.getNamespaces().entrySet())
         resourceManager.registerNamespace(new DefaultNamespace(ns.getKey(), ns.getValue(), repo));
      for (Statement st : collector.getStatements()) {
         Resource s = getResource(st.getSubject(), resourceManager);
         Resource p = getResource(st.getPredicate(), resourceManager);
         Resource v = getResource(st.getObject(), resourceManager);
         s.addProperty(p, v);
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

   private Resource getResource(Value v, ResourceManager resourceManager) {
      if (v instanceof Literal)
         return new LiteralResource(v, resourceManager);
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
            return createLiteralUri(iri.stringValue(), resourceManager);
         id = iri.getLocalName();
      }
      else if (v instanceof URI) {
         URI uri = (URI)v;
         ns = getNamespace(uri.getNamespace(), resourceManager);
         if (ns == null)
            return createLiteralUri(uri.stringValue(), resourceManager);
         id = uri.getLocalName();
      }
      else
         throw new RuntimeException("Unknown value type: "+v.getClass().getName());

      Repository repo = ns.getRepository();
      Object obj = repo.getOne(ns.getPrefix(), id);
      if (obj == null) {
         Resource r = new DefaultResource(id, ns, resourceManager);
         repo.add(ns.getPrefix(), r);
         return r;
      }
      else if (obj instanceof Resource)
         return (Resource)obj;
      else
         throw new RuntimeException("Unexpected resource type: "+obj.getClass().getName());
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

   private Resource createLiteralUri(String url, ResourceManager resourceManager) {
      try { return new LiteralResource(new java.net.URI(url), resourceManager); }
      catch (URISyntaxException e) { throw new RuntimeException(e); }
   }

}
