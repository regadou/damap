package org.regadou.damai;

public interface Reference<T> {

   String getId();

   T getValue();

   Class<T> getType();

   void setValue(T value);
}
