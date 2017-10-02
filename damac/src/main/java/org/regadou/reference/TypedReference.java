package org.regadou.reference;

import org.regadou.damai.Reference;

public abstract class TypedReference<T> implements Reference<T> {

   private Class<T> type;
   private Class target;
   private String method;
   private Class[] parameters;

   protected TypedReference(Class<T> type) {
      this.type = type;
   }

   protected TypedReference(Class target, String method, Class...parameters) {
      this.target = target;
      this.method = method;
      this.parameters = (parameters == null) ? new Class[0] : parameters;
   }

   @Override
   public Class<T> getType() {
      if (type == null) {
         if (target == null || method == null)
            type = (Class<T>)Object.class;
         else {
            try { type = (Class<T>)target.getMethod(method, parameters).getReturnType(); }
            catch (NoSuchMethodException|SecurityException e) { throw new RuntimeException(e); }
         }
      }
      return type;
   }
}
