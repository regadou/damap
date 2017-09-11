package org.regadou.script;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.expression.SimpleExpression;
import org.regadou.reference.GenericReference;
import org.regadou.util.ArrayWrapper;
import org.regadou.util.FilterableIterable;

public class CommandAction implements Action {

   private static final Class[] SINGLE_PARAMETERS_TYPES = new Class[]{Object.class};
   private static final Class[] DOUBLE_PARAMETERS_TYPES = new Class[]{Object.class, Object.class};
   private static final String START_EXPRESSION = "([{";
   private static final String END_EXPRESSION = ")]}";

   public static Collection<CommandAction> getActions(Configuration configuration) {
      GenericComparator comparator = new GenericComparator(configuration);
      Collection<CommandAction> actions = new ArrayList<>();
      for (Command cmd : Command.values()) {
         CommandAction action = new CommandAction(cmd, configuration, comparator);
         actions.add(action);
      }
      return actions;
   }

   private Configuration configuration;
   private GenericComparator comparator;
   private Command command;

   public CommandAction(Command command, Configuration configuration, GenericComparator comparator) {
      this.command = command;
      this.configuration = configuration;
      this.comparator = comparator;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public boolean equals(Object that) {
      if (that instanceof Command)
         return that == command;
      if (that instanceof CommandAction)
         return ((CommandAction)that).command == command;
      return false;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public Object execute(Object... parameters) {
      Object path = null, data = null;
      if (parameters != null) {
         switch (parameters.length) {
            default:
            case 2:
               data = parameters[1];
            case 1:
               path = parameters[0];
               if (!command.isDataNeeded())
                  break;
            case 0:
         }
      }

      ScriptContext context = configuration.getContextFactory().getScriptContext();
      Reference result = new GenericReference(null, context, true);
      List parts = getPathParts(path);
      int stop = (command == Command.DESTROY) ? parts.size()-1 : parts.size();
      for (int p = 0; p < stop; p++) {
         Object part = parts.get(p);
         result = getProperty(result.getValue(), part, context);
         if (result == null)
            return (command == Command.DESTROY) ? false : null;
      }

      switch (command) {
         case SET:
            return comparator.setValue(result, data);
         case GET:
            return result;
         case CREATE:
            return comparator.addValue(result, data);
         case UPDATE:
            return comparator.mergeValue(result, data);
         case DESTROY:
            return comparator.removeValue(result, parts.get(stop));
         default:
            throw new RuntimeException("Unknown command "+command);
      }
   }

   @Override
   public String getName() {
      return command.name();
   }

   @Override
   public Class getReturnType() {
      return (command == Command.DESTROY) ? Boolean.class : Object.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return command.isDataNeeded() ? DOUBLE_PARAMETERS_TYPES : SINGLE_PARAMETERS_TYPES;
   }

   public Command getCommand() {
      return command;
   }

   private List getPathParts(Object path) {
      path = comparator.getValue(path);
      if (path instanceof List)
         return (List)path;
      if (path instanceof Collection)
         return new ArrayList((Collection)path);
      if (path instanceof CharSequence) {
         String txt = path.toString().trim();
         String separator = txt.contains("/") ? "/" : ".";
         while (txt.startsWith(separator))
            txt = txt.substring(1);
         while (txt.endsWith(separator))
            txt = txt.substring(0, txt.length()-1);
         txt = txt.trim();
         if (txt.isEmpty())
            return Collections.EMPTY_LIST;
         List parts = new ArrayList();
         for (String part : txt.split(separator)) {
            part = part.trim();
            Object value = isExpression(part)
                         ? new SimpleExpression(configuration, part.substring(1,part.length()-1))
                         : part;
            parts.add(value);
         }
         return parts;
      }
      if (path == null)
         return Collections.EMPTY_LIST;
      if (path.getClass().isArray())
         return new ArrayWrapper(path);
      if (path instanceof Reference)
         return getPathParts(((Reference)path).getValue());
      return Arrays.asList(path);
   }

   private Reference getProperty(Object value, Object property, ScriptContext cx) {
      if (property == null)
         return null;
      if (property instanceof Collection)
         return getArrayProperty(value, ((Collection)property).toArray(), cx);
      if (property instanceof Map)
         return getArrayProperty(value, ((Map)property).keySet().toArray(), cx);
      if (property.getClass().isArray())
         return getArrayProperty(value, property, cx);

      PropertyManager propertyManager = configuration.getPropertyManager();
      if (property instanceof Expression) {
         Filterable filterable;
         if (value instanceof Filterable)
            filterable = (Filterable)value;
         else {
            Collection src = configuration.getConverter().convert(value, Collection.class);
            filterable = new FilterableIterable(propertyManager, src);
         }
         Collection result = filterable.filter((Expression)property);
         return new GenericReference(null, result, true);
      }
      if (property instanceof Reference && ((Reference)property).getId() == null)
         return getProperty(value, ((Reference)property).getValue(), cx);
      return propertyManager.getProperty(value, String.valueOf(property));
   }

   private Reference getArrayProperty(Object value, Object property, ScriptContext cx) {
      int length = Array.getLength(property);
      List values = new ArrayList(length);
      for (int i = 0; i < length; i++)
         values.add(getProperty(value, Array.get(property, i), cx));
      return new GenericReference(null, values, true);
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
