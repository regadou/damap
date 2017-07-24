package org.regadou.damai;

public interface Expression extends Reference<Reference> {

   Action getAction();

   Reference[] getTokens();

   void addToken(Reference token);

}
