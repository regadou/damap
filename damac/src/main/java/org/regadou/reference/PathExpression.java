package org.regadou.reference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.script.GenericComparator;

public class PathExpression implements Expression {

   private static final String START_EXPRESSION = "([{";
   private static final String END_EXPRESSION = ")]}";
   private Configuration configuration;
   private GenericComparator comparator;
   private Command command;
   private List path = new ArrayList();
   private Object data;
   private String text;

   public PathExpression(Configuration configuration, Command command, String path, Object data) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
      this.command = (command == null) ? Command.GET : command;
      this.data = data;
      if (path != null) {
         while (path.startsWith("/"))
            path = path.substring(1);
         while (path.endsWith("/"))
            path = path.substring(0, path.length()-1);
         path = path.trim();
         if (!path.isEmpty()) {
            for (String part : path.split("/")) {
               Object p = isExpression(part) ? new SimpleExpression(configuration, part.substring(1,part.length()-1)) : part;
               this.path.add(p);
            }
         }
      }
   }

   @Override
   public String toString() {
      if (text == null) {
         StringJoiner joiner = new StringJoiner(" ", "(", ")");
         joiner.add(command.getName());
         StringJoiner parts = new StringJoiner(" ", "[", "]");
         for (Object part : path)
            joiner.add(part.toString());
         joiner.add(parts.toString());
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
      tokens.add(new ReferenceHolder(null, path, true));
      if (data != null)
         tokens.add((data instanceof Reference) ? (Reference)data : new ReferenceHolder(null, data, true));
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public Reference getValue(ScriptContext context) {
      ScriptContext oldContext = configuration.getContextFactory().getScriptContext();
      if (context == null) {
         context = oldContext;
         oldContext = null;
      }
      else
         configuration.getContextFactory().setScriptContext(context);

      try {
         Reference result = new ReferenceHolder(null, context, true);
         Reference parent = null;
         for (Object part : path) {
            parent = result;
            result = getProperty(result.getValue(), part, context);
            if (result == null)
               return null;
         }
         switch (command) {
            case SET:
               comparator.setValue(result, data);
            case GET:
               return result;
            case CREATE:
               return getReference(comparator.addValue(result, data));
            case MODIFY:
               return getReference(comparator.mergeValue(result, data));
            case DESTROY:
               comparator.removeValue(parent, result.getName());
               return new ReferenceHolder(null, null, true);
            default:
               throw new RuntimeException("Unknown command "+command);
         }
      }
      finally {
         if (oldContext != null)
            configuration.getContextFactory().setScriptContext(oldContext);
      }
   }

   @Override
   public String getName() {
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
         Collection result = comparator.getFilteredCollection(value, (Expression)property);
         return new ReferenceHolder(null, result, true);
      }
      return configuration.getPropertyManager().getProperty(value, String.valueOf(property));
   }

   private Reference getArrayProperty(Object value, Object property, ScriptContext cx) {
      int length = Array.getLength(property);
      List values = new ArrayList(length);
      for (int i = 0; i < length; i++)
         values.add(getProperty(value, Array.get(property, i), cx));
      return new ReferenceHolder(null, values, true);
   }

   private Reference getReference(Object value) {
      return (value instanceof Reference) ? (Reference)value : new ReferenceHolder(null, value, true);
   }

   private boolean isExpression(String txt) {
      if (txt == null || txt.length() < 2)
         return false;
      int index = START_EXPRESSION.indexOf(txt.charAt(0));
      if (index < 0)
         return false;
      return END_EXPRESSION.charAt(index) == txt.charAt(txt.length()-1);
   }
}
