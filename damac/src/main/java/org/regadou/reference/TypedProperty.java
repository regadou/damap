package org.regadou.reference;

import org.regadou.damai.Property;

public abstract class TypedProperty<P> extends TypedReference<Object> implements Property<P,Object> {

   private P parent;
   private Class<P> parentType;

   public TypedProperty(P parent, Class<P> parentType, Class type) {
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
   public P getParent() {
      return parent;
   }

   @Override
   public Class<P> getParentType() {
      return parentType;
   }
}
