package org.regadou.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.script.ScriptContext;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.GenericReference;

public class ScriptContextMap implements Map<String,Object> {

   private ScriptContextFactory factory;

   public ScriptContextMap(ScriptContextFactory factory) {
      this.factory = factory;
   }

   @Override
   public int size() {
      return keySet().size();
   }

   @Override
   public boolean isEmpty() {
      return keySet().isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return keySet().contains(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   @Override
   public Object get(Object key) {
      ScriptContext cx = factory.getScriptContext();
      int scope = getScope(cx, key);
      return (scope < 0) ? null : cx.getAttribute(key.toString(), scope);
   }

   @Override
   public Object put(String key, Object value) {
      ScriptContext cx = factory.getScriptContext();
      int scope = cx.getAttributesScope(key);
      if (scope < 0)
         scope = cx.getScopes().get(0);
      Object old = cx.getAttribute(key, scope);
      cx.setAttribute(key, value, scope);
      return old;
   }

   @Override
   public Object remove(Object key) {
      ScriptContext cx = factory.getScriptContext();
      int scope = getScope(cx, key);
      if (scope >= 0) {
         String name = key.toString();
         Object old = cx.getAttribute(name, scope);
         cx.removeAttribute(name, scope);
         return old;
      }
      return null;
   }

   @Override
   public void putAll(Map<? extends String, ? extends Object> m) {
      if (m != null) {
         for (String key : m.keySet())
            put(key, m.get(key));
      }
   }

   @Override
   public void clear() {
      ScriptContext cx = factory.getScriptContext();
      for (Integer scope : cx.getScopes())
         cx.getBindings(scope).clear();
   }

   @Override
   public Set<String> keySet() {
      ScriptContext cx = factory.getScriptContext();
      Set<String> keys = new TreeSet<>();
      for (Integer scope : cx.getScopes())
         keys.addAll(cx.getBindings(scope).keySet());
      return keys;
   }

   @Override
   public Collection values() {
      ScriptContext cx = factory.getScriptContext();
      Collection values = new ArrayList();
      for (String key : keySet())
         values.add(cx.getAttribute(key));
      return values;
   }

   @Override
   public Set<Entry<String, Object>> entrySet() {
      ScriptContext cx = factory.getScriptContext();
      Set<Entry<String, Object>> entries = new LinkedHashSet<>();
      for (String key : keySet())
         entries.add(new GenericReference(key, cx.getAttribute(key), true).toMapEntry());
      return entries;
   }

   private int getScope(ScriptContext cx, Object key) {
      if (key == null)
         return -1;
      if (cx == null)
         cx = factory.getScriptContext();
      return cx.getAttributesScope(key.toString());
   }
}
