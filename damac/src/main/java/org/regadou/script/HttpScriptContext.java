package org.regadou.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Configuration;
import org.regadou.util.EnumerationSet;
import org.regadou.util.MapAdapter;

public class HttpScriptContext implements ScriptContext {

   public static final int SESSION_SCOPE = (GLOBAL_SCOPE + ENGINE_SCOPE) / 2;

   private HttpServletRequest request;
   private HttpServletResponse response;
   private Map<Integer,Bindings> scopes = new TreeMap<>();
   private Reader reader;
   private Writer writer;
   private PrintWriter error;

   public HttpScriptContext(HttpServletRequest request, HttpServletResponse response, Configuration configuration) {
      this.request = request;
      this.response = response;
      Bindings bindings = configuration.getEngineManager().getBindings();
      bindings.put("ENGINE_SCOPE", ENGINE_SCOPE);
      bindings.put("SESSION_SCOPE", SESSION_SCOPE);
      bindings.put("GLOBAL_SCOPE", GLOBAL_SCOPE);
      scopes.put(GLOBAL_SCOPE, bindings);
      if (request != null) {
         Map<String,Object> map = new MapAdapter<>(
                 () -> new EnumerationSet(request.getAttributeNames()),
                 request::getAttribute,
                 request::setAttribute,
                 request::removeAttribute);
         bindings = new SimpleBindings(map);
         bindings.put("request", new BeanMap(request));
         scopes.put(ENGINE_SCOPE, bindings);
         HttpSession session = request.getSession(false);
         if (session != null) {
            map = new MapAdapter<>(
                    () -> new EnumerationSet(session.getAttributeNames()),
                    session::getAttribute,
                    session::setAttribute,
                    session::removeAttribute);
            scopes.put(SESSION_SCOPE, new SimpleBindings(map));
         }
      }
      else
         scopes.put(ENGINE_SCOPE, new SimpleBindings());

      error = new PrintWriter(System.err);

   }

   @Override
   public String toString() {
      Set keys = new TreeSet();
      for (Bindings bindings : scopes.values())
         keys.addAll(bindings.keySet());
      return keys.toString();
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
      for (Integer scope : scopes.keySet()) {
         Bindings bindings = scopes.get(scope);
         if (bindings != null && bindings.containsKey(name))
            return bindings.get(name);
      }
      return null;
   }

   @Override
   public int getAttributesScope(String name) {
      for (Integer scope : scopes.keySet()) {
         Bindings bindings = scopes.get(scope);
         if (bindings != null && bindings.containsKey(name))
            return scope;
      }
      return -1;
   }

   @Override
   public Writer getWriter() {
      if (writer == null && response != null) {
         try { writer = new OutputStreamWriter(response.getOutputStream()); }
         catch (IOException e) {}
      }
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
         error = null;
      else if (writer instanceof PrintWriter)
         error = (PrintWriter)writer;
      else
         error = new PrintWriter(writer);
   }

   @Override
   public Reader getReader() {
      if (reader == null && request != null) {
         try { reader = new InputStreamReader(request.getInputStream()); }
         catch (IOException e) {}
      }
      return reader;
   }

   @Override
   public void setReader(Reader reader) {
      this.reader = reader;
   }

   @Override
   public List<Integer> getScopes() {
      return new ArrayList<>(scopes.keySet());
   }
}
