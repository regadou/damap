package org.regadou.system;

import org.regadou.reference.ReferenceHolder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;

public class ContextWrapper implements ScriptContext {

   private Context context;
   private Map<Integer,Bindings> scopes = new TreeMap<>();
   private BufferedReader reader;
   private Writer writer;
   private PrintWriter error;

   public ContextWrapper() {
      this(Context.currentContext());
   }

   public ContextWrapper(Context context) {
      this.context = context;
      scopes.put(GLOBAL_SCOPE, context.getEngineManager().getBindings());
      scopes.put(ENGINE_SCOPE, new SimpleBindings(context.getProperty(Map.class)));
      InputStream input = context.getProperty(InputStream.class);
      if (input != null)
         reader = new BufferedReader(new InputStreamReader(input));
      OutputStream output = context.getProperty(OutputStream.class, new ReferenceHolder("name", "output"));
      if (output != null)
         writer = new OutputStreamWriter(output);
      output = context.getProperty(OutputStream.class, new ReferenceHolder("name", "error"));
      if (output != null)
         error = new PrintWriter(new OutputStreamWriter(output));
   }

   public Context getWrapper() {
      return context;
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      scopes.put(scope, bindings);
   }

   @Override
   public Bindings getBindings(int scope) {
      return scopes.get(scope);
   }

   @Override
   public void setAttribute(String name, Object value, int scope) {
      Bindings bindings = scopes.get(scope);
      if (bindings != null)
         bindings.put(name, value);
   }

   @Override
   public Object getAttribute(String name, int scope) {
      Bindings bindings = scopes.get(scope);
      return (bindings == null) ? null : bindings.get(name);
   }

   @Override
   public Object removeAttribute(String name, int scope) {
      Bindings bindings = scopes.get(scope);
      return (bindings == null) ? null : bindings.remove(name);
   }

   @Override
   public Object getAttribute(String name) {
       for (Bindings bindings : scopes.values()) {
         if (bindings.containsKey(name))
            return bindings.get(name);
      }
       return null;
   }

   @Override
   public int getAttributesScope(String name) {
      for (Map.Entry<Integer,Bindings> entry : scopes.entrySet()) {
         if (entry.getValue().containsKey(name))
            return entry.getKey();
      }
      return -1;
   }

   @Override
   public Writer getWriter() {
      return writer;
   }

   @Override
   public Writer getErrorWriter() {
      return error;
   }

   @Override
   public void setWriter(Writer writer) {
      this.writer = writer;
   }

   @Override
   public void setErrorWriter(Writer writer) {
      if (writer == null)
         this.error = null;
      else if (writer instanceof PrintWriter)
         this.error = (PrintWriter)writer;
      else
         this.error = new PrintWriter(writer);
   }

   @Override
   public Reader getReader() {
      return reader;
   }

   @Override
   public void setReader(Reader reader) {
      if (reader == null)
         this.reader = null;
      else if (reader instanceof BufferedReader)
         this.reader = (BufferedReader)reader;
      else
         this.reader = new BufferedReader(reader);
   }

   @Override
   public List<Integer> getScopes() {
      return new ArrayList<>(scopes.keySet());
   }
}
