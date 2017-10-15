package org.regadou.expression;

import java.util.*;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.action.ActionBuilder;
import org.regadou.damai.Reference;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.reference.GenericReference;
import org.regadou.reference.MapEntryReference;

public class DefaultExpression extends CompiledScript implements Expression<Reference> {

   private static Map<Operator,Action> OPERATORS;
   private static Action EVAL_ACTION;

   private Configuration configuration;
   private ScriptEngine engine;
   private String text;
   private List<Reference> arguments = new ArrayList<>();
   private Action action;

   public DefaultExpression(ScriptEngine engine, List<Reference> tokens, Configuration configuration) {
      this.engine = engine;
      this.configuration = configuration;
      if (tokens != null) {
         for (Reference token : tokens)
            addToken(token);
      }
   }

   public DefaultExpression(ScriptEngine engine, Expression[] expressions, Configuration configuration) {
      this.engine = engine;
      this.configuration = configuration;
      if (expressions != null)
         arguments = Arrays.asList(expressions);
      action = EVAL_ACTION;
   }

   @Override
   public String toString() {
      if (text == null) {
         text = (action == null) ? "" : action.getName();
         for (Object token : arguments) {
            if (!text.isEmpty())
               text += " ";
            text += String.valueOf(token);
         }
         text = "("+text+")";
      }
      return text;
   }

   @Override
   public Object eval(ScriptContext context) throws ScriptException {
      return getValue(context);
   }

   @Override
   public ScriptEngine getEngine() {
      return engine;
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

   @Override
   public Action getAction() {
      return action;
   }

   @Override
   public Reference[] getArguments() {
      return arguments.toArray(new Reference[arguments.size()]);
   }

   @Override
   public void addToken(Reference token) {
      text = null;
      if (action == null && (action = isAction(token)) != null) {
         if (arguments.size() > 1) {
             //TODO: try to make an entity out of it
            Reference subject = new GenericReference(null, arguments);
            arguments = new ArrayList();
            arguments.add(subject);
         }
      }
      else
         arguments.add(token);
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
         if (action != null) {
            Object value;
            if (arguments.isEmpty())
               value = action;
            else
               value = action.execute(arguments.toArray());
            if (value instanceof Reference)
               return (Reference)value;
            else if (value instanceof Map.Entry)
               return new MapEntryReference((Map.Entry)value);
            else
               return new GenericReference(null, value);
         }
         else {
            switch (arguments.size()) {
               case 0:
                  return null;
               case 1:
                  return arguments.get(0);
               default:
                  if (arguments.get(0) instanceof Expression) {
                     Reference result = null;
                     for (Reference token : arguments) {
                        if (token instanceof Expression)
                           result = toReference(((Expression)token).getValue(context));
                        else
                           result = token;
                     }
                     return result;
                  }
                  //TODO: check if we have properties enumeration for an entity
                  return new GenericReference(null, arguments);
            }
         }
      }
      finally {
         if (oldContext != null)
            configuration.getContextFactory().setScriptContext(oldContext);
      }
   }

   public boolean isEmpty() {
      return action == null && arguments.isEmpty();
   }

   public Action isAction(Object token) {
      if (token instanceof Operator) {
         if (OPERATORS == null) {
            OPERATORS = new TreeMap<>();
            ActionBuilder builder = new ActionBuilder(configuration)
                    .setWantOptimized(true)
                    .setWantSymbols(false)
                    .setIgnorePrecedence(true)
                    .setWantUpperCase(true);
            for (Operator op : Operator.values())
               OPERATORS.put(op, builder.buildAction(op));
         }
         return OPERATORS.get((Operator)token);
      }
      if (token instanceof Action)
         return (Action)token;
      if (token instanceof Expression || token instanceof Property)
         return null;
      if (token instanceof Reference)
         return isAction(((Reference)token).getValue());
      return null;
   }

   private Reference toReference(Object value) {
      return (value instanceof Reference) ? (Reference) value : new GenericReference(null, value, true);
   }
}
