package org.regadou.factory;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;

public class ScriptContextPropertyFactory implements PropertyFactory<ScriptContext> {

   @Override
   public Property getProperty(ScriptContext cx, String name) {
      int scope = cx.getAttributesScope(name);
      return (scope < 0) ? null : getContextProperty(cx, name, scope);
   }

   @Override
   public String[] getProperties(ScriptContext cx) {
      Set<String> names = new TreeSet<>();
      for (Integer scope : cx.getScopes()) {
         Bindings b = cx.getBindings(scope);
         if (b != null)
            names.addAll(b.keySet());
      }
      return names.toArray(new String[names.size()]);
   }

   @Override
   public Property addProperty(ScriptContext cx, String name, Object value) {
      if (name == null) {
         if (value instanceof Reference) {
            Reference ref = (Reference)value;
            name = ref.getId();
            if (name == null)
               ref.getType().getName();
         }
         else
            name = (value == null) ? "value" : value.getClass().getName();
      }
      int scope = cx.getAttributesScope(name);
      if (scope < 0) {
         List<Integer> scopes = cx.getScopes();
         if (scopes.isEmpty()) {
            scope = ScriptContext.ENGINE_SCOPE;
            cx.setBindings(new SimpleBindings(), scope);
         }
         scope = scopes.get(0);
         cx.setAttribute(name, value, scope);
         return getContextProperty(cx, name, scope);
      }
      return null;
   }

   @Override
   public boolean removeProperty(ScriptContext cx, String name) {
      int scope = cx.getAttributesScope(name);
      if (scope >= 0) {
         cx.removeAttribute(name, scope);
         return cx.getAttributesScope(name) != scope;
      }
      return false;
   }

   private Property getContextProperty(ScriptContext cx, String name, int scope) {
      return new Property<ScriptContext,Object>() {

         @Override
         public String toString() {
            return name;
         }
         @Override
         public ScriptContext getOwner() {
            return cx;
         }

         @Override
         public Class<ScriptContext> getOwnerType() {
            return ScriptContext.class;
         }

         @Override
         public String getId() {
            return name;
         }

         @Override
         public Class<Object> getType() {
            return Object.class;
         }

         @Override
         public Object getValue() {
            return cx.getAttribute(name, scope);
         }

         @Override
         public void setValue(Object value) {
            cx.setAttribute(name, value, scope);
         }
      };
   }
}
