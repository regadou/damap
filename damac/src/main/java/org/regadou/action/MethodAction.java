package org.regadou.action;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.Action;
import org.regadou.damai.Converter;

public class MethodAction implements Action {

   private Converter converter;
   private List<Method> methods;
   private String name;
   private Class returnType;
   private Class[] parameters;

   public MethodAction(Converter converter, Method method) {
      this.converter = converter;
      name = method.getName();
      methods = Arrays.asList(method);
      returnType = method.getReturnType();
      parameters = method.getParameterTypes();
   }

   public MethodAction(Converter converter, Class type, String name) throws NoSuchMethodException {
      this.converter = converter;
      this.name = name;
      methods = new ArrayList<>();
      for (Method m : type.getMethods()) {
         if (m.getName().equals(name)) {
            methods.add(m);
            if (returnType == null || m.getReturnType().isAssignableFrom(returnType))
               returnType = m.getReturnType();
            else if (!returnType.isAssignableFrom(m.getReturnType()))
               returnType = Object.class;
            if (parameters == null ||  m.getParameterCount() > parameters.length)
               parameters = m.getParameterTypes();
         }
      }
      if (methods.isEmpty())
         throw new NoSuchMethodException("Method "+name+" not found in class "+type.getName());
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public boolean equals(Object that) {
      return that instanceof MethodAction && ((MethodAction)that).methods.equals(methods);
   }

   @Override
   public int hashCode() {
      return methods.hashCode();
   }

   @Override
   public Object execute(Object ... parameters) {
      Object target = null;
      if (parameters.length > 0) {
         List p = new ArrayList(Arrays.asList(parameters));
         target = p.remove(0);
         parameters = p.toArray();
      }
      switch (methods.size()) {
         case 0:
            return null;
         case 1:
            return executeMethod(methods.get(0), target, parameters);
         default:
            Method m = findBestMethod(parameters);
            return (m == null) ? null : executeMethod(m, target, parameters);
      }
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Class getReturnType() {
      return returnType;
   }

   @Override
   public Class[] getParameterTypes() {
      return parameters;
   }

   private Object executeMethod(Method method, Object target, Object[] params) {
      Class[] types = method.getParameterTypes();
      Object[] args = (params.length == types.length) ? null : new Object[types.length];
      for (int p = 0; p < types.length; p++) {
         Object param = (p >= params.length) ? null : params[p];
         if (param == null || !types[p].isInstance(param)) {
            param = converter.convert(param, types[p]);
            if (args == null) {
               args = new Object[types.length];
               for (int a = 0; a < p; a++)
                  args[a] = params[a];
            }
            args[p] = param;
         }
         else if (args != null)
            args[p] = param;
      }
      if (args == null)
         args = params;
      if (!method.getDeclaringClass().isInstance(target))
         target = converter.convert(target, method.getDeclaringClass());
      try { return method.invoke(target, args); }
      catch (Exception e) {
         RuntimeException rte = (e instanceof RuntimeException)
                              ? (RuntimeException)e
                              : new RuntimeException(e);
         throw rte;
      }
   }

   private Method findBestMethod(Object[] params) {
      List<Method> same = new ArrayList<>();
      List<Method> longer = new ArrayList<>();
      Method other = null;
      for (Method m : methods) {
         int n = m.getParameterCount();
         if (n == params.length)
            same.add(m);
         else if (n > params.length)
            longer.add(m);
         else if (other == null || n > other.getParameterCount())
            other = m;
      }

      switch (same.size()) {
         case 0:
            break;
         case 1:
            return same.get(0);
         default:
            Method m = bestMatch(same, params);
            if (m != null)
               return m;
      }

      switch (longer.size()) {
         case 0:
            return other;
         case 1:
            return longer.get(0);
         default:
            Method m = bestMatch(same, params);
            return (m != null) ? m : other;
      }
   }

   private Method bestMatch(List<Method> methods, Object[] params) {
      Method bestMethod = null;
      int bestScore = -1;
      int bestNulls = 0;
      for (Method m : methods) {
         int score = 0;
         int nulls= 0;
         Class[] types = m.getParameterTypes();
         for (int p = 0; p < params.length; p++) {
            Object param = params[p];
            if (param == null)
               nulls++;
            else if (types[p].isInstance(param))
               score++;
         }
         if (score > bestScore || (score == bestScore && nulls > bestNulls)) {
            bestMethod = m;
            bestScore = score;
            bestNulls = nulls;
         }
      }
      return bestMethod;
   }
}
