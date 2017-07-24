package org.regadou.damai;

import java.util.Collection;

@FunctionalInterface
public interface Filter {
   <T> Collection<T> filter(Collection<T> collection, Collection<Reference> criteria);
}
