package org.regadou.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.action.AllFunction;
import org.regadou.action.BinaryAction;
import org.regadou.collection.ScriptContextMap;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class DamaiScriptEngineFactory implements ScriptEngineFactory {

   private Configuration configuration;
   private Map<String,Reference> keywords;

   @Inject
   public DamaiScriptEngineFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String getEngineName() {
      return "Regadou Damai Script";
   }

   @Override
   public String getEngineVersion() {
      return "0.1";
   }

   @Override
   public List<String> getExtensions() {
      return Arrays.asList("damai");
   }

   @Override
   public List<String> getMimeTypes() {
      return Arrays.asList("text/x-damai", "text/damai");
   }

   @Override
   public List<String> getNames() {
      return Arrays.asList("damai");
   }

   @Override
   public String getLanguageName() {
      return "damai";
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
         List<Action> actions = new ArrayList<>();
         for (Operator op : Operator.values())
            actions.add(new BinaryAction(configuration, op.getName(), op, null, 0));
         for (Command cmd : Command.values())
            actions.add(new BinaryAction(configuration, cmd.getName(), cmd, null, 0));
         actions.add(new AllFunction(configuration.getPropertyManager(), keywords, new ScriptContextMap(configuration.getContextFactory())));
         for (Action action : actions) {
            String name = action.getName().toLowerCase();
            keywords.put(name, new GenericReference(name, action, true));
         }
      }
      return new SexlScriptEngine(this, configuration, keywords);
   }
}
