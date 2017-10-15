package org.regadou.factory;

import java.io.File;
import java.io.InputStream;
import org.regadou.mime.DefaultMimeHandler;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.activation.FileTypeMap;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.script.ScriptEngineFactory;
import javax.swing.text.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;
import org.regadou.damai.Expression;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.action.GenericComparator;
import org.regadou.mime.ScriptEngineMimeHandler;
import org.regadou.mime.CsvHandler;
import org.regadou.mime.HtmlHandler;
import org.regadou.mime.ImageHandler;
import org.regadou.mime.JsonHandler;
import org.regadou.mime.RdfHandler;
import org.regadou.system.StringInput;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class DefaultMimeHandlerFactory implements MimeHandlerFactory {

   private static final String[] RDF_FILES = {"xsd.ttl", "rdf.ttl", "rdfs.ttl"};
   //TODO: add damai.ttl file

   private final Map<String, MimeHandler> handlers = new TreeMap<>();
   private Configuration configuration;
   private GenericComparator comparator;

   @Inject
   public DefaultMimeHandlerFactory(Configuration configuration) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
      Converter converter = configuration.getConverter();
      for (MimeHandler handler : createDefaultHandlers())
         registerHandler(handler);
      for (String mimetype : new TreeSet<>(Arrays.asList(ImageIO.getReaderMIMETypes()))) {
         if (!mimetype.startsWith("x-"))
            registerHandler(new ImageHandler(mimetype, converter));
      }
      for (ScriptEngineFactory factory : configuration.getEngineManager().getEngineFactories())
         registerHandler(new ScriptEngineMimeHandler(factory.getScriptEngine(), configuration));
      for (Field field : RDFFormat.class.getFields()) {
         int mod = field.getModifiers();
         if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && RDFFormat.class.isAssignableFrom(field.getType())) {
            try {
               RDFFormat format = (RDFFormat)field.get(null);
               if (format != RDFFormat.RDFA)
                  registerHandler(new RdfHandler(format, configuration));
            }
            catch (Exception e) { throw new RuntimeException(e); }
         }
      }

      FileTypeMap typemap = configuration.getTypeMap();
      for (String name : RDF_FILES) {
         try {
            URL file = getClass().getResource("/"+name);
            if (file != null) {
               String mimetype = typemap.getContentType(name);
               MimeHandler handler = getHandler(mimetype);
               if (handler != null) {
                  InputStream input = file.openStream();
                  Object value = handler.load(input, "utf8");
                  if (value instanceof Expression)
                     ((Expression)value).getValue();
                  input.close();
               }
            }
         }
         catch (Exception e) { e.printStackTrace(); }
      }
   }

   @Override
   public boolean registerHandler(MimeHandler handler) {
      Map<String, MimeHandler> newHandlers = new HashMap<>();
      for (String type : handler.getMimetypes()) {
         if (handlers.containsKey(type) && handler != handlers.get(type))
            return false;
         newHandlers.put(type, handler);
      }
      handlers.putAll(newHandlers);
      return true;
   }

   @Override
   public MimeHandler getHandler(String mimetype) {
      MimeHandler handler = handlers.get(mimetype);
      if (handler == null && mimetype.indexOf('/') < 0) {
         if (mimetype.indexOf('.') < 0)
            mimetype = "." + mimetype;
         mimetype = configuration.getTypeMap().getContentType(mimetype);
         if (mimetype != null)
            handler = handlers.get(mimetype);
      }
      return handler;
   }

   @Override
   public Collection<String> getMimetypes() {
      return handlers.keySet();
   }

   private MimeHandler[] createDefaultHandlers() {
      return new MimeHandler[]{

         new DefaultMimeHandler((input, charset) -> {
            return new StringInput(input, charset).toString();
         }, (output, charset, value) -> {
               String txt = (value == null) ? "null" : comparator.getString(value);
               output.write(txt.getBytes(charset));
               output.flush();
         }, "text/plain"),

         new DefaultMimeHandler((input, charset) -> {
            Properties p = new Properties();
            p.load(new InputStreamReader(input, charset));
            return p;
         }, (output, charset, value) -> {
            Properties p;
            if (value instanceof Properties)
               p = (Properties)value;
            else {
               Map map = (value instanceof Map) ? (Map)value : configuration.getConverter().convert(value, Map.class);
               p = new Properties();
               for (Object key : map.keySet())
                  p.setProperty(String.valueOf(key), configuration.getConverter().convert(value, String.class));
            }
            p.store(new OutputStreamWriter(output, charset), "");
         }, "text/x-java-properties"),

         new DefaultMimeHandler((input, charset) -> {
            try { return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input); }
            catch (ParserConfigurationException|SAXException e) { throw new RuntimeException(e); }
         }, (output, charset, value) -> {
            try {
               TransformerFactory.newInstance().newTransformer().transform(
                    new DOMSource((Node)configuration.getConverter().convert(value, Document.class)),
                    new StreamResult(output)
               );
            }
            catch (TransformerException e) { throw new RuntimeException(e); }
         }, "application/xml", "text/xml"),

         new HtmlHandler(configuration),

         new JsonHandler(configuration),

         new CsvHandler(configuration.getConverter())
      };
   }
}
