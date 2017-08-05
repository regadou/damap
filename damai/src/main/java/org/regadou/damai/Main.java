package org.regadou.damai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.activation.FileTypeMap;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

public class Main implements ScriptContextFactory, ConverterManager, Configuration {

   public static void main(String[] args) {
      Main config = new Main(args);
      //This is only for testing for now
      for (Method m : config.getClass().getDeclaredMethods()) {
         if (m.getParameterCount() > 0 || !Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers()))
            continue;
         System.out.print(m.getName() + " = ");
         try { System.out.println(config.convert(m.invoke(config), String.class)); }
         catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
         }
      }
   }

   private static final String PROPERTY_PREFIX = Configuration.class.getName() + ".";

   private static final Map<Class, Class> PRIMITIVES_MAP = new LinkedHashMap<>();
   static {
      Class[] wrappers = new Class[]{
         Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
         Boolean.class, Character.class
      };
      try {
         for (Class wrapper : wrappers)
            PRIMITIVES_MAP.put((Class)wrapper.getField("TYPE").get(null), wrapper);
      }
      catch (NoSuchFieldException|SecurityException|IllegalArgumentException|IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private final ThreadLocal<ScriptContext> currentContext = new ThreadLocal() {
      @Override
      protected synchronized ScriptContext initialValue() { return null; }
   };

   // first key is target class and second key is source class
   private final Map<Class,Map<Class,Converter>> converters = new LinkedHashMap<>();
   private final Map<String,Object> properties = new LinkedHashMap<>();
   private URL[] classpath;
   private ClassLoader classLoader;
   private boolean loadingClassLoader;
   private ScriptContextFactory contextFactory;
   private ConverterManager converterManager;
   private ScriptEngineManager engineManager;
   private MimeHandlerFactory handlerFactory;
   private PropertyFactory propertyFactory;
   private ResourceManager resourceManager;
   private FileTypeMap typeMap;

   public Main(String[] args) {
      Properties props = null;
      for (String arg : args) {
         if (props == null)
            props = new Properties();
         try { props.load(getReader(arg)); }
         catch (IOException e) { throw new RuntimeException(e); }
      }
      setProperties(properties);
   }

   public Main(Map properties) {
      setProperties(properties);
   }

   @Override
   public ScriptContext getScriptContext(Reference...properties) {
      ScriptContext cx = currentContext.get();
      if (cx == null) {
         cx = new SimpleScriptContext();
         currentContext.set(cx);
      }
      return cx;
   }

   @Override
   public boolean closeScriptContext(ScriptContext context) {
      if (currentContext.get() == context) {
         currentContext.set(null);
         return true;
      }
      return false;
   }

   @Override
   public <S, T> Converter<S, T> getConverter(Class<S> sourceClass, Class<T> targetClass) {
      Map<Class,Converter> map = converters.get(targetClass);
      if (map != null) {
         Converter c = map.get(sourceClass);
         if (c != null)
            return c;
      }
      return value -> {
         try { return (T)convert(value, targetClass); }
         catch (Exception e) {
            RuntimeException rte = (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
            throw rte;
         }
      };
   }

   @Override
   public <S, T> void registerConverter(Class<S> sourceClass, Class<T> targetClass, Converter<S, T> converter) {
      Map<Class,Converter> map = converters.get(targetClass);
      if (map == null) {
         map = new LinkedHashMap<>();
         converters.put(targetClass, map);
      }
      map.put(sourceClass, converter);
   }

   @Override
   public URL[] getClasspath() {
      if (classpath == null)
         classpath = findProperty(URL[].class, "classpath");
      return classpath;
   }

   @Override
   public ScriptContextFactory getContextFactory() {
      if (contextFactory == null) {
         contextFactory = findProperty(ScriptContextFactory.class, "contextFactory");
         if (contextFactory == null)
            contextFactory = this;
      }
      return contextFactory;
   }

   @Override
   public ConverterManager getConverterManager() {
      if (converterManager == null) {
         converterManager = findProperty(ConverterManager.class, "converterManager");
         if (converterManager == null)
            converterManager = this;
      }
      return converterManager;
   }

   @Override
   public ScriptEngineManager getEngineManager() {
      if (engineManager == null)
         engineManager = findProperty(ScriptEngineManager.class, "engineManager");
      return engineManager;
   }

   @Override
   public MimeHandlerFactory getHandlerFactory() {
      if (handlerFactory == null)
         handlerFactory = findProperty(MimeHandlerFactory.class, "handlerFactory");
      return handlerFactory;
   }

   @Override
   public PropertyFactory getPropertyFactory() {
      if (propertyFactory == null)
         propertyFactory = findProperty(PropertyFactory.class, "propertyFactory");
      return propertyFactory;
   }

   @Override
   public ResourceManager getResourceManager() {
      if (resourceManager == null)
         resourceManager = findProperty(ResourceManager.class, "resourceManager");
      return resourceManager;
   }

   @Override
   public FileTypeMap getTypeMap() {
      if (typeMap == null) {
         typeMap = findProperty(FileTypeMap.class, "typeMap");
         if (typeMap == null)
            typeMap = FileTypeMap.getDefaultFileTypeMap();
      }
      return typeMap;
   }

   public ClassLoader getClassLoader() {
      if (classLoader == null) {
         loadingClassLoader = true;
         classLoader = new URLClassLoader(getClasspath());
         loadingClassLoader = false;
      }
      return classLoader;
   }

   private void setProperties(Map properties) {
      if (properties == null || properties.isEmpty())
         properties = System.getProperties();
      this.properties.put(Configuration.class.getName(), this);
      for (Object key : properties.keySet()) {
         if (key != null) {
            String name = key.toString().trim();
            if (!name.isEmpty())
               this.properties.put(name, properties.get(key));
         }
      }
   }

   private <T> T findProperty(Class<T> type, String name) {
      Object value = properties.get(type.getName());
      if (value == null && name != null)
         value = properties.get(PROPERTY_PREFIX + name);
      if (value != null && value instanceof CharSequence && value.toString().trim().isEmpty())
         value = null;
      try { return (T)convert(value, type); }
      catch (Exception e) {
         RuntimeException rte = (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
         throw rte;
      }
   }

   private Object convert(Object value, Class type) throws Exception {
      if (type.isAssignableFrom(Void.class))
         return null;
      Class valueType = (value == null) ? Void.class : value.getClass();
      if (type.isAssignableFrom(valueType))
         return value;
      if (type == String.class)
         return toString(value);
      if (type == URL.class)
         return toUrl(value);
      if (type == Class.class)
         return toClass(value);
      if (type.isPrimitive())
         return convert((value == null) ? "0" : value.toString(), PRIMITIVES_MAP.get(type));
      if (type.isArray())
         return toArray(value, type.getComponentType());
      if (type.isInterface() || Modifier.isAbstract(type.getModifiers()))
         return newInstance(toClass(value));
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
      if (type == null)
         return null;
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
      for (int p = 0; p < params.length; p++)
         params[p] = findProperty(types[p], null);
      try { return c.newInstance(params); }
      catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
         return null;
      }
   }

   private Object toArray(Object src, Class subtype) {
      if (src instanceof Collection)
         src = ((Collection)src).toArray();
      else if (src instanceof CharSequence) {
         String txt = src.toString().trim();
         src = txt.isEmpty() ? new String[0] : txt.split(",");
      }
      else if (src == null)
         src = new Object[0];
      else if (!src.getClass().isArray())
         src = new Object[]{src};

      int length = Array.getLength(src);
      Object dst = Array.newInstance(subtype, length);
      for (int i = 0; i < length; i++) {
         try { Array.set(dst, i, convert(Array.get(src, i), subtype)); }
         catch (Exception e) {}
      }
      return dst;
   }

   private String toString(Object value) {
      if (value == null)
         return "null";
      Class type = value.getClass();
      if (type == Class.class)
         return ((Class)type).getName();
      if (value instanceof char[])
         return new String((char[])value);
      if (type.isArray())
         return "("+String.join(" ", (String[])toArray(value, String.class))+")";
      return value.toString();
   }

   private URL toUrl(Object value) throws MalformedURLException {
      if (value instanceof URL)
         return (URL)value;
      if (value instanceof URI)
         return ((URI)value).toURL();
      if (value instanceof File)
         return ((File)value).toURI().toURL();
      if (value == null)
         return null;
      String txt = value.toString().trim();
      try { return new URL(txt); }
      catch (MalformedURLException e) { return new File(txt).toURI().toURL(); }
   }

   private Class toClass(Object value)  {
      if (value instanceof Class)
         return (Class)value;
      if (value == null)
         return null;
      String className = value.toString().trim();
      try { return Class.forName(className); }
      catch (ClassNotFoundException e) {
         if (loadingClassLoader)
            throw new RuntimeException("getClassLoader() is reentrant while converting "+className+" to class");
         try { return getClassLoader().loadClass(className); }
         catch (ClassNotFoundException e2) { return null; }
      }
   }

   private Reader getReader(String path) throws IOException {
      InputStream input;
      try { input = new URL(path).openStream(); }
      catch (MalformedURLException ex) {
         input = getClass().getResourceAsStream(path);
         if (input == null)
            input = new FileInputStream(path);
      }
      return new InputStreamReader(input);
   }
}
