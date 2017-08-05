package org.regadou.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.ReferenceHolder;
import org.regadou.reference.UrlReference;
import org.regadou.script.InteractiveScript;
import org.regadou.util.StringInput;

public class Main {

   public static final List<String> OPTIONS = Arrays.asList("debug", "lang", "script", "interactive");

   public static void main(String[] args) throws ScriptException, IOException {
      Map<String,String> options = new LinkedHashMap<>();
      args = parseArgs(args, options);
      if (args != null) {
         Configuration conf = new Context(new ReferenceHolder("input", System.in),
                                          new ReferenceHolder("output", System.out),
                                          new ReferenceHolder("error", System.err));
         ScriptEngineManager scriptManager = conf.getEngineManager();
         for (int a = 0; a < args.length; a++) {
            Reference r = ((Context)conf).getReference(args[a]);
            if (r == null)
               throw new RuntimeException("Cannot load file "+args[a]);
            UrlReference u = (r instanceof UrlReference) ? (UrlReference)r : null;
            ScriptEngine engine = (u == null) ? null : scriptManager.getEngineByMimeType(u.getMimetype());
            if (engine == null)
               System.out.println(r.getValue());
            else {
               List<String> list = Arrays.asList(args).subList(a+1, args.length);
               ((Context)conf).getReference("arguments").setValue(Arrays.asList(list.toArray(new String[list.size()])));
               engine.eval(new StringInput(u.getInputStream(), "utf8").toString());
               break;
            }
         }

         boolean interactive = options.containsKey("interactive") || args.length == 0;
         String script = options.get("script");
         if (script == null && !interactive)
            return;
         String lang = options.get("lang");
         if (lang == null)
            lang = scriptManager.getEngineFactories().iterator().next().getLanguageName();
         ScriptEngine engine = scriptManager.getEngineByExtension(lang);
         if (engine == null) {
            engine = scriptManager.getEngineByName(lang);
            if (engine == null) {
               System.out.println("*** ERROR: invalid lang option: "+lang);
               printAvailableLanguages(scriptManager);
               return;
            }
         }

         ((Context)conf).getReference("arguments").setValue(Arrays.asList(args));
         if (script != null)
            engine.eval(script);
         if (interactive)
            new InteractiveScript(conf, engine, "\n? ", "= ", new String[]{"exit", "quit"}).run(conf.getContextFactory().getScriptContext());
      }
      else {
         StringBuilder buffer = new StringBuilder();
         OPTIONS.stream().forEach(o -> buffer.append(" ").append(o));
         System.out.println("Available options: "+buffer);
      }
   }

   private static String[] parseArgs(String[] args, Map<String, String> options) {
      List<String> params = new ArrayList<>();
      boolean gotEndOfOptions = false, gotErrors = false;
      for (int i = 0; i < args.length; i++) {
         String arg = args[i];
         if (arg == null) {
         }
         else if (gotEndOfOptions) {
            params.add(arg);
         }
         else if (arg.equals("-") || arg.equals("--")) {
            gotEndOfOptions = true;
         }
         else if (arg.startsWith("-")) {
            if (!arg.startsWith("--") && arg.length() == 2 && Character.isLetter(arg.charAt(1))
                                      && i+1 < args.length && !args[i+1].startsWith("-")) {
               i++;
               arg += "="+args[i];
            }
            Reference option = parseOption(arg, options);
            if (option == null) {
               gotErrors = true;
            }
            else if (!gotErrors && option.getName().equals("debug") && option.getValue().equals("true")) {
               try {
                  System.out.println("*** press enter after starting debugger ***");
                  new BufferedReader(new InputStreamReader(System.in)).readLine();
               }
               catch (IOException e) { throw new RuntimeException(e); }
            }
         }
         else {
            params.add(arg);
         }
      }

      return gotErrors ? null : params.toArray(new String[params.size()]);
   }

   private static Reference parseOption(String arg, Map<String, String> options) {
      String name, value;
      while (arg.startsWith("-")) {
         arg = arg.substring(1);
      }
      int eq = arg.indexOf('=');
      int dp = arg.indexOf(':');
      if (eq < 0 && dp < 0) {
         name = arg;
         value = "true";
      }
      else  {
         if (eq < 0 || (dp > 0 && dp < eq)) {
            eq = dp;
         }
         name = arg.substring(0,eq);
         value = arg.substring(eq+1);
      }

      if (name.length() == 0) {
         System.out.println("*** ERROR: empty name in option "+arg);
         return null;
      }
      else if (name.length() == 1) {
         for (String option : OPTIONS) {
            if (option.startsWith(name)) {
               name = option;
               break;
            }
         }
      }

      if (!OPTIONS.contains(name)) {
         System.out.println("*** ERROR: invalid option name "+name);
         return null;
      }
      options.put(name, value);
      return new ReferenceHolder(name, value);
   }

   private static void printAvailableLanguages(ScriptEngineManager manager) {
      Collection<String> languages = new TreeSet<>();
      manager.getEngineFactories().stream().forEach(factory -> {
         languages.addAll(factory.getExtensions());
      });
      System.out.println("Available languages: "+languages);
   }
}
