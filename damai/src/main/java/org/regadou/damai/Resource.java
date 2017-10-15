package org.regadou.damai;

public interface Resource<T> extends Reference<T> {

   String getLocalName();

   Reference getOwner();

   String[] getProperties();

   Reference getProperty(String property);

   void setProperty(String property, Reference value);

   boolean addProperty(String property, Reference value);
}
