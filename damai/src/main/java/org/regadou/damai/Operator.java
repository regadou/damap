package org.regadou.damai;

public enum Operator implements Action {

   ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, EXPONANT, ROOT, LOG,
   LESSER, LESSEQ, GREATER, GREATEQ, EQUAL, NOTEQUAL,
   AND, OR, NOT;

   @Override
   public Object execute(Object... parameters) {
      throw new UnsupportedOperationException("Not implemented");
   }

}
