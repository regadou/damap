package org.regadou.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerInput;
import org.regadou.damai.MimeHandlerOutput;

public class DefaultMimeHandler implements MimeHandler {

   private List<String> mimetypes = new ArrayList<>();
   private MimeHandlerInput inputHandler;
   private MimeHandlerOutput outputHandler;

   public DefaultMimeHandler(MimeHandlerInput inputHandler, MimeHandlerOutput outputHandler, String...mimetypes) {
      this.mimetypes = Arrays.asList(mimetypes);
      this.inputHandler = inputHandler;
      this.outputHandler = outputHandler;
   }

   @Override
   public String[] getMimetypes() {
      return mimetypes.toArray(new String[mimetypes.size()]);
   }

   @Override
   public MimeHandlerInput getInputHandler(String mimetype) {
      return inputHandler;
   }

   @Override
   public MimeHandlerOutput getOutputHandler(String mimetype) {
      return outputHandler;
   }
}
