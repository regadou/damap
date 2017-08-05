package org.regadou.damai;

public interface MimeHandler {

   String[] getMimetypes();

   MimeHandlerInput getInputHandler(String mimetype);

   MimeHandlerOutput getOutputHandler(String mimetype);

}
