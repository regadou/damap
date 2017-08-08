package org.regadou.system;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.regadou.reference.ReferenceHolder;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import javax.activation.FileTypeMap;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;
import org.regadou.damai.ResourceManager;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.FilterExpression;
import org.regadou.damai.Converter;

public class Context implements Configuration, ScriptContextFactory, Closeable {

   private static final ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal() {
      @Override
      protected synchronized Context initialValue() { return null; }
   };

   private static Injector FIRST_INJECTOR;

   @Deprecated
   public static Context currentContext() {
      Context cx = CURRENT_CONTEXT.get();
      if (cx == null) {
         cx = new Context();
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
   private Injector injector;

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
      if (injector == null) {
         for (Context cx = parent; cx != null; cx = cx.parent) {
            if (cx.injector != null) {
               injector = cx.injector;
               break;
            }
         }
         if (injector == null) {
            if (FIRST_INJECTOR == null)
               FIRST_INJECTOR = Guice.createInjector(new GuiceModule());
            injector = FIRST_INJECTOR;
         }
      }
   }

   @Override
   public String toString() {
      String username = (user == null) ? "?" : user.getName();
      return "Context#"+username+"#"+hashCode();
   }

   @Override
   public URL[] getClasspath() {
      return new URL[0];
   }

   public URL getInitScript() {
      return null;
   }

   @Override
   public Bindings getGlobalScope() {
      ScriptEngineManager manager = injector.getInstance(ScriptEngineManager.class);
      return (manager == null) ? null : manager.getBindings();
   }

   @Override
   public ScriptContextFactory getContextFactory() {
      return injector.getInstance(ScriptContextFactory.class);
   }

   @Override
   public Converter getConverter() {
      return injector.getInstance(Converter.class);
   }

   @Override
   public ScriptEngineManager getEngineManager() {
      return injector.getInstance(ScriptEngineManager.class);
   }

   @Override
   public MimeHandlerFactory getHandlerFactory() {
      return injector.getInstance(MimeHandlerFactory.class);
   }

   @Override
   public PropertyFactory getPropertyFactory() {
      return injector.getInstance(PropertyFactory.class);
   }

   @Override
   public ResourceManager getResourceManager() {
      return injector.getInstance(ResourceManager.class);
   }

   @Override
   public FileTypeMap getTypeMap() {
      return injector.getInstance(FileTypeMap.class);
   }

   @Override
   public void close() {
      if (CURRENT_CONTEXT.get() == this)
         CURRENT_CONTEXT.set(this.parent);
   }

   @Override
   public ScriptContext getScriptContext(Reference...properties) {
      if (scriptContextStack.isEmpty())
         scriptContextStack.add(new ContextWrapper(this));
      return scriptContextStack.lastElement();
   }

   @Override
   public boolean closeScriptContext(ScriptContext context) {
      if (context instanceof ContextWrapper) {
         ((ContextWrapper)context).getWrapper().close();
         return true;
      }
      return false;
   }

   public <T> T getProperty(Class<T> type, Reference ... properties) {
      PropertyFactory propertyFactory = injector.getInstance(PropertyFactory.class);
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
      Collection fields = (Collection)new FilterExpression(propertyFactory, Arrays.asList(getClass().getDeclaredFields()), criteria).getValue().getValue();
      if (!fields.isEmpty()) {
         try { return (T)((Field)fields.iterator().next()).get(this); }
         catch (IllegalArgumentException|IllegalAccessException e) { throw new RuntimeException(e); }
      }
      return null;
   }

   protected void makeCurrent() {
      Context cx = CURRENT_CONTEXT.get();
      if (cx != null)
         throw new RuntimeException("Context already set on this thread: "+cx);
      CURRENT_CONTEXT.set(this);
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

