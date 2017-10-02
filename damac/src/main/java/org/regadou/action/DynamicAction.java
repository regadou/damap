package org.regadou.action;

import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.script.DefaultScriptContext;

public class DynamicAction implements Action {

   private Configuration configuration;
   private String name;
   private String[] paramNames;
   private Class[] paramTypes;
   private Expression expression;

   public DynamicAction(Configuration configuration, Object[] params, Expression expression) {
      this.configuration = configuration;
      this.expression = expression;
      setParams(params);
   }

   @Override
   public Object execute(Object... parameters) {
      ScriptContext cx = new DefaultScriptContext(configuration.getEngineManager());
      Converter conv = configuration.getConverter();
      for (int p = 0; p < paramNames.length; p++) {
         Object value = (p < parameters.length) ? parameters[p] : null;
         cx.setAttribute(paramNames[p], conv.convert(value, paramTypes[p]), ScriptContext.ENGINE_SCOPE);
      }
      return expression.getValue(cx);
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public String getName() {
      return (name == null) ? Action.super.getName() : name;
   }

   @Override
   public Class getReturnType() {
      //TODO: detect return type by checking return type of last expression
      return Object.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return paramTypes;
   }

   public void setName(String name) {
      if (this.name == null)
         this.name = name;
   }

   private void setParams(Object[] src) {
      if (paramNames == null) {
         paramNames = new String[0];
         paramTypes = new Class[0];
      }
      else {
         paramNames = new String[src.length];
         paramTypes = new Class[src.length];
         for (int p = 0; p < src.length; p++)
            setParam(src[p], p);
      }
   }

   private void setParam(Object param, int index) {
      if (param == null) {
         paramNames[index] = "p"+index;
         paramTypes[index] = Object.class;
      }
      else if (param instanceof Reference) {
         Reference r = (Reference)param;
         paramNames[index] = r.getId();
         paramTypes[index] = configuration.getConverter().convert(r.getValue(), Class.class);
      }
      else if (param instanceof Map.Entry) {
         Map.Entry e = (Map.Entry)param;
         paramNames[index] = e.getKey().toString();
         paramTypes[index] = configuration.getConverter().convert(e.getValue(), Class.class);
      }
      else {
         paramNames[index] = param.toString();
         paramTypes[index] = Object.class;
      }
   }
}
