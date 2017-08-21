package org.regadou.util;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Bootstrap;
import org.regadou.damai.Converter;

public class InterfaceBuilder implements InvocationHandler, Serializable {

/******************************* Beginning of the test section *****************************/

   public static void main(String[] args) {
      if (args.length == 0) {
         showHelp();
         return;
      }

      String className = null;
      File file = null;
      Map members = new LinkedHashMap();
      try {
         for (String arg : args) {
            arg = arg.trim();
            if (arg.equals(""))
               continue;
            else if (arg.equals("debug")) {
               try {
                  System.out.println("*** press enter after starting debugger ***");
                  new BufferedReader(new InputStreamReader(System.in)).readLine();
               }
               catch (Exception e) {
                  System.out.println("Error pausing for debugger: "+e);
               }
               continue;
            }

            String name = null;
            int index = arg.indexOf('=');
            if (index >= 0) {
               name = arg.substring(0, index);
               arg = arg.substring(index+1);
            }
            Object member = getDeclaration(arg);
            if (member != null) {
               if (name == null)
                  name = "#"+members.size();
               members.put(name, member);
            }
            else if (arg.indexOf('.') > 0) {
               if (className == null)
                  className = arg;
               else if (file == null)
                  file = new File(arg);
               else
                  System.out.println("WARNING: unused argument "+arg);
            }
            else if (arg.indexOf('/') > 0 || arg.indexOf('\\') > 0) {
               if (file == null)
                  file = new File(arg);
               else
                  System.out.println("WARNING: unused argument "+arg);
            }
            else if (className == null)
               className = arg;
            else if (file == null)
               file = new File(arg);
            else
               System.out.println("WARNING: unused argument "+arg);
         }
         if (className == null)
            System.out.println("No class name specified");
         else if (members.isEmpty())
            System.out.println("No property or method specified");
         else {
            Class iface = newInterface(className, members, file);
            showResults(iface, file);
         }
      }
      catch (Exception e) { e.printStackTrace(); }
   }

   private static void showHelp() {
      System.out.println(
          "Please describe an interface definition with the following rules:\n" +
          "- each program argument will be a class member (property, method, constructor)\n" +
          "- name=java-class adds a property (getter and setter)\n" +
          "- [name=]java-class.field adds a property with same type as the specified field\n" +
          "- [name=]java-class.method(param-types) adds a method in the interface which will use the static method specified\n" +
          "- [name=]java-class(param-types) adds a constructor for runtime instantiaition of this interface\n" +
          "- any other parameter is either the class name or the build/classpath folder for writting the class file on disk\n" +
          ""
      );
   }

   private static void showResults(Class iface, File file) throws Exception {
      String className = iface.getName();
      String dst;
      if (file == null)
         dst = ":";
      else {
         File classFile = new File(file+File.separator+className.replace('.',File.separatorChar)+".class");
         dst = " in file "+classFile+" ("+classFile.length()+" bytes):";
      }
      System.out.println("Class "+className+" has been created"+dst);
      Method[] methods = iface.getMethods();
      System.out.println(methods.length+" methods created");
      for (Method method : methods)
         System.out.println("  "+method);
      System.out.println("Properties of the created instance:");
      Object instance = newInstance(iface); //TODO: test the constructor call with args
      Map map = (instance instanceof Map) ? (Map)instance : new BeanMap(instance);
      System.out.println("  "+map);
   }

/******************************* End of the test section *****************************/

/**************************** Public methods made to use this class *****************************/

   // creates a new interface based on the name and member definitions given
   // if file is not null, the class file will be save in this folder
   public static Class newInterface(String name, Map members, File file) throws Exception {
      InterfaceDefinition i = interfaces.get(name);
      if (i != null)
         throw new RuntimeException("Class "+name+" is already defined");
      i = new InterfaceClassLoader(InterfaceBuilder.class.getClassLoader()).newInterface(name, members);
      interfaces.put(name, i);
      saveInterface(i, file);
      return i.type;
   }

   // updates an interface already created with the InterfaceBuilder class
   // the class file on disk will be updated if it was created
   public static Class updateInterface(String name, Map members) throws Exception {
      InterfaceDefinition i = interfaces.get(name);
      if (i == null)
         throw new RuntimeException("Class "+name+" is not defined");
      i = new InterfaceClassLoader(InterfaceBuilder.class.getClassLoader()).newInterface(name, members);
      interfaces.put(name, i);
      saveInterface(i, i.file);
      return i.type;
   }

   // creates a new instance of the given
   public static <T> T newInstance(Class<T> type, Object...args) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      return (T)Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new InterfaceBuilder(type, args));
   }

   private static Object getDeclaration(String arg) throws Exception {
      Class[] params = null;
      int index = arg.indexOf('(');
      if (index > 0) {
         String[] parts = arg.substring(index+1).replace(')', ' ').trim().split(",");
         arg = arg.substring(0, index);
         params = new Class[parts.length];
         for (int i = 0; i < params.length; i++)
            params[i] = converter.convert(parts[i].trim(), Class.class);
      }

      try {
         Class c = converter.convert(arg, Class.class);
         return (params == null) ? c : c.getConstructor(params);
      }
      catch (Exception e) {
         index = arg.lastIndexOf('.');
         if (index > 0) {
            String name = arg.substring(index+1);
            try {
               Class c = converter.convert(arg.substring(0,index), Class.class);
               if (params == null) {
                  try { return c.getField(name); }
                  catch (Exception e2) {}
               }
               else {
                  try { return c.getMethod(name, params); }
                  catch (Exception e2) {}
               }
            }
            catch (Exception e2) {}
         }
      }
      return null;
   }


/******************** Private section containing the internal operations ***********************/

   // Interface definition inner class to hold informations (mostly for runtime execution)
   private static class InterfaceDefinition {
      public Class type;
      public File file;
      public byte[] bytes;
      Map<String,Class> properties = new LinkedHashMap<>();
      Map<String,List<Method>> methods = new LinkedHashMap<>();
      List<Constructor> constructors = new ArrayList<>();
   }
   private static final Map<String,InterfaceDefinition> interfaces = new LinkedHashMap<>();
   private static final Converter converter = new Bootstrap();

   InterfaceDefinition iface;
   Map data;

   private InterfaceBuilder(Class type, Object...args) throws InstantiationException, IllegalAccessException, InvocationTargetException {
      iface = interfaces.get(type.getName());
      if (iface == null) {
         iface = new InterfaceDefinition();
         iface.type = type;
      }
      for (Constructor c : iface.constructors) {
         if (paramsMatches(c.getParameterTypes(), args)) {
            Object o = c.newInstance(args);
            data = new BeanMap(o);
            break;
         }
      }
      if (data == null)
         data = new LinkedHashMap<>();
      for (Map.Entry<String,Class> entry : iface.properties.entrySet())
         data.put(entry.getKey(), converter.convert(null, entry.getValue()));
   }

   public Class getInterface() {
      return iface.type;
   }

   public Object getTarget() {
      return (data instanceof BeanMap) ? ((BeanMap)data).getBean(): data;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String name = method.getName();
      Class[] types = method.getParameterTypes();
      Class returnType = method.getReturnType();
      if (args == null)
         args = new Object[0];

      if (types.length != args.length)
           throw new IllegalArgumentException("Expected "+types.length+" parameters but got "+args.length);
      List<Method> methods = iface.methods.get(name);
      if (methods != null) {
         List lst = new ArrayList(Arrays.asList(data));
         lst.addAll(Arrays.asList(args));
         Object[] params = lst.toArray();
         for (Method m : methods) {
            if (paramsMatches(m.getParameterTypes(), params))
               return converter.convert(m.invoke(data, params), returnType);
         }
      }

      String property;
      switch (types.length) {
         case 0:
            property = getProperty(name, "get");
            if (property == null)
               property = getProperty(name, "is");
            if (property != null)
               return converter.convert(data.get(property), returnType);
            break;
         case 1:
            property = getProperty(name, "set");
            if (property != null)
               return converter.convert(data.get(property), types[0]);
            break;
      }

      Object target = (data instanceof BeanMap) ? ((BeanMap)data).getBean() : data;
      return target.getClass().getMethod(name, types).invoke(target, args);
   }

   private static void saveInterface(InterfaceDefinition i, File file) throws IOException {
      if (file == null)
         return;
      else if (file.exists() && !file.isDirectory())
         throw new RuntimeException(file+" exists and is not a directory");

      String className = i.type.getName();
      File classFile = new File(file+File.separator+className.replace('.',File.separatorChar)+".class");
      File parent = classFile.getParentFile();
      if (!parent.isDirectory()) {
         if (!parent.mkdirs())
            throw new RuntimeException(file+" cannot be created");
      }

      OutputStream output = new FileOutputStream(classFile);
      output.write(i.bytes);
      output.close();
      i.file = file;
   }

   private static boolean paramsMatches(Class[] mtypes, Object[] ptypes) {
      if (mtypes.length != ptypes.length)
         return false;
      for (int i = 0; i < mtypes.length; i++) {
         Class t = mtypes[i];
         Object v = ptypes[i];
         if (v == null) {
            if (t.isPrimitive())
               return false;
         }
         else if (!t.isAssignableFrom(v.getClass()))
            return false;
      }
      return true;
   }

   private static String getProperty(String name, String start) {
      if (!name.startsWith(start))
         return null;
      int n = start.length();
      if (name.length() < n+1)
         return null;
      if (!Character.isUpperCase(name.charAt(n)))
         return null;
      return name.substring(n, n+1).toLowerCase() + name.substring(n+1);
   }

   private static class InterfaceClassLoader extends ClassLoader {

      private static final int CONSTANT_Utf8 = 1, CONSTANT_Class = 7;
      private final Map<List<?>, Integer> poolMap = new LinkedHashMap<List<?>, Integer>();
      private int poolIndex = 1;

      private static class MethodDefinition {
         public String name;
         public Class[] types;
         public Class returnType;
         public Class[] exceptions;
      }

      public InterfaceClassLoader(ClassLoader parent) { super(parent); }

      public InterfaceDefinition newInterface(String className, Map members) throws Exception {
         Class c = findLoadedClass(className);
         if (c != null)
            throw new RuntimeException(className+" has already been loaded by a parent class loader");
         InterfaceDefinition iface = new InterfaceDefinition();
         List<MethodDefinition> methods = new ArrayList<>();
         for (Object e : members.entrySet()) {
            Map.Entry entry = (Map.Entry)e;
            addMethods(iface, methods, entry.getKey(), entry.getValue());
         }

         // write the class date to a byte array
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         DataOutputStream dout = new DataOutputStream(bout);

         dout.writeInt(0xcafebabe);     // u4 magic
         dout.writeShort(0);            // u2 minor_version
         dout.writeShort(45);           // u2 major_version (Java 1.0.2)

         byte[] afterConstantPool = buildAfterConstantPool(className, methods);
         writeConstantPool(dout);
         dout.write(afterConstantPool);
         iface.bytes = bout.toByteArray();
         iface.type = defineClass(className, iface.bytes, 0, iface.bytes.length);
         return iface;
      }

      private void addMethods(InterfaceDefinition iface, List<MethodDefinition> methods, Object key, Object value) throws Exception {
         String name = validateMemberName(key);
         if (value instanceof Constructor) {
            iface.constructors.add((Constructor)value);
            return;
         }
         else if (value instanceof Method) {
            Method method = (Method)value;
            int mods = method.getModifiers();
            Class[] paramtypes = method.getParameterTypes();
            if (!Modifier.isStatic(mods) || !Modifier.isPublic(mods) || paramtypes.length == 0)
               throw new RuntimeException("Method in interface definition must be public static with at least one arg");
            String methodName = method.getName();
            if (name == null)
               name = methodName;
            List<Method> lst = iface.methods.get(methodName);
            if (lst == null) {
               lst = new ArrayList<>();
               iface.methods.put(methodName, lst);
            }
            lst.add(method);
            List<Class<?>> types = new ArrayList<>(Arrays.asList(method.getParameterTypes()));
            types.remove(0);
            addMethodDefinition(methods, null, name, types, method.getReturnType(), method.getExceptionTypes());
         }
         else if (value instanceof Field) {
            Field field = (Field)value;
            Class type = field.getType();
            if (name == null)
               name = field.getName();
            iface.properties.put(name, type);
            String prefix = (type == Boolean.class || type == Boolean.TYPE) ? "is" : "get";
            addMethodDefinition(methods, prefix, name, new Class[0], type, null);
            addMethodDefinition(methods, "set", name, new Class[]{type}, Void.TYPE, null);
         }
         else if (value instanceof Class) {
            Class type = (Class)value;
            if (name == null) {
               name = type.getName();
               int index = name.lastIndexOf('.');
               if (index >= 0)
                  name = name.substring(index+1);
               name = validateMemberName(name);
               if (name == null)
                  throw new RuntimeException("Invalid property name "+key+" for type "+type.getName());
            }

            iface.properties.put(name, type);
            String prefix = (type == Boolean.class || type == Boolean.TYPE) ? "is" : "get";
            addMethodDefinition(methods, prefix, name, new Class[0], type, null);
            addMethodDefinition(methods, "set", name, new Class[]{type}, Void.TYPE, null);
         }
         else if (value instanceof CharSequence)
            addMethods(iface, methods, key, getDeclaration(value.toString()));
         else
            throw new RuntimeException("Don't know what to do with "+value);
      }

      private void addMethodDefinition(List<MethodDefinition> methods, String prefix, String name, List<Class<?>> types, Class returnType, Class[] exceptions) {
         addMethodDefinition(methods, prefix, name, types.toArray(new Class[types.size()]), returnType, exceptions);
      }

      private void addMethodDefinition(List<MethodDefinition> methods, String prefix, String name, Class[] types, Class returnType, Class[] exceptions) {
         MethodDefinition def = new MethodDefinition();
         def.name = (prefix == null) ? name : prefix+name.substring(0,1).toUpperCase()+name.substring(1);
         def.types = types;
         def.returnType = returnType;
         def.exceptions = (exceptions == null) ? new Class[0] : exceptions;
         methods.add(def);
      }

      private String validateMemberName(Object name) {
         if (name == null)
            return null;
         char[] chars = name.toString().trim().toCharArray();
         if (chars.length == 0)
            return null;
         char c = chars[0];
         if (Character.isUpperCase(c))
            chars[0] = Character.toLowerCase(c);
         else if (!Character.isLowerCase(c))
            return null;
         for (int i = 1; i < chars.length; i++) {
            c = chars[i];
            if (c <= 32)
               chars[i] = '_';
            else if (c >= 127)
               return null;
            else if (c != '_' && !Character.isUpperCase(c) && !Character.isLowerCase(c) && !Character.isDigit(c))
               return null;
         }
         return new String(chars);
      }

      private byte[] buildAfterConstantPool(String className, List<MethodDefinition> methods) throws IOException {
          ByteArrayOutputStream bout = new ByteArrayOutputStream();
          DataOutputStream dout = new DataOutputStream(bout);

          dout.writeShort(Modifier.PUBLIC|Modifier.INTERFACE|Modifier.ABSTRACT); // u2 access_flags
          dout.writeShort(classConstant(className)); // u2 this_class
          dout.writeShort(classConstant(Object.class.getName()));  // u2 super_class
          dout.writeShort(0);              // u2 interfaces_count
          dout.writeShort(0);              // u2 fields_count
          dout.writeShort(methods.size()); // u2 methods_count

          for (MethodDefinition method : methods) {
              dout.writeShort(Modifier.PUBLIC|Modifier.ABSTRACT);             // u2 access_flags
              dout.writeShort(stringConstant(method.name));          // u2 name_index
              dout.writeShort(stringConstant(methodDescriptor(method)));  // u2 descriptor_index
              dout.writeShort(1);                                             // u2 attributes_count
              dout.writeShort(stringConstant("Exceptions"));                  // u2 attribute_name_index
              dout.writeInt(2*(method.exceptions.length+1));                // u4 attribute_length:
              dout.writeShort(method.exceptions.length);                      // (u2 number_of_exceptions
              for (Class exception : method.exceptions)
                  dout.writeShort(classConstant(exception.getName()));      // (u2 exception_index)
          }

          dout.writeShort(0);              // u2 attributes_count (for class)
          return bout.toByteArray();
      }

      private String methodDescriptor(MethodDefinition method) {
          StringBuilder sb = new StringBuilder("(");
          for (Class<?> param : method.types)
              sb.append(classCode(param));
          sb.append(")").append(classCode(method.returnType));
          return sb.toString();
      }

      private String classCode(Class<?> c) {
          if (c == void.class)
              return "V";
          Class<?> arrayClass = Array.newInstance(c, 0).getClass();
          return arrayClass.getName().substring(1).replace('.', '/');
      }

      private int stringConstant(String s) {
          return constant(CONSTANT_Utf8, s);
      }

      private int classConstant(String s) {
          int classNameIndex = stringConstant(s.replace('.', '/'));
          return constant(CONSTANT_Class, classNameIndex);
      }

      private int constant(Object... data) {
          List<?> dataList = Arrays.asList(data);
          if (poolMap.containsKey(dataList))
              return poolMap.get(dataList);
          poolMap.put(dataList, poolIndex);
          return poolIndex++;
      }

      private void writeConstantPool(DataOutputStream dout) throws IOException {
          dout.writeShort(poolIndex);
          int i = 1;
          for (List<?> data : poolMap.keySet()) {
              assert(poolMap.get(data).equals(i++));
              int tag = (Integer) data.get(0);
              dout.writeByte(tag);          // u1 tag
              switch (tag) {
                  case CONSTANT_Utf8:
                      dout.writeUTF((String) data.get(1));
                      break;                // u2 length + u1 bytes[length]
                  case CONSTANT_Class:
                      dout.writeShort((Integer) data.get(1));
                      break;                // u2 name_index
                  default:
                      throw new AssertionError();
              }
          }
      }
   }
}
