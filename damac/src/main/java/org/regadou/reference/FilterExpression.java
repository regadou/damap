package org.regadou.reference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.script.ScriptContext;
import org.regadou.damai.Action;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;

public class FilterExpression implements Expression {

   private PropertyFactory propertyFactory;
   private Collection items;
   private List<Reference> conditions = new ArrayList<>();

   public FilterExpression(PropertyFactory propertyFactory, Collection items, Reference...conditions) {
      this(propertyFactory, items, (conditions == null) ? null : Arrays.asList(conditions));
   }

   public FilterExpression(PropertyFactory propertyFactory, Collection items, List<Reference> conditions) {
      this.propertyFactory = propertyFactory;
      this.items = items;
      if (conditions != null)
         this.conditions.addAll(conditions);
   }

   @Override
   public String toString() {
      return getValue().toString();
   }

   @Override
   public Action getAction() {
      return Operator.IS;
   }

   @Override
   public Reference[] getTokens() {
      if (!conditions.isEmpty())
         return new Reference[]{new ReferenceHolder(null, items), new ReferenceHolder(null, conditions)};
      else if (items != null)
         return new Reference[]{new ReferenceHolder(null, items)};
      else
         return new Reference[0];
   }

   @Override
   public void addToken(Reference token) {
      if (!conditions.isEmpty() || items != null)
         conditions.add(token);
      else {
         Object value = token.getValue();
         if (value instanceof Collection)
            items = (Collection)value;
         else if (value instanceof Object[])
            items = Arrays.asList((Object[])value);
         else if (value == null)
            items = Collections.EMPTY_LIST;
         else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object[] array = new Object[length];
            items = Arrays.asList(array);
            for (int i = 0; i < length; i++)
               array[i] = Array.get(value, i);
         }
         else
            items = Arrays.asList(value);
      }
   }

   @Override
   public String getName() {
      return null;
   }

   @Override
   public Reference getValue() {
      Collection result;
      if (items == null || items.isEmpty())
         result = Collections.EMPTY_LIST;
      else if (conditions.isEmpty())
         result = items;
      else {
         result = new ArrayList();
         for (Object element : items) {
            boolean rejected = false;
            Map<String,Property> properties = propertyFactory.getProperties(element);
            for (Reference condition : conditions) {
               Property p = properties.get(condition.getName());
               Object value = (p == null) ? null : p.getValue();
               if (compare(value, condition.getValue()) != 0) {
                  rejected = true;
                  break;
               }
            }
            if (!rejected)
               result.add(element);
         }
      }

      return new ReferenceHolder(null, result);
   }

   @Override
   public Class<Reference> getType() {
      return Reference.class;
   }

   @Override
   public void setValue(Reference value) {
      throw new UnsupportedOperationException("Setting a filter expression is not supported");
   }

   @Override
   public Reference getValue(ScriptContext context) {
      return getValue();
   }

   public int compare(Object o1, Object o2) {
      String t1 = (o1 == null) ? "" : o1.toString();
      String t2 = (o2 == null) ? "" : o2.toString();
      return t1.compareTo(t2);
   }
}
