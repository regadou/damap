package org.regadou.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Reference;
import org.regadou.script.OperatorAction;

public class SimpleExpression implements Expression<Reference> {

   private static class TokenList extends ArrayList {
      public TokenList(Object t1, Object t2) {
         super(Arrays.asList(t1, t2));
      }
   }

   private static final String ALPHA_SYMBOLS = "_$.-";
   private static final String ESCAPE_CHARS = "'`\"";
   private static final Map<String,OperatorAction> OPERATORS = new TreeMap<>();

   private Configuration configuration;
   private String text;
   private Action action;
   private Object param1, param2;

   public SimpleExpression(Configuration configuration, Action action, Object param1, Object param2) {
      this.configuration = configuration;
      this.action = action;
      this.param1 = param1;
      this.param2 = param2;
   }

   public SimpleExpression(Configuration configuration, String text) {
      this.configuration = configuration;
      this.text = text;
      if (text != null)
         parseExpression(text.toCharArray());
   }

   @Override
   public String toString() {
      if (text == null) {
         StringJoiner joiner = new StringJoiner(" ", "(", ")");
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

   @Override
   public Action getAction() {
      return action;
   }

   @Override
   public Reference[] getTokens() {
      if (param2 == null)
         return (param1 == null) ? new Reference[0] : new Reference[]{getReference(param1)};
      if (param1 == null)
         return new Reference[]{null, getReference(param2)};
      return new Reference[]{getReference(param1), getReference(param2)};
   }

   @Override
   public Reference getValue(ScriptContext context) {
      ScriptContext oldContext;
      if (context == null)
         oldContext = null;
      else {
         oldContext = configuration.getContextFactory().getScriptContext();
         configuration.getContextFactory().setScriptContext(context);
      }

      try {
         if (action != null)
            return getReference(action.execute(new Object[]{param1, param2}));
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

   private void parseExpression(char[] chars) {
      param1 = param2 = action = null;
      StringBuilder word = null;
      char escape = 0;
      boolean symbol = false;
      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (escape > 0) {
            if (c == escape) {
               escape = 0;
               insertToken(word.toString());
               word = null;
            }
            else
               word.append(c);
         }
         else if (isBlank(c))
            word = getToken(word, symbol);
         else if (ESCAPE_CHARS.indexOf(c) >= 0) {
            getToken(word, symbol);
            word = new StringBuilder();
            escape = c;
         }
         else if (word == null) {
            word = new StringBuilder(c+"");
            symbol = isSymbol(c);
         }
         else if (symbol && !isSymbol(c)) {
            getToken(word, symbol);
            word = new StringBuilder(c+"");
            symbol = false;
         }
         else if (!symbol && isSymbol(c)) {
            getToken(word, symbol);
            word = new StringBuilder(c+"");
            symbol = true;
         }
         else
            word.append(c);
      }
      getToken(word, symbol);
   }

   private StringBuilder getToken(StringBuilder chars, boolean symbol) {
      if (chars != null) {
         String word = chars.toString();
         List<OperatorAction> ops = symbol ? getOperators(word) : Collections.EMPTY_LIST;
         switch (ops.size()) {
            case 0:
               Object token;
               try { token = new Double(word); }
               catch (Exception e) { token = new ScriptContextProperty(configuration.getContextFactory(), word); }
               insertToken(token);
               break;
            case 1:
               insertToken(ops.get(0));
               break;
            default:
               boolean first = false;
               for (OperatorAction op : ops) {
                  if (first)
                     first = false;
                  else
                     insertToken(null);
                  insertToken(op);
               }
         }
      }
      return null;
   }

   private void insertToken(Object token) {
      if (token instanceof Action) {
         if (action == null)
            action = (Action)token;
         else {
            int oldprec = (action instanceof OperatorAction) ? ((OperatorAction)action).getPrecedence() : 0;
            int newprec = (token instanceof OperatorAction) ? ((OperatorAction)token).getPrecedence() : 0;
            if (newprec > oldprec)
               param2 = new SimpleExpression(configuration, (Action)token, param2, null);
            else {
               param1 = new SimpleExpression(configuration, action, param1, param2);
               action = (Action)token;
               param2 = null;
            }
         }
      }
      else if (action == null)
         param1 = setParameter(param1, token);
      else if (param2 == null)
         param2 = token;
      else if (param1 instanceof SimpleExpression && ((SimpleExpression)param1).param1 == null)
         ((SimpleExpression)param1).param2 = setParameter(((SimpleExpression)param1).param2, token);
      else
         param2 = setParameter(param2, token);
   }

   private Object setParameter(Object old, Object token) {
      if (old == null)
         return token;
      else if (old instanceof SimpleExpression) {
         SimpleExpression s  = (SimpleExpression)old;
         if (s.action == null)
            s.param1 = setParameter(s.param1, token);
         else
            s.param2 = setParameter(s.param2, token);
         return s;
      }
      else if (old instanceof TokenList) {
         ((TokenList)old).add(token);
         return old;
      }
      else
         return new TokenList(old, token);
   }

   private List<OperatorAction> getOperators(String word) {
      if (!isSymbol(word.charAt(0)))
         return Collections.EMPTY_LIST;
      if (OPERATORS.isEmpty()) {
         for (OperatorAction op : OperatorAction.getActions(configuration))
            OPERATORS.put(op.getName(), op);
      }
      OperatorAction op = OPERATORS.get(word);
      if (op != null)
         return Arrays.asList(op);
      for (int i = word.length() - 1; i > 0; i--) {
         OperatorAction op1 = OPERATORS.get(word.substring(0, i));
         OperatorAction op2 = OPERATORS.get(word.substring(i));
         if (op1 != null && op2 != null)
            return Arrays.asList(op1, op2);
      }
      //TODO: try to split in 3 or more tokens if word length > 2
      return Collections.EMPTY_LIST;
   }

   private boolean isBlank(char c) {
      return c <= 0x20 || (c >= 0x7F && c <= 0xA0);
   }

   private boolean isSymbol(char c) {
      if (ALPHA_SYMBOLS.indexOf(c) >= 0)
         return false;
      return (c > ' ' && c < '0') || (c > '9' && c < 'A')
          || (c > 'Z' && c < 'a') || (c > 'z' && c < 0x7F);
   }

   private boolean isDigit(char c) {
      return (c >= '0' && c <= '9');
   }

   private boolean isAlpha(char c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
          || (c >= 0xC0 && c <= 0x2AF) || ALPHA_SYMBOLS.indexOf(c) >= 0;
   }
}
