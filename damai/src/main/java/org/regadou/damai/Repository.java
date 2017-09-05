package org.regadou.damai;

import java.util.Collection;
import java.util.Map;

public interface Repository<T> {

   Collection<String> getItems();

   Map<String,Class> getKeys(String item);

   Collection<String> getPrimaryKeys(String item);

   Collection<Object> getIds(String item);

   Collection<T> getAll(String item);

   Collection<T> getAny(String item, Expression exp);

   T getOne(String item, Object id);

   T insert(String item, T entity);

   T save(String item, T entity);

   boolean delete(String item, Object id);
}
