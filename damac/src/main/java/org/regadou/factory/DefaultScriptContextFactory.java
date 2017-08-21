package org.regadou.factory;

import javax.inject.Inject;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.script.DefaultScriptContext;

public class DefaultScriptContextFactory implements ScriptContextFactory {

   private final ThreadLocal<ScriptContext> currentContext = new ThreadLocal() {
      @Override
      protected synchronized ScriptContext initialValue() { return null; }
   };

   private ScriptEngineManager manager;

   @Inject
   public DefaultScriptContextFactory(ScriptEngineManager manager) {
      this.manager = manager;
   }

   @Override
   public ScriptContext getScriptContext(Reference... properties) {
      ScriptContext cx = currentContext.get();
      if (cx == null) {
         cx = new DefaultScriptContext(manager, properties);
         currentContext.set(cx);
      }
      return cx;
   }

   @Override
   public void setScriptContext(ScriptContext context) {
      currentContext.set(context);
   }

}
