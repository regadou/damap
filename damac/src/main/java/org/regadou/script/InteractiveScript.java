package org.regadou.script;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;

public class InteractiveScript implements Closeable {

   private ScriptContextFactory contextFactory;
   private ScriptEngine engine;
   private String inputPrompt;
   private String resultPrefix;
   private List<String> endWords;
   private AtomicBoolean running = new AtomicBoolean(false);

   public InteractiveScript(ScriptContextFactory contextFactory, ScriptEngine engine, String inputPrompt, String resultPrefix, String[] endWords) {
      this.contextFactory = contextFactory;
      this.engine = engine;
      this.inputPrompt = (inputPrompt == null) ? "" : inputPrompt;
      this.resultPrefix = (resultPrefix == null) ? "" : resultPrefix;
      this.endWords = (endWords == null) ? Collections.EMPTY_LIST : Arrays.asList(endWords);
   }

   @Override
   public void close() throws IOException {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   public void run(ScriptContext context) {
      if (running.get())
         throw new RuntimeException("This interactive script is already running");
      running.set(true);
      if (context == null) {
         context = engine.getContext();
         if (context == null)
            context = contextFactory.getScriptContext();
      }
      BufferedReader reader = getReader(context);
      Writer writer = getWriter(context);
      PrintWriter error = getErrorWriter(context);
      while (running.get()) {
         try {
            writer.write(inputPrompt);
            writer.flush();
            String txt = reader.readLine();
            if (txt == null) {
               reader = null;
               continue;
            }
            txt = txt.trim();
            if (endWords.contains(txt))
               running.set(false);
            else if (!txt.isEmpty()) {
               Object result = engine.eval(txt, context);
               while (result instanceof Reference) {
                  if (result instanceof Expression)
                     result = ((Expression)result).getValue(context);
                  else
                     result = ((Reference)result).getValue();
               }
               if (result != null) {
                  writer.write(resultPrefix+result+"\n");
                  writer.flush();
               }
            }
         }
         catch (Throwable t) {
            t.printStackTrace(error);
         }
      }
   }

   private BufferedReader getReader(ScriptContext context) {
      Reader reader = context.getReader();
      if (reader == null)
         return new BufferedReader(new InputStreamReader(System.in));
      else if (reader instanceof BufferedReader)
         return (BufferedReader)reader;
      else
         return new BufferedReader(reader);
   }

   private Writer getWriter(ScriptContext context) {
      Writer writer = context.getWriter();
      return (writer == null) ? new OutputStreamWriter(System.out) : writer;
   }

   private PrintWriter getErrorWriter(ScriptContext context) {
      Writer writer = context.getErrorWriter();
      if (writer == null)
         return new PrintWriter(System.err);
      else if (writer instanceof PrintWriter)
         return (PrintWriter)writer;
      else
         return new PrintWriter(writer);
   }
}
