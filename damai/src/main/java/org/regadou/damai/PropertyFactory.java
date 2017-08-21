package org.regadou.damai;

public interface PropertyFactory<T> {

   Property getProperty(T value, String name);

   String[] getProperties(T value);
}
