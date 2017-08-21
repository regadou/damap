package org.regadou.script;

import javax.inject.Inject;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;

public class ExtendedScriptEngineManager extends ScriptEngineManager {

   private Configuration configuration;

   @Inject
   public ExtendedScriptEngineManager(Configuration configuration) {
      super();
      this.configuration = configuration;
      ScriptEngineFactory[] factories = new ScriptEngineFactory[]{
         new SexlScriptEngineFactory(configuration),
         new JvmslScriptEngineFactory(configuration)
      };
      for (ScriptEngineFactory factory : factories) {
         registerEngineName(factory.getEngineName(), factory);
         for (String mimetype : factory.getMimeTypes())
            registerEngineMimeType(mimetype, factory);
         for (String extension : factory.getExtensions())
            registerEngineExtension(extension, factory);
      }
   }
}
