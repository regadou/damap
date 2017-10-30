package org.regadou.mime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.regadou.collection.StaticMap;
import org.regadou.damai.Converter;
import org.regadou.damai.MimeHandler;
import org.regadou.number.Complex;
import org.regadou.number.Probability;

public class CsvHandler implements MimeHandler {

   private static final String[] MIMETYPES = new String[]{"text/csv"};
   private static final char[] SPLITTERS = new char[]{',', ';', '\t'};

   private static class ParsingStatus {
      private String[] fields;
      private char separator;
      int lineno;
   }

   private Converter converter;

   public CsvHandler(Converter converter) {
      this.converter = converter;
   }

   @Override
   public String[] getMimetypes() {
      return MIMETYPES;
   }

   @Override
   public Object load(InputStream input, String charset) throws IOException {
      //TODO: must implement a Collection that is linked to a BufferedReader to load its elelments
      Collection rows = new ArrayList();
      BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset));
      ParsingStatus status = new ParsingStatus();
      String  line;
      while ((line = reader.readLine()) != null) {
         status.lineno++;
         Object row = getLineColumns(line, status);
         if (row != null)
            rows.add(row);
      }
      return rows;
   }

   @Override
   public void save(OutputStream output, String charset, Object value) throws IOException {
      output.write(print(value, charset));
      output.flush();
   }

   private Object getLineColumns(String line, ParsingStatus status) {
      if (line == null)
         return null;
      line = line.trim();
      if (line.isEmpty())
         return null;

      char[] chars = line.toCharArray();
      List cells = new ArrayList();
      char instr = 0;
      StringBuilder buffer = null;

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (instr > 0) {
            buffer.append(c);
            if (c == instr) {
               i++;
               c = chars[i];
               if (i < chars.length && c == instr)
                  continue;
               instr = 0;
            }
            else
               continue;
         }

         switch (c) {
            case '"':
            case '\'':
               if (buffer == null)
                  buffer = new StringBuilder();
               else if (!buffer.toString().trim().isEmpty()) {
                  buffer.append(c);
                  continue;
               }
               buffer.append(c);
               instr = c;
               break;
            case ',':
            case ';':
            case '\t':
               if (status.separator == 0 || status.separator == c) {
                  status.separator = c;
                  addCell(cells, (buffer == null) ? "" : buffer.toString(), status);
                  break;
               }
            default:
               if (buffer == null)
                  buffer = new StringBuilder();
               buffer.append(c);
         }
      }

      if (instr > 0)
         throw new RuntimeException("Missing closing quote at line "+status.lineno+": "+instr);
      if (buffer != null)
         addCell(cells, buffer.toString(), status);

      if (status.fields == null) {
         for (Object cell : cells) {
            if (cell instanceof String && !cell.toString().trim().isEmpty())
                  continue;
            status.fields = new String[0];
            return cells;
         }
         status.fields = (String[])cells.toArray(new String[cells.size()]);
         return null;
      }
      else if (status.fields.length == 0)
         return cells;
      else
         return new StaticMap(status.fields, cells.toArray());
   }

   private void addCell(List cells, String txt, ParsingStatus status) {
      txt = txt.trim();
      Object value = txt;
      if (!txt.isEmpty()) {
         char first = txt.charAt(0);
         if ((first == '"' || first == '\''))
            value = txt.substring(1, txt.length()-1);
         else
            value = getValue(txt);
      }
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
         rows = converter.convert(obj, Collection.class).toArray();
      else if (obj instanceof Iterable)
         rows = converter.convert(obj, Collection.class).toArray();
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
         dst.add(converter.convert(obj, Collection.class).toArray());
      else if (obj instanceof Iterable)
         dst.add(converter.convert(obj, Collection.class).toArray());
      else if (obj instanceof Number || obj instanceof Boolean)
         dst.add(new Object[]{obj.toString()});
      else {
         Map map = converter.convert(first, Map.class);
         fields = new ArrayList(map.keySet());
         dst.add(map.values().toArray());
      }

      for (int r = 1; r < rows.length; r++) {
         Object row = rows[r];
         if (fields != null) {
            List cells = new ArrayList();
            Map map = converter.convert(row, Map.class);
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
            dst.add(converter.convert(row, Collection.class).toArray());
         else if (row instanceof Iterable)
            dst.add(converter.convert(row, Collection.class).toArray());
         else if (row instanceof Number || row instanceof Boolean)
            dst.add(new Object[]{row.toString()});
         else
            dst.add(converter.convert(row, Map.class).values().toArray());
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
                  splitter[0] = SPLITTERS[0];
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
      for (char c : SPLITTERS) {
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

   private Object getValue(String txt) {
      if (txt.equals("true"))
         return Boolean.TRUE;
      if (txt.equals("false"))
         return Boolean.FALSE;
      try {
         if (txt.endsWith("%"))
            return new Probability(txt);
         if (txt.indexOf('I') >= 0 || txt.indexOf('i') >= 0)
            return new Complex(txt);
         if (txt.indexOf('.') >= 0 || txt.indexOf('E') >= 0 || txt.indexOf('e') >= 0)
            return new Double(txt);
         if (txt.startsWith("0x"))
            return Long.parseLong(txt.substring(2), 16);
         return new Long(txt);
      }
      catch (NumberFormatException e) {
         Object t = parseTime(txt);
         return (t == null) ? txt : t;
      }
   }

   private Object parseTime(String txt) {
      int[] parts = new int[]{0, 0, 0, 0, 0, 0};
      int date = 0, time = 0;
      for (String part : txt.toLowerCase().replace('"', ' ').split(" ")) {
         if (part.isEmpty())
            continue;
         int m = MONTHS.indexOf(part);
         if (m >= 0) {
            parts[MONTH] = m%12+1;
            date++;
         }
         else if (part.contains("-")) {
            String[] subparts = part.split("-");
            switch (subparts.length) {
               default:
                  continue;
               case 3:
                  parts[DAY] = new Float(subparts[2]).intValue();
               case 2:
                  parts[MONTH] = new Float(subparts[1]).intValue();
               case 1:
                  parts[YEAR] = new Float(subparts[0]).intValue();
            }
            date += subparts.length;
         }
         else if (part.contains(":")) {
            String[] subparts = part.split(":");
            switch (subparts.length) {
               default:
                  continue;
               case 3:
                  parts[SECOND] = new Float(subparts[2]).intValue();
               case 2:
                  parts[MINUTE] = new Float(subparts[1]).intValue();
               case 1:
                  parts[HOUR] = new Float(subparts[0]).intValue();
            }
            time += subparts.length;
         }
         else if (part.equals("pm") && parts[HOUR] < 12)
            parts[HOUR] += 12;
         else if (part.equals("am") && parts[HOUR] > 11)
            parts[HOUR] -= 12;
         else {
            try {
               int n = Integer.parseInt(part);
               if (n > 200)
                  parts[YEAR] = n;
               else if (n > 31)
                  parts[YEAR] = n+1900;
               else if (n > 0)
                  parts[DAY] = n;
               else
                  continue;
               date++;
            }
            catch (NumberFormatException e) {}
         }
      }

      if (date == 0) {
         if (time == 0)
            return null;
         return new Time(parts[HOUR], parts[MINUTE], parts[SECOND]);
      }
      else if (time == 0)
         return new Date(parts[YEAR], parts[MONTH], parts[DAY]);
      return new Timestamp(parts[YEAR], parts[MONTH], parts[DAY], parts[HOUR], parts[MINUTE], parts[SECOND], 0);
   }

   private static final int YEAR=0, MONTH=1, DAY=2, HOUR=3, MINUTE=4, SECOND=5;
   private static final List<String> MONTHS = Arrays.asList(
       "jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec,jan,fev,mar,avr,mai,jun,jui,aou,sep,oct,nov,dec".split(",")
   );
}
