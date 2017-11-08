package org.regadou.damai;

import java.util.Collection;

public enum Operator implements StandardAction {

   ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, POWER, ROOT, LOGARITHM,
   LESS, LESSEQUAL, MORE, MOREQUAL, EQUAL, NOTEQUAL,
   AND, OR, NOT, IN, FROM, TO, IS, DO, HAVE, JOIN, CASE, WHILE;

   private static final Class[] PARAMETERS_TYPES = new Class[]{Object.class, Object.class};

   @Override
   public Object execute(Object... parameters) {
      throw new UnsupportedOperationException("Not implemented");
   }

   @Override
   public String getName() {
      return name();
   }

   @Override
   public Class getReturnType() {
      switch (this) {
         case POWER:
         case ROOT:
         case LOGARITHM:
         case MULTIPLY:
         case DIVIDE:
         case MODULO:
            return Number.class;
         case ADD:
         case SUBTRACT:
            return Object.class; //Collection|Number|String|Map
         case LESS:
         case LESSEQUAL:
         case MORE:
         case MOREQUAL:
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
         case CASE:
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

   @Override
   public int getPrecedence() {
      switch (this) {
         case POWER: return 10;
         case ROOT: return 9;
         case LOGARITHM: return 8;
         case MULTIPLY: return 7;
         case DIVIDE: return 7;
         case MODULO: return 7;
         case ADD: return 6;
         case SUBTRACT: return 6;
         case NOT: return 5;
         case FROM: return 4;
         case TO: return 4;
         case LESS: return 3;
         case LESSEQUAL: return 3;
         case MORE: return 3;
         case MOREQUAL: return 3;
         case EQUAL: return 3;
         case NOTEQUAL: return 3;
         case AND: return 2;
         case OR: return 2;
         case IN: return 1;
         case CASE: return -1;
         case WHILE: return -2;
         case DO: return -3;
         case HAVE: return -4;
         case JOIN: return -5;
         case IS: return -6;
         default: throw new RuntimeException("Operator "+this+" has a programming problem");
      }
   }

   @Override
   public StandardAction getStandardAction() {
      return this;
   }

   @Override
   public boolean hasSideEffects() {
      return false;
   }
}
