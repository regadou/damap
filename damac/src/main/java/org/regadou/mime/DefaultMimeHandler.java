package org.regadou.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.MimeHandler;

public class DefaultMimeHandler implements MimeHandler {

   @FunctionalInterface
   public static interface InputHandler {
      Object load(InputStream input, String charset) throws IOException;
   }

   @FunctionalInterface
   public static interface OutputHandler {
      void save(OutputStream output, String charset, Object value) throws IOException;
   }

   private List<String> mimetypes = new ArrayList<>();
   private InputHandler inputHandler;
   private OutputHandler outputHandler;

   public DefaultMimeHandler(InputHandler inputHandler, OutputHandler outputHandler, String...mimetypes) {
      this.mimetypes = Arrays.asList(mimetypes);
      this.inputHandler = inputHandler;
      this.outputHandler = outputHandler;
   }

   @Override
   public String[] getMimetypes() {
      return mimetypes.toArray(new String[mimetypes.size()]);
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      return inputHandler.load(input, charset);
   }

   public void save(OutputStream output, String charset, Object value) throws IOException {
      outputHandler.save(output, charset, value);
   }
}
