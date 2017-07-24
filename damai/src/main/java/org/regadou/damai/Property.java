package org.regadou.damai;

public interface Property<P,T> extends Reference<T> {

   P getParent();

   Class<P> getParentType();
}
