package org.regadou.resource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import org.regadou.damai.Configuration;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.mime.DefaultFileTypeMap;
import org.regadou.reference.GenericReference;
import org.regadou.repository.FileSystemRepository;

public class Url implements Resource, Closeable {

   private Configuration configuration;
   private URL url;
   private String localName;
   private Reference parent;
   private String mimetype;
   private URLConnection connection;
   private Object value;

   public Url(URL url, Configuration configuration) {
      this.url = url;
      this.configuration = configuration;
   }

   public Url(URI uri, Configuration configuration) throws MalformedURLException {
      this.url = uri.toURL();
      this.configuration = configuration;
   }

   public Url(File file, Configuration configuration) throws MalformedURLException {
      this.url = file.toURI().toURL();
      this.configuration = configuration;
   }

   public Url(String url, Configuration configuration) throws MalformedURLException {
      this.url = new URL(url);
      this.configuration = configuration;
   }

   public String getMimetype() {
      if (mimetype == null) {
         if ("file".equals(url.getProtocol())) {
            File file = new File(url.getPath());
            if (file.isDirectory())
               mimetype = DefaultFileTypeMap.FOLDER_MIMETYPE;
         }
         else {
            try { mimetype = getConnection().getContentType(); }
            catch (IOException e) { throw new RuntimeException(e); }
         }
         if (mimetype == null || mimetype.isEmpty() || mimetype.equals("content/unknown")) {
            String[] parts = url.toString().split("#")[0].split("\\?")[0].split("/");
            String last = parts[parts.length-1];
            if (last.isEmpty())
               mimetype = "text/html";
            else {
               int dot = last.lastIndexOf('.');
               if (dot < 0)
                  mimetype = "text/html";
               else if (dot == 0)
                  mimetype = "text/plain";
               else
                  mimetype = configuration.getTypeMap().getContentType(last);
            }
         }
      }
      return mimetype;
   }

   public InputStream getInputStream() {
      try { return getConnection().getInputStream(); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   public OutputStream getOutputStream() {
      try { return getConnection().getOutputStream(); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public String toString() {
      return url.toString();
   }

   @Override
   public String getId() {
      return url.toString();
   }

   @Override
   public Object getValue() {
      //TODO: check if resource was modified since last downloaded
      if (value == null) {
         if (DefaultFileTypeMap.FOLDER_MIMETYPE.equals(getMimetype()))
            return value = new FileSystemRepository(url, configuration);
         try {
            InputStream input = getConnection().getInputStream();
            String charset = connection.getContentEncoding();
            if (charset == null || charset.isEmpty())
               charset = Charset.defaultCharset().toString();
            value = configuration.getHandlerFactory()
                                   .getHandler(getMimetype())
                                   .load(input, charset);
         }
         catch (Exception e) {
            throw new RuntimeException("Error getting value for "+url+" with mimetype "+mimetype, e);
         }
      }
      return value;
   }

   @Override
   public Class getType() {
      return Object.class;
      //TODO: guess type based on mimetype that uses getType() method in MimeHandler interface
   }

   @Override
   public void setValue(Object value) {
      this.value = value;
      try {
         OutputStream output = getConnection().getOutputStream();
         String charset = connection.getContentEncoding();
         if (charset == null || charset.isEmpty())
            charset = Charset.defaultCharset().toString();
         configuration.getHandlerFactory()
                      .getHandler(getMimetype())
                      .save(output, charset, value);
      }
      catch (Exception e) { throw new RuntimeException("Error setting value for "+url, e); }
   }

   @Override
   public String getLocalName() {
      if (localName == null)
         findParentAndLocalName();
      return localName;
   }

   @Override
   public Reference getOwner() {
      if (parent == null)
         findParentAndLocalName();
      return parent;
   }

   @Override
   public String[] getProperties() {
      return getPropertyFactory().getProperties(getValue());
   }

   @Override
   public Reference getProperty(String property) {
      return getPropertyFactory().getProperty(getValue(), property);
   }

   @Override
   public void setProperty(String property, Reference value) {
      Property p = getPropertyFactory().getProperty(getValue(), property);
      if (p != null)
         p.setValue(value);
   }

   @Override
   public boolean addProperty(String property, Reference value) {
      return getPropertyFactory().addProperty(getValue(), property, value) != null;
   }

   @Override
   public void close() throws IOException {
      connection = null;
   }

   private URLConnection getConnection() throws IOException {
      if (connection == null)
         connection = url.openConnection();
      return connection;
   }

   private PropertyFactory getPropertyFactory() {
      Object v = getValue();
      Class type = (v == null) ? Void.class : v.getClass();
      return configuration.getPropertyManager().getPropertyFactory(type);
   }

   private void findParentAndLocalName() {
      String uri = url.toString();
      while (uri.endsWith("#"))
         uri = uri.substring(0, uri.length()-1);
      int index = uri.lastIndexOf('#');
      if (index < 0) {
         while (uri.endsWith("/"))
            uri = uri.substring(0, uri.length()-1);
         index = uri.lastIndexOf('/');
         if (index < 0) {
            localName = uri;
            parent = new GenericReference(null, null, true);
            return;
         }
      }
      try {
         parent = new Url(uri.substring(0, index), configuration);
         localName = uri.substring(index+1);
      }
      catch (MalformedURLException e) { throw new RuntimeException(e); }
   }
}
