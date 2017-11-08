package org.regadou.action;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
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
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.PropertyManager;
import org.regadou.damai.Reference;
import org.regadou.damai.StandardAction;
import org.regadou.expression.DefaultExpression;
import org.regadou.reference.GenericReference;

public class DefaultAction implements Action {

   private static final String FUNCTIONAL_METHOD_NAME = "apply";
   private static enum Type { COLLECTION, NUMERIC, ENTITY, STRING }

   private Configuration configuration;
   private GenericComparator comparator;
   private String name;
   private StandardAction standardAction;
   private BiFunction function;
   private Class returnType;
   private Class[] parameterTypes;
   private int precedence;

   public DefaultAction(Configuration configuration, String name, StandardAction standardAction) {
      this(configuration, name, standardAction, null, null, null, null);
   }

   public DefaultAction(Configuration configuration, String name, StandardAction standardAction, BiFunction function) {
      this(configuration, name, standardAction, function, null, null, null);
   }

   public DefaultAction(Configuration configuration, String name, StandardAction standardAction, BiFunction function, Integer precedence) {
      this(configuration, name, standardAction, function, precedence, null, null);
   }

   public DefaultAction(Configuration configuration, String name, StandardAction standardAction, BiFunction function, Integer precedence, Class returnType) {
      this(configuration, name, standardAction, function, precedence, returnType, null);
   }

   public DefaultAction(Configuration configuration, String name, StandardAction standardAction, BiFunction function, Integer precedence, Class returnType, Class[] parameterTypes) {
      this.configuration = configuration;
      this.name = name;
      this.standardAction = (standardAction == null) ? StandardAction.NOOP : standardAction;
      this.function = (function == null) ? getFunction() : function;
      this.precedence = (precedence == null) ? this.standardAction.getPrecedence() : precedence;
      this.returnType = returnType;
      this.parameterTypes = parameterTypes;
      this.comparator = configuration.getInstance(GenericComparator.class);
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
      int length = getParameterTypes().length;
      if (parameters == null)
         parameters = new Object[2];
      else if (parameters.length < length) {
         Object[] old = parameters;
         parameters = new Object[Math.max(length, 2)];
         for (int p = 0; p < parameters.length; p++)
            parameters[p] = (p < old.length) ? old[p] : null;
      }
      else if (parameters.length > length && standardAction instanceof Operator) {
         Object result = parameters[0];
         for (int p = 1; p < parameters.length; p++)
            result = function.apply(result, parameters[p]);
         return result;
      }
      return function.apply(parameters[0], parameters[1]);
   }

   @Override
   public String getName() {
      if (name == null) {
         name = standardAction.getName();
         if (name == null)
            name = Action.super.getName();
      }
      return name;
   }

   @Override
   public Class getReturnType() {
      if (returnType == null)
         findTypes();
      return returnType;
   }

   @Override
   public Class[] getParameterTypes() {
      if (parameterTypes == null)
         findTypes();
      return parameterTypes;
   }

   @Override
   public int getPrecedence() {
      return precedence;
   }

   @Override
   public StandardAction getStandardAction() {
      return standardAction;
   }

   private void findTypes() {
      for (Method method : function.getClass().getMethods()) {
         if (FUNCTIONAL_METHOD_NAME.equals(method.getName())) {
            returnType = method.getReturnType();
            parameterTypes = method.getParameterTypes();
            break;
         }
      }
   }

   private BiFunction getFunction() {
      if (standardAction instanceof Operator) {
         switch ((Operator)standardAction) {
            case EQUAL:
               return (p1, p2) -> comparator.compare(p1, p2) == 0;
            case NOTEQUAL:
               return (p1, p2) -> comparator.compare(p1, p2) != 0;
            case LESS:
               return (p1, p2) -> comparator.compare(p1, p2) < 0;
            case LESSEQUAL:
               return (p1, p2) -> comparator.compare(p1, p2) <= 0;
            case MORE:
               return (p1, p2) -> comparator.compare(p1, p2) > 0;
            case MOREQUAL:
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
            case POWER:
               return (p1, p2) -> Math.pow(getNumber(p1), getNumber(p2));
            case ROOT:
               return (p1, p2) -> Math.pow(getNumber(p1), 1/getNumber(p2));
            case LOGARITHM:
               return (p1, p2) -> Math.log(getNumber(p1)) / Math.log(getNumber(p2));
            case MULTIPLY:
               return (p1, p2) -> getNumber(p1) * getNumber(p2);
            case DIVIDE:
               return (p1, p2) -> getNumber(p1) / getNumber(p2);
            case MODULO:
               return (p1, p2) -> getNumber(p1) % getNumber(p2);
            case CASE:
               return CASE_FUNCTION;
            case WHILE:
               return WHILE_FUNCTION;
            case DO:
               return DO_FUNCTION;
            case IS:
               return IS_FUNCTION;
            default:
               return (p1, p2) -> null;
         }

      }
      else if (standardAction instanceof Command) {
         return (path, data) -> {
            boolean isDestroy = standardAction == Command.DESTROY;
            while (path instanceof Expression)
               path = ((Expression)path).getValue();
            Reference result;
            Object last = null;
            if (path instanceof Reference) {
               result = (Reference)path;
               if (isDestroy && result instanceof Property) {
                  last = result.getId();
                  Object parent = ((Property)result).getOwner();
                  result = (parent instanceof Reference) ? (Reference)parent : new GenericReference(null, parent, true);
               }
            }
            else if (path == null)
               return isDestroy ? false : null;
            else {
               ScriptContext context = configuration.getContextFactory().getScriptContext();
               result = new GenericReference(null, context, true);
               List parts = getPathParts(path);
               int stop;
               if (isDestroy) {
                  stop = parts.size() - 1;
                  last = parts.get(stop);
               }
               else
                  stop = parts.size();
               for (int p = 0; p < stop; p++) {
                  Object part = parts.get(p);
                  result = getProperty(result.getValue(), part);
                  if (result == null)
                     return isDestroy ? false : null;
               }
            }

            switch ((Command)standardAction) {
               case SET:
                  return comparator.setValue(result, data);
               case GET:
                  return result;
               case CREATE:
                  return comparator.addValue(result, data);
               case UPDATE:
                  return comparator.mergeValue(result, data);
               case DESTROY:
                  return (last == null) ? false : comparator.removeValue(result, last);
               default:
                  throw new RuntimeException("Unknown command "+standardAction);
            }
         };
      }
      else
         return (p1, p2) -> {
            Object[] params;
            if (p1 == null)
               params = (p2 == null) ? new Object[0] : new Object[]{null, p2};
            else if (p2 == null)
               params = new Object[]{p1};
            else
               params = new Object[]{p1, p2};
            return standardAction.execute(params);
         };
   }

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

      if (property instanceof Expression) {
         Filterable filterable;
         boolean returnBoolean = false;
         if (value instanceof Filterable)
            filterable = (Filterable)value;
         else if (value instanceof Collection)
            filterable = new FilterableIterable(configuration, (Collection)value);
         else if (value == null) {
            filterable = new FilterableIterable(configuration);
            returnBoolean = true;
         }
         else if (value.getClass().isArray())
            filterable = new FilterableIterable(configuration, new ArrayWrapper(value));
         else
            filterable = new FilterableIterable(configuration, value);
         Collection filtered = filterable.filter((Expression)property);
         Object result = returnBoolean ? !filtered.isEmpty() : filtered;
         return new GenericReference(null, result, true);
      }
      if (property instanceof Reference && ((Reference)property).getId() == null)
         return getProperty(value, ((Reference)property).getValue());
      PropertyManager propertyManager = configuration.getPropertyManager();
      Property p = propertyManager.getProperty(value, String.valueOf(property));
      if (p == null) {
         PropertyFactory f = propertyManager.getPropertyFactory(value.getClass());
         p = f.addProperty(value, String.valueOf(property), null);
      }
      return p;
   }

   private Reference getArrayProperty(Object value, Object property) {
      int length = Array.getLength(property);
      List values = new ArrayList(length);
      for (int i = 0; i < length; i++)
         values.add(getProperty(value, Array.get(property, i)));
      return new GenericReference(null, values, true);
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

   private Collection getCollection(Object src) {
      if (src instanceof Collection)
         return (Collection)src;
      if (src == null)
         return Collections.EMPTY_SET;
      if (src.getClass().isArray())
         return new ArrayWrapper(src);
      if (src instanceof Reference)
         return getCollection(((Reference)src).getValue());
      return Arrays.asList(src);
   }

   private Expression getExpression(Object src) {
      if (src instanceof Expression)
         return (Expression)src;
      Reference token = (src instanceof Reference) ? (Reference)src : new GenericReference(null, src, true);
      return new DefaultExpression(null, Collections.singletonList(token), configuration);
   }

   private boolean getBoolean(Object src) {
      Boolean result = configuration.getConverter().convert(comparator.getValue(src), Boolean.class);
      return (result == null) ? false : result.booleanValue();
   }

   private final BiFunction MERGE_FUNCTION = (p1, p2) -> {
      if (comparator.isEmpty(p1))
         return comparator.isEmpty(p2) ? Collections.EMPTY_MAP : p2;
      if (comparator.isEmpty(p2))
         return p1;
      Map m1 = new LinkedHashMap(comparator.getMap(p1));
      Map m2 = comparator.getMap(p2);
      for (Object key : m2.keySet()) {
         Object oldValue = m1.get(key);
         Object newValue = m2.get(key);
         if (oldValue == null)
            m1.put(key, newValue);
         else if (oldValue instanceof Collection)
            ((Collection)oldValue).addAll(getCollection(newValue));
         else if (oldValue.getClass().isArray()) {
            Collection c = new ArrayList(new ArrayWrapper(oldValue));
            c.addAll(getCollection(newValue));
            m1.put(key, newValue);
         }
         else if (newValue == null)
            ;
         else if (newValue instanceof Collection || newValue.getClass().isArray()) {
            Collection c = new ArrayList();
            c.add(oldValue);
            c.addAll(getCollection(newValue));
            m1.put(key, c);
         }
         else
            m1.put(key, new ArrayList(Arrays.asList(oldValue, newValue)));
      }
      return m1;
   };

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

   private final BiFunction ADD_FUNCTION = (p1, p2) -> {
      p1 = comparator.getValue(p1);
      p2 = comparator.getValue(p2);
      switch (getType(p1, p2)) {
         case NUMERIC:
            return comparator.getNumeric(p1, 0d) + comparator.getNumeric(p2, 0d);
         case STRING:
            return comparator.getString(p1) + comparator.getString(p2);
         case ENTITY:
            return MERGE_FUNCTION.apply(p1, p2);
         case COLLECTION:
         default:
            return JOIN_FUNCTION.apply(p1, p2);
      }
   };

   private final BiFunction SUBTRACT_FUNCTION = (p1, p2) -> {
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

   private final BiFunction IN_FUNCTION = (p1, p2) -> {
      Iterator i = comparator.getIterator(p2);
      while (i.hasNext()) {
         if (comparator.compare(p1, i.next()) == 0)
            return true;
      }
      return false;
   };

   private final BiFunction TO_FUNCTION = (p1, p2) -> {
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

   private final BiFunction CASE_FUNCTION = (p1, p2) -> {
      return getBoolean(p1) ? comparator.getValue(p2) : false;
   };

   private final BiFunction WHILE_FUNCTION = (p1, p2) -> {
      Object result = null;
      while (getBoolean(p1))
         result = comparator.getValue(p2);
      return result;
   };

   private final BiFunction DO_FUNCTION = (p1, p2) -> {
      Object[] params = configuration.getConverter().convert(comparator.getValue(p1), Collection.class).toArray();
      return new DynamicAction(configuration, params, getExpression(p2));
   };

   private final BiFunction IS_FUNCTION = (p1, p2) -> {
      Object value = comparator.getValue(p1);
      Class type = configuration.getConverter().convert(comparator.getValue(p2), Class.class);
      if (type == null)
         return value == null;
      return type.isInstance(value);
   };
}
