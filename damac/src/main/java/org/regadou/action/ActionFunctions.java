package org.regadou.action;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import javax.script.ScriptContext;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.collection.ArrayWrapper;
import org.regadou.collection.FilterableIterable;
import org.regadou.collection.Range;
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.Operator;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.reference.GenericReference;

public class ActionFunctions {

   private static enum Type { COLLECTION, NUMERIC, ENTITY, STRING }
   private static final BiFunction NOOP = (x, y) -> null;

   private static ActionFunctions INSTANCE;

   public static BiFunction getFunction(Action action, Configuration configuration) {
      if (INSTANCE == null)
         INSTANCE = new ActionFunctions(configuration);
      return INSTANCE.getFunction(action);
   }

   public static int getPrecedence(Action action) {
      if (action instanceof Operator) {
         switch ((Operator)action) {
            case EXPONANT: return 10;
            case ROOT: return 9;
            case LOG: return 8;
            case MULTIPLY: return 7;
            case DIVIDE: return 7;
            case MODULO: return 7;
            case ADD: return 6;
            case SUBTRACT: return 6;
            case NOT: return 5;
            case FROM: return 4;
            case TO: return 4;
            case LESSER: return 3;
            case LESSEQ: return 3;
            case GREATER: return 3;
            case GREATEQ: return 3;
            case EQUAL: return 3;
            case NOTEQUAL: return 3;
            case AND: return 2;
            case OR: return 2;
            case IN: return 1;
            case IF: return -1;
            case ELSE: return -2;
            case WHILE: return -3;
            case DO: return -4;
            case HAVE: return -5;
            case JOIN: return -6;
            case IS: return -7;
            default: throw new RuntimeException("Unknown operator "+action);
         }
      }
      else if (action instanceof BinaryAction)
         return ((BinaryAction)action).getPrecedence();
      else
         return 0;
   }

   private Configuration configuration;
   private GenericComparator comparator;

   private ActionFunctions(Configuration configuration) {
      this.configuration = configuration;
      this.comparator = new GenericComparator(configuration);
   }

   private BiFunction getFunction(Action action) {
      if (action instanceof Operator) {
         switch ((Operator)action) {
            case EQUAL:
               return (p1, p2) -> comparator.compare(p1, p2) == 0;
            case NOTEQUAL:
               return (p1, p2) -> comparator.compare(p1, p2) != 0;
            case LESSER:
               return (p1, p2) -> comparator.compare(p1, p2) < 0;
            case LESSEQ:
               return (p1, p2) -> comparator.compare(p1, p2) <= 0;
            case GREATER:
               return (p1, p2) -> comparator.compare(p1, p2) > 0;
            case GREATEQ:
               return (p1, p2) -> comparator.compare(p1, p2) >= 0;
            case AND:
               return (p1, p2) -> !comparator.isEmpty(p1) && !comparator.isEmpty(p2);
            case OR:
               return (p1, p2) -> !comparator.isEmpty(p1) || !comparator.isEmpty(p2);
            case NOT:
               return (p1, p2) -> comparator.isEmpty(p1) && comparator.isEmpty(p2);
            case IN:
               return IN_FUNCTION;
            case ADD:
               return ADD_FUNCTION;
            case SUBTRACT:
               return SUBTRACT_FUNCTION;
            case JOIN:
               return JOIN_FUNCTION;
            case HAVE:
               return (p1, p2) -> getProperty(comparator.getValue(p1), p2);
            case FROM:
               return (p1, p2) -> getProperty(comparator.getValue(p2), p1);
            case TO:
               return TO_FUNCTION;
            case EXPONANT:
               return (p1, p2) -> Math.pow(getNumber(p1), getNumber(p2));
            case ROOT:
               return (p1, p2) -> Math.pow(getNumber(p1), 1/getNumber(p2));
            case LOG:
               return (p1, p2) -> Math.log(getNumber(p1)) / Math.log(getNumber(p2));
            case MULTIPLY:
               return (p1, p2) -> getNumber(p1) * getNumber(p2);
            case DIVIDE:
               return (p1, p2) -> getNumber(p1) / getNumber(p2);
            case MODULO:
               return (p1, p2) -> getNumber(p1) % getNumber(p2);
            case IF:
            case ELSE:
            case WHILE:
            case DO:
            case IS:
            default:
               return (p1, p2) -> null;
         }

      }
      else if (action instanceof Command) {
         return (path, data) -> {
            ScriptContext context = configuration.getContextFactory().getScriptContext();
            Reference result = new GenericReference(null, context, true);
            List parts = getPathParts(path);
            boolean isDestroy = action == Command.DESTROY;
            int stop = isDestroy ? parts.size()-1 : parts.size();
            for (int p = 0; p < stop; p++) {
               Object part = parts.get(p);
               result = getProperty(result.getValue(), part);
               if (result == null)
                  return isDestroy ? false : null;
            }

            switch ((Command)action) {
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
                  throw new RuntimeException("Unknown command "+action);
            }
         };
      }
      else if (action instanceof BinaryAction)
         return ((BinaryAction)action).getFunction();
      else
         return NOOP;
   }

   private Type getType(Object...params) {
      Type type = null;
      for (Object p : params) {
         if (p instanceof Map) {
            if (type != Type.NUMERIC)
               type = Type.ENTITY;
         }
         else if (comparator.isIterable(p))
            return Type.COLLECTION;
         else if (p instanceof Number || p instanceof Boolean || p instanceof Date)
            type = Type.NUMERIC;
         else if (comparator.isStringable(p)) {
            if (type == null)
               type = Type.STRING;
         }
         else if (type != Type.NUMERIC)
            type = Type.ENTITY;
      }
      return (type == null) ? Type.COLLECTION : type;
   }

   private double getNumber(Object src) {
      Collection c;
      if (src instanceof Number)
         return ((Number)src).doubleValue();
      if (src instanceof Boolean)
         return (Boolean)src ? 1d : 0d;
      if (src == null)
         return 0d;
      if (src instanceof CharSequence) {
         String txt = src.toString().trim();
         try { return Double.parseDouble(txt); }
         catch (Exception e) { return txt.length(); }
      }
      if (src instanceof Date)
         return ((Date)src).getTime();
      if (src instanceof Reference)
         return getNumber(((Reference)src).getValue());

      if (src.getClass().isArray())
         c = new ArrayWrapper(src);
      else if (src instanceof Collection)
         c = (Collection)src;
      else if (src instanceof Map)
         c = ((Map)src).values();
      else
         c = new BeanMap(src).values();

      switch (c.size()) {
         case 0:
            return 0;
         case 1:
            return getNumber(c.iterator().next());
         default:
            return c.size();
      }
   }

   private Object merge(Object p1, Object p2) {
      if (comparator.isEmpty(p1))
         return comparator.isEmpty(p2) ? Collections.EMPTY_MAP : p2;
      if (comparator.isEmpty(p2))
         return p1;
      Map m1 = new LinkedHashMap(comparator.getMap(p1));
      Map m2 = comparator.getMap(p2);
      for (Object key : m2.keySet()) {
         if (m1.containsKey(key))
            m1.put(key, ADD_FUNCTION.apply(m1.get(key), m2.get(key)));
         else
            m1.put(key, m2.get(key));
      }
      return m1;
   }

   private final BiFunction JOIN_FUNCTION = (p1, p2) -> {
      List result = new ArrayList();
      Iterator it = comparator.getIterator(p1);
      while (it.hasNext())
         result.add(it.next());
      it = comparator.getIterator(p2);
      while (it.hasNext())
         result.add(it.next());
      return result;
   };

   private BiFunction ADD_FUNCTION = (p1, p2) -> {
      p1 = comparator.getValue(p1);
      p2 = comparator.getValue(p2);
      switch (getType(p1, p2)) {
         case NUMERIC:
            return comparator.getNumeric(p1, 0d) + comparator.getNumeric(p2, 0d);
         case STRING:
            return comparator.getString(p1) + comparator.getString(p2);
         case ENTITY:
            return merge(p1, p2);
         case COLLECTION:
         default:
            return JOIN_FUNCTION.apply(p1, p2);
      }
   };

   private BiFunction SUBTRACT_FUNCTION = (p1, p2) -> {
      p1 = comparator.getValue(p1);
      p2 = comparator.getValue(p2);
      switch (getType(p1, p2)) {
         case NUMERIC:
            return comparator.getNumeric(p1, 0d) - comparator.getNumeric(p2, 0d);
         case STRING:
            String s1 = comparator.getString(p1);
            String s2 = comparator.getString(p2);
            int index = s1.indexOf(s2);
            if (index >= 0)
               s1 = s1.substring(0, index) + s1.substring(index + s2.length());
            return s1;
         case ENTITY:
            Map map = new LinkedHashMap(comparator.getMap(p1));
            for (Object key : comparator.getMap(p2).keySet())
               map.remove(key);
            return map;
         case COLLECTION:
         default:
            List list = new ArrayList();
            Iterator it = comparator.getIterator(p1);
            while (it.hasNext())
               list.add(it.next());
            it = comparator.getIterator(p2);
            while (it.hasNext())
               list.remove(it.next());
            return list;
      }
   };

   private BiFunction IN_FUNCTION = (p1, p2) -> {
      Iterator i = comparator.getIterator(p2);
      while (i.hasNext()) {
         if (comparator.compare(p1, i.next()) == 0)
            return true;
      }
      return false;
   };

   private BiFunction TO_FUNCTION = (p1, p2) -> {
      p1 = comparator.getValue(p1);
      p2 = comparator.getValue(p2);
      if (p2 instanceof Class)
         return comparator.convert(p1, (Class)p2);
      else if (p2 instanceof Number)
         return new Range(comparator.getNumeric(p1, 0d), (Number)p2);
      else if (p2 instanceof Collection) {
         Collection dst;
         if (p2 instanceof SortedSet)
            dst = new TreeSet((SortedSet)p2);
         else if (p2 instanceof Set)
            dst = new LinkedHashSet((Set)p2);
         else
            dst = new ArrayList((Collection)p2);
         dst.add(p1);
         return dst;
      }
      else if (p2 != null && p2.getClass().isArray()) {
         Collection dst = new ArrayList(new ArrayWrapper(p2));
         dst.add(p1);
         return dst;
      }
      else
         return p2;
   };

   private List getPathParts(Object path) {
      path = comparator.getValue(path);
      if (path instanceof List)
         return (List)path;
      if (path instanceof Collection)
         return new ArrayList((Collection)path);
      if (path == null)
         return Collections.EMPTY_LIST;
      if (path.getClass().isArray())
         return new ArrayWrapper(path);
      if (path instanceof Reference)
         return getPathParts(((Reference)path).getValue());
      return Arrays.asList(path);
   }

   private Reference getProperty(Object value, Object property) {
      if (property == null)
         return null;
      if (property instanceof Collection)
         return getArrayProperty(value, ((Collection)property).toArray());
      if (property instanceof Map)
         return getArrayProperty(value, ((Map)property).keySet().toArray());
      if (property.getClass().isArray())
         return getArrayProperty(value, property);

      PropertyManager propertyManager = configuration.getPropertyManager();
      if (property instanceof Expression) {
         Filterable filterable;
         boolean returnBoolean = false;
         if (value instanceof Filterable)
            filterable = (Filterable)value;
         else if (value instanceof Collection)
            filterable = new FilterableIterable(propertyManager, (Collection)value);
         else if (value == null) {
            filterable = new FilterableIterable(propertyManager);
            returnBoolean = true;
         }
         else if (value.getClass().isArray())
            filterable = new FilterableIterable(propertyManager, new ArrayWrapper(value));
         else
            filterable = new FilterableIterable(propertyManager, value);
         Collection filtered = filterable.filter((Expression)property);
         Object result = returnBoolean ? !filtered.isEmpty() : filtered;
         return new GenericReference(null, result, true);
      }
      if (property instanceof Reference && ((Reference)property).getId() == null)
         return getProperty(value, ((Reference)property).getValue());
      return propertyManager.getProperty(value, String.valueOf(property));
   }

   private Reference getArrayProperty(Object value, Object property) {
      int length = Array.getLength(property);
      List values = new ArrayList(length);
      for (int i = 0; i < length; i++)
         values.add(getProperty(value, Array.get(property, i)));
      return new GenericReference(null, values, true);
   }
}
