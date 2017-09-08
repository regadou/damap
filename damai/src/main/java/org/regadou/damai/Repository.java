package org.regadou.damai;

import java.util.Collection;
import java.util.Map;

public interface Repository<T> {

   Collection<String> getItems();

   Map<String,Class> getKeys(String item);

   Collection<String> getPrimaryKeys(String item);

   Collection<Object> getIds(String item);

   Collection<T> getAny(String item, Expression filter);

   T getOne(String item, Object id);

   T add(String item, T entity);

   T update(String item, T entity);

   boolean remove(String item, Object id);
}
