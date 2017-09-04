package org.regadou.script;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.regadou.damai.Configuration;

public class JvmslScriptEngineFactory implements ScriptEngineFactory {

   private Configuration configuration;

   @Inject
   public JvmslScriptEngineFactory(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String getEngineName() {
      return "Regadou JVMSL";
   }

   @Override
   public String getEngineVersion() {
      return "0.1";
   }

   @Override
   public List<String> getExtensions() {
      return Arrays.asList("jvmsl");
   }

   @Override
   public List<String> getMimeTypes() {
      return Arrays.asList("text/x-jvmsl", "text/jvmsl");
   }

   @Override
   public List<String> getNames() {
      return Arrays.asList("jvmsl");
   }

   @Override
   public String getLanguageName() {
      return "jvmsl";
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
      return new JvmslScriptEngine(this, configuration);
   }
}
