package org.regadou.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.script.ScriptContext;
import org.regadou.action.BinaryAction;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Repository;
import org.regadou.expression.SimpleExpression;
import org.regadou.reference.UrlReference;
import org.regadou.action.GenericComparator;
import org.regadou.damai.Action;
import org.regadou.script.PropertiesScriptContext;

public class FileSystemRepository implements Repository<UrlReference> {

   private static final String PRIMARY_KEY = "name";

   private transient File folder;
   private transient Map<String, Class> keys = new TreeMap<>();
   private transient Configuration configuration;
   private transient PropertyFactory factory;
   private transient GenericComparator comparator;
   private transient Action equals;
   private Collection<String> items;

   public FileSystemRepository(URL url, Configuration configuration) {
      validateUrl(url, configuration);
   }

   public FileSystemRepository(File file, Configuration configuration) {
      validateFile(file, configuration);
   }

   public FileSystemRepository(String uri, Configuration configuration) {
      try { validateUrl(new URL(uri), configuration); }
      catch (MalformedURLException e) { validateFile(new File(uri), configuration); }
   }

   @Override
   public Collection<String> getItems() {
      return (items = Arrays.asList(folder.list()));
   }

   @Override
   public Map<String, Class> getKeys(String item) {
      File file = new File(folder + "/" + item);
      if (!file.exists())
         return null;
      if (keys == null) {
         keys = new TreeMap<>();
         for (String key : factory.getProperties(file)) {
            Property p = factory.getProperty(file, key);
            if (p != null)
               keys.put(key, p.getType());
         }
      }
      return keys;
   }

   @Override
   public Collection<String> getPrimaryKeys(String item) {
      return Collections.singleton(PRIMARY_KEY);
   }

   @Override
   public Collection<Object> getIds(String item) {
      File file = new File(folder + "/" + item);
      if (file.isDirectory())
         return Arrays.asList((Object[])file.list());
      return null;
   }

   @Override
   public Collection<UrlReference> getAny(String item, Expression filter) {
      Collection<UrlReference> found = new ArrayList<>();
      File file = new File(folder + "/" + item);
      if (file.isDirectory()) {
         for (File f : file.listFiles()) {
            ScriptContext cx = new PropertiesScriptContext(item, configuration.getPropertyManager(),
                                                                 configuration.getContextFactory());
            if (!comparator.isEmpty(filter.getValue(cx))) {
               try { found.add(new UrlReference(f, configuration)); }
               catch (MalformedURLException e) { throw new RuntimeException(e); }
            }
         }
      }
      return found;
   }

   @Override
   public UrlReference getOne(String item, Object id) {
      Expression filter = new SimpleExpression(configuration, equals, PRIMARY_KEY, id);
      Collection<UrlReference> found = getAny(item, filter);
      return found.isEmpty() ? null : found.iterator().next();
   }

   @Override
   public UrlReference add(String item, UrlReference entity) {
      throw new RuntimeException("FileSystemRepository does not support add method yet! Coming soon ...");
   }

   @Override
   public UrlReference update(String item, UrlReference entity) {
      throw new RuntimeException("FileSystemRepository does not support update method yet! Coming soon ...");
   }

   @Override
   public boolean remove(String item, Object id) {
      throw new RuntimeException("FileSystemRepository does not support remove method yet! Coming soon ...");
   }

   private void validateFile(File file, Configuration configuration) {
       if (!file.isDirectory())
         throw new RuntimeException(file+" is not a directory");
      this.folder = file;
      this.configuration = configuration;
      this.factory = configuration.getPropertyManager().getPropertyFactory(File.class);
      this.comparator = new GenericComparator(configuration);
      this.equals = new BinaryAction(configuration, null, Operator.EQUAL);
   }

   private void validateUrl(URL url, Configuration configuration) {
      if (!"file".equals(url.getProtocol()))
         throw new RuntimeException("Unsupported scheme "+url.getProtocol());
      validateFile(new File(url.getPath()), configuration);
   }
}
