package org.regadou.action;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.regadou.damai.Action;
import org.regadou.damai.Converter;

public class FieldAction implements Action {

   private Converter converter;
   private Field field;
   private boolean isStatic;

   public FieldAction(Converter converter, Field field) {
      this.converter = converter;
      this.field = field;
      this.isStatic = Modifier.isStatic(field.getModifiers());
   }

   @Override
   public String toString() {
      return getName();
   }

   @Override
   public boolean equals(Object that) {
      return that instanceof FieldAction && ((FieldAction)that).field.equals(field);
   }

   @Override
   public int hashCode() {
      return field.hashCode();
   }

   @Override
   public Object execute(Object ... parameters) {
      try {
         switch (parameters.length) {
            case 0:
               if (isStatic)
                     return field.get(null);
               throw new RuntimeException("Missing instance value");
            default:
            case 2:
               field.set(parameters[0], parameters[1]);
            case 1:
               return field.get(parameters[0]);
         }
      }
      catch (IllegalArgumentException|IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public String getName() {
      return field.getName();
   }

   @Override
   public Class getReturnType() {
      return field.getType();
   }

   @Override
   public Class[] getParameterTypes() {
      return new Class[]{field.getDeclaringClass(), field.getType()};
   }

}
