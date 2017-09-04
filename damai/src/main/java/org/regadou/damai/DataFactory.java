package org.regadou.damai;

import java.util.Collection;

public interface DataFactory {

   DataType getRootType();

   DataType createType(String id, DataType parent, PropertyFactory propertyFactory, Class...classes);

   Data newInstance(String id, DataType type, Object...parameters);

   Data getInstance(Object data);

   Collection<Data> getInstances(Expression filter);
}
