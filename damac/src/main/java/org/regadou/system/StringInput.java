package org.regadou.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class StringInput implements CharSequence {

   private Reader reader;
   private String text;

   public StringInput(InputStream input, String charset) {
      try { reader = new InputStreamReader(input, charset); }
      catch (Exception e) { reader = new InputStreamReader(input); }
   }

   public StringInput(Reader reader) {
      this.reader = reader;
   }

   @Override
   public String toString() {
      if (text == null) {
         try {
            StringBuilder buffer = new StringBuilder();
            char[] chars = new char[1024];
            for (int got = 0; (got = reader.read(chars)) >= 0;) {
               if (got > 0)
                  buffer.append(chars, 0, got);
            }
            text = buffer.toString();
         }
         catch (IOException e) { throw new RuntimeException(e); }
      }
      return text;
   }

   @Override
   public int length() {
      return toString().length();
   }

   @Override
   public char charAt(int index) {
      //TODO: we could be more intelligent and buffer the loading from the reader
      return toString().charAt(index);
   }

   @Override
   public CharSequence subSequence(int start, int end) {
      //TODO: we could be more intelligent and buffer the loading from the reader
      return toString().subSequence(start, end);
   }
}
