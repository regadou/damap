package org.regadou.damai;

public interface PropertyManager {

   Property getProperty(Object value, String name);

   <T> PropertyFactory<T> getPropertyFactory(Class<T> type);

   <T> boolean registerPropertyFactory(Class<T> type, PropertyFactory<T> factory);
}
