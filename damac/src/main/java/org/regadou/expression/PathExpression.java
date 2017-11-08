package org.regadou.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import javax.script.ScriptContext;
import org.regadou.action.ActionBuilder;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;
import org.regadou.action.GenericComparator;

public class PathExpression implements Expression<Reference> {

   private static final String START_EXPRESSION = "([{";
   private static final String END_EXPRESSION = ")]}";
   private static Map<Command, Action> COMMAND_ACTIONS;

   private Configuration configuration;
   private Map keywords;
   private GenericComparator comparator;
   private Action command;
   private String path;
   private Object data;
   private List parts;
   private String text;

   public PathExpression(Configuration configuration, Map keywords, Command command, String path, Object data) {
      this.configuration = configuration;
      this.keywords = keywords;
      this.comparator = configuration.getInstance(GenericComparator.class);
      this.command = getAction(command);
      this.path = path;
      this.data = data;
   }

   @Override
   public String toString() {
      if (text == null) {
         StringJoiner joiner = new StringJoiner(" ", "(", ")");
         joiner.add(command.getName());
         joiner.add(path);
         if (data != null)
            joiner.add(comparator.getString(data));
         text = joiner.toString();
      }
      return text;
   }

   @Override
   public Action getAction() {
      return command;
   }

   @Override
   public Reference[] getArguments() {
      List<Reference> tokens = new ArrayList<>();
      tokens.add(new GenericReference(null, path, true));
      if (data != null)
         tokens.add((data instanceof Reference) ? (Reference)data : new GenericReference(null, data, true));
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public Reference getValue(ScriptContext context) {
      ScriptContext oldContext = configuration.getContextFactory().getScriptContext();
      if (context == null || context == oldContext)
         oldContext = null;
      else
         configuration.getContextFactory().setScriptContext(context);

      try {
         if (parts == null) {
            String txt = (path == null) ? "" : path.trim();
            String separator = txt.contains("/") ? "/" : ".";
            while (txt.startsWith(separator))
               txt = txt.substring(1);
            while (txt.endsWith(separator))
               txt = txt.substring(0, txt.length()-1);
            txt = txt.trim();
            if (txt.isEmpty())
               parts = Collections.EMPTY_LIST;
            else {
               parts = new ArrayList();
               for (String part : txt.split(separator)) {
                  part = part.trim();
                  Object value = isExpression(part)
                               ? new SimpleExpression(configuration, part.substring(1,part.length()-1), keywords)
                               : part;
                  parts.add(value);
               }
            }
         }
         Object value = command.execute(parts, data);
         return (value instanceof Reference) ? (Reference)value : new GenericReference(null, value, true);
      }
      finally {
         if (oldContext != null)
            configuration.getContextFactory().setScriptContext(oldContext);
      }
   }

   @Override
   public String getId() {
      return null;
   }

   @Override
   public Reference getValue() {
      return getValue(null);
   }

   @Override
   public Class<Reference> getType() {
      return Reference.class;
   }

   @Override
   public void setValue(Reference value) {
      Reference result = getValue();
      if (result != null)
         result.setValue(value);
   }

   private Action getAction(Command command) {
      if (COMMAND_ACTIONS == null) {
         COMMAND_ACTIONS = new TreeMap<>();
         ActionBuilder builder = new ActionBuilder(configuration)
                 .setWantOptimized(false)
                 .setWantSymbols(false)
                 .setIgnorePrecedence(false);
         for (Command cmd : Command.values())
            COMMAND_ACTIONS.put(cmd, builder.buildAction(cmd));
      }
      if (command == null)
         command = Command.GET;
      return COMMAND_ACTIONS.get(command);
   }

   private boolean isExpression(CharSequence txt) {
      if (txt == null || txt.length() < 2)
         return false;
      int index = START_EXPRESSION.indexOf(txt.charAt(0));
      if (index < 0)
         return false;
      return END_EXPRESSION.charAt(index) == txt.charAt(txt.length()-1);
   }
}
