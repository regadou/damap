package org.regadou.damai;

public interface Data {

    String getId();

    DataType getType();

    Data getProperty(String property);

    void setProperty(String property, Data value);
}
