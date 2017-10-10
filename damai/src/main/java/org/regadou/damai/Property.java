package org.regadou.damai;

public interface Property<P,T> extends Reference<T> {

   P getOwner();

   Class<P> getOwnerType();
}
