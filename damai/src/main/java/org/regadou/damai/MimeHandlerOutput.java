package org.regadou.damai;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface MimeHandlerOutput {

   void save(OutputStream output, String charset, Object value) throws IOException;

}
