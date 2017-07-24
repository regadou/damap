package org.regadou.damai;

public interface Reference<T> {

   String getName();

   T getValue();

   Class<T> getType();

   void setValue(T value);
}
