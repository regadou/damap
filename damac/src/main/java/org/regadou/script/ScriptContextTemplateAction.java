package org.regadou.script;

import java.util.Collection;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.util.ArrayWrapper;

public class ScriptContextTemplateAction implements Action {

   private static final Class[] PARAMETERS = new Class[]{ScriptContext.class, Object.class};

   private Configuration configuration;

   public ScriptContextTemplateAction(Configuration configuration) {
      this.configuration = configuration;
   }

   public String getName() {
      return "set_context";
   }

   public Class getReturnType() {
      return Object.class;
   }

   public Class[] getParameterTypes() {
      return null;
   }

   @Override
   public Object execute(Object... parameters) {
      ScriptContext cx = null;
      Object value = null;
      if (parameters != null) {
         switch (parameters.length) {
            default:
            case 2:
               value = parameters[1];
            case 1:
               Object p = parameters[0];
               while (p instanceof Reference)
                  p = ((Reference)p).getValue();
               if (p instanceof ScriptContext)
                  cx = (ScriptContext)p;
            case 0:
         }
      }
      if (cx == null)
         cx = configuration.getContextFactory().getScriptContext();
      setContext(cx, value);
      return value;
   }

   private void setContext(ScriptContext cx, Object value) {
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
               }
               if (scope != null)
                  setScope(cx, scope, map.get(key));
            }
         }
      }
      else if (value.getClass().isArray()) {
         for (Object e : new ArrayWrapper(value))
            setContext(cx, e);
      }
      else if (value instanceof Collection) {
         for (Object e : (Collection)value)
            setContext(cx, e);
      }
      else
         setContext(cx, new BeanMap(value));
   }

   private void setScope(ScriptContext cx, int scope, Object value) {
      if (value == null || value instanceof Number || value instanceof Boolean || value instanceof Action
                        || value instanceof CharSequence || value instanceof Class)
         ;
      else if (value.getClass().isArray()) {
         for (Object e : new ArrayWrapper(value))
            setScope(cx, scope, e);
      }
      else if (value instanceof Collection) {
         for (Object e : (Collection)value)
            setScope(cx, scope, e);
      }
      else {
         Bindings b = cx.getBindings(scope);
         if (b == null)
            cx.setBindings(b = new SimpleBindings(), scope);
         Map map = (value instanceof Map) ? (Map)value : new BeanMap(value);
         for (Object key : map.keySet()) {
            if (key instanceof CharSequence)
               b.put(key.toString(), map.get(key));
         }
      }
   }
}
