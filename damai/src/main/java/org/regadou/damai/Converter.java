package org.regadou.damai;

import java.util.function.Function;

public interface Converter {

   <T> T convert(Object value, Class<T> type);

   <S,T> void registerFunction(Class<S> sourceClass, Class<T> targetClass, Function<S,T> function);
}
