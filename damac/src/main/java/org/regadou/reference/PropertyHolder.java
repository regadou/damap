package org.regadou.reference;

import org.regadou.damai.Property;

public class PropertyHolder<P,T> implements Property<P,T> {

   private P parent;
   private String name;
   private T value;
   private boolean readonly;

   public PropertyHolder(P parent, String name, T value) {
      this(parent, name, value, false);
   }

   public PropertyHolder(P parent, String name, T value, boolean readonly) {
      this.parent = parent;
      this.name = name;
      this.value = value;
      this.readonly = readonly;
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
   public void setValue(T value) {
      if (!readonly)
         this.value = value;
   }
}
