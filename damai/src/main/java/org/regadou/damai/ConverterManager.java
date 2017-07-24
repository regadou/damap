package org.regadou.damai;

public interface ConverterManager {

   <S,T> Converter<S,T> getConverter(Class<S> sourceClass, Class<T> targetClass);

   <S,T> void registerConverter(Class<S> sourceClass, Class<T> targetClass, Converter<S,T> converter);
}
