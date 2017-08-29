package org.regadou.damai;

import java.util.Collection;
import java.util.Map;

public interface Repository {

   Collection<String> getItems();

   Collection<String> getPrimaryKeys(String item);

   Collection<Object> getIds(String item);

   Collection<Map> getAll(String item);

   Collection<Map> getAny(String item, Expression exp);

   Map getOne(String item, Object id);

   Map save(String item, Map entity);

   boolean delete(String item, Object id);
}
