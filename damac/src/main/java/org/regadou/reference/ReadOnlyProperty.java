package org.regadou.reference;

import org.regadou.damai.Property;

public class ReadOnlyProperty<P,T> implements Property<P,T> {

   private P parent;
   private String name;
   private T value;

   public ReadOnlyProperty(P parent, String name, T value) {
      this.parent = parent;
      this.name = name;
      this.value = value;
   }

   @Override
   public P getParent() {
      return parent;
   }

   @Override
   public Class<P> getParentType() {
      return (Class<P>)((parent == null) ? Void.class : parent.getClass());
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public T getValue() {
      return value;
   }

   @Override
   public Class<T> getType() {
      return (Class<T>)((value == null) ? Void.class : value.getClass());
   }

   @Override
   public void setValue(T value) {}

}
