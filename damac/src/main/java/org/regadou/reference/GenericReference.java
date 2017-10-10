package org.regadou.reference;

import java.util.Map;
import org.regadou.damai.Reference;

public class GenericReference implements Reference {

   private String id;
   private Object value;
   private boolean readonly;
   private Map.Entry entry;

   public GenericReference(String id) {
      this.id = id;
   }

   public GenericReference(String id, Object value) {
      this.id = id;
      this.value = value;
   }

   public GenericReference(String id, Object value, boolean readonly) {
      this.id = id;
      this.value = value;
      this.readonly = readonly;
   }

   @Override
   public String toString() {
      return (id == null) ? String.valueOf(value) : id;
   }

   @Override
   public String getId() {
      return id;
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
               return id;
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
