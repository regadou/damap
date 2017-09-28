package org.regadou.property;

import org.regadou.damai.Property;

public class GenericProperty<P,T> implements Property<P,T> {

   private P parent;
   private String name;
   private T value;
   private boolean readonly;

   public GenericProperty(P parent, String name, T value) {
      this(parent, name, value, false);
   }

   public GenericProperty(P parent, String name, T value, boolean readonly) {
      this.parent = parent;
      this.name = name;
      this.value = value;
      this.readonly = readonly;
   }

   @Override
   public String toString() {
      return name+"@"+parent;
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
   public String getId() {
      return name;
   }

   @Override
   public T getValue() {
      return value;
   }

   @Override
   public Class<T> getType() {
      return (Class<T>)((value == null) ? Object.class : value.getClass());
   }

   @Override
   public void setValue(T value) {
      if (!readonly)
         this.value = value;
   }
}
