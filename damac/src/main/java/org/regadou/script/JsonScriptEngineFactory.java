package org.regadou.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.action.AllAction;
import org.regadou.action.BinaryAction;
import org.regadou.action.ErrorAction;
import org.regadou.action.InputAction;
import org.regadou.action.LinkAction;
import org.regadou.action.OutputAction;
import org.regadou.collection.ScriptContextMap;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
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
         List constants = new ArrayList(Arrays.asList(true, false, null));
         for (Operator op : Operator.values())
            constants.add(new BinaryAction(configuration, getSymbol(op), op));
         constants.add(new BinaryAction(configuration, ":", Command.SET, null, -10));
         constants.add(new AllAction(configuration.getPropertyManager(), keywords, new ScriptContextMap(configuration.getContextFactory())));
         constants.add(new LinkAction(configuration));
         constants.add(new InputAction(configuration));
         constants.add(new OutputAction(configuration));
         constants.add(new ErrorAction(configuration));
         for (Object constant : constants) {
            String name = (constant instanceof Action) ? ((Action)constant).getName() : String.valueOf(constant);
            keywords.put(name, new GenericReference(name, constant, true));
         }
      }
      return new JsonScriptEngine(this, configuration, keywords);
   }

   private String getSymbol(Operator operator) {
      switch (operator) {
         case ADD: return "+";
         case SUBTRACT: return "-";
         case MULTIPLY: return "*";
         case DIVIDE: return "/";
         case MODULO: return "%";
         case EXPONANT: return "^";
         case ROOT: return "\\/";
         case LOG: return "\\";
         case LESSER: return "<";
         case LESSEQ: return "<=";
         case GREATER: return ">";
         case GREATEQ: return ">=";
         case EQUAL: return "==";
         case NOTEQUAL: return "!=";
         case AND: return "&&";
         case OR: return "||";
         case NOT: return "!";
         case IN: return "@";
         case FROM: return "<-";
         case TO: return "->";
         case IS: return "?:";
         case DO: return "=>";
         case HAVE: return ".";
         case JOIN: return ",";
         case IF: return "?";
         case ELSE: return ":";
         case WHILE: return "?*";
         default: throw new RuntimeException("Unknown operator "+operator);
      }
   }
}
