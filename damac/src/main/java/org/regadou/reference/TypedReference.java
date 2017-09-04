package org.regadou.reference;

import org.regadou.damai.Reference;

public abstract class TypedReference<T> implements Reference<T> {

   private Class type;
   private Class target;
   private String method;
   private Class[] parameters;

   protected TypedReference(Class type) {
      this.type = type;
   }

   protected TypedReference(Class target, String method, Class...parameters) {
      this.target = target;
      this.method = method;
      this.parameters = (parameters == null) ? new Class[0] : parameters;
   }

   @Override
   public Class getType() {
      if (type == null) {
         if (target == null || method == null)
            type = Object.class;
         else {
            try { type = target.getMethod(method, parameters).getReturnType(); }
            catch (NoSuchMethodException|SecurityException e) { throw new RuntimeException(e); }
         }
      }
      return type;
   }
}
