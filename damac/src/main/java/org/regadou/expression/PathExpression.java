package org.regadou.expression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;
import org.regadou.action.CommandAction;
import org.regadou.action.GenericComparator;

public class PathExpression implements Expression<Reference> {

   private static Map<Command, Action> COMMAND_ACTIONS;

   private Configuration configuration;
   private GenericComparator comparator;
   private Action command;
   private String path;
   private Object data;
   private String text;

   public PathExpression(Configuration configuration, Command command, String path, Object data) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
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
   public Reference[] getTokens() {
      List<Reference> tokens = new ArrayList<>();
      tokens.add(new GenericReference(null, path, true));
      if (data != null)
         tokens.add((data instanceof Reference) ? (Reference)data : new GenericReference(null, data, true));
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public Reference getValue(ScriptContext context) {
      ScriptContext oldContext = configuration.getContextFactory().getScriptContext();
      if (context == null)
         oldContext = null;
      else
         configuration.getContextFactory().setScriptContext(context);

      try {
         Object value = command.execute(path, data);
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
         COMMAND_ACTIONS = new LinkedHashMap<>();
         for (CommandAction action : CommandAction.getActions(configuration))
            COMMAND_ACTIONS.put(action.getCommand(), action);
      }
      if (command == null)
         command = Command.GET;
      return COMMAND_ACTIONS.get(command);
   }
}
