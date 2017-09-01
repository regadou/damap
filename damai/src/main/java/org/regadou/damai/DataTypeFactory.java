package org.regadou.damai;

public interface DataTypeFactory {

    DataType createDataType(String id, DataType parent, PropertyFactory propertyFactory, Class...classes);

    DataType getDataType(String id);

    DataType[] getDataTypes(DataType parent);

    DataType getRootDataType();
}
