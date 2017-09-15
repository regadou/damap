package org.regadou.action;

import java.util.ArrayList;
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
import org.regadou.damai.Action;
import org.regadou.damai.Command;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.collection.ArrayWrapper;
import org.regadou.collection.Range;

public class OperatorAction implements Action {

   private static enum Type { COLLECTION, NUMERIC, ENTITY, STRING }
   private static final Class[] PARAMETERS_TYPES = new Class[]{Object.class, Object.class};

   public static Collection<OperatorAction> getActions(Configuration configuration) {
      GenericComparator comparator = new GenericComparator(configuration);
      BiFunction getterFunction = new CommandAction(Command.GET, configuration, comparator).getFunction();
      Collection<OperatorAction> actions = new ArrayList<>();
      for (Operator op : Operator.values()) {
         OperatorAction action = new OperatorAction(op, comparator, getterFunction);
         actions.add(action);
      }
      return actions;
   }

   private GenericComparator comparator;
   private Operator operator;
   private BiFunction function;
   private BiFunction getterFunction;

   protected OperatorAction() {}

   public OperatorAction(Operator operator, GenericComparator comparator, BiFunction getterFunction) {
      this.operator = operator;
      this.comparator = comparator;
      this.getterFunction = getterFunction;
      function = getFunction();
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public boolean equals(Object that) {
      if (that instanceof Operator)
         return that == operator;
      if (that instanceof OperatorAction)
         return ((OperatorAction)that).operator == operator;
      return false;
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public Object execute(Object... parameters) {
      if (parameters == null || parameters.length == 0)
         parameters = new Object[]{null, null};
      else if (parameters.length == 1)
         parameters = new Object[]{parameters[0], null};
      Object value = parameters[0];
      for (int p = 1; p < parameters.length; p++)
         value = function.apply(value, parameters[p]);
      return value;
   }

   @Override
   public String getName() {
      switch (operator) {
         case ADD: return "+";
         case SUBTRACT: return "-";
         case MULTIPLY: return "*";
         case DIVIDE: return "/";
         case MODULO: return "%";
         case EXPONANT: return "^";
         case ROOT: return "\\/";
         case LOG: return "\\";
         case LESSER: return "<";
         case LESSEQ: return "<=";
         case GREATER: return ">";
         case GREATEQ: return ">=";
         case EQUAL: return "=";
         case NOTEQUAL: return "!=";
         case AND: return "&";
         case OR: return "|";
         case NOT: return "!";
         case IN: return "@";
         case FROM: return "<-";
         case TO: return "->";
         case IS: return ":";
         case DO: return "=>";
         case HAVE: return ".";
         case JOIN: return ",";
         case IF: return "?";
         case ELSE: return "::";
         case WHILE: return "*?";
         default: throw new RuntimeException("Unknown operator "+operator);
      }
   }

   @Override
   public Class getReturnType() {
      switch (operator) {
         case EXPONANT:
         case ROOT:
         case LOG:
         case MULTIPLY:
         case DIVIDE:
         case MODULO:
            return Number.class;
         case ADD:
         case SUBTRACT:
            return Object.class; //Collection|Number|String|Map
         case LESSER:
         case LESSEQ:
         case GREATER:
         case GREATEQ:
         case EQUAL:
         case NOTEQUAL:
         case AND:
         case OR:
         case NOT:
         case IN:
         case IS:
            return Boolean.class;
         case HAVE:
         case FROM:
         case TO:
            return Object.class;
         case IF:
         case ELSE:
         case WHILE:
         case DO:
            return Object.class;
         case JOIN:
            return Collection.class;
         default:
            return Object.class;
      }
   }

   @Override
   public Class[] getParameterTypes() {
      return PARAMETERS_TYPES;
   }

   public int getPrecedence() {
      switch (operator) {
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
         default: throw new RuntimeException("Unknown operator "+operator);
      }
   }

   public Operator getOperator() {
      return operator;
   }

   public BiFunction getFunction() {
      switch (operator) {
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
            return (p1, p2) -> getterFunction.apply(p1, p2);
         case FROM:
            return (p1, p2) -> getterFunction.apply(p2, p1);
         case TO:
            return TO_FUNCTION;
         default:
         case EXPONANT:
         case ROOT:
         case LOG:
         case MULTIPLY:
         case DIVIDE:
         case MODULO:
         case IF:
         case ELSE:
         case WHILE:
         case DO:
         case IS:
            return (p1, p2) -> null;
      }
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

   private final BiFunction ADD_FUNCTION = (p1, p2) -> {
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
}
