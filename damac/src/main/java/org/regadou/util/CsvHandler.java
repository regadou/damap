package org.regadou.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.regadou.damai.Configuration;
import org.regadou.damai.MimeHandler;
import org.regadou.damai.MimeHandlerInput;
import org.regadou.damai.MimeHandlerOutput;

public class CsvHandler implements MimeHandler {

   private static final String[] mimetypes = new String[]{"text/csv"};
   private final char[] splitters = new char[]{',', ';', '\t'};
   private Configuration configuration;

   public CsvHandler(Configuration configuration) {
      this.configuration = configuration;
   }

   @Override
   public String[] getMimetypes() {
      return mimetypes;
   }

   @Override
   public MimeHandlerInput getInputHandler(String mimetype) {
      return (input, charset) -> {
         List<Object[]> lines = new ArrayList<>();
         BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
         String  line;
         char[] separator = new char[1];
         while ((line = reader.readLine()) != null)
            lines.add(getLineColumns(line, separator));
         return lines.toArray(new Object[lines.size()][]);
      };
   }

   @Override
   public MimeHandlerOutput getOutputHandler(String mimetype) {
      return (output, charset, value) -> {
         output.write(print(value, charset));
         output.flush();
      };
   }

   private Object[] getLineColumns(String line,  char[] separator) {
      if (line == null)
         return new Object[0];
      line = line.trim();
      if (line.equals(""))
         return new Object[0];

      char[] chars = line.toCharArray();
      List cells = new ArrayList();
      char instr = 0;
      int start = 0;

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (instr > 0) {
            if (c == instr)
               instr = 0;
            continue;
         }

         switch (c) {
            case '"':
            case '\'':
               instr = c;
               break;
            case ',':
            case ';':
            case '\t':
               if (separator[0] == 0)
                  separator[0] = c;
               else if (separator[0] != c)
                  continue;
               addCell(cells, chars, start, i);
         }
      }

      if (start >= 0)
         addCell(cells, chars, start, chars.length);
      return cells.toArray();
   }

   private void addCell(List cells, char[] chars, int start, int end) {
      String txt = new String(chars,start,end-start).trim();
      Object value;
      if (txt.equals(""))
         value = txt;
      else if (txt.startsWith("'") || txt.startsWith("\"")) {
         if (txt.charAt(0) == txt.charAt(txt.length()-1))
            value = txt.substring(1,txt.length()-1);
         else
            value = txt.substring(1);
      }
      else
         value = txt;
      cells.add(value);
   }

   private byte[] print(Object obj, String charset) throws UnsupportedEncodingException {
      Object[] rows;
      if (obj == null)
         return new byte[0];
      else if (obj instanceof CharSequence)
         return obj.toString().getBytes(charset);
      else if (obj instanceof char[])
         return new String((char[])obj).getBytes(charset);
      else if (obj instanceof byte[])
         return (byte[])obj;
      else if (obj.getClass().isArray())
         rows = configuration.getConverter().convert(obj, Collection.class).toArray();
      else if (obj instanceof Iterable)
         rows = configuration.getConverter().convert(obj, Collection.class).toArray();
      else if (obj instanceof Number || obj instanceof Boolean)
         return obj.toString().getBytes(charset);
      else
         rows = new Object[]{obj};

      if (rows.length == 0)
         return new byte[0];
      List fields = null;
      List<Object[]> dst = new ArrayList<>();
      char[] splitter = new char[]{'\0'};
      Object first = rows[0];
      if (first == null)
         dst.add(new Object[0]);
      else if (first instanceof CharSequence)
         dst.add(splitString(first.toString(), splitter));
      else if (first instanceof char[])
         dst.add(splitString(new String((char[])first), splitter));
      else if (first instanceof byte[])
         dst.add(splitString(new String((byte[])first, charset), splitter));
      else if (first.getClass().isArray())
         dst.add(configuration.getConverter().convert(obj, Collection.class).toArray());
      else if (obj instanceof Iterable)
         dst.add(configuration.getConverter().convert(obj, Collection.class).toArray());
      else if (obj instanceof Number || obj instanceof Boolean)
         dst.add(new Object[]{obj.toString()});
      else {
         Map map = configuration.getConverter().convert(first, Map.class);
         fields = new ArrayList(map.keySet());
         dst.add(map.values().toArray());
      }

      for (int r = 1; r < rows.length; r++) {
         Object row = rows[r];
         if (fields != null) {
            List cells = new ArrayList();
            Map map = configuration.getConverter().convert(row, Map.class);
            for (Object f : fields)
               cells.add(map.get(f));
            for (Object key : map.keySet()) {
               if (!fields.contains(key)) {
                  fields.add(key);
                  cells.add(map.get(key));
               }
            }
            dst.add(cells.toArray());
         }
         else if (row == null)
            dst.add(new String[0]);
         else if (row instanceof CharSequence)
            dst.add(splitString(row.toString(), splitter));
         else if (row instanceof char[])
            dst.add(splitString(new String((char[])row), splitter));
         else if (row instanceof byte[])
            dst.add(splitString(new String((byte[])row), splitter));
         else if (row.getClass().isArray())
            dst.add(configuration.getConverter().convert(row, Collection.class).toArray());
         else if (row instanceof Iterable)
            dst.add(configuration.getConverter().convert(row, Collection.class).toArray());
         else if (row instanceof Number || row instanceof Boolean)
            dst.add(new Object[]{row.toString()});
         else
            dst.add(configuration.getConverter().convert(row, Map.class).values().toArray());
      }

      if (fields != null)
         dst.add(0, fields.toArray());

      StringBuilder buffer = new StringBuilder();
      for (Object[] row : dst) {
         if (buffer.length() > 0)
            buffer.append("\n");
         String sep = null;
         for (Object cell : row) {
            if (sep == null) {
               if (splitter[0] == 0)
                  splitter[0] = splitters[0];
               sep = splitter[0] + "";
            }
            else
               buffer.append(sep);
            String txt = getString(cell);
            if (txt.indexOf(splitter[0]) >= 0) {
               char pad = (txt.indexOf('\'') >= 0) ? '"' : '\'';
               txt = pad + txt + pad;
            }
            buffer.append(txt);
         }
      }

      return buffer.toString().getBytes();
   }

   private String[] splitString(String txt, char[] splitter) {
      if (splitter[0] != 0)
         return txt.split(splitter[0]+"");
      String[] parts = null;
      for (char c : splitters) {
         String[] p = txt.split(c+"");
         if (parts == null || p.length > parts.length) {
            parts = p;
            if (p.length > 1)
               splitter[0] = c;
         }
      }
      return parts;
   }

   private String getString(Object obj) {
      if (obj == null)
         return "";
      else if (obj instanceof char[])
         return new String((char[])obj);
      else if (obj instanceof byte[])
         return new String((byte[])obj);
      else if (obj.getClass().isArray()) {
         StringBuilder buffer = new StringBuilder("(");
         int n = Array.getLength(obj);
         for (int i = 0; i < n; i++) {
            if (i > 0)
               buffer.append(",");
            buffer.append(getString(Array.get(obj, i)));
         }
         return buffer.append(")").toString();
      }
      else if (obj instanceof Collection)
         return getString(((Collection)obj).toArray());
      else if (obj instanceof Map)
         return getString(((Map)obj).values().toArray());
      else
         return obj.toString();
   }
}
