package org.regadou.damai;

public interface Reference<T> {

   String getId();

   Class<T> getType();

   T getValue();

   void setValue(T value);
}
