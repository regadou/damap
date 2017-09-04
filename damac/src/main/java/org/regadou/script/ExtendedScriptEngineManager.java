package org.regadou.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;

public class ExtendedScriptEngineManager extends ScriptEngineManager {

   private Configuration configuration;
   private Set<ScriptEngineFactory> factories = new LinkedHashSet<>();

   @Inject
   public ExtendedScriptEngineManager(Configuration configuration) {
      super();
      this.configuration = configuration;
      Collection<ScriptEngineFactory> factories = new ArrayList<>();
      for (ScriptEngineFactory factory : super.getEngineFactories()) {
         factories.add(factory);
      }
      factories.add(new JvmslScriptEngineFactory(configuration));
      factories.add(new SexlScriptEngineFactory(configuration));
      for (ScriptEngineFactory factory : factories) {
         registerEngineName(factory.getEngineName(), factory);
         for (String mimetype : factory.getMimeTypes())
            registerEngineMimeType(mimetype, factory);
         for (String extension : factory.getExtensions())
            registerEngineExtension(extension, factory);
      }
   }

   public List<ScriptEngineFactory> getEngineFactories() {
      return new ArrayList(factories);
   }

   @Override
   public void registerEngineName(String name, ScriptEngineFactory factory) {
      super.registerEngineName(name, factory);
      factories.add(factory);
   }

   @Override
   public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
      super.registerEngineMimeType(type, factory);
      factories.add(factory);
   }

   @Override
   public void registerEngineExtension(String extension, ScriptEngineFactory factory) {
      super.registerEngineExtension(extension, factory);
      factories.add(factory);
   }
}