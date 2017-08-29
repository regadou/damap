package org.regadou.script;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.ScriptContextFactory;

public class PropertiesScriptContext implements ScriptContext {

   private static final int PROPERTIES_SCOPE = ENGINE_SCOPE / 2;

   private Object src;
   private PropertyManager propertyManager;
   private ScriptContextFactory contextFactory;

   public PropertiesScriptContext(Object src, PropertyManager propertyManager, ScriptContextFactory contextFactory) {
      this.src = src;
      this.propertyManager = propertyManager;
      this.contextFactory = contextFactory;
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      if (contextFactory != null)
         contextFactory.getScriptContext().setBindings(bindings, scope);
   }

   @Override
   public Bindings getBindings(int scope) {
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getBindings(scope);
   }

   @Override
   public void setAttribute(String name, Object value, int scope) {
      if (scope == PROPERTIES_SCOPE) {
         Property p = propertyManager.getProperty(src, name);
         if (p != null) {
            p.setValue(value);
            return;
         }
      }
      if (contextFactory != null)
         contextFactory.getScriptContext().setAttribute(name, value, scope);
   }

   @Override
   public Object getAttribute(String name, int scope) {
      if (scope == PROPERTIES_SCOPE) {
         Property p = propertyManager.getProperty(src, name);
         if (p != null)
            return p.getValue();
      }
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getAttribute(name, scope);
   }

   @Override
   public Object removeAttribute(String name, int scope) {
      return (contextFactory == null) ? null : contextFactory.getScriptContext().removeAttribute(name, scope);
   }

   @Override
   public Object getAttribute(String name) {
      Property p = propertyManager.getProperty(src, name);
      if (p != null)
         return p.getValue();
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getAttribute(name);
   }

   @Override
   public int getAttributesScope(String name) {
      Property p = propertyManager.getProperty(src, name);
      if (p != null)
         return PROPERTIES_SCOPE;
      return (contextFactory == null) ? -1 : contextFactory.getScriptContext().getAttributesScope(name);
   }

   @Override
   public Writer getWriter() {
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getWriter();
   }

   @Override
   public Writer getErrorWriter() {
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getErrorWriter();
   }

   @Override
   public void setWriter(Writer writer) {
      if (contextFactory != null)
         contextFactory.getScriptContext().setWriter(writer);
   }

   @Override
   public void setErrorWriter(Writer writer) {
      if (contextFactory != null)
         contextFactory.getScriptContext().setErrorWriter(writer);
   }

   @Override
   public Reader getReader() {
      return (contextFactory == null) ? null : contextFactory.getScriptContext().getReader();
   }

   @Override
   public void setReader(Reader reader) {
      if (contextFactory != null)
         contextFactory.getScriptContext().setReader(reader);
   }

   @Override
   public List<Integer> getScopes() {
      if (contextFactory == null)
         return Collections.singletonList(PROPERTIES_SCOPE);
      List<Integer> scopes = contextFactory.getScriptContext().getScopes();
      if (!scopes.contains(PROPERTIES_SCOPE)) {
         Set<Integer> set = new TreeSet<>(scopes);
         set.add(PROPERTIES_SCOPE);
         scopes = new ArrayList<>(set);
      }
      return scopes;
   }
}
