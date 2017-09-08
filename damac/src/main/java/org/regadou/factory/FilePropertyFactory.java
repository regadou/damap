package org.regadou.factory;

import java.io.File;
import java.util.Date;
import javax.activation.FileTypeMap;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.property.GenericProperty;

public class FilePropertyFactory implements PropertyFactory<File> {

   private static final String[] PROPERTIES = new String[]{
      "name", "parent", "date", "size", "mimetype",
      "exists", "directory", "readable", "writable", "executable"
   };

   private FileTypeMap fileTypeMap;

   public FilePropertyFactory(FileTypeMap fileTypeMap) {
      this.fileTypeMap = fileTypeMap;
   }

   @Override
   public Property getProperty(File file, String name) {
      return new GenericProperty(file, name, getValue(file, name), true);
   }

   @Override
   public String[] getProperties(File file) {
      return PROPERTIES;
   }

   @Override
   public Property addProperty(File file, String name, Object value) {
      return null;
   }

   @Override
   public boolean removeProperty(File file, String name) {
      return false;
   }

   private Object getValue(File file, String name) {
      switch (name) {
         case "name": return file.getName();
         case "mimetype": return fileTypeMap.getContentType(file);
         case "parent": return file.getParentFile();
         case "date": return new Date(file.lastModified());
         case "size": return file.length();
         case "exists": return file.exists();
         case "directory": return file.isDirectory();
         case "readable": return file.canRead();
         case "writable": return file.canWrite();
         case "executable": return file.canExecute();
      }
      return null;
   }
}
