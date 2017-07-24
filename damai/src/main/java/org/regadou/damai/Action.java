package org.regadou.damai;

@FunctionalInterface
public interface Action<T> {

   T execute(Object ... parameters);

   default String getName() {
      return "Action#"+hashCode();
   }

   default Class<T> getReturnType() {
      return null;
   }

   default Class[] getParameterTypes() {
      return null;
   }
}
