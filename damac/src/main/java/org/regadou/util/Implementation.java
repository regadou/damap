package org.regadou.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.regadou.damai.Action;

/**
 * This class is a work in progress for now
 * It will eventually be able to implement any interface based on the map given in the constructor
 * Keys in the map can be either properties where this implementation will look for getters and setters
 * or method names in which cases corresponding values must be convertible to an org.regadou.damai.Action implementation
 */
public class Implementation implements InvocationHandler {

   private static final Map<Class, Class> primitives = new LinkedHashMap<>();
   static {
      Class[] wrappers = new Class[]{
         Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
         Boolean.class, Character.class
      };
      try {
         for (Class wrapper : wrappers)
            primitives.put((Class)wrapper.getField("TYPE").get(null), wrapper);
      }
      catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private Class implemented;
   private String fieldPrefix;
   private Map<String,Object> properties = new LinkedHashMap<>();
   private Map<Method,Object> methods = new LinkedHashMap<>();

   public Implementation(Class implemented, Map properties) {
      this.implemented = implemented;
      this.fieldPrefix = implemented.getName() + ".";
      if (properties == null || properties.isEmpty())
         properties = System.getProperties();
      for (Object key : properties.keySet()) {
         if (key != null) {
            String name = key.toString().trim();
            if (!name.isEmpty()) {
               Object value = properties.get(key);
               if (name.startsWith(fieldPrefix))
                  name = name.substring(fieldPrefix.length());
               else if (name.contains(".")) {
                  try {
                     Class type = Class.forName(name);
                     for (Method method : implemented.getMethods()) {
                        if (method.getReturnType().isAssignableFrom(type)) {
                           name = method.getName();
                           value = newInstance(type);
                           break;
                        }
                     }
                  }
                  catch (ClassNotFoundException e) { continue; }
               }
               this.properties.put(name, value);
            }
         }
      }
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (!methods.containsKey(method)) {
         String name = method.getName();
         if (properties.containsKey(name))
            methods.put(method, properties.get(name));
         else {
         //TODO: detect if it is a getter, setter or other method
         }
      }
      Class returnType = method.getReturnType();
      Object value = methods.get(method);
      if (value instanceof Action && returnType.isAssignableFrom(Action.class))
         return convert(((Action)value).execute(args), returnType);
      //TODO: how can we pass args if any ?
      Object result = convert(value, returnType);
      if (result != null && value != result)
         methods.put(method, result);
      return result;
   }

   private Object convert(Object value, Class type) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      if (type.isAssignableFrom(Void.class))
         return null;
      Class valueType = (value == null) ? Void.class : value.getClass();
      if (type.isAssignableFrom(valueType))
         return value;
      if (type == Class.class) {
         try { return (value == null) ? Void.class : Class.forName(value.toString()); }
         catch (ClassNotFoundException e) { return null; }
      }
      if (type.isInterface() && value != null) {
         try {
            Class impl = (value instanceof Class) ? (Class)value : Class.forName(value.toString());
            return newInstance(impl);
         }
         catch (ClassNotFoundException e) {}
      }
      if (type.isPrimitive())
         return convert((value == null) ? "0" : value.toString(), primitives.get(type));
      if (type.isArray())
         return toArray(value, type.getComponentType());
      for (Constructor c : type.getConstructors()) {
         Class[] params = c.getParameterTypes();
         switch (params.length) {
            case 0:
               if (value == null)
                  return c.newInstance();
               break;
            case 1:
               if (value != null && params[0].isAssignableFrom(valueType))
                  return c.newInstance(value);
         }
      }
      return null;
   }

   private Object newInstance(Class type) {
      Constructor[] constructors = type.getConstructors();
      switch (constructors.length) {
         case 0:
            return null;
         case 1:
            return newInstance(constructors[0]);
         default:
            for (Constructor c : constructors) {
               Object result = newInstance(c);
               if (result != null)
                  return result;
            }
            return null;
      }
   }

   private Object newInstance(Constructor c) {
      Class[] types = c.getParameterTypes();
      Object[] params = new Object[types.length];
      for (int p = 0; p < params.length; p++) {
         //TODO: convert value as implementer of interface (class or toString().toClass())
         //check for probable dependency injection in constructor
      }
      try { return c.newInstance(params); }
      catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
         return null;
      }
   }

   private Object toArray(Object src, Class subtype) {
      if (src instanceof Collection)
         src = ((Collection)src).toArray();
      else if (src instanceof CharSequence)
         src = src.toString().trim().split(",");
      else if (src == null)
         src = new Object[0];
      else if (src.getClass().isArray())
         ;
      else
         src = new Object[]{src};

      int length = Array.getLength(src);
      Object dst = Array.newInstance(subtype, length);
      for (int i = 0; i < length; i++) {
         try { Array.set(dst, i, convert(Array.get(src, i), subtype)); }
         catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {}
      }
      return dst;
   }
}
