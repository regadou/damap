package org.regadou.action;

import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;
import org.regadou.damai.StandardAction;

public class LogicalAction implements Action {

   private GenericComparator comparator;
   private Operator operator;
   private String name;
   private int precedence;
   private boolean stopValue;

   public LogicalAction(Configuration configuration, Operator operator, String name, Integer precedence) {
      switch (operator) {
         case EQUAL:
            stopValue = false;
            break;
         case NOTEQUAL:
            stopValue = false;
            break;
         case LESS:
            stopValue = false;
            break;
         case LESSEQUAL:
            stopValue = false;
            break;
         case MORE:
            stopValue = false;
            break;
         case MOREQUAL:
            stopValue = false;
            break;
         case AND:
            stopValue = false;
            break;
         case OR:
            stopValue = true;
            break;
         case NOT:
            stopValue = false;
            break;
         case CASE:
            break;
         default:
            throw new RuntimeException(operator+" is not a logical operator");
      }

      this.operator = operator;
      this.name = name;
      this.precedence = (precedence == null) ? operator.getPrecedence() : precedence;
      this.comparator = configuration.getInstance(GenericComparator.class);
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public String getName() {
      if (name == null)
         name = operator.getName();
      return name;
   }

   @Override
   public Class getReturnType() {
      return (operator == Operator.CASE) ? Object.class : Boolean.class;
   }

   @Override
   public int getPrecedence() {
      return precedence;
   }

   @Override
   public StandardAction getStandardAction() {
      return operator;
   }

   @Override
   public Object execute(Object... parameters) {
      if (operator == Operator.CASE) {
         int last = (parameters.length % 2 == 0) ? -1 : parameters.length-1;
         for (int p = 0; p < parameters.length; p += 2) {
            if (p == last)
               return parameters[p];
            if (comparator.isEmpty(parameters[p]))
               break;
            return parameters[p+1];
         }
         return null;
      }
      switch (parameters.length) {
         case 0:
            parameters = new Object[2];
            break;
         case 1:
            parameters = new Object[]{parameters[0], stopValue};
            break;
      }
      Object previous = parameters[0];
      boolean result = stopValue;
      for (int p = 1; p < parameters.length; p++) {
         Object current = parameters[p];
         result = compareValues(previous, current);
         if (result == stopValue)
            return result;
      }
      return result;
   }

   private boolean compareValues(Object p1, Object p2) {
      switch (operator) {
         case EQUAL:
            return comparator.compare(p1, p2) == 0;
         case NOTEQUAL:
            return comparator.compare(p1, p2) != 0;
         case LESS:
            return comparator.compare(p1, p2) < 0;
         case LESSEQUAL:
            return comparator.compare(p1, p2) <= 0;
         case MORE:
            return comparator.compare(p1, p2) > 0;
         case MOREQUAL:
            return comparator.compare(p1, p2) >= 0;
         case AND:
            return !comparator.isEmpty(p1) && !comparator.isEmpty(p2);
         case OR:
            return !comparator.isEmpty(p1) || !comparator.isEmpty(p2);
         case NOT:
            return comparator.isEmpty(p1) && comparator.isEmpty(p2);
         default:
            throw new RuntimeException(operator+" is not a boolean comparator");
      }
   }
}
