package org.regadou.action;

import java.lang.reflect.Method;
import java.util.function.BiFunction;
import org.regadou.damai.Action;
import org.regadou.damai.Configuration;
import org.regadou.damai.Operator;

public class BinaryAction implements Action {

   private static final String FUNCTIONAL_METHOD_NAME = "apply";

   private Configuration configuration;
   private String name;
   private Action parentAction;
   private BiFunction function;
   private Integer precedence;
   private Class returnType;
   private Class[] parameterTypes;

   public BinaryAction(Configuration configuration, String name, Action parentAction) {
      this(configuration, name, parentAction, null, null, null, null);
   }

   public BinaryAction(Configuration configuration, String name, Action parentAction, BiFunction function) {
      this(configuration, name, parentAction, function, null, null, null);
   }

   public BinaryAction(Configuration configuration, String name, Action parentAction, BiFunction function, Integer precedence) {
      this(configuration, name, parentAction, function, precedence, null, null);
   }

   public BinaryAction(Configuration configuration, String name, Action parentAction, BiFunction function, Integer precedence, Class returnType) {
      this(configuration, name, parentAction, function, precedence, returnType, null);
   }

   public BinaryAction(Configuration configuration, String name, Action parentAction, BiFunction function, Integer precedence, Class returnType, Class[] parameterTypes) {
      this.configuration = configuration;
      this.name = name;
      this.parentAction = parentAction;
      this.function = function;
      this.precedence = precedence;
      this.returnType = returnType;
      this.parameterTypes = parameterTypes;
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public Object execute(Object... parameters) {
      getFunction();
      int length = getParameterTypes().length;
      if (parameters == null)
         parameters = new Object[2];
      else if (parameters.length < length) {
         Object[] old = parameters;
         parameters = new Object[Math.max(length, 2)];
         for (int p = 0; p < parameters.length; p++)
            parameters[p] = (p < old.length) ? old[p] : null;
      }
      else if (parameters.length > length && getParentAction() instanceof Operator) {
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
         name = getParentAction().getName();
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

   public BiFunction getFunction() {
      if (function == null)
         function = ActionFunctions.getFunction(getParentAction(), configuration);
      return function;
   }

   public int getPrecedence() {
      if (precedence == null)
         precedence = ActionFunctions.getPrecedence(getParentAction());
      return precedence;
   }

   public Action getParentAction() {
      if (parentAction == null)
         parentAction = NOOP;
      return parentAction;
   }

   private void findTypes() {
      for (Method method : getFunction().getClass().getMethods()) {
         if (FUNCTIONAL_METHOD_NAME.equals(method.getName())) {
            returnType = method.getReturnType();
            parameterTypes = method.getParameterTypes();
            break;
         }
      }
   }
}
