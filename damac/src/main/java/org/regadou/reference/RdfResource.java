package org.regadou.reference;

import java.util.LinkedHashMap;
import org.regadou.damai.Namespace;
import org.regadou.damai.Resource;

public class RdfResource implements Resource {

   private String name;
   private Namespace namespace;
   private Object value;

   public RdfResource(String name, Namespace namespace, Object value) {
      this.name = name;
      this.namespace = namespace;
      this.value = value;
   }

   @Override
   public Namespace getNamespace() {
      return namespace;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public Object getValue() {
      if (value == null)
         value = new LinkedHashMap();
      return value;
   }

   @Override
   public Class getType() {
      return (value == null) ? Object.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      if (this.value == null)
         this.value = value;
   }
}
