package org.regadou.damai;

public interface DataType extends Data {

   DataType getParent();
   
   PropertyFactory<Data> getPropertyFactory();
}
