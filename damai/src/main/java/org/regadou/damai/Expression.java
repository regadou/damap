package org.regadou.damai;

import javax.script.ScriptContext;

public interface Expression extends Reference<Reference> {

   Action getAction();

   Reference[] getTokens();

   void addToken(Reference token);

   Reference getValue(ScriptContext context);

}
