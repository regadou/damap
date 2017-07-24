package org.regadou.system;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Comparator;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import org.regadou.util.DefaultComparator;
import org.regadou.factory.DefaultConverterManager;
import org.regadou.util.DefaultFilter;
import org.regadou.factory.DefaultInstanceFactory;
import org.regadou.factory.DefaultPropertyFactory;
import org.regadou.factory.DefaultResourceManager;
import org.regadou.factory.UrlResourceFactory;
import org.regadou.damai.ConverterManager;
import org.regadou.damai.Filter;
import org.regadou.damai.InstanceFactory;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.factory.FileResourceFactory;

public class GuiceModule extends AbstractModule {

   @Override
   public void configure() {
      bind(InstanceFactory.class).to(DefaultInstanceFactory.class).in(Singleton.class);
      bind(ConverterManager.class).to(DefaultConverterManager.class).in(Singleton.class);
      bind(PropertyFactory.class).to(DefaultPropertyFactory.class).in(Singleton.class);
      bind(Filter.class).to(DefaultFilter.class).in(Singleton.class);
      bind(Comparator.class).to(DefaultComparator.class).in(Singleton.class);
   }

   @Provides
   @Singleton
   public ScriptEngineManager getScriptEngineManager() {
      ScriptEngineManager manager = new ScriptEngineManager();
      Bindings global = new SimpleBindings();
      manager.setBindings(global);
      return manager;
   }

   @Provides
   @Singleton
   public ResourceManager getResourceManager() {
      ResourceManager manager = new DefaultResourceManager();
      manager.registerFactory(new UrlResourceFactory(manager));
      manager.registerFactory(new FileResourceFactory(manager));
      return manager;
   }

   @Provides
   @Singleton
   public FileTypeMap getFileTypeMap() {
      FileTypeMap map = new MimetypesFileTypeMap(getClass().getResourceAsStream("/mimetypes"));
      FileTypeMap.setDefaultFileTypeMap(map);
      return map;
   }
}