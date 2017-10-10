package org.regadou.factory;

import java.io.File;
import java.lang.reflect.Field;
import org.regadou.resource.Url;
import java.net.MalformedURLException;
import javax.inject.Inject;
import org.regadou.action.FieldAction;
import org.regadou.action.MethodAction;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.reference.GenericReference;

public class NoSchemeResourceFactory implements ResourceFactory {

   private static final char[] FILE_CHARS = "./\\".toCharArray();

   private Configuration configuration;
   private ResourceManager resourceManager;
   private String[] schemes = new String[1];

   @Inject
   public NoSchemeResourceFactory(ResourceManager resourceManager, Configuration configuration) {
      this.resourceManager = resourceManager;
      this.configuration = configuration;
   }

   @Override
   public Reference getResource(String id) {
      try { return new GenericReference(id, Class.forName(id), true); }
      catch (ClassNotFoundException e) {
         int last = id.lastIndexOf('.');
         if (last > 0) {
            try {
               Class type = Class.forName(id.substring(0, last));
               String name = id.substring(last+1);
               return getMember(type, name);
            }
            catch (ClassNotFoundException e2) {}
         }
      }

      if (!canBeFile(id))
         return null;
      File f = new File(id);
      if (!f.exists() && !f.getParentFile().isDirectory())
         return null;
      try { return new Url(f.toURI().toURL(), configuration); }
      catch (MalformedURLException e) { return null; }
   }

   @Override
   public String[] getSchemes() {
      return schemes;
   }

   @Override
   public ResourceManager getResourceManager() {
      return resourceManager;
   }

   private boolean canBeFile(String name) {
      for (char c : FILE_CHARS) {
         if (name.indexOf(c) >= 0)
            return true;
      }
      return false;
   }

   private Reference getMember(Class type, String name) {
      Action member = null;
      int index = name.indexOf('(');
      if (index > 0) {
         if (!name.endsWith(")"))
            return null;
         String signature = name.substring(index+1, name.length()-1);
         name = name.substring(0, index);
         if (signature.length() == 1) {
            try { member = new MethodAction(configuration.getConverter(), type, name); }
            catch (NoSuchMethodException e) {}
         }
         else {
            String[] params = signature.isEmpty() ? new String[0] : signature.split(",");
            Class[] types = new Class[params.length];
            for (int p = 0; p < params.length; p++) {
               try { types[p] = Class.forName(params[p]); }
               catch (ClassNotFoundException e) { types[p] = Object.class; }
            }
            try { member = new MethodAction(configuration.getConverter(), type.getMethod(name, types)); }
            catch (NoSuchMethodException|SecurityException e) { return null; }
         }
      }
      else {
         for (Field f : type.getFields()) {
            if (f.getName().equals(name)) {
               member = new FieldAction(configuration.getConverter(), f);
               break;
            }
         }
         if (member == null) {
            try { member = new MethodAction(configuration.getConverter(), type, name); }
            catch (NoSuchMethodException e) {}
         }
      }
      return (member == null) ? null : new GenericReference(member.getName(), member, true);
   }
}
