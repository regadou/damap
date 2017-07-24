package org.regadou.damai;

public interface InstanceFactory {

   <T> T getInstance(Class<T> type, Reference ... properties);

   void registerInstance(Class iface, Class impl, Reference ... properties);
}
