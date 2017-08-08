package org.regadou.factory;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
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
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.script.ScriptEngineMimeHandler;
import org.regadou.util.CsvHandler;
import org.regadou.util.JsonHandler;
import org.regadou.util.StringInput;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class DefaultMimeHandlerFactory implements MimeHandlerFactory {

   private final Map<String, MimeHandler> handlers = new LinkedHashMap<>();
   private Configuration configuration;

   @Inject
   public DefaultMimeHandlerFactory(Configuration configuration) {
      this.configuration = configuration;
      for (MimeHandler handler : createDefaultHandlers())
         registerHandler(handler);
      for (ScriptEngineFactory factory : configuration.getEngineManager().getEngineFactories())
         registerHandler(new ScriptEngineMimeHandler(factory.getScriptEngine(), configuration));
   }

   @Override
   public void registerHandler(MimeHandler handler) {
      for (String type : handler.getMimetypes()) {
         if (handlers.containsKey(type))
            System.out.println("***** Warning: overriding content handler "+handlers.get(type)+" ("+type+") with "+handler);
         handlers.put(type, handler);
      }
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
               output.write(String.valueOf(value).getBytes(charset));
               output.flush();
         }, "text/plain", "text/html"),

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

         new DefaultMimeHandler((input, charset) -> {
            return ImageIO.read(input);
         }, (output, charset, value) -> {
               output.write(String.valueOf(value).getBytes(charset));
               output.flush();
         }, new TreeSet<>(Arrays.asList(ImageIO.getReaderMIMETypes())).toArray(new String[0])),

         new JsonHandler(configuration),

         new CsvHandler(configuration)
      };
   }
}
