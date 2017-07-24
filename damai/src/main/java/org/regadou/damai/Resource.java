package org.regadou.damai;

import java.io.InputStream;
import java.io.OutputStream;

public interface Resource extends Reference {

   String getUri();

   String getMimetype();

   InputStream getInputStream();

   OutputStream getOutputStream();
}
