package org.regadou.script;

import java.io.Reader;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.damai.Configuration;
import org.regadou.util.StringInput;

public class DefaultCompiledScript extends CompiledScript {

   private Configuration configuration;
   private ScriptEngine engine;
   private Reader reader;
   private String script;

   public DefaultCompiledScript(ScriptEngine engine, Reader reader, Configuration configuration) {
      this.configuration = configuration;
      this.engine = engine;
      this.reader = reader;
   }

   public DefaultCompiledScript(ScriptEngine engine, String script, Configuration configuration) {
      this.configuration = configuration;
      this.engine = engine;
      this.script = script;
   }

   @Override
   public Object eval() throws ScriptException {
      return eval((ScriptContext)null);
   }

   @Override
   public Object eval(Bindings bindings) throws ScriptException {
      if (bindings == null)
         return eval((ScriptContext)null);
      return engine.eval(getScript(), bindings);
   }

   @Override
   public Object eval(ScriptContext context) throws ScriptException {
      if (context == null)
         context = configuration.getContextFactory().getScriptContext();
      return engine.eval(getScript(), context);
   }

   @Override
   public ScriptEngine getEngine() {
      return engine;
   }

   private String getScript() {
      if (script == null) {
         if (reader == null)
            script = "";
         script = new StringInput(reader).toString();
      }
      return script;
   }
}
