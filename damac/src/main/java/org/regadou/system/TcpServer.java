package org.regadou.system;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.GenericReference;
import org.regadou.script.InteractiveScript;

public class TcpServer implements Closeable {

   private String address;
   private ScriptContextFactory contextFactory;
   private ScriptEngineManager engineManager;
   private ServerSocket server;
   private Map<Socket,InteractiveScript> clients = new ConcurrentHashMap<>();
   private long sleep = 1000;
   private AtomicBoolean running = new AtomicBoolean(false);

   public TcpServer(String address, ScriptEngineManager engineManager, ScriptContextFactory contextFactory) {
      this(address, engineManager, contextFactory, true);
   }

   public TcpServer(String address, ScriptEngineManager engineManager, ScriptContextFactory contextFactory, boolean listen) {
      this.address = address;
      this.engineManager = engineManager;
      this.contextFactory = contextFactory;
      if (listen)
         listen();
   }

   @Override
   public String toString() {
      return address;
   }

   @Override
   public void close() {
      running.set(false);
      if (server != null) {
         try { server.close(); }
         catch (Exception e) {}
         server = null;
      }
      Iterator iter = clients.keySet().iterator();
      while (iter.hasNext())
         close((Socket)iter.next());
   }

   public String getUri() {
      return address;
   }

   public Collection<InteractiveScript> getClients() {
      return clients.values();
   }

   public boolean isRunning() {
      return running.get();
   }

   public void setRunning(boolean run) {
      if (running.get() == run)
         return;
      if (run)
         listen();
      else
         running.set(false);
   }

   protected void finalize() {
      close();
   }

   private void listen() {
      if (running.get())
         return;
      initServer();
      new Thread() {
         @Override
         public void run() {
            while (running.get()) {
               try {
                  final Socket s = server.accept();
                  if (s != null) {
                     new Thread(new Runnable() {
                        public void run() {
                           try { readClient(s); }
                           catch (IOException e) { throw new RuntimeException(e); }
                        }
                     }).start();
                  }
               }
               catch (Exception e) { e.printStackTrace(); }
               try { sleep(sleep); }
               catch (Exception e) { running.set(false); }
            }
            close();
         }
      }.start();
   }

   private synchronized void initServer() {
      if (server != null)
         return;
      String address = this.address;
      if (address.startsWith("tcp:"))
         address = address.substring(4);
      while (address.startsWith("/"))
         address = address.substring(1);
      String[] parts = address.split(":");
      switch (parts.length) {
         case 1:
            try { server = new ServerSocket(Integer.parseInt(parts[0])); }
            catch (Exception e) { throw new RuntimeException(e); }
            break;
         case 2:
            try { server = new ServerSocket(Integer.parseInt(parts[1]), 0, InetAddress.getByName(parts[0])); }
            catch (Exception e) { throw new RuntimeException(e); }
            break;
         default:
            throw new RuntimeException("Invalid tcp server address "+this.address);
      }
      running.set(true);
   }

   private synchronized void close(Socket s) {
      try {
         clients.remove(s).close();
         s.close();
      }
      catch (Exception e) {}
   }

   private void readClient(Socket s) throws IOException {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
         Writer writer = new OutputStreamWriter(s.getOutputStream());
         //TODO: implement a login process similar to getScriptEngine
         ScriptEngine engine = getScriptEngine(reader, writer);
         if (engine != null) {
            String name = s.getRemoteSocketAddress().toString().replace("/", "");
            ScriptContext cx = contextFactory.getScriptContext(new GenericReference("reader", reader),
               new GenericReference("writer", writer),
               new GenericReference("errorWriter", writer)
            );
            InteractiveScript repl = new InteractiveScript(contextFactory, engine, "\n? ", "= ", new String[]{"exit", "quit"});
            clients.put(s, repl);
            repl.run(cx);
         }
      }
      finally { close(s); }
   }

   private ScriptEngine getScriptEngine(BufferedReader reader, Writer writer) throws IOException {
      ScriptEngine engine = null;
      while (engine == null) {
         writer.write("\nLanguage: ");
         writer.flush();
         String lang = reader.readLine();
         if (lang == null)
            break;
         lang = lang.trim();
         if (!lang.isEmpty()) {
            engine = engineManager.getEngineByExtension(lang);
            if (engine == null) {
               engine = engineManager.getEngineByName(lang);
               if (engine == null) {
                  writer.write("Invalid language "+lang+"\n");
                  writer.flush();
               }
            }
         }
      }
      return engine;
   }
}
