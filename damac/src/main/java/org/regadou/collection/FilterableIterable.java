package org.regadou.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.script.ScriptContext;
import org.regadou.damai.Expression;
import org.regadou.damai.Filterable;
import org.regadou.damai.PropertyManager;
import org.regadou.action.GenericComparator;
import org.regadou.script.PropertiesScriptContext;

public class FilterableIterable<T> implements Iterable<T>, Filterable {

   private PropertyManager manager;
   private Collection<T> items;
   private GenericComparator comparator;

   public FilterableIterable(PropertyManager manager, Collection<T> items) {
      this.manager = manager;
      this.items = items;
      this.comparator = new GenericComparator(null);
   }

   public FilterableIterable(PropertyManager manager, T...items) {
      this.manager = manager;
      this.items = Arrays.asList(items);
   }

   @Override
   public Collection<T> filter(Expression filter) {
      if (filter == null)
         return items;
      List<T> dst = new ArrayList();
      for (T item : items) {
         ScriptContext cx = new PropertiesScriptContext(item, manager, null);
         if (!comparator.isEmpty(filter.getValue(cx)))
            dst.add(item);
      }
      return dst;
   }

   @Override
   public Iterator<T> iterator() {
      return items.iterator();
   }
}
