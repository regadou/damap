package org.regadou.property;

import org.regadou.damai.Property;
import org.regadou.reference.TypedReference;

public abstract class TypedProperty<P,T> extends TypedReference<T> implements Property<P,T> {

   private P parent;
   private Class<P> parentType;

   public TypedProperty(P parent, Class<P> parentType, Class<T> type) {
      super(type);
      this.parent = parent;
      this.parentType = parentType;
   }

   public TypedProperty(P parent, Class<P> parentType, String method, Class...parameters) {
      super(parent.getClass(), method, parameters);
      this.parent = parent;
      this.parentType = parentType;
   }

   public TypedProperty(P parent, Class<P> parentType, Class target, String method, Class...parameters) {
      super(target, method, parameters);
      this.parent = parent;
      this.parentType = parentType;
   }

   @Override
   public String toString() {
      return getId()+"@"+parent;
   }

   @Override
   public P getOwner() {
      return parent;
   }

   @Override
   public Class<P> getOwnerType() {
      return parentType;
   }
}
