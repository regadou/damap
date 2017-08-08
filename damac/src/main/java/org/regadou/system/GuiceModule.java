package org.regadou.system;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.activation.FileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;
import org.regadou.factory.DefaultConverterManager;
import org.regadou.factory.DefaultPropertyFactory;
import org.regadou.factory.DefaultResourceManager;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.factory.DefaultFileTypeMap;
import org.regadou.factory.DefaultMimeHandlerFactory;
import org.regadou.damai.Converter;

public class GuiceModule extends AbstractModule {

   @Override
   public void configure() {
      bind(Converter.class).to(DefaultConverterManager.class).in(Singleton.class);
      bind(PropertyFactory.class).to(DefaultPropertyFactory.class).in(Singleton.class);
      bind(ResourceManager.class).to(DefaultResourceManager.class).in(Singleton.class);
      bind(FileTypeMap.class).to(DefaultFileTypeMap.class).in(Singleton.class);
      bind(MimeHandlerFactory.class).to(DefaultMimeHandlerFactory.class).in(Singleton.class);
   }

   @Provides
   public Configuration getConfiguration() {
      return Context.currentContext();
   }

   @Provides
   public ScriptContextFactory getScriptContextFactory() {
      return Context.currentContext();
   }

   @Singleton
   @Provides
   public ScriptEngineManager getScriptEngineManager(Configuration cfg) {
      ScriptEngineManager manager = new ScriptEngineManager();
      Bindings global = cfg.getGlobalScope();
      if (global != null)
         manager.setBindings(global);
      return manager;
   }
}