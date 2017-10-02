package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Reference;

public class GenericReference implements Reference {

   private String name;
   private Object value;
   private boolean readonly;
   private Map.Entry entry;

   public GenericReference(String name) {
      this.name = name;
   }

   public GenericReference(String name, Object value) {
      this.name = name;
      this.value = value;
   }

   public GenericReference(String name, Object value, boolean readonly) {
      this.name = name;
      this.value = value;
      this.readonly = readonly;
   }

   @Override
   public String toString() {
      return (name == null) ? String.valueOf(value) : name;
   }

   @Override
   public String getId() {
      return name;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Class getType() {
      return Object.class;
   }

   @Override
   public void setValue(Object value) {
      if (!readonly)
         this.value = value;
   }

   public Map.Entry<String,Object> toMapEntry() {
      if (entry == null) {
         GenericReference me = this;
         entry = new Map.Entry<String,Object>() {
            @Override
            public String getKey() {
               return name;
            }

            @Override
            public Object getValue() {
               return value;
            }

            @Override
            public Object setValue(Object value) {
               Object old = me.value;
               me.setValue(value);
               return old;
            }
         };
      }
      return entry;
   }
}
