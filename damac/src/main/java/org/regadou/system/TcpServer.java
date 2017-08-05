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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.regadou.damai.Configuration;
import org.regadou.script.InteractiveScript;

public class TcpServer implements Closeable {

   private String address;
   private Configuration configuration;
   private ServerSocket server;
   private Map<Socket,InteractiveScript> clients = new LinkedHashMap<>();
   private long sleep = 1000;
   private AtomicBoolean running = new AtomicBoolean(false);

   public TcpServer(String address, Configuration configuration) {
      this(address, configuration, true);
   }

   public TcpServer(String address, Configuration configuration, boolean listen) {
      this.address = address;
      this.configuration = configuration;
      if (listen)
         listen();
   }

   public String getUri() {
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

   public void listen() {
      if (running.get())
         return;
      initServer();
      running.set(true);
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

   protected void finalize() {
      close();
   }

   private void initServer() {
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
   }

   private void close(Socket s) {
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
            ScriptContext cx = engine.getContext();
            cx.setReader(reader);
            cx.setWriter(writer);
            InteractiveScript repl = new InteractiveScript(configuration, engine, "\n? ", "= ", new String[]{"exit", "quit"});
            clients.put(s, repl);
            repl.run(cx);
         }
      }
      finally { close(s); }
   }

   private ScriptEngine getScriptEngine(BufferedReader reader, Writer writer) throws IOException {
      ScriptEngineManager manager = configuration.getEngineManager();
      ScriptEngine engine = null;
      while (engine == null) {
         writer.write("\nLanguage: ");
         writer.flush();
         String lang = reader.readLine();
         if (lang == null)
            break;
         lang = lang.trim();
         if (!lang.isEmpty()) {
            engine = manager.getEngineByExtension(lang);
            if (engine == null) {
               engine = manager.getEngineByName(lang);
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
