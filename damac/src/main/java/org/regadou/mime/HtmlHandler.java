package org.regadou.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.regadou.action.GenericComparator;
import java.util.Iterator;
import java.util.Map;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.Reference;
import org.regadou.reference.MapEntryReference;
import org.regadou.system.StringInput;

public class HtmlHandler implements MimeHandler {

   public static class Link {
      private String uri;
      private Object value;
      public Link(String uri, Object value) {
         if (uri != null) {
            while (uri.endsWith("/"))
               uri = uri.substring(0, uri.length()-1);
         }
         this.uri = uri;
         this.value = value;
      }
   }

   private static final String[] MIMETYPES = new String[]{"text/html", "application/xhtml+xml"};

   private GenericComparator comparator;

   public HtmlHandler(Configuration configuration) {
      comparator = configuration.getInstance(GenericComparator.class);
   }

   @Override
   public String[] getMimetypes() {
      return MIMETYPES;
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      return new StringInput(input, charset).toString();
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      String uri = null;
      if (value instanceof Link) {
         Link link = (Link)value;
         value = link.value;
         if (value != null && !comparator.isStringable(value))
            uri = link.uri;
      }
      output.write(printTag(value, uri).getBytes(charset));
      output.flush();
   }

   private String printTag(Object src, String uri) {
      if (src instanceof Reference) {
         Reference r = (Reference)src;
         String name = r.getId();
         return printTag((name == null || name.trim().isEmpty()) ? r.getValue() : name, uri);
      }
      if (src instanceof Map.Entry)
         return printTag(new MapEntryReference((Map.Entry)src), uri);
      if (src instanceof Class)
         return ((Class)src).getName();
      if (comparator.isStringable(src)) {
         String txt = escapeHtml(src.toString());
         if (uri != null)
            txt = "<a href='"+uri+"'>"+txt+"</a>";
         return txt;
      }
      if (src == null)
         return "";

      Iterator i = comparator.getIterator(src);
      StringBuilder buffer = new StringBuilder("<ul>\n");
      while (i.hasNext()) {
         Object e = i.next();
         String suburi = (uri == null) ? null : uri + "/" + e;
         buffer.append("<li>\n").append(printTag(e, suburi)).append("</li>\n");
      }
      return buffer.append("</ul>\n").toString();
   }

   private String escapeHtml(String txt) {
      //TODO: replace accents with html escape
      return txt.replace("\n", "<br>\n")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("&", "&amp;");
   }
}
