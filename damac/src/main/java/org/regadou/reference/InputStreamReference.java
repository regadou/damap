package org.regadou.reference;

import java.io.IOException;
import java.io.InputStream;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.damai.Reference;

public class InputStreamReference implements Reference {

   private MimeHandlerFactory handlerFactory;
   private InputStream inputStream;
   private String mimetype;
   private String charset;
   private Object value;

   public InputStreamReference(MimeHandlerFactory handlerFactory, InputStream inputStream, String mimetype, String charset) {
      this.handlerFactory = handlerFactory;
      this.inputStream = inputStream;
      this.mimetype = mimetype;
      this.charset = charset;
   }

   @Override
   public String getName() {
      return null;
   }

   @Override
   public Object getValue() {
      if (value == null) {
         MimeHandler handler = handlerFactory.getHandler(mimetype);
         if (handler == null)
            value = "";
         else {
            try { value = handler.getInputHandler(mimetype).load(inputStream, charset); }
            catch (IOException e) { throw new RuntimeException(e); }
         }
      }
      return value;
   }

   @Override
   public Class getType() {
      return (value == null) ? Object.class : value.getClass();
   }

   @Override
   public void setValue(Object value) {
      this.value = value;
   }

}
