package org.regadou.damai;

@FunctionalInterface
public interface Action<T> {

   T execute(Object ... parameters);

   default String getName() {
      return "Action#"+hashCode();
   }

   default Class<T> getReturnType() {
      return (Class<T>)Object.class;
   }

   default Class[] getParameterTypes() {
      return null;
   }

   default int getPrecedence() {
      return 0;
   }

   default StandardAction getStandardAction() {
      return null;
   }
}
