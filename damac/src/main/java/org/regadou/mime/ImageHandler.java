package org.regadou.mime;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.imageio.ImageIO;
import org.regadou.damai.Converter;
import org.regadou.damai.MimeHandler;

public class ImageHandler implements MimeHandler {

   private String[] mimetypes = new String[1];
   private String type;
   private Converter converter;

   public ImageHandler(String mimetype, Converter converter) {
      mimetypes[0] = mimetype;
      type = mimetype.split("/")[1];
      if (type.startsWith("x-"))
         type = type.substring(2);
      if (type.startsWith("vnd."))
         type = type.substring(4);
      int dot = type.lastIndexOf('.');
      if (dot > 0)
         type = type.substring(dot+1);
      this.converter = converter;
   }

   @Override
   public String[] getMimetypes() {
      return mimetypes;
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      return ImageIO.read(input);
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      RenderedImage image = converter.convert(value, RenderedImage.class);
      ImageIO.write(image, type, output);
   }
}
