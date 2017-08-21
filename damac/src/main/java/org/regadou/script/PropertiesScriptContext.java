package org.regadou.script;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.regadou.damai.Configuration;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.ScriptContextFactory;

public class PropertiesScriptContext implements ScriptContext {

   private static final int DEFAULT_SCOPE = ENGINE_SCOPE;

   private Object src;
   private PropertyManager propertyManager;
   private ScriptContextFactory contextFactory;

   public PropertiesScriptContext(Object src, Configuration configuration) {
      this.src = src;
      propertyManager = configuration.getPropertyManager();
      contextFactory = configuration.getContextFactory();
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      contextFactory.getScriptContext().setBindings(bindings, scope);
   }

   @Override
   public Bindings getBindings(int scope) {
      return contextFactory.getScriptContext().getBindings(scope);
   }

   @Override
   public void setAttribute(String name, Object value, int scope) {
       contextFactory.getScriptContext().setAttribute(name, value, scope);
   }

   @Override
   public Object getAttribute(String name, int scope) {
      if (scope == DEFAULT_SCOPE) {
         Property p = propertyManager.getProperty(src, name);
         if (p != null)
            return p.getValue();
      }
      return contextFactory.getScriptContext().getAttribute(name, scope);
   }

   @Override
   public Object removeAttribute(String name, int scope) {
      return contextFactory.getScriptContext().removeAttribute(name, scope);
   }

   @Override
   public Object getAttribute(String name) {
      Property p = propertyManager.getProperty(src, name);
      if (p != null)
         return p.getValue();
      return contextFactory.getScriptContext().getAttribute(name);
   }

   @Override
   public int getAttributesScope(String name) {
      Property p = propertyManager.getProperty(src, name);
      if (p != null)
         return DEFAULT_SCOPE;
      return contextFactory.getScriptContext().getAttributesScope(name);
   }

   @Override
   public Writer getWriter() {
      return contextFactory.getScriptContext().getWriter();
   }

   @Override
   public Writer getErrorWriter() {
      return contextFactory.getScriptContext().getErrorWriter();
   }

   @Override
   public void setWriter(Writer writer) {
      contextFactory.getScriptContext().setWriter(writer);
   }

   @Override
   public void setErrorWriter(Writer writer) {
      contextFactory.getScriptContext().setErrorWriter(writer);
   }

   @Override
   public Reader getReader() {
      return contextFactory.getScriptContext().getReader();
   }

   @Override
   public void setReader(Reader reader) {
      contextFactory.getScriptContext().setReader(reader);
   }

   @Override
   public List<Integer> getScopes() {
      List<Integer> scopes = contextFactory.getScriptContext().getScopes();
      if (!scopes.contains(DEFAULT_SCOPE)) {
         Set<Integer> set = new TreeSet<>(scopes);
         set.add(DEFAULT_SCOPE);
         scopes = new ArrayList<>(set);
      }
      return scopes;
   }
}
