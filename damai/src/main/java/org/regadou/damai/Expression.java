package org.regadou.damai;

import javax.script.ScriptContext;

public interface Expression<T> extends Reference<T> {

   Action getAction();

   T[] getArguments();

   T getValue(ScriptContext context);

   default void addToken(T token) {
      throw new UnsupportedOperationException("Cannot add tokens with "+getClass().getName());
   }

}
