package org.regadou.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;
import org.regadou.reference.MapEntryReference;

public class PrecedenceExpression extends DefaultExpression {

   private static class TokenList extends ArrayList<Reference> implements Reference<List> {

      public TokenList(Reference r1, Reference r2) {
         super(Arrays.asList(r1, r2));
      }

      @Override
      public String getId() {
         return null;
      }

      @Override
      public List getValue() {
         return new ArrayList(this);
      }

      @Override
      public Class getType() {
         return List.class;
      }

      @Override
      public void setValue(List value) {}
   }

   private Configuration configuration;
   private String text;
   private Action action;
   private Reference param1, param2;

   public PrecedenceExpression(ScriptEngine engine, Configuration configuration) {
      super(engine, Collections.EMPTY_LIST, configuration);
      this.configuration = configuration;
   }

   @Override
   public String toString() {
      if (text == null) {
         StringJoiner joiner = new StringJoiner(" ", "(", ")");
         //TODO: print in infix notation if action is an operator
         if (action != null)
            joiner.add(action.getName());
         if (param1 != null) {
            joiner.add(param1.toString());
            if (param2 != null)
               joiner.add(param2.toString());
         }
         else if (param2 != null)
            joiner.add("()").add(param2.toString());
         text = joiner.toString();
      }
      return text;
   }

   @Override
   public boolean isEmpty() {
      return action == null && param1 == null && param2 == null;
   }

   @Override
   public Action getAction() {
      return action;
   }

   @Override
   public Reference[] getArguments() {
      if (param2 == null)
         return (param1 == null) ? new Reference[0] : new Reference[]{getReference(param1)};
      if (param1 == null)
         return new Reference[]{null, getReference(param2)};
      return new Reference[]{getReference(param1), getReference(param2)};
   }

   @Override
   public Reference getValue(ScriptContext context) {
      ScriptContext oldContext = configuration.getContextFactory().getScriptContext();
      if (context == null || context == oldContext)
         oldContext = null;
      else
         configuration.getContextFactory().setScriptContext(context);

      try {
         if (action != null)
            return getReference(action.execute(getParameters(action.getParameterTypes())));
         else if (param1 == null)
            return getReference(param2);
         else if (param2 == null)
            return getReference(param1);
         else
            return getReference(new Object[]{param1, param2});
      }
      finally {
         if (oldContext != null)
            configuration.getContextFactory().setScriptContext(oldContext);
      }
   }

   @Override
   public void addToken(Reference token) {
      text = null;
      Action atoken;
      if ((atoken = isAction(token)) != null) {
         if (action == null)
            action = atoken;
         else {
            int oldprec = action.getPrecedence();
            int newprec = atoken.getPrecedence();
            if (newprec > oldprec)
               param2 = newExpression(atoken, param2);
            else {
               param1 = newExpression(action, param1, param2);
               action = atoken;
               param2 = null;
            }
         }
      }
      else if (action == null || (param1 == null && param2 == null))
         param1 = setParameter(param1, token);
      else
         param2 = setParameter(param2, token);
   }

   private Expression newExpression(Object...tokens) {
      Expression exp = new PrecedenceExpression(getEngine(), configuration);
      for (Object token : tokens)
         exp.addToken(getReference(token));
      return exp;
   }

   private Reference setParameter(Reference old, Reference token) {
      if (old == null)
         return token;
      else if (old instanceof Expression) {
         ((Expression)old).addToken(token);
         return old;
      }
      else if (old instanceof TokenList) {
         ((TokenList)old).add(token);
         return old;
      }
      else
         return new TokenList(old, token);
   }

   private Reference getReference(Object value) {
      if (value instanceof Reference)
         return (Reference)value;
      else if (value instanceof Map.Entry)
         return new MapEntryReference((Map.Entry)value);
      else if (value == null)
         return null;
      else
         return new GenericReference(null, value);
   }

   private Object[] getParameters(Class[] types) {
      if (types == null) {
         if (param2 == null)
            return (param1 == null) ? new Object[0] : new Object[]{param1};
         return new Object[]{param1, param2};
      }
      Object[] params = new Object[types.length];
      for (int p = 0; p < params.length; p++) {
         switch (p) {
            case 0:
               params[0] = param1;
               break;
            case 1:
               params[1] = param2;
               break;
            default:
               params[p] = null;
         }
      }
      return params;
   }
}
