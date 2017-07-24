package org.regadou.factory;

import org.apache.commons.convert.ConversionException;
import org.apache.commons.convert.Converters;
import org.regadou.damai.Converter;
import org.regadou.damai.ConverterManager;

public class DefaultConverterManager implements ConverterManager {

   public static class ConverterWrapper<S,T> implements Converter<S,T> {

      private org.apache.commons.convert.Converter<S,T> wrappedConverter;

      public ConverterWrapper(org.apache.commons.convert.Converter<S,T> converter) {
         wrappedConverter = converter;
      }

      @Override
      public T convert(S source) {
         return wrappedConverter.convert(source);
      }
   }

   @Override
   public <S, T> Converter<S, T> getConverter(Class<S> sourceClass, Class<T> targetClass) {
      org.apache.commons.convert.Converter converter = Converters.getConverter(sourceClass, targetClass);
      if (converter == null)
         return null;
      else if (converter instanceof Converter)
         return (Converter)converter;
      else
         return new ConverterWrapper(converter);
   }

   @Override
   public <S, T> void registerConverter(Class<S> sourceClass, Class<T> targetClass, Converter<S, T> converter) {
      org.apache.commons.convert.Converter apacheConverter;
      if (converter instanceof org.apache.commons.convert.Converter)
         apacheConverter = (org.apache.commons.convert.Converter)converter;
      else if (converter instanceof ConverterWrapper)
         apacheConverter = ((ConverterWrapper)converter).wrappedConverter;
      else
         apacheConverter = new org.apache.commons.convert.Converter<S,T>() {
            @Override
            public boolean canConvert(Class src, Class dst) {
               return sourceClass.isAssignableFrom(src) && targetClass.isAssignableFrom(dst);
            }

            @Override
            public T convert(S s) throws ConversionException {
               return converter.convert(s);
            }

            @Override
            public Class<S> getSourceClass() {
               return sourceClass;
            }

            @Override
            public Class<T> getTargetClass() {
               return targetClass;
            }
         };

      Converters.registerConverter(apacheConverter, sourceClass, targetClass);
   }

}
