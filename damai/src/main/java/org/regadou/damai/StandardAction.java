package org.regadou.damai;

public interface StandardAction extends Action {

   public static StandardAction NOOP = new StandardAction() {
      @Override
      public boolean hasSideEffects() {
         return false;
      }

      @Override
      public Object execute(Object... parameters) {
         return parameters;
      }
   };

   boolean hasSideEffects();
}
