package org.regadou.damai;

public interface Data {

    String getId();

    DataType getDatatype();

    Data getProperty(String property);

    void setProperty(String property, Data value);
}
