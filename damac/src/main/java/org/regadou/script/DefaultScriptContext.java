package org.regadou.script;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import org.regadou.damai.Reference;

public class DefaultScriptContext implements ScriptContext {

   private Map<Integer,Bindings> scopes = new TreeMap<>();
   private Reader reader;
   private Writer writer;
   private Writer error;

   public DefaultScriptContext(ScriptContext cx) {
      reader = cx.getReader();
      writer = cx.getWriter();
      error = cx.getErrorWriter();
      for (Integer scope : cx.getScopes())
         scopes.put(scope, cx.getBindings(scope));
   }

   public DefaultScriptContext(ScriptEngineManager manager, Reference ... properties) {
      if (properties != null) {
         for (Reference property : properties) {
            Method m = getSetter(property);
            if (m != null) {
               try { m.invoke(this, property.getValue()); }
               catch (IllegalArgumentException|IllegalAccessException|InvocationTargetException e) {
                  throw new RuntimeException(e);
               }
            }
         }
      }
      if (!scopes.containsKey(GLOBAL_SCOPE))
         scopes.put(GLOBAL_SCOPE, manager.getBindings());
      if (!scopes.containsKey(ENGINE_SCOPE))
         scopes.put(ENGINE_SCOPE, new SimpleBindings());
   }

   @Override
   public String toString() {
      Set keys = new TreeSet();
      for (Bindings bindings : scopes.values())
         keys.addAll(bindings.keySet());
      return keys.toString();
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      scopes.put(scope, bindings);
   }

   @Override
   public Bindings getBindings(int scope) {
      return scopes.get(scope);
   }

   @Override
   public void setAttribute(String name, Object value, int scope) {
      Bindings bindings = scopes.get(scope);
      if (bindings != null)
         bindings.put(name, value);
   }

   @Override
   public Object getAttribute(String name, int scope) {
      Bindings bindings = scopes.get(scope);
      return (bindings == null) ? null : bindings.get(name);
   }

   @Override
   public Object removeAttribute(String name, int scope) {
      Bindings bindings = scopes.get(scope);
      return (bindings == null) ? null : bindings.remove(name);
   }

   @Override
   public Object getAttribute(String name) {
      for (Bindings bindings : scopes.values()) {
         if (bindings.containsKey(name))
            return bindings.get(name);
      }
       return null;
   }

   @Override
   public int getAttributesScope(String name) {
      for (Map.Entry<Integer,Bindings> entry : scopes.entrySet()) {
         if (entry.getValue().containsKey(name))
            return entry.getKey();
      }
      return -1;
   }

   @Override
   public Writer getWriter() {
      return writer;
   }

   @Override
   public Writer getErrorWriter() {
      return error;
   }

   @Override
   public void setWriter(Writer writer) {
      this.writer = writer;
   }

   @Override
   public void setErrorWriter(Writer writer) {
      this.error = writer;
   }

   @Override
   public Reader getReader() {
      return reader;
   }

   @Override
   public void setReader(Reader reader) {
      this.reader = reader;
   }

   @Override
   public List<Integer> getScopes() {
      return new ArrayList<>(scopes.keySet());
   }

   private Method getSetter(Reference property) {
      if (property == null)
         return null;
      String name = property.getName();
      if (name != null && !name.trim().isEmpty()) {
         if (name.indexOf('.') < 0) {
            Object value = property.getValue();
            Class type = (value == null) ? null : value.getClass();
            String methodName = "set"+name.substring(0,1).toUpperCase()+name.substring(1);
            for (Method m : getClass().getMethods()) {
               if (m.getName().equals(methodName) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(type))
                  return m;
            }
         }
         else {
            try {
               Class type = Class.forName(name);
               for (Method m : getClass().getMethods()) {
                  if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(type))
                     return m;
               }
            }
            catch (ClassNotFoundException ex) {}
         }
      }
      return null;
   }
}
