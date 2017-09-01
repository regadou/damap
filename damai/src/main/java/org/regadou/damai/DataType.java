package org.regadou.damai;

public interface DataType extends Data {

   PropertyFactory getPropertyFactory();

   Data getInstance(Object data);

   Data[] getInstances(Expression filter);
}
