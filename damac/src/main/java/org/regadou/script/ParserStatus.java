package org.regadou.script;

import java.util.*;
import javax.script.ScriptContext;

/*
This class is really not thread-safe and must only be used
     on a single thread for a single parsing phase
*/
public class ParserStatus {
   public int pos;
   public char end, end2;
   public char[] chars;
   public ScriptContext cx;
   public Object previousToken;
   public Set<Integer> lines = new TreeSet<>();

   public ParserStatus(String txt, ScriptContext cx) {
      this.chars = (txt == null) ? new char[0] : txt.toCharArray();
      this.cx = cx;
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
