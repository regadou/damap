package org.regadou.action;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import org.regadou.damai.Operator;

public class CustomOperator extends OperatorAction {

   private static final String FUNCTIONAL_METHOD_NAME = "apply";

   private BiFunction function;
   private String name;
   private int precedence;
   private Operator operator;
   private Class returnType;
   private Class[] parameters;

   public CustomOperator(BiFunction function, String name, int precedence, Operator operator) {
      this.function = function;
      this.name = name;
      this.precedence = precedence;
      this.operator = operator;
      for (Method method : function.getClass().getMethods()) {
         if (FUNCTIONAL_METHOD_NAME.equals(method.getName())) {
            returnType = method.getReturnType();
            parameters = method.getParameterTypes();
            break;
         }
      }
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public boolean equals(Object that) {
      return (that == null) ? false : toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Class getReturnType() {
      return returnType;
   }

   @Override
   public Class[] getParameterTypes() {
      return parameters;
   }

   public int getPrecedence() {
      return precedence;
   }

   public Operator getOperator() {
      return operator;
   }

   public BiFunction getFunction() {
      return function;
   }
}
