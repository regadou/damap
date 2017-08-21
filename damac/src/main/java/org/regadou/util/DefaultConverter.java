package org.regadou.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.regadou.damai.Converter;

public class DefaultConverter implements Converter {

   // first key is targetClass, second key is sourceClass
   private Map<Class,Map<Class,Function>> functionsMap = new LinkedHashMap<>();

   @Override
   public <T> T convert(Object value, Class<T> type) {
      Map<Class,Function> functions = functionsMap.get(type);
      if (functions == null)
         return null;
      ClassIterator iterator = new ClassIterator(value);
      while (iterator.hasNext()) {
         Class c = iterator.next();
         Function function = functions.get(c);
         if (function != null)
            return (T)function.apply(value);
      }
      return null;
   }

   @Override
   public <S, T> void registerFunction(Class<S> sourceClass, Class<T> targetClass, Function<S, T> function) {
      Map<Class,Function> functions = functionsMap.get(targetClass);
      if (functions == null) {
         functions = new LinkedHashMap<>();
         functionsMap.put(targetClass, functions);
      }
      functions.put(sourceClass, function);
   }
}
