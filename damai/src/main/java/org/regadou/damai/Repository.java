package org.regadou.damai;

import java.util.Collection;
import java.util.Map;

public interface Repository {

    public Collection<String> getTypes();

    public Collection<Map<String,Object>> getAll(String type);

    public Map<String,Object> getOne(String type, Object id);

    public Map<String,Object> save(String type, Map<String,Object> entity);

    public boolean delete(String type, Object id);
}

