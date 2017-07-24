package org.regadou.script;

import java.io.IOException;
import org.regadou.number.Time;
import org.regadou.number.Complex;
import org.regadou.number.Probability;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.regadou.system.Context;
import org.regadou.system.ContextWrapper;

public class SexlScriptEngine implements ScriptEngine, Compilable {

   private ScriptEngineFactory factory;
   private String punctuationChars = ",;:.!?";
   private String openingChars = "([{";
   private String closingChars = ")]}";
   private String quotingChars = "\"'`";
   private String symbolChars = "#%&*+-/<=>@\\^|~";
   private String alphaSymbols = "_";
   private int apostrophe = 0;
   private String commentEnding = "\n\r\0";

   private ScriptContext defaultContext;

   protected SexlScriptEngine(ScriptEngineFactory factory) {
      this.factory = factory;
   }

   @Override
   public Object eval(String script) throws ScriptException {
      return execute(script, defaultContext);
   }

   @Override
   public Object eval(Reader reader) throws ScriptException {
      try { return execute(Context.currentContext().read(reader), defaultContext); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public Object eval(String script, Bindings n) throws ScriptException {
      return execute(script, n);
   }

   @Override
   public Object eval(Reader reader, Bindings n) throws ScriptException {
      try { return execute(Context.currentContext().read(reader), n); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public Object eval(String script, ScriptContext context) throws ScriptException {
      return execute(script, context);
   }

   @Override
   public Object eval(Reader reader, ScriptContext context) throws ScriptException {
      try { return execute(Context.currentContext().read(reader), context); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public void put(String key, Object value) {
      if (defaultContext != null) {
         Bindings bindings = defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
         if (bindings == null) {
            bindings = new SimpleBindings();
            defaultContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
         }
         bindings.put(key, value);
      }
   }

   @Override
   public Object get(String key) {
      if (defaultContext != null) {
         Bindings bindings = defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
         if (bindings != null) {
            return bindings.get(key);
         }
      }
      return null;
   }

   @Override
   public Bindings createBindings() {
      return new SimpleBindings();
   }

   @Override
   public Bindings getBindings(int scope) {
      return (defaultContext == null) ? null : defaultContext.getBindings(scope);
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      if (defaultContext == null) {
         defaultContext = new SimpleScriptContext();
      }
      defaultContext.setBindings(bindings, scope);
   }

   @Override
   public ScriptContext getContext() {
      return defaultContext;
   }

   @Override
   public void setContext(ScriptContext context) {
      defaultContext = context;
   }

   @Override
   public ScriptEngineFactory getFactory() {
      return factory;
   }

   @Override
   public CompiledScript compile(String script) throws ScriptException {
      if (defaultContext == null) {
         defaultContext = new ContextWrapper();
      }
      return parseExpression(new ParserStatus(defaultContext, script));
    }

   @Override
   public CompiledScript compile(Reader reader) throws ScriptException {
      try { return compile(Context.currentContext().read(reader)); }
      catch (IOException e) { throw new RuntimeException(e); }
   }

   public boolean isAlpha(char c) {
      return alphaSymbols.indexOf(c) >= 0 || isLetter(c) || isAccent(c);
   }

   public boolean isSymbol(char c) {
      return symbolChars.indexOf(c) >= 0;
   }

   public boolean isOpener(char c) {
      return openingChars.indexOf(c) >= 0;
   }

   public boolean isCloser(char c) {
      return closingChars.indexOf(c) >= 0;
   }

   public boolean isQuote(char c) {
      return quotingChars.indexOf(c) >= 0;
   }

   public boolean isPunctuation(char c) {
      return punctuationChars.indexOf(c) >= 0;
   }

   public boolean isBlank(char c) {
      return c <= 0x20 || (c >= 0x7F && c <= 0xA0);
   }

   public boolean isDigit(char c) {
      return (c >= '0' && c <= '9');
   }

   public boolean isLetter(char c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
   }

   public boolean isAccent(char c) {
      return c >= 0xC0 && c <= 0x2AF;
   }

   private Object execute(String txt, Bindings bindings) {
      ScriptContext cx = new SimpleScriptContext();
      cx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
      return Context.currentContext().execute(parseExpression(new ParserStatus(cx, txt)), cx);
   }

   private Object execute(String txt, ScriptContext context) {
      if (context == null)
         context = defaultContext;
      return Context.currentContext().execute(parseExpression(new ParserStatus(context, txt)), context);
   }

   private CompiledExpression parseExpression(ParserStatus status) {
      List tokens = new ArrayList();
      char end = status.end;
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == end)
            break;
         else if (!isBlank(c)) {
            Object token = getToken(c, status);
            if (token != null) {
               tokens.add(token);
               status.previousToken = token;
            }
         }
         else
            status.linecount();
      }

      if (end > 0 && c != end)
         throw new RuntimeException("Syntax error: closing character "+end+" missing");
      return new CompiledExpression(this, tokens);
   }

   private Object getToken(char c, ParserStatus status) {
      if (isQuote(c)) {
         status.pos++;
         status.end = c;
         return parseString(status);
      }
      else if (isOpener(c)) {
         status.pos++;
         status.end = closingChars.charAt(openingChars.indexOf(c));
         return parseExpression(status);
      }
      else if (isCloser(c)) {
         throw new RuntimeException("Invalid end of sequence "+c);
      }
      else if (isDigit(c))
         return parseNumber(status);
      else if (isDigit(c))
         return parseNumber(status);
      else if (isAlpha(c))
         return parseName(status);
      else {
         switch (c) {
            case '\'':
            case '`':
               if (apostrophe > 0)
                  return parseName(status);
               else if (apostrophe < 0)
                  return null;
            case '+':
            case '-':
            case '.':
               if (isDigit(status.nextChar()))
                  return parseNumber(status);
               break;
            case '#':
               if (commentEnding.indexOf(status.previousChar()) >= 0)
                  return parseComment(status, c);
               break;
         }
         return parseSymbol(status);
      }
   }

   private String parseString(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      int start = status.pos;
      char end = (char)status.end;

      for (; status.pos < status.chars.length; status.pos++) {
         status.linecount();
         char c = status.chars[status.pos];
         if (c == end)
            return buffer.toString();
         else if (c == '\\') {
            status.pos++;
            if (status.pos >= status.chars.length)
               break;
            status.linecount();
            c = status.chars[status.pos];
            switch (c) {
               case 'b':
                  buffer.append('\b');
                  break;
               case 'f':
                  buffer.append('\f');
                  break;
               case 'n':
                  buffer.append('\n');
                  break;
               case 'r':
                  buffer.append('\r');
                  break;
               case 't':
                  buffer.append('\t');
                  break;
               case 'x':
                  try {
                     int ascii = Integer.parseInt(new String(status.chars,status.pos+1, 2), 16);
                     buffer.append((char)ascii);
                     status.pos += 2;
                  } catch (Exception e) {
                     throw new RuntimeException("Invalid ascii escape: "+e.getMessage());
                  }
                  break;
               case 'u':
                  try {
                     int ascii = Integer.parseInt(new String(status.chars,status.pos+1, 4), 16);
                     buffer.append((char)ascii);
                     status.pos += 4;
                  } catch (Exception e) {
                     throw new RuntimeException("Invalid unicode escape: "+e.getMessage());
                  }
                  break;
               case '"':
               case '\'':
               case '\\':
                  buffer.append(c);
                  break;
                default:
                  throw new RuntimeException("Invalid escape \\"+c);
            }
         }
         else
            buffer.append(c);
      }

      throw new RuntimeException("End of string not found after "+new String(status.chars, start, status.pos-start));
   }

   private Number parseNumber(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      boolean end=false, digit=false, hexa=false, decimal=false, exponent=false,
              complex=false, time=false, sign=false;

      for (; status.pos < status.chars.length; status.pos++) {
         char c = status.chars[status.pos];
         if (c == status.end) {
            status.pos--;
            break;
         }
         switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
               digit = sign = true;
               break;
            case '-':
               if (isBlank(status.nextChar())) {
                  end = true;
                  break;
               }
               else if (time)
                  break;
               else if (digit && !sign && !decimal && !hexa && !exponent && !complex) {
                  time = true;
                  break;
               }
            case '+':
               if (isBlank(status.nextChar())) {
                  end = true;
                  break;
               }
               else if (digit || sign)
                  end = true;
               else
                  sign = true;
               break;
            case '.':
               if (isBlank(status.nextChar()) || decimal || exponent || hexa)
                  end = true;
               else if (!time)
                  decimal = true;
               break;
            case ':':
               if (isBlank(status.nextChar()))
                  end = true;
               else if (time)
                  break;
               else if (!digit || decimal || hexa || exponent || complex)
                  end = true;
               else
                  time = true;
               break;
            case '%':
               if (digit && !hexa && !decimal && !exponent && !complex && !time)
                  return new Probability(buffer.append(c).toString());
               else
                  end = true;
               break;
            case 'e':
            case 'E':
               if (hexa)
                  break;
               else if (!digit || exponent)
                  end = true;
               else {
                  exponent = decimal = true;
                  digit = sign = false;
               }
               break;
            case 'a':
            case 'A':
            case 'b':
            case 'B':
            case 'c':
            case 'C':
            case 'd':
            case 'D':
            case 'f':
            case 'F':
               if (!hexa)
                  end = true;
               break;
            case 'x':
            case 'X':
               if (hexa || decimal || exponent || complex || time)
                  end = true;
               else if (buffer.toString().equals("0"))
                  hexa = true;
               else
                  end = true;
               break;
            case 'i':
            case 'I':
               if (hexa || complex || time)
                  end = true;
               else {
                  complex = true;
                  decimal = exponent = digit = sign = false;
               }
               break;
            case 't':
            case 'T':
               if (time)
                  break;
            default:
               end = true;
         }
         if (end) {
            status.pos--;
            break;
         }
         else
            buffer.append(c);
      }

      String txt = buffer.toString();
      if (!digit)
         return null;
      else if (hexa)
         return Integer.parseInt(txt.substring(2), 16);
      else if (complex)
         return new Complex(txt);
      else if (decimal || exponent)
         return new Double(txt);
      else if (time)
         return new Time(txt);
      else
         return new Long(txt);
   }

   private Object parseName(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      boolean uri = false, java = false;

      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (c == status.end) {
            status.pos--;
            break;
         }
         else if (isBlank(c))
            break;
         else if (uri || java)
            continue;
         char next = status.nextChar();
         switch (c) {
            case '.':
               if (isLetter(next)) {
                  uri = true;
                  continue;
               }
               break;
            case ':':
               if (!isAlpha(next) && !isDigit(next) && next != '/' && next != '.')
                  break;
               uri = true;
               continue;
            case '-':
               if (isAlpha(next))
                  continue;
               break;
            case '\'':
            case '`':
               if (apostrophe > 0)
                  continue;
               else if (apostrophe < 0) {
                  status.pos++;
                  length++;
               }
               break;
         }
         if (!isAlpha(c) && !isDigit(c)) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);

      return new ScriptContextReference(status.cx, txt);
   }

   private Object parseSymbol(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (isPunctuation(c)) {
            if (start != status.pos)
               status.pos--;
            else
               length++;
            break;
         }
         else if (c == status.end || isBlank(c) || !isSymbol(c)) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);
      return new ScriptContextReference(status.cx, txt);
   }

   private Object parseComment(ParserStatus status, char end) {
      int sequence = 0;
      while (status.pos < status.chars.length && status.chars[status.pos] == end) {
         status.linecount();
         status.pos++;
         sequence++;
      }

      for (; status.pos < status.chars.length; status.pos++) {
         status.linecount();
         char c = status.chars[status.pos];
         if (sequence == 1) {
            if (c == '\n' || c == '\r') {
               status.pos--;
               break;
            }
         }
         else if (c == end) {
            int got = 0;
            while (status.pos < status.chars.length && status.chars[status.pos] == end) {
               status.linecount();
               status.pos++;
               got++;
            }
            if (got >= sequence) {
               status.pos--;
               break;
            }
         }
      }
      return null;
   }
}
