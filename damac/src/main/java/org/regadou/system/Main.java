package org.regadou.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.regadou.damai.Bootstrap;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;
import org.regadou.resource.Url;
import org.regadou.script.InteractiveScript;

public class Main {

   private static final List<String> OPTIONS = Arrays.asList("debug", "config", "lang", "script", "interactive");
   private static final String DEFAULT_LANG = "damai";

   public static void main(String[] args) throws ScriptException, IOException {
      Map<String,String> options = new LinkedHashMap<>();
      args = parseArgs(args, options);
      if (args != null) {
         String configOpt = options.get("config");
         Configuration conf = (configOpt == null) ? new GuiceConfiguration() : new Bootstrap(configOpt);
         ScriptContext cx = conf.getContextFactory().getScriptContext(new GenericReference("reader", new BufferedReader(new InputStreamReader(System.in))),
            new GenericReference("writer", new OutputStreamWriter(System.out)),
            new GenericReference("errorWriter", new OutputStreamWriter(System.err))
         );
         ScriptEngineManager scriptManager = conf.getEngineManager();
         for (int a = 0; a < args.length; a++) {
            Reference r = conf.getResourceManager().getResource(args[a]);
            if (r == null)
               throw new RuntimeException("Cannot load uri "+args[a]);
            Url u = (r instanceof Url) ? (Url)r : null;
            ScriptEngine engine = (u == null) ? null : scriptManager.getEngineByMimeType(u.getMimetype());
            if (engine == null)
               System.out.println(r.getValue());
            else {
               List<String> list = Arrays.asList(args).subList(a+1, args.length);
               conf.getResourceManager().getResource("arguments").setValue(Arrays.asList(list.toArray(new String[list.size()])));
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
            lang = DEFAULT_LANG;
         ScriptEngine engine = lang.contains("/") ? scriptManager.getEngineByMimeType(lang)
                                                  : scriptManager.getEngineByExtension(lang);
         if (engine == null) {
            engine = scriptManager.getEngineByName(lang);
            if (engine == null) {
               System.out.println("*** ERROR: invalid lang option: "+lang);
               Collection<String> languages = new TreeSet<>();
               scriptManager.getEngineFactories().stream().forEach(factory -> {
                  languages.addAll(factory.getExtensions());
               });
               System.out.println("Available languages: "+languages);
               return;
            }
         }

         conf.getResourceManager().getResource("arguments").setValue(Arrays.asList(args));
         if (script != null)
            engine.eval(script);
         if (interactive)
            new InteractiveScript(conf.getContextFactory(), engine, "\n? ", "= ", new String[]{"exit", "quit"}).run(cx);
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
            else if (!gotErrors && option.getId().equals("debug") && option.getValue().equals("true")) {
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
      return new GenericReference(name, value);
   }
}
