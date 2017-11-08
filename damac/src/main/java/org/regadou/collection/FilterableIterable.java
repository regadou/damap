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
import org.regadou.damai.Configuration;
import org.regadou.script.PropertiesScriptContext;

public class FilterableIterable<T> implements Iterable<T>, Filterable {

   private PropertyManager manager;
   private Collection<T> items;
   private GenericComparator comparator;

   public FilterableIterable(Configuration configuration, T...items) {
      this(configuration, Arrays.asList(items));
   }

   public FilterableIterable(Configuration configuration, Collection<T> items) {
      this.manager = configuration.getPropertyManager();
      this.items = items;
      this.comparator = configuration.getInstance(GenericComparator.class);
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
