package org.regadou.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import javax.inject.Inject;
import org.regadou.damai.Filter;
import org.regadou.damai.Property;
import org.regadou.damai.PropertyFactory;
import org.regadou.damai.Reference;

public class DefaultFilter implements Filter   {

   private PropertyFactory propertyFactory;
   private Comparator comparator;

   @Inject
   public DefaultFilter(PropertyFactory propertyFactory, Comparator comparator) {
      this.propertyFactory = propertyFactory;
      this.comparator = comparator;
   }

   @Override
   public <T> Collection<T> filter(Collection<T> collection, Collection<Reference> criteria) {
      if (collection == null || collection.isEmpty())
         return Collections.EMPTY_LIST;
      if (criteria == null || criteria.isEmpty())
         return collection;
      Collection filtered = new ArrayList();

      for (T element : collection) {
         boolean rejected = false;
         Map<String,Property> properties = propertyFactory.getProperties(element);
         for (Reference criterion : criteria) {
            Property p = properties.get(criterion.getName());
            Object value = (p == null) ? null : p.getValue();
            if (comparator.compare(value, criterion.getValue()) != 0) {
               rejected = true;
               break;
            }
         }
         if (!rejected)
            filtered.add(element);
      }

      return filtered;
   }
}
