package org.regadou.system;

import org.regadou.reference.ReferenceHolder;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.regadou.damai.Action;
import org.regadou.damai.Converter;
import org.regadou.damai.ConverterManager;
import org.regadou.damai.Expression;
import org.regadou.damai.Filter;
import org.regadou.damai.InstanceFactory;
import org.regadou.damai.Printable;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;

public class Context implements Closeable {

   private static ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal() {
      @Override
      protected synchronized Context initialValue() { return null; }
   };

   private InstanceFactory firstInstanceFactory;

   public static Context currentContext() {
      return currentContext(true, new Reference[0]);
   }

   public static Context currentContext(boolean create, Reference ... properties) {
      Context cx = CURRENT_CONTEXT.get();
      if (cx == null && create) {
         cx = new Context(properties);
         CURRENT_CONTEXT.set(cx);
      }
      return cx;
   }

   private Context parent;
   private Principal user;
   private InputStream input;
   private OutputStream output, error;
   private Map<String, Reference> dictionary;
   private Stack<ScriptContext> scriptContextStack = new Stack<>();
   private InstanceFactory instanceFactory;
   private ScriptEngineManager scriptEngineManager;
   private ConverterManager converterManager;
   private ResourceManager resourceManager;
   private PropertyFactory propertyFactory;
   private Filter defaultFilter;

   public Context(Reference ... properties) {
      if (CURRENT_CONTEXT.get() != null)
         throw new RuntimeException("Context already set on this thread");
      CURRENT_CONTEXT.set(this);

      if (properties != null) {
         for (Reference property : properties) {
            Field field = getField(property);
            if (field != null) {
               field.setAccessible(true);
               try { field.set(this, property.getValue()); }
               catch (IllegalArgumentException|IllegalAccessException e) {
                  throw new RuntimeException(e);
               }
            }
         }
      }

      if (dictionary == null)
         dictionary = new TreeMap<>();
      if (error == null)
         error = System.err;
      if (instanceFactory != null && firstInstanceFactory == null)
         firstInstanceFactory = instanceFactory;
   }

   @Override
   public String toString() {
      String username = (user == null) ? "?" : user.getName();
      return "Context#"+username+"#"+hashCode();
   }

   @Override
   public void close() {
      if (CURRENT_CONTEXT.get() == this)
         CURRENT_CONTEXT.set(this.parent);
   }

   public <T> T getProperty(Class<T> type, Reference ... properties) {
      if (defaultFilter == null)
         defaultFilter = getInstance(Filter.class);
      List<Reference> criteria;
      if (properties == null || properties.length == 0)
         criteria = (type == null) ? Collections.EMPTY_LIST
                                   : Collections.singletonList(new ReferenceHolder("type", type));
      else if (type == null)
         criteria = Arrays.asList(properties);
      else {
         criteria = new ArrayList(Arrays.asList(properties));
         criteria.add(0, new ReferenceHolder("type", type));
      }
      Collection<Field> fields = defaultFilter.filter(Arrays.asList(getClass().getDeclaredFields()), criteria);
      if (!fields.isEmpty()) {
         try { return (T)fields.iterator().next().get(this); }
         catch (IllegalArgumentException|IllegalAccessException e) { throw new RuntimeException(e); }
      }
      return null;
   }

   public <T> T getInstance(Class<T> type, Reference ... properties) {
      if (instanceFactory == null) {
         for (Context cx = parent; cx != null; cx = cx.parent) {
            if (cx.instanceFactory != null) {
               instanceFactory = cx.instanceFactory;
               break;
            }
         }
         if (instanceFactory == null) {
            if (firstInstanceFactory == null)
               throw new RuntimeException("Cannot find instance factory for "+this);
            instanceFactory = firstInstanceFactory;
         }
      }
      return instanceFactory.getInstance(type, properties);
   }

   public Reference getReference(String name) {
      if (name == null || name.trim().isEmpty())
         return null;
      for (Context cx = this; cx != null; cx = cx.parent) {
         if (cx.dictionary.containsKey(name))
            return cx.dictionary.get(name);
      }
      if (name.indexOf(':') < 0 && name.indexOf('/') < 0 && name.indexOf('\\') < 0) {
         Reference r = new ReferenceHolder(name, null);
         dictionary.put(name, r);
         return r;
      }
      return getResource(name);
   }

   public Resource getResource(String uri) {
      if (uri == null || uri.trim().isEmpty())
         return null; //TODO: return new TextStream(name, InputStream, OutputStream);
      int index = uri.indexOf(':');
      String scheme = (index < 0) ? null : uri.substring(0, index);
      if (resourceManager == null)
         resourceManager = getInstance(ResourceManager.class);
      ResourceFactory factory = resourceManager.getFactory(scheme);
      return (factory == null) ? null : factory.getResource(uri);
   }

   public Property getProperty(Object value, String name) {
      if (propertyFactory == null)
         propertyFactory = getInstance(PropertyFactory.class);
      Map<String,Property> properties = propertyFactory.getProperties(value);
      return (properties == null) ? null : properties.get(name);
   }

   public String read(InputStream input, String charset) throws IOException {
      return read(new InputStreamReader(input, charset));
   }

   public String read(Reader reader) throws IOException {
      StringBuilder buffer = new StringBuilder();
      char[] chars = new char[1024];
      for (int got = 0; (got = reader.read(chars)) >= 0;) {
         if (got > 0)
            buffer.append(chars, 0, got);
      }
      return buffer.toString();
   }

   public Object run(ScriptEngine engine, String inputPrompt, String resultPrefix, String[] endWords) {
      return ((ContextWrapper)getScriptContext()).run(engine, inputPrompt, resultPrefix, endWords);
   }

   public Reference execute(Expression expression, ScriptContext context) {
      if (expression == null)
         return null;
      if (context != null)
         scriptContextStack.push(context);
      Reference result = expression.getValue();
      if (context != null)
         scriptContextStack.pop();
      return result;
   }

   public Reference execute(Action function, Reference ... parameters) {
      if (function == null)
         return new ReferenceHolder(null, parameters);
      if (parameters == null)
         parameters = new Reference[0];
      Object value = function.execute((Object[])parameters);
      return (value instanceof Reference) ? (Reference)value : new ReferenceHolder(null, value);
   }

   public Object execute(String code, String lang) {
      ScriptEngine engine = getScriptEngine(lang);
      if (engine == null)
         throw new RuntimeException("Unknown language "+lang);
      try { return engine.eval(code, getScriptContext()); }
      catch (ScriptException e) { throw new RuntimeException(e); }
   }

   public String print(Object data, String lang) {
      ScriptEngine engine = getScriptEngine(lang);
      if (engine == null)
         throw new RuntimeException("Unknown language "+lang);
      if (engine instanceof Printable)
         return ((Printable)engine).print(data);
      throw new RuntimeException(lang+" is not printable");
   }

   public <S,T> T convert(Object data, Class<T> type) {
      if (converterManager == null)
         converterManager = getInstance(ConverterManager.class);
      Class srcClass = (data == null) ? Void.class : data.getClass();
      Converter<S,T> converter = converterManager.getConverter(srcClass, type);
      if (converter == null)
         throw new RuntimeException("Cannot find converter to "+type.getName());
      return converter.convert((S)data);
   }

   protected void makeCurrent() {
      Context cx = CURRENT_CONTEXT.get();
      if (cx != null)
         throw new RuntimeException("Context already set on this thread: "+cx);
      CURRENT_CONTEXT.set(this);
   }

   private ScriptEngine getScriptEngine(String lang) {
      if (scriptEngineManager == null)
         scriptEngineManager = getInstance(ScriptEngineManager.class);
      if (lang.indexOf('/') > 0) {
         ScriptEngine engine = scriptEngineManager.getEngineByMimeType(lang);
         if (engine != null)
            return engine;
      }
      ScriptEngine engine = scriptEngineManager.getEngineByExtension(lang);
      if (engine != null)
         return engine;
      return scriptEngineManager.getEngineByName(lang);
   }

   private ScriptContext getScriptContext() {
      if (scriptContextStack.isEmpty())
         scriptContextStack.add(new ContextWrapper(this));
      return scriptContextStack.lastElement();
   }

   private Field getField(Reference property) {
      if (property == null)
         return null;
      String name = property.getName();
      if (name == null || name.trim().isEmpty())
         return null;
      if (name.indexOf('.') < 0) {
         try { return getClass().getDeclaredField(name); }
         catch (NoSuchFieldException|SecurityException e) {}
      }
      else {
         try {
            Class type = Class.forName(name);
            for (Field field : getClass().getDeclaredFields()) {
               if (field.getType().isAssignableFrom(type))
                  return field;
            }
         }
         catch (ClassNotFoundException ex) {}
      }
      return null;
   }
}

