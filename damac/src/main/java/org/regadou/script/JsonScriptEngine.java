package org.regadou.script;

import org.regadou.property.ScriptContextProperty;
import org.regadou.expression.DefaultExpression;
import java.io.Reader;
import java.util.*;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.damai.Configuration;
import org.regadou.damai.Converter;
import org.regadou.damai.Printable;
import org.regadou.damai.Reference;
import org.regadou.expression.PrecedenceExpression;
import org.regadou.number.Complex;
import org.regadou.number.Probability;
import org.regadou.number.Time;
import org.regadou.reference.GenericReference;
import org.regadou.util.StringInput;


public class JsonScriptEngine implements ScriptEngine, Compilable, Printable {

   private static final int MINIMUM_TERMINALS = 3;
   private static final String SYNTAX_SYMBOLS = "()[]{}\"'`,;";
   private static final char FIRST_ACCENT = 0xC0;
   private static final char LAST_ACCENT = 0x2AF;

   private ScriptEngineFactory factory;
   private Configuration configuration;
   private ScriptContext context;
   private List<String> schemes;
   private Map<String,Reference> keywords;

   public JsonScriptEngine(ScriptEngineFactory factory, Configuration configuration, Map<String,Reference> keywords) {
      this.factory = factory;
      this.configuration = configuration;
      this.schemes = Arrays.asList(configuration.getResourceManager().getSchemes());
      this.keywords = keywords;
   }

   @Override
   public Object eval(String script) throws ScriptException {
      return eval(script, getContext());
   }

   @Override
   public Object eval(Reader reader) throws ScriptException {
      return eval(new StringInput(reader).toString(), getContext());
   }

   @Override
   public Object eval(String script, Bindings bindings) throws ScriptException {
      ScriptContext cx;
      if (bindings == null)
         cx = getContext();
      else {
         cx = new DefaultScriptContext();
         cx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
         cx.setBindings(configuration.getEngineManager().getBindings(), ScriptContext.GLOBAL_SCOPE);
      }
      return eval(script, cx);
   }

   @Override
   public Object eval(Reader reader, Bindings n) throws ScriptException {
      return eval(new StringInput(reader).toString(), n);
   }

   @Override
   public Object eval(String script, ScriptContext context) throws ScriptException {
      return parse(script, context);
   }

   @Override
   public Object eval(Reader reader, ScriptContext context) throws ScriptException {
      return eval(new StringInput(reader).toString(), context);
   }

   @Override
   public void put(String key, Object value) {
      ScriptContext cx = getContext();
      Bindings bindings = cx.getBindings(ScriptContext.ENGINE_SCOPE);
      if (bindings == null) {
         bindings = new SimpleBindings();
         cx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
      }
      bindings.put(key, value);
   }

   @Override
   public Object get(String key) {
      Bindings bindings = getContext().getBindings(ScriptContext.ENGINE_SCOPE);
      return (bindings != null) ? bindings.get(key) : null;
   }

   @Override
   public Bindings createBindings() {
      return new SimpleBindings();
   }

   @Override
   public Bindings getBindings(int scope) {
      return getContext().getBindings(scope);
   }

   @Override
   public void setBindings(Bindings bindings, int scope) {
      getContext().setBindings(bindings, scope);
   }

   @Override
   public ScriptContext getContext() {
      return (context == null) ? configuration.getContextFactory().getScriptContext() : context;
   }

   @Override
   public void setContext(ScriptContext context) {
      this.context = context;
   }

   @Override
   public ScriptEngineFactory getFactory() {
      return factory;
   }

   @Override
   public CompiledScript compile(String script) throws ScriptException {
      return parse(script, getContext());
    }

   @Override
   public CompiledScript compile(Reader reader) throws ScriptException {
      return parse(new StringInput(reader).toString(), getContext());
   }

   @Override
   public String print(Object obj) {
      return configuration.getConverter().convert(obj, String.class);
   }

   private boolean isBlank(char c) {
      return c <= 0x20 || (c >= 0x7F && c <= 0xA0);
   }

   private boolean isSymbol(char c) {
      if (c == '_')
         return false;
      return (c > ' ' && c < '0') || (c > '9' && c < 'A')
          || (c > 'Z' && c < 'a') || (c > 'z' && c < 0x7F);
   }

   private boolean isDigit(char c) {
      return (c >= '0' && c <= '9');
   }

   private boolean isAlpha(char c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'
          || (c >= FIRST_ACCENT && c <= LAST_ACCENT);
   }

   private DefaultExpression parse(String txt, ScriptContext cx) {
      try { return parseExpression(new ParserStatus(txt, cx)); }
      catch (Exception e) {
         throw new RuntimeException("Exception while parsing the following text:\n"+txt, e);
      }
   }

   private Reference getToken(char c, ParserStatus status) {
      switch (c) {
         case '\'':
         case '`':
         case '"':
            status.pos++;
            status.end = status.end2 = c;
            return parseString(status);
         case '(':
            status.pos++;
            status.end = status.end2 = ')';
            return parseExpression(status);
         case '[':
            status.pos++;
            return parseArray(status);
         case '{':
            status.pos++;
            return parseObject(status);
         case '+':
         case '.':
         case '-':
            return isDigit(status.nextChar())
                 ? parseNumber(status) : parseSymbol(status);
         case ')':
         case ']':
         case '}':
            throw new RuntimeException("Invalid end of sequence "+c);
         case ',':
         case ';':
            return keywords.get(c+"");
         default:
            if (isDigit(c))
               return parseNumber(status);
            else if (isAlpha(c))
               return parseName(status);
            else
               return parseSymbol(status);
      }
   }

   private DefaultExpression parseExpression(ParserStatus status) {
      List<Reference> expressions = null;
      DefaultExpression exp = new PrecedenceExpression(this, configuration);
      char end = status.end;
      char end2 = status.end2;
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == end || c == end2)
            break;
         else if (!isBlank(c)) {
            Reference token = getToken(c, status);
            if (token != null) {
               exp.addToken(token);
               status.previousToken = token;
            }
            else if (!exp.isEmpty()) {
               if (expressions == null)
                  expressions = new ArrayList<>();
               expressions.add(exp);
               exp = new PrecedenceExpression(this, configuration);
            }
         }
      }

      if (end > 0 && c != end)
         throw new RuntimeException("Syntax error: closing character "+end+" missing");
      if (expressions == null)
         return exp;
      if (!exp.isEmpty())
         expressions.add(exp);
      return new DefaultExpression(this, expressions, configuration);
   }

   private Reference parseToken(ParserStatus status) {
      Reference elem = null;
      int start = status.pos;
      char end = status.end;
      char end2 = status.end2;
      char c = 0;
      boolean gotBlank = false;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == end || c == end2)
            break;
         else if (isBlank(c))
            gotBlank = true;
         else if (elem != null)
            throw new RuntimeException("Syntax error (element already found) after "+new String(status.chars, start, status.pos-start));
         else if (gotBlank)
            throw new RuntimeException("Syntax error (space in identifier) after "+new String(status.chars, start, status.pos-start));
         else
            elem = getToken(c, status);
      }

      if (end != 0 && status.pos >= status.chars.length) {
         String endchars = ""+status.end;
         if (status.end != status.end2)
            endchars += status.end2;
         throw new RuntimeException("Syntax error (end character "+endchars+" not found) after "+new String(status.chars, start, status.pos-start));
      }
      else
         return elem;
   }

   private Reference parseString(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      int start = status.pos;
      char end = status.end;
      char end2 = status.end2;
      int terminals = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         char c = status.chars[status.pos];
         if (c == end || c == end2) {
            if (terminals == 0 || terminals >= MINIMUM_TERMINALS)
               return new GenericReference(null, buffer.toString(), true);
            else
               terminals++;
         }
         else if (terminals > 0) {
            while (terminals > 1) {
               buffer.append(end);
               terminals--;
            }
            buffer.append(c);
         }
         else if (c == '\\') {
            status.pos++;
            if (status.pos >= status.chars.length)
               break;
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
         else if (c < 0x20 && buffer.length() == 0) {
            terminals++;
            buffer.append(c);
         }
         else
            buffer.append(c);
      }

      throw new RuntimeException("End of string not found after "+new String(status.chars, start, status.pos-start));
   }

   private Reference parseNumber(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      boolean end=false, digit=false, hexa=false, decimal=false, exponent=false,
              complex=false, time=false, sign=false;

      for (; status.pos < status.chars.length; status.pos++) {
         char c = status.chars[status.pos];
         if (c == status.end || c == status.end2) {
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
               if (time)
                  break;
               else if (digit && !sign && !decimal && !hexa && !exponent && !complex) {
                  time = true;
                  break;
               }
            case '+':
               if (digit || sign)
                  end = true;
               else
                  sign = true;
               break;
            case '.':
               if (decimal || exponent || hexa)
                  end = true;
               else if (!time)
                  decimal = true;
               break;
            case ':':
               if (time)
                  break;
               else if (!digit || decimal || hexa || exponent || complex)
                  end = true;
               else
                  time = true;
               break;
            case '%':
               if (digit && !hexa && !decimal && !exponent && !complex && !time)
                  return new GenericReference(null, new Probability(buffer.append(c).toString()), true);
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
         return new GenericReference(null, new Integer(Integer.parseInt(txt.substring(2), 16)), true);
      else if (complex)
         return new GenericReference(null, new Complex(txt), true);
      else if (decimal || exponent)
         return new GenericReference(null, new Double(txt), true);
      else if (time)
         return new GenericReference(null, new Time(txt), true);
      else
         return new GenericReference(null, new Long(txt), true);
   }

   private Reference parseArray(ParserStatus status) {
      List lst = new ArrayList();
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == ']')
            break;
         else if (!isBlank(c)) {
            status.end = ',';
            status.end2 = ']';
            Object token = parseToken(status);
            if (token != null)
               lst.add(token);
            c = status.chars[status.pos];
            if (c == ']')
               break;
            else if (token == null)
               throw new RuntimeException("No token between commas");
         }
      }

      if (c != ']')
         throw new RuntimeException("Missing end of array ]");
      return new GenericReference(null, lst, true);
   }

   private Reference parseObject(ParserStatus status) {
      Map map = new LinkedHashMap();
      String key = null;
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == '}')
            break;
         else if (!isBlank(c)) {
            status.end = (key == null) ? ':' : ',';
            status.end2 = '}';
            Object token = parseToken(status);
            if (token == null)
               ;
            else if (key == null) {
               if (token instanceof String)
                  key = token.toString();
               else if (token instanceof Map.Entry)
                  key = ((Map.Entry)token).getKey().toString();
               else
                  throw new RuntimeException(token+" is not a valid object key");
            } else {
               map.put(key, token);
               key = null;
            }
            c = status.chars[status.pos];
            if (c == '}')
               break;
            else if (token == null)
               throw new RuntimeException("No token in colon-comma sequence");
         }
      }

      if (key != null)
         throw new RuntimeException("key "+key+" does not have a value");
      else if (c != '}')
         throw new RuntimeException("Missing end of object }");
      Object obj = map;
      Object type = map.get("class");
      if (type != null) {
         try {
            Converter converter = configuration.getConverter();
            Class klass = Class.forName(type.toString());
            BeanMap bean = new BeanMap(klass.newInstance());
            for (Object k : map.keySet()) {
               String name = String.valueOf(k);
               if (!bean.containsKey(name))
                  continue;
               Class t = bean.getType(name);
               Object value = map.get(k);
               if (!t.isAssignableFrom((value == null) ? Void.class : value.getClass()))
                  value = converter.convert(value, t);
               bean.put(name, value);
            }
            obj = bean.getBean();
         }
         catch (ClassNotFoundException e) {}
         catch (Exception e) { map.put("error", e.toString()); }
      }
      return new GenericReference(null, obj, true);
   }

   private Reference parseName(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      char next;
      boolean uri = false, java = false;
      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (c == status.end || c == status.end2) {
            status.pos--;
            break;
         }
         else if (uri) {
            if (isBlank(c))
               break;
            continue;
         }
         else if (java) {
            next = status.nextChar();
            if (isAlpha(next) || isDigit(next) || next == '.')
               continue;
            break;
         }
         switch (c) {
            case ':':
               if (java)
                  break;
               next = status.nextChar();
               if (!isAlpha(next) && next != '/') {
                  c = ' ';
                  break;
               }
               String scheme = new String(status.chars, start, status.pos-start);
               if (!schemes.contains(scheme))
                  throw new RuntimeException("unknown uri scheme "+scheme);
               uri = true;
               continue;
            case '.':
               if (isAlpha(status.nextChar())
                   && Package.getPackage(new String(status.chars, start, length)) != null) {
                  java = true;
                  continue;
               }
         }
         if (isBlank(c) || isSymbol(c)) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);
      if (keywords.containsKey(txt))
         return keywords.get(txt);
      if (uri || java) {
         Reference r = configuration.getResourceManager().getResource(txt);
         if (r != null)
            return r;
      }
      return (status.cx != null) ? new ScriptContextProperty(status.cx, txt)
                                 : new ScriptContextProperty(configuration.getContextFactory(), txt);
   }

   private Reference parseSymbol(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (SYNTAX_SYMBOLS.indexOf(c) >= 0) {
            if (start != status.pos)
               status.pos--;
            else
               length++;
            break;
         }
         else if (isBlank(c) || !isSymbol(c) || c == status.end || c == status.end2) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);
      if (!keywords.containsKey(txt))
         throw new RuntimeException("Unknown symbol "+txt);
      return keywords.get(txt);
   }
}
