package org.regadou.system;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.net.URL;
import javax.activation.FileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Bootstrap;
import org.regadou.damai.Configuration;
import org.regadou.factory.DefaultPropertyManager;
import org.regadou.factory.DefaultResourceManager;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.mime.DefaultFileTypeMap;
import org.regadou.factory.DefaultMimeHandlerFactory;
import org.regadou.damai.Converter;
import org.regadou.factory.DefaultScriptContextFactory;
import org.regadou.script.ExtendedScriptEngineManager;
import org.regadou.damai.PropertyManager;

public class GuiceConfiguration extends AbstractModule implements Configuration {

   private Injector injector;

   public GuiceConfiguration() {
      injector = Guice.createInjector(this);
   }

   @Override
   public void configure() {
      bind(PropertyManager.class).to(DefaultPropertyManager.class).in(Singleton.class);
      bind(ResourceManager.class).to(DefaultResourceManager.class).in(Singleton.class);
      bind(FileTypeMap.class).to(DefaultFileTypeMap.class).in(Singleton.class);
      bind(MimeHandlerFactory.class).to(DefaultMimeHandlerFactory.class).in(Singleton.class);
      bind(ScriptContextFactory.class).to(DefaultScriptContextFactory.class).in(Singleton.class);
   }

   @Provides
   @Singleton
   public Configuration getConfiguration() {
      return this;
   }

   @Provides
   @Singleton
   public Converter getDefaultConverter() {
      return new Bootstrap();
   }

   @Provides
   @Singleton
   public ScriptEngineManager getScriptEngineManager(Configuration configuration) {
      ScriptEngineManager manager = new ExtendedScriptEngineManager(configuration);
      manager.getBindings().put(Configuration.class.getName(), this);
      return manager;
   }

   @Override
   public URL[] getClasspath() {
      return new URL[0];
   }

   @Override
   public Bindings getGlobalScope() {
      return injector.getInstance(ScriptEngineManager.class).getBindings();
   }

   @Override
   public URL getInitScript() {
      return null;
   }

   @Override
   public ScriptContextFactory getContextFactory() {
      return injector.getInstance(ScriptContextFactory.class);
   }

   @Override
   public Converter getConverter() {
      return injector.getInstance(Converter.class);
   }

   @Override
   public ScriptEngineManager getEngineManager() {
      return injector.getInstance(ScriptEngineManager.class);
   }

   @Override
   public MimeHandlerFactory getHandlerFactory() {
      return injector.getInstance(MimeHandlerFactory.class);
   }

   @Override
   public PropertyManager getPropertyManager() {
      return injector.getInstance(PropertyManager.class);
   }

   @Override
   public ResourceManager getResourceManager() {
      return injector.getInstance(ResourceManager.class);
   }

   @Override
   public FileTypeMap getTypeMap() {
      return injector.getInstance(FileTypeMap.class);
   }
}