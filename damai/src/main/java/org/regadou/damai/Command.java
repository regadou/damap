package org.regadou.damai;

public enum Command implements Action {

   // all commands take 2 parameters (path and data)
   // path is usually represented by an array of elements (string, number or expression)
   // data is usually a map or an array of maps
   // all commands take 2 parameters (path and data)
   // path is usually represented by an array of elements (string, number or expression)
   // data is usually a map or an array of maps
   GET,     // returns the object represented by the path (data parameter ignored)
   SET,     // replaces the object represented by the path with an object constructed from the data parameter
   CREATE,  // adds to the collection represented by the path an object constructed from the data parameter
   UPDATE,  // updates the object represented by the path with the data parameter
   DESTROY; // removes the object represented by the path from its parent collection (data parameter ignored)

   private static final Class[] SINGLE_PARAMETERS_TYPES = new Class[]{Object.class};
   private static final Class[] DOUBLE_PARAMETERS_TYPES = new Class[]{Object.class, Object.class};

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
      return (this == DESTROY) ? Boolean.class : Object.class;
   }

   @Override
   public Class[] getParameterTypes() {
      return isDataNeeded() ? DOUBLE_PARAMETERS_TYPES : SINGLE_PARAMETERS_TYPES;
   }

   public boolean isDataNeeded() {
      return this == SET || this == CREATE || this == UPDATE;
   }
}
