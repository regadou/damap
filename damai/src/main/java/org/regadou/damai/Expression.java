package org.regadou.damai;

import javax.script.ScriptContext;

public interface Expression extends Reference<Reference> {

   Action getAction();

   Reference[] getTokens();

   Reference getValue(ScriptContext context);

   default void addToken(Reference token) {
      throw new UnsupportedOperationException("Cannot add tokens with "+getClass().getName());
   }

}
