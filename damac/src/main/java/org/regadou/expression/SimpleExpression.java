package org.regadou.expression;

import org.regadou.property.ScriptContextProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class SimpleExpression extends PrecedenceExpression {

   private static final String ALPHA_SYMBOLS = "_$.-";
   private static final String ESCAPE_CHARS = "'`\"";

   private Configuration configuration;
   private String text;

   public SimpleExpression(Configuration configuration, Action action, Object param1, Object param2) {
      super(null, configuration);
      this.configuration = configuration;
      addTokens((action == null) ? Action.NOOP : action, param1, param2);
   }

   public SimpleExpression(Configuration configuration, String text, Map keywords) {
      super(null, configuration);
      this.configuration = configuration;
      this.text = text;
      if (text != null)
         parseExpression(text.toCharArray(), keywords);
   }

   @Override
   public String toString() {
      if (text == null)
         text = super.toString();
      return text;
   }

   @Override
   public void addToken(Reference token) {
      throw new UnsupportedOperationException("Adding tokens is not supported with "+getClass().getName());
   }

   private void addTokens(Object...tokens) {
      for (Object token : tokens) {
         Reference ref = (token instanceof Reference) ? (Reference)token : new GenericReference(null, token, true);
         super.addToken(ref);
      }
   }

   private void parseExpression(char[] chars, Map keywords) {
      StringBuilder word = null;
      char escape = 0;
      boolean symbol = false;
      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (escape > 0) {
            if (c == escape) {
               escape = 0;
               addTokens(word.toString());
               word = null;
            }
            else
               word.append(c);
         }
         else if (isBlank(c))
            word = getToken(word, symbol, keywords);
         else if (ESCAPE_CHARS.indexOf(c) >= 0) {
            getToken(word, symbol, keywords);
            word = new StringBuilder();
            escape = c;
         }
         else if (word == null) {
            word = new StringBuilder(c+"");
            symbol = isSymbol(c);
         }
         else if (symbol && !isSymbol(c)) {
            getToken(word, symbol, keywords);
            word = new StringBuilder(c+"");
            symbol = false;
         }
         else if (!symbol && isSymbol(c)) {
            getToken(word, symbol, keywords);
            word = new StringBuilder(c+"");
            symbol = true;
         }
         else
            word.append(c);
      }
      getToken(word, symbol, keywords);
   }

   private StringBuilder getToken(StringBuilder chars, boolean symbol, Map keywords) {
      if (chars != null) {
         String word = chars.toString();
         List<Action> ops = symbol ? getOperators(word, keywords) : Collections.EMPTY_LIST;
         switch (ops.size()) {
            case 0:
               Object token;
               if (keywords.containsKey(word))
                  token = keywords.get(word);
               else {
                  try { token = new Double(word); }
                  catch (Exception e) { token = new ScriptContextProperty(configuration.getContextFactory(), word); }
               }
               addTokens(token);
               break;
            case 1:
               addTokens(ops.get(0));
               break;
            default:
               boolean first = false;
               for (Action op : ops) {
                  if (first)
                     first = false;
                  else
                     addTokens(null);
                  addTokens(op);
               }
         }
      }
      return null;
   }

   private List<Action> getOperators(String word, Map keywords) {
      if (!isSymbol(word.charAt(0)))
         return Collections.EMPTY_LIST;
      Object op = keywords.get(word);
      while (op instanceof Reference)
         op = ((Reference)op).getValue();
      if (op instanceof Action)
         return Arrays.asList((Action)op);
      for (int i = word.length() - 1; i > 0; i--) {
         Object op1 = keywords.get(word.substring(0, i));
         while (op1 instanceof Reference)
            op1 = ((Reference)op1).getValue();
         if (op1 instanceof Action) {
            Object op2 = keywords.get(word.substring(i));
            while (op2 instanceof Reference)
               op2 = ((Reference)op1).getValue();
            if (op2 instanceof Action)
               return Arrays.asList((Action)op1, (Action)op2);
         }
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
}
