package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Reference;

public class ReferenceHolder implements Reference {

   private String name;
   private Object value;
   private boolean readonly;

   public ReferenceHolder(String name) {
      this.name = name;
   }

   public ReferenceHolder(String name, Object value) {
      this.name = name;
      this.value = value;
   }

   public ReferenceHolder(String name, Object value, boolean readonly) {
      this.name = name;
      this.value = value;
      this.readonly = readonly;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Class getType() {
      return (value == null) ? Void.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      if (!readonly)
         this.value = value;
   }

   public Map.Entry toMapEntry() {
      ReferenceHolder me = this;
      return new Map.Entry() {
         @Override
         public Object getKey() {
            return name;
         }

         @Override
         public Object getValue() {
            return value;
         }

         @Override
         public Object setValue(Object value) {
            me.setValue(value);
            return me.value;
         }
      };
   }
}
