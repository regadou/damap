package org.regadou.script;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.damai.Configuration;
import org.regadou.system.Context;

public class SexlScriptEngineFactory implements ScriptEngineFactory {

   private Configuration configuration;

   public SexlScriptEngineFactory() {
      this.configuration = Context.currentContext();
   }

   @Inject
   public SexlScriptEngineFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String getEngineName() {
      return "sexl";
   }

   @Override
   public String getEngineVersion() {
      return "0.1";
   }

   @Override
   public List<String> getExtensions() {
      return Arrays.asList("sexl");
   }

   @Override
   public List<String> getMimeTypes() {
      return Arrays.asList("text/x-sexl", "text/sexl");
   }

   @Override
   public List<String> getNames() {
      return Arrays.asList("sexl");
   }

   @Override
   public String getLanguageName() {
      return "sexl";
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
      return new SexlScriptEngine(this, configuration);
   }
}
