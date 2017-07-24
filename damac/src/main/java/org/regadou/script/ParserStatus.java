package org.regadou.script;

import java.util.*;
import javax.script.ScriptContext;

public class ParserStatus {
   public ScriptContext cx;
   public int pos;
   public char end;
   public char[] chars;
   public Object previousToken;
   public Set<Integer> lines = new TreeSet<>();

   public ParserStatus(ScriptContext cx, String txt) {
      this.cx = cx;
      this.chars = (txt == null) ? new char[0] : txt.toCharArray();
   }

   public char nextChar() {
      return (pos+1 >= chars.length) ? '\0' : chars[pos+1];
   }

   public char previousChar() {
      return (pos <= 0) ? '\0' : chars[pos-1];
   }

   public void linecount() {
      if (chars[pos] == '\n')
         lines.add(pos);
   }

   public int lineno() {
      int line = 1;
      Iterator<Integer> i = lines.iterator();
      while (i.hasNext()) {
         line++;
         if (i.next() > pos)
            break;
      }
      return line;
   }
}
