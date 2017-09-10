package org.regadou.expression;

import java.util.*;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.regadou.damai.Reference;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.reference.GenericReference;
import org.regadou.reference.MapEntryReference;
import org.regadou.script.OperatorAction;

public class CompiledExpression extends CompiledScript implements Expression<Reference> {

   private Configuration configuration;
   private ScriptEngine engine;
   private Map<Operator,OperatorAction> operators;
   private String text;
   private List<Reference> tokens = new ArrayList<>();
   private Action action;

   public CompiledExpression(ScriptEngine engine, Collection<Reference> tokens, Configuration configuration) {
      this.engine = engine;
      this.configuration = configuration;
      if (tokens != null) {
         for (Reference token : tokens)
            addToken(token);
      }
   }

   @Override
   public String toString() {
      if (text == null) {
         text = (action == null) ? "" : action.getName();
         for (Object token : tokens) {
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
   public Reference[] getTokens() {
      return tokens.toArray(new Reference[tokens.size()]);
   }

   @Override
   public void addToken(Reference token) {
      text = null;
      if (action == null && (action = isAction(token)) != null) {
         if (tokens.size() > 1) {
             //TODO: try to make an entity out of it
            Reference subject = new GenericReference(null, tokens);
            tokens = new ArrayList();
            tokens.add(subject);
         }
      }
      else
         tokens.add(token);
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
            if (tokens.isEmpty())
               value = action;
            else
               value = action.execute(tokens.toArray());
            if (value instanceof Reference)
               return (Reference)value;
            else if (value instanceof Map.Entry)
               return new MapEntryReference((Map.Entry)value);
            else
               return new GenericReference(null, value);
         }
         else {
            switch (tokens.size()) {
               case 0:
                  return null;
               case 1:
                  return tokens.get(0);
               default:
                  if (tokens.get(0) instanceof Expression) {
                     Reference result = null;
                     for (Reference token : tokens) {
                        if (token instanceof Expression)
                           result = toReference(((Expression)token).getValue(context));
                        else
                           result = token;
                     }
                     return result;
                  }
                  //TODO: check if we have properties enumeration for an entity
                  return new GenericReference(null, tokens);
            }
         }
      }
      finally {
         if (oldContext != null)
            configuration.getContextFactory().setScriptContext(oldContext);
      }
   }

   public boolean isEmpty() {
      return action == null && tokens.isEmpty();
   }

   private Action isAction(Object token) {
      if (token instanceof Operator) {
         if (operators == null) {
            operators = new TreeMap<>();
            for (OperatorAction op : OperatorAction.getActions(configuration))
               operators.put(op.getOperator(), op);
         }
         return operators.get((Operator)token);
      }
      if (token instanceof Action)
         return (Action)token;
      if (token instanceof Reference)
         return isAction(((Reference)token).getValue());
      return null;
   }

   private Reference toReference(Object value) {
      return (value instanceof Reference) ? (Reference) value : new GenericReference(null, value, true);
   }
}
