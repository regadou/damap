package org.regadou.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
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
         char[] chars = line.trim().toCharArray();
         if (chars.length == 0)
            return;
         int start = -1;
         String mimetype = null;
         for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c <= 32) {
               if (start >= 0) {
                  String txt = new String(chars, start, i - start).toLowerCase();
                  if (mimetype == null)
                     mimetype = txt;
                  else
                     extensions.put(txt, mimetype);
                  start = -1;
               }
            }
         }
         if (start >= 0 && mimetype != null)
            extensions.put(new String(chars, start, chars.length - start).toLowerCase(), mimetype);
      });
      try { reader.close(); }
      catch (Exception e) {}
   }
}
