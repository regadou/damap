package org.regadou.reference;

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
import org.regadou.damai.Reference;
import org.regadou.mime.DefaultFileTypeMap;
import org.regadou.repository.FileSystemRepository;

public class UrlReference implements Reference, Closeable {

   private Configuration configuration;
   private URL url;
   private String mimetype;
   private URLConnection connection;
   private Object content;

   public UrlReference(URL url, Configuration configuration) {
      this.url = url;
      this.configuration = configuration;
   }

   public UrlReference(URI uri, Configuration configuration) throws MalformedURLException {
      this.url = uri.toURL();
      this.configuration = configuration;
   }

   public UrlReference(File file, Configuration configuration) throws MalformedURLException {
      this.url = file.toURI().toURL();
      this.configuration = configuration;
   }

   public UrlReference(String url, Configuration configuration) throws MalformedURLException {
      this.url = new URL(url);
      this.configuration = configuration;
   }

   public String getUri() {
      return url.toString();
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
   public String getName() {
      return url.toString();
   }

   @Override
   public Object getValue() {
      //TODO: check if resource was modified since last downloaded
      if (content == null) {
         if (DefaultFileTypeMap.FOLDER_MIMETYPE.equals(getMimetype()))
            return content = new FileSystemRepository(url, configuration);
         try {
            InputStream input = getConnection().getInputStream();
            String charset = connection.getContentEncoding();
            if (charset == null || charset.isEmpty())
               charset = Charset.defaultCharset().toString();
            content = configuration.getHandlerFactory()
                                   .getHandler(getMimetype())
                                   .load(input, charset);
         }
         catch (Exception e) {
            throw new RuntimeException("Error getting value for "+url+" with mimetype "+mimetype, e);
         }
      }
      return content;
   }

   @Override
   public Class getType() {
      return Object.class; //TODO: we might guess type based on mimetype
   }

   @Override
   public void setValue(Object value) {
      content = value;
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

   private URLConnection getConnection() throws IOException {
      if (connection == null)
         connection = url.openConnection();
      return connection;
   }

   @Override
   public void close() throws IOException {
      connection = null;
   }
}
