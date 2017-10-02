package org.regadou.damai;

import java.util.Collection;

public enum Operator implements Action {

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
}
