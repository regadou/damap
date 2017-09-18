package org.regadou.damai;

@FunctionalInterface
public interface Action<T> {

   public static Action NOOP = p -> p;

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
}
