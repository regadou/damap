package org.regadou.reference;

import java.io.IOException;
import java.io.InputStream;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerFactory;
import org.regadou.damai.Reference;

public class InputStreamReference implements Reference {

   private String id;
   private InputStream inputStream;
   private String mimetype;
   private String charset;
   private Object value;
   private MimeHandlerFactory handlerFactory;

   public InputStreamReference(String id, InputStream inputStream, String mimetype, String charset, MimeHandlerFactory handlerFactory) {
      this.id = id;
      this.inputStream = inputStream;
      this.mimetype = mimetype;
      this.charset = charset;
      this.handlerFactory = handlerFactory;
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public Object getValue() {
      if (value == null) {
         MimeHandler handler = handlerFactory.getHandler(mimetype);
         if (handler == null)
            value = "";
         else {
            try { value = handler.load(inputStream, charset); }
            catch (IOException e) { throw new RuntimeException(e); }
         }
      }
      return value;
   }

   @Override
   public Class getType() {
      return Object.class;
   }

   @Override
   public void setValue(Object value) {}

}
