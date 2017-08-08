package org.regadou.damai;

import java.util.Collection;

public interface MimeHandlerFactory {

   void registerHandler(MimeHandler handler);

   MimeHandler getHandler(String mimetype);

   Collection<String> getMimetypes();
}
