package org.regadou.damai;

import java.util.Collection;

public interface Repository {

    public Collection<Class> getTypes();

    public Collection<String> getNames();

    public Class getType(String name);

    public <T> Collection<T> getAll(Class<T> type);

    public <T> T getOne(Class<T> type, Object id);

    public <T> T save(T entity);

    public <T> boolean delete(Class<T> type, Object id);
}

