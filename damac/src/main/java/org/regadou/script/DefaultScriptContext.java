package org.regadou.script;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Action;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;
import org.regadou.collection.ArrayWrapper;

public class DefaultScriptContext implements ScriptContext {

   private Map<Integer,Bindings> scopes = new TreeMap<>();
   private Reader reader;
   private Writer writer;
   private Writer error;

   public DefaultScriptContext() {
   }

   public DefaultScriptContext(Object data) {
      setContext(data);
   }

   public DefaultScriptContext(ScriptContext cx) {
      reader = cx.getReader();
      writer = cx.getWriter();
      error = cx.getErrorWriter();
      for (Integer scope : cx.getScopes())
         scopes.put(scope, cx.getBindings(scope));
   }

   public DefaultScriptContext(ScriptEngineManager manager, Reference ... properties) {
      if (properties != null) {
         for (Reference property : properties)
            setProperty(property);
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

   private void setProperty(Reference property) {
      Method m = getSetter(property);
      if (m != null) {
         try { m.invoke(this, property.getValue()); }
         catch (IllegalArgumentException|IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
         }
      }
}

   private Method getSetter(Reference property) {
      if (property == null)
         return null;
      String name = property.getId();
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

   private void setContext(Object value) {
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      if (value == null || value instanceof Number || value instanceof Boolean || value instanceof Action
                        || value instanceof CharSequence || value instanceof Class)
         ;
      else if (value instanceof Map) {
         Map map = (Map)value;
         for (Object key : map.keySet()) {
            if (key instanceof CharSequence) {
               Integer scope = null;
               String name = key.toString();
               switch (name) {
                  case "engine":
                  case "request":
                     scope = ScriptContext.ENGINE_SCOPE;
                     break;
                  case "user":
                  case "session":
                     scope = HttpScriptContext.SESSION_SCOPE;
                     break;
                  case "global":
                  case "servlet":
                  case "app":
                  case "application":
                  case "singleton":
                     scope = ScriptContext.GLOBAL_SCOPE;
                     break;
                  default:
                     setProperty(new GenericReference(name, map.get(key)));
                     continue;
               }
               setScope(scope, map.get(key));
            }
         }
      }
      else if (value.getClass().isArray()) {
         for (Object e : new ArrayWrapper(value))
            setContext(e);
      }
      else if (value instanceof Collection) {
         for (Object e : (Collection)value)
            setContext(e);
      }
      else
         setContext(new BeanMap(value));
   }

   private void setScope(int scope, Object value) {
      if (value == null || value instanceof Number || value instanceof Boolean || value instanceof Action
                        || value instanceof CharSequence || value instanceof Class)
         ;
      else if (value.getClass().isArray()) {
         for (Object e : new ArrayWrapper(value))
            setScope(scope, e);
      }
      else if (value instanceof Collection) {
         for (Object e : (Collection)value)
            setScope(scope, e);
      }
      else {
         Bindings b = getBindings(scope);
         if (b == null)
            setBindings(b = new SimpleBindings(), scope);
         Map map = (value instanceof Map) ? (Map)value : new BeanMap(value);
         for (Object key : map.keySet()) {
            if (key instanceof CharSequence)
               b.put(key.toString(), map.get(key));
         }
      }
   }
}
