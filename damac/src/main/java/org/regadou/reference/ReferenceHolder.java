package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Reference;

public class ReferenceHolder<T> implements Reference<T> {

   private String name;
   private T value;
   private boolean readonly;

   public ReferenceHolder(String name) {
      this.name = name;
   }

   public ReferenceHolder(String name, T value) {
      this.name = name;
      this.value = value;
   }

   public ReferenceHolder(String name, T value, boolean readonly) {
      this.name = name;
      this.value = value;
      this.readonly = readonly;
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
   public Class getType() {
      return (value == null) ? Void.class : value.getClass();
   }

   @Override
   public void setValue(T value) {
      if (!readonly)
         this.value = value;
   }

   public Map.Entry<String,T> toMapEntry() {
      ReferenceHolder<T> me = this;
      return new Map.Entry<String,T>() {
         @Override
         public String getKey() {
            return name;
         }

         @Override
         public T getValue() {
            return value;
         }

         @Override
         public T setValue(T value) {
            T old = me.value;
            me.setValue(value);
            return old;
         }
      };
   }
}
