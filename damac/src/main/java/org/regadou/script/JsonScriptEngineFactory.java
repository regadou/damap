package org.regadou.script;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.action.ActionBuilder;
import org.regadou.action.AllAction;
import org.regadou.action.DefaultAction;
import org.regadou.action.ErrorAction;
import org.regadou.action.InputAction;
import org.regadou.action.LinkAction;
import org.regadou.action.OutputAction;
import org.regadou.collection.ScriptContextMap;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class JsonScriptEngineFactory implements ScriptEngineFactory {

   private Configuration configuration;
   private Map<String,Reference> keywords;

   @Inject
   public JsonScriptEngineFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String getEngineName() {
      return "JsonScript";
   }

   @Override
   public String getEngineVersion() {
      return "0.1";
   }

   @Override
   public List<String> getExtensions() {
      return Arrays.asList("jsons");
   }

   @Override
   public List<String> getMimeTypes() {
      return Arrays.asList("text/x-jsonscript", "text/jsonscript");
   }

   @Override
   public List<String> getNames() {
      return Arrays.asList("jsonscript");
   }

   @Override
   public String getLanguageName() {
      return "JsonScript";
   }

   @Override
   public String getLanguageVersion() {
      return "0.1";
   }

   @Override
   public Object getParameter(String key) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public String getMethodCallSyntax(String obj, String m, String... args) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public String getOutputStatement(String toDisplay) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public String getProgram(String... statements) {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public ScriptEngine getScriptEngine() {
      if (keywords == null) {
         keywords = new TreeMap<>();
         for (Object value : new Object[]{true, false, null})
            keywords.put(String.valueOf(value), new GenericReference(String.valueOf(value), value, true));
         List<Action> actions = new ActionBuilder(configuration)
                 .setWantCommands(false)
                 .setWantOperators(true)
                 .setWantOptimized(true)
                 .setWantSymbols(true)
                 .setWantStandard(true)
                 .addActions(new DefaultAction(configuration, "=", Command.SET, null, -10),
                             new AllAction(configuration, keywords, new ScriptContextMap(configuration.getContextFactory())))
                 .addActions(LinkAction.class, InputAction.class, OutputAction.class, ErrorAction.class)
                 .buildAll();
         for (Action action : actions)
            keywords.put(action.getName(), new GenericReference(action.getName(), action, true));
      }
      return new JsonScriptEngine(this, configuration, keywords);
   }
}
