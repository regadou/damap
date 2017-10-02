package org.regadou.property;

import org.regadou.damai.Property;

public class GenericProperty implements Property {

   private Object parent;
   private String name;
   private Object value;
   private boolean readonly;

   public GenericProperty(Object parent, String name, Object value) {
      this(parent, name, value, false);
   }

   public GenericProperty(Object parent, String name, Object value, boolean readonly) {
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
   public Object getParent() {
      return parent;
   }

   @Override
   public Class getParentType() {
      return Object.class;
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
}
