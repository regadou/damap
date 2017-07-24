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
import javax.activation.FileTypeMap;
import org.regadou.damai.Resource;
import org.regadou.system.Context;

public class UrlResource implements Resource, Closeable {

   private URL url;
   private String mimetype;
   private URLConnection connection;
   private Object content;

   public UrlResource(URL url) {
      this.url = url;
   }

   public UrlResource(URI uri) throws MalformedURLException {
      this.url = uri.toURL();
   }

   public UrlResource(File file) throws MalformedURLException {
      this.url = file.toURI().toURL();
   }

   public UrlResource(String url) throws MalformedURLException {
      this.url = new URL(url);
   }

   @Override
   public String getUri() {
      return url.toString();
   }

   @Override
   public String getMimetype() {
      if (mimetype == null) {
         try { mimetype = getConnection().getContentType(); }
         catch (IOException e) { throw new RuntimeException(e); }
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
                  mimetype = Context.currentContext().getInstance(FileTypeMap.class).getContentType(last);
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
         try {
            InputStream input = getConnection().getInputStream();
            String charset = connection.getContentEncoding();
            if (charset == null || charset.isEmpty())
               charset = Charset.defaultCharset().toString();
            content = Context.currentContext().read(input, charset);
         }
         catch (IOException e) { throw new RuntimeException(e); }
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
         output.write(((value == null) ? "" : value.toString()).getBytes(charset));
      }
      catch (IOException e) { throw new RuntimeException(e); }
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
