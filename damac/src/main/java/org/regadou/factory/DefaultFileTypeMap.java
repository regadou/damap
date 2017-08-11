package org.regadou.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.activation.FileTypeMap;

public class DefaultFileTypeMap extends FileTypeMap {

   public  static final String  DEFAULT_MIMETYPE = "application/octet-stream";
   public  static final String  FOLDER_MIMETYPE = "inode/directory";
   public  static final String  TEXT_MIMETYPE = "text/plain";
   private static DefaultFileTypeMap FIRST_INSTANCE;

   private final Map<String,String> extensions = new TreeMap<>();

   public DefaultFileTypeMap() {
      super();
      if (FIRST_INSTANCE == null) {
         FIRST_INSTANCE = this;
         FileTypeMap.setDefaultFileTypeMap(FIRST_INSTANCE);
      }
      loadMimetypes(getClass().getResourceAsStream("/mimetypes"));
   }

   @Override
   public String getContentType(File file) {
      if (file == null)
         return DEFAULT_MIMETYPE;
      if (file.isDirectory())
         return FOLDER_MIMETYPE;
      return getContentType(file.toString());
   }

   @Override
   public String getContentType(String string) {
      if (string != null) {
         int last = Math.max(string.lastIndexOf('/'), string.lastIndexOf('\\'));
         if (last >= 0)
            string = string.substring(last+1);
         if (string.isEmpty())
            return FOLDER_MIMETYPE;
         last = string.lastIndexOf('.');
         if (last < 0)
            return TEXT_MIMETYPE;
         String type = extensions.get(string.substring(last+1).toLowerCase());
         if (type != null)
            return type;
         if (last == 0)
            return TEXT_MIMETYPE;
      }
      return DEFAULT_MIMETYPE;
   }

   public Map<String,String> getMapping() {
      return extensions;
   }

   private void loadMimetypes(InputStream input) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      reader.lines().forEach(line -> {
         int escape = line.indexOf('#');
         if (escape >= 0)
            line = line.substring(0, escape);
         String mimetype = null;
         Enumeration e = new StringTokenizer(line);
         while (e.hasMoreElements()) {
            if (mimetype == null)
               mimetype = e.nextElement().toString();
            else
               extensions.put(e.nextElement().toString(), mimetype);
         }
      });
      try { reader.close(); }
      catch (Exception e) {}
   }
}
