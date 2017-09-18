package org.regadou.script;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.action.BinaryAction;
import org.regadou.collection.IgnoreCaseMap;
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
         for (Operator op : Operator.values()) {
            String name = op.name();
            keywords.put(name, new GenericReference(name, new BinaryAction(configuration, name, op, null, 0), true));
         }
         for (Command cmd : Command.values()) {
            String name = cmd.name();
            keywords.put(name, new GenericReference(name, new BinaryAction(configuration, name, cmd, null, 0), true));
         }
         keywords = new IgnoreCaseMap(keywords, k -> (k == null) ? "" : k.toString().trim().toUpperCase());
      }
      return new SexlScriptEngine(this, configuration, keywords);
   }
}
