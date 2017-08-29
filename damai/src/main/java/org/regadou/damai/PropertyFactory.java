package org.regadou.damai;

public interface PropertyFactory<T> {

   Property getProperty(T parent, String name);

   String[] getProperties(T parent);

   Property addProperty(T parent, String name, Object value);

   boolean removeProperty(T parent, String name);
}
