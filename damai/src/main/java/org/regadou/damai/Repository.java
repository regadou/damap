package org.regadou.damai;

import java.util.Collection;
import javax.script.Bindings;

public interface Repository {

   Collection<String> getTypes();

   Collection<String> getPrimaryKeys(String type);

   Collection<Object> getIds(String type);

   Collection<Bindings> getAll(String type);

   Collection<Bindings> getAny(String type, Expression exp);

   Bindings getOne(String type, Object id);

   Bindings save(String type, Bindings entity);

   boolean delete(String type, Object id);
}
