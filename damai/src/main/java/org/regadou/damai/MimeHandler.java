package org.regadou.damai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface MimeHandler {

   String[] getMimetypes();

   Object load(InputStream input, String charset) throws IOException;

   void save(OutputStream output, String charset, Object value) throws IOException;

}
