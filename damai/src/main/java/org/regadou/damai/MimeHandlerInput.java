package org.regadou.damai;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface MimeHandlerInput {

   Object load(InputStream input, String charset) throws IOException;
}
