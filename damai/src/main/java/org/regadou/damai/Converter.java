package org.regadou.damai;

@FunctionalInterface
public interface Converter<S,T> {

   T convert(S source);
}
