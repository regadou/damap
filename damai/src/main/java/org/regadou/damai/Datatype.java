package org.regadou.damai;

public interface Datatype {

   String getId();

   PropertyFactory getPropertyFactory();

   Object getInstance(Object data);
}
