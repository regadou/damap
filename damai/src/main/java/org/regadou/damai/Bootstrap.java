package org.regadou.damai;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Function;
import javax.activation.FileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public class Bootstrap implements Configuration, Converter {

   private static boolean DEBUG = false;

   public static void main(String[] args) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      Writer writer = new OutputStreamWriter(System.out);
      Configuration config = new Bootstrap(checkDebugArg(args, reader));
      ScriptContext cx = config.getContextFactory().getScriptContext();
      cx.setReader(reader);
      cx.setWriter(writer);
      cx.setErrorWriter(new OutputStreamWriter(System.err));
      if (DEBUG)
         printDebugInfo(config);
      URL init = config.getInitScript();
      if (init != null) {
         Reference r = config.getResourceManager().getResource(init.toString());
         System.out.println(r.getValue());
      }
   }

   public static void printDebugInfo(Configuration config) throws IOException {
      for (Method m : config.getClass().getDeclaredMethods()) {
         int mod = m.getModifiers();
         if (m.getParameterCount() == 0 && Modifier.isPublic(mod) && !Modifier.isStatic(mod)) {
            Object value;
            try {
               value = m.invoke(config);
               if (value instanceof Object[])
                  value = Arrays.asList((Object[])value);
            }
            catch (Exception e) { value = e; }
            String name = m.getName();
            if (name.startsWith("get"))
               name = name.substring(3);
            System.out.println(name+" = "+value);
         }
      }
      System.out.println("uri schemes = "+Arrays.asList(config.getResourceManager().getSchemes()));
      System.out.println("mime types = "+config.getHandlerFactory().getMimetypes());
      Collection<String> engines = new TreeSet<>();
      for (ScriptEngineFactory factory : config.getEngineManager().getEngineFactories())
         engines.add(factory.getEngineName());
      System.out.println("script engines = "+engines);
   }

   private static final String PROPERTY_PREFIX = Configuration.class.getName() + ".";

   private static final List<Class> DECIMAL_NUMBERS = Arrays.asList(
      Float.class, Double.class, BigDecimal.class
   );

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

   private static String[] checkDebugArg(String[] src, BufferedReader reader) {
      List<String> dst = new ArrayList<>(Arrays.asList(src));
      Iterator<String> it = dst.iterator();
      while (it.hasNext()) {
         String arg = it.next();
         while (arg.startsWith("-"))
            arg = arg.substring(1);
         if (arg.equals("debug")) {
            it.remove();
            DEBUG = true;
         }
      }
      if (DEBUG) {
         try {
            System.out.println("*** press enter after starting debugger ***");
            reader.readLine();
         }
         catch (IOException e) { throw new RuntimeException(e); }
         return dst.toArray(new String[dst.size()]);
      }
      else
         return src;
   }

   // first key is targetClass, second key is sourceClass
   private Map<Class,Map<Class,Function>> converters = new LinkedHashMap<>();
   private final Map<String,Object> properties = new LinkedHashMap<>();
   private URL[] classpath;
   private Bindings globalScope;
   private URL initScript;
   private ScriptContextFactory contextFactory;
   private Converter converterManager;
   private ScriptEngineManager engineManager;
   private MimeHandlerFactory handlerFactory;
   private PropertyManager propertyManager;
   private ResourceManager resourceManager;
   private FileTypeMap typeMap;
   private ClassLoader classLoader;

   public Bootstrap(String...args) {
      Properties properties = null;
      for (String arg : args) {
         if (arg == null || arg.trim().isEmpty())
            continue;
         if (properties == null)
            properties = new Properties();
         try { properties.load(toReader(arg)); }
         catch (IOException e) { throw new RuntimeException(e); }
      }
      setProperties(properties);
   }

   public Bootstrap(Map properties) {
      setProperties(properties);
   }

   @Override
   public <S, T> void registerFunction(Class<S> sourceClass, Class<T> targetClass, Function<S, T> function) {
      Map<Class,Function> functions = converters.get(targetClass);
      if (functions == null) {
         functions = new LinkedHashMap<>();
         converters.put(targetClass, functions);
      }
      functions.put(sourceClass, function);
   }

   @Override
   public URL[] getClasspath() {
      if (classpath == null)
         classpath = findProperty(URL[].class, "classpath");
      return classpath;
   }

   @Override
   public Bindings getGlobalScope() {
      if (globalScope == null)
         globalScope = findProperty(Bindings.class, "globalScope");
      return globalScope;
   }

   @Override
   public URL getInitScript() {
      if (initScript == null)
         initScript = findProperty(URL.class, "initScript");
      return initScript;
   }

   @Override
   public ScriptContextFactory getContextFactory() {
      if (contextFactory == null)
         contextFactory = findProperty(ScriptContextFactory.class, "contextFactory");
      return contextFactory;
   }

   @Override
   public Converter getConverter() {
      if (converterManager == null) {
         converterManager = findProperty(Converter.class, "converterManager");
         if (converterManager == null)
            converterManager = this;
      }
      return converterManager;
   }

   @Override
   public ScriptEngineManager getEngineManager() {
      if (engineManager == null) {
         engineManager = findProperty(ScriptEngineManager.class, "engineManager");
         if (engineManager != null) {
            Bindings global = getGlobalScope();
            if (global != null)
               engineManager.setBindings(global);
            else
               global = engineManager.getBindings();
            String key = Configuration.class.getName();
            if (!global.containsKey(key))
               global.put(key, this);
         }
      }
      return engineManager;
   }

   @Override
   public MimeHandlerFactory getHandlerFactory() {
      if (handlerFactory == null)
         handlerFactory = findProperty(MimeHandlerFactory.class, "handlerFactory");
      return handlerFactory;
   }

   @Override
   public PropertyManager getPropertyManager() {
      if (propertyManager == null)
         propertyManager = findProperty(PropertyManager.class, "propertyManager");
      return propertyManager;
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

   @Override
   public <T> T convert(Object value, Class<T> type) {
      if (type == null || Void.class.isAssignableFrom(type))
         return null;
      if (value instanceof Reference && !type.isAssignableFrom(value.getClass())) {
         do {
            value = ((Reference)value).getValue();
         } while (value instanceof Reference);
      }
      Class valueType = (value == null) ? Void.class : value.getClass();
      if (type.isAssignableFrom(valueType))
         return (T)value;
      Map<Class,Function> functions = converters.get(type);
      if (functions != null) {
         for (Class srcType : functions.keySet()) {
            if (srcType.isInstance(valueType))
               return (T)functions.get(srcType).apply(value);
         }
      }
      if (CharSequence.class.isAssignableFrom(type))
         return (T)toString(value);
      if (Number.class.isAssignableFrom(type))
         return (T)toNumber(value, type);
      if (Boolean.class.isAssignableFrom(type))
         return (T)toBoolean(value);
      if (URL.class.isAssignableFrom(type))
         return (T)toUrl(value);
      if (Class.class.isAssignableFrom(type))
         return (T)toClass(value);
      if (InputStream.class.isAssignableFrom(type))
         return (T)toInputStream(value);
      if (Reader.class.isAssignableFrom(type))
         return (T)toReader(value);
      if (Collection.class.isAssignableFrom(type)) {
         if (value instanceof Collection)
            return (T)value;
         return (T)Arrays.asList((Object[])toArray(value, Object.class));
      }
      if (Map.class.isAssignableFrom(type))
         return (T)(value instanceof Map ? value : toMap(value));
      if (type.isPrimitive())
         return (T)convert((value == null) ? "0" : value.toString(), PRIMITIVES_MAP.get(type));
      if (type.isArray())
         return (T)toArray(value, type.getComponentType());
      for (Constructor c : type.getConstructors()) {
         try {
            Class[] params = c.getParameterTypes();
            switch (params.length) {
               case 0:
                  if (value == null)
                     return (T)c.newInstance();
                  break;
               case 1:
                  if (value != null && params[0].isAssignableFrom(valueType))
                     return (T)c.newInstance(value);
            }
         }
         catch (Exception e) {}
      }
      return (T)newInstance(toClass(value));
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
      if (type == null)
         type = (Class<T>)Object.class;
      Object value = properties.get(type.getName());
      if (value == null && name != null)
         value = properties.get(PROPERTY_PREFIX + name);
      if (value != null && value instanceof CharSequence && value.toString().trim().isEmpty())
         value = null;
      if (value == null && type.isInterface())
         return (T)(type.isAssignableFrom(this.getClass()) ? this : null);
      try { return (T)convert(value, type); }
      catch (Exception e) {
         RuntimeException rte = (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
         throw rte;
      }
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
         params[p] = getConfig(types[p]);
      try { return c.newInstance(params); }
      catch (InstantiationException|IllegalAccessException|IllegalArgumentException|InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }

   private Object getConfig(Class type) {
      if (Configuration.class.isAssignableFrom(type))
         return this;
      for (Method method : getClass().getMethods()) {
         if (method.getParameterCount() == 0 && method.getReturnType().isAssignableFrom(type)) {
            try { return method.invoke(this); }
            catch (Exception e) { throw new RuntimeException(e); }
         }
      }
      return null;
   }

   private Object toArray(Object src, Class subtype) {
      if (src instanceof Collection)
         src = ((Collection)src).toArray();
      else if (src instanceof Map)
         src = ((Map)src).entrySet().toArray();
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

   private Map toMap(Object value) {
      if (value instanceof Map)
         return (Map)value;
      if (value == null)
         return new LinkedHashMap();
      if (value instanceof Collection) {
         Map map = new LinkedHashMap();
         for (Object e : (Collection)value) {
            addEntry(map, e);
         }
         return map;
      }
      if (value.getClass().isArray()) {
         Map map = new LinkedHashMap();
         int length = Array.getLength(value);
         for (int i = 0; i < length; i++)
            addEntry(map, Array.get(value, i));
         return map;
      }
      Class beanClass = toClass("org.apache.commons.beanutils.BeanMap");
      try { return (Map)beanClass.getConstructor(Object.class).newInstance(value); }
      catch (Exception e) { return null; }
   }

   private String toString(Object value) {
      if (value == null)
         return "null";
      Class type = value.getClass();
      if (type == Class.class)
         return ((Class)type).getName();
      if (value instanceof char[])
         return new String((char[])value);
      if (value instanceof Collection || value instanceof Map || type.isArray())
         return "("+String.join(" ", (String[])toArray(value, String.class))+")";
      return value.toString();
   }

   private Object toNumber(Object value, Class type) {
      String txt = (value == null) ? "0" : value.toString();
      try {
         if (!DECIMAL_NUMBERS.contains(type)) {
            int index = txt.indexOf('.');
            if (index >= 0)
               txt = txt.substring(0, index);
         }
         if (txt.trim().isEmpty())
            txt = "0";
         return type.getConstructor(String.class).newInstance(txt);
      }
      catch (Exception e) { return null; }
   }

   private Boolean toBoolean(Object value) {
      if (value instanceof Boolean)
         return (Boolean)value;
      if (value instanceof Number)
         return ((Number)value).doubleValue() != 0;
      if (value == null)
         return false;
      String txt = value.toString().trim().toLowerCase();
      switch (txt) {
         case "true":
         case "1":
            return true;
         case "false":
         case "0":
         case "":
            return false;
         default:
            return null;
      }
   }

   private URL toUrl(Object value) {
      try {
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
         catch (MalformedURLException e) {
            URL url = getClass().getResource(txt);
            return (url != null) ? url : new File(txt).toURI().toURL();
         }
      }
      catch (MalformedURLException e){ return null; }
   }

   private Class toClass(Object value)  {
      if (value instanceof Class)
         return (Class)value;
      if (value == null)
         return null;
      String className = value.toString().trim();
      try { return Class.forName(className); }
      catch (ClassNotFoundException e) {
         if (classLoader == null) {
            URL[] cp = getClasspath();
            classLoader = (cp == null || cp.length == 0) ? this.getClass().getClassLoader()
                                                         : new URLClassLoader(cp);
         }
         try { return classLoader.loadClass(className); }
         catch (ClassNotFoundException e2) { return null; }
      }
   }

   private InputStream toInputStream(Object value) {
      if (value instanceof InputStream)
         return (InputStream)value;
      if (value instanceof CharSequence || value instanceof URL || value instanceof URI || value instanceof File) {
         String path = value.toString();
         try { return new URL(path).openStream(); }
         catch (MalformedURLException e) {
            InputStream input = getClass().getResourceAsStream(path);
            try { return (input != null) ? input : new FileInputStream(path); }
            catch (IOException e2) { throw new RuntimeException(e2); }
         }
         catch (IOException e) { throw new RuntimeException(e); }
      }
      if (value instanceof byte[])
         return new ByteArrayInputStream((byte[])value);
      return new ByteArrayInputStream(String.valueOf(value).getBytes());
   }

   private Reader toReader(Object value) {
      if (value instanceof Reader)
         return (Reader)value;
      if (value instanceof InputStream)
         return new InputStreamReader((InputStream)value);
      return new InputStreamReader(toInputStream(value));
   }

   private void addEntry(Map map, Object value) {
      if (value instanceof Expression)
         addEntry(map, ((Expression)value).getValue());
      else if (value instanceof Reference) {
         Reference r = (Reference)value;
         map.put(r.getName(), r.getValue());
      }
      else if (value instanceof Map.Entry) {
         Map.Entry e = (Map.Entry)value;
         map.put(e.getKey(), e.getValue());
      }
      else
         map.put(map.size(), value);
   }

   private static Map getProperties(Object obj) {
      if (obj instanceof Map)
         return (Map)obj;
      if (obj == null)
         return Collections.EMPTY_MAP;
      if (obj instanceof Collection)
         return getProperties(((Collection)obj).toArray());
      Class type = obj.getClass();
      Map map = new LinkedHashMap();
      if (type.isArray()) {
         int length = Array.getLength(obj);
         map.put("length", length);
         for (int i = 0; i < length; i++)
            map.put(String.valueOf(i), Array.get(obj, i));
         return map;
      }

      String name = null;
      try {
         for (Field f : type.getFields()) {
            name = f.getName();
            map.put(name, f.get(obj));
         }
         for (Method m : type.getMethods()) {
            name = getPropertyName(m.getName());
            if (name != null && m.getParameterCount() == 0)
               map.put(name, m.invoke(obj));
         }
         return map;
      }
      catch (Exception e) { throw new RuntimeException("Error setting value for "+name, e); }
   }

   private static String getPropertyName(String name) {
      if (name.startsWith("get") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))
         return name.substring(3, 4).toLowerCase() + name.substring(4);
      if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2)))
         return name.substring(2, 3).toLowerCase() + name.substring(3);
      if (name.startsWith("set") && name.length() > 3 && Character.isUpperCase(name.charAt(3)))
         return name.substring(3, 4).toLowerCase() + name.substring(4);
      return null;
   }
}
