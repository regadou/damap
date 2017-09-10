package org.regadou.script;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.damai.ScriptContextFactory;
import org.regadou.reference.GenericReference;
import org.regadou.util.FilterableIterable;

public class CommandAction implements Action {

   private static final Class[] SINGLE_PARAMETERS_TYPES = new Class[]{Object.class};
   private static final Class[] DOUBLE_PARAMETERS_TYPES = new Class[]{Object.class, Object.class};

   public static Collection<CommandAction> getActions(Configuration configuration) {
      GenericComparator comparator = new GenericComparator(configuration);
      ScriptContextFactory contextFactory = configuration.getContextFactory();
      PropertyManager propertyManager = configuration.getPropertyManager();
      Converter converter = configuration.getConverter();
      Collection<CommandAction> actions = new ArrayList<>();
      for (Command cmd : Command.values()) {
         CommandAction action = new CommandAction(cmd, comparator, contextFactory, propertyManager, converter);
         actions.add(action);
      }
      return actions;
   }

   private GenericComparator comparator;
   private ScriptContextFactory contextFactory;
   private PropertyManager propertyManager;
   private Converter converter;
   private Command command;

   public CommandAction(Command command, GenericComparator comparator, ScriptContextFactory contextFactory, PropertyManager propertyManager, Converter converter) {
      this.command = command;
      this.comparator = comparator;
      this.contextFactory = contextFactory;
      this.propertyManager = propertyManager;
      this.converter = converter;
   }

   @Override
   public Object execute(Object... parameters) {
      Object path = null, data = null;
      if (parameters != null) {
         switch (parameters.length) {
            case 1:
               path = parameters[0];
               if (!command.isDataNeeded())
                  break;
            case 2:
            default:
               data = parameters[1];
            case 0:
         }
      }

      ScriptContext context = contextFactory.getScriptContext();
      Reference result = new GenericReference(null, context, true);
      Reference parent = null;
      for (Object part : getPathParts(path)) {
         parent = result;
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
            return comparator.removeValue(parent, result.getId());
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
      if (path == null)
         return new ArrayList();
      if (path instanceof CharSequence) {
         String txt = path.toString().trim();
         String separator = txt.contains("/") ? "/" : ".";
         while (txt.startsWith(separator))
            txt = txt.substring(1);
         while (txt.endsWith(separator))
            txt = txt.substring(0, txt.length()-1);
         txt = txt.trim();
         List parts = new ArrayList();
         if (!txt.isEmpty()) {
            for (String part : txt.split(separator))
               parts.add(part.trim());
         }
         return parts;
      }
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
      if (property instanceof Expression) {
         Filterable filterable;
         if (value instanceof Filterable)
            filterable = (Filterable)value;
         else {
            Collection src = converter.convert(value, Collection.class);
            filterable = new FilterableIterable(propertyManager, src);
         }
         Collection result = filterable.filter((Expression)property);
         return new GenericReference(null, result, true);
      }
      return propertyManager.getProperty(value, String.valueOf(property));
   }

   private Reference getArrayProperty(Object value, Object property, ScriptContext cx) {
      int length = Array.getLength(property);
      List values = new ArrayList(length);
      for (int i = 0; i < length; i++)
         values.add(getProperty(value, Array.get(property, i), cx));
      return new GenericReference(null, values, true);
   }
}
