package org.regadou.damai;

public interface MimeHandlerFactory {

   void registerHandler(MimeHandler handler);

   MimeHandler getHandler(String mimetype);
}
