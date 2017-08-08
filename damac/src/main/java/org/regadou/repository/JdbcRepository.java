package org.regadou.repository;

import java.sql.Statement;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.regadou.damai.Configuration;
import org.regadou.damai.Repository;

public class JdbcRepository implements Repository, Closeable {

   private Map<String, String[]> primaryKeys = new LinkedHashMap<>();
   private JdbcConnectionInfo info;
   private Configuration configuration;
   private Connection connection;
   private Statement statement;

   public JdbcRepository(JdbcConnectionInfo info, Configuration configuration) {
      this.info = info;
      this.configuration = configuration;
      try {
         openConnection();
         DatabaseMetaData dmd = connection.getMetaData();
         ResultSet rs = dmd.getTables(info.getDatabase(), null, null, null);
         while (rs.next()) {
            String type = rs.getString("table_type").toLowerCase();
            if (type.equals("table")) {
               String table = rs.getString("table_name");
               List<String> keys = new ArrayList<>();
               ResultSet rs2 = dmd.getPrimaryKeys(null, null, table);
               while (rs2.next())
                  keys.add(rs2.getString("COLUMN_NAME").toLowerCase());
               primaryKeys.put(table.toLowerCase(), keys.toArray(new String[keys.size()]));
               rs2.close();
            }
         }
         rs.close();
      }
      catch (SQLException | IOException e) { throw new RuntimeException(e); }
   }

   @Override
   public String toString() {
      return info.getUrl();
   }

   @Override
   public boolean equals(Object that) {
      return toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }

   @Override
   public Collection<String> getTypes() {
      return primaryKeys.keySet();
   }

   @Override
   public Collection<String> getPrimaryKeys(String type) {
      String[] keys = primaryKeys.get(type);
      return (keys == null) ? Collections.EMPTY_LIST : Arrays.asList(keys);
   }

   @Override
   public Collection<Object> getIds(String type) {
      Collection<Object> ids = new ArrayList<>();
      String[] keys = primaryKeys.get(type);
      if (keys == null || keys.length == 0)
         return ids;
      String sql = "select " +  String.join(", ", keys) + " from " + type;
      try {
         for (Bindings row : getRows(statement.executeQuery(sql))) {
            if (keys.length == 1)
               ids.add(row.get(keys[0]));
            else {
               Object[] id = new Object[keys.length];
               for (int k = 0; k < keys.length; k++)
                  id[k] = row.get(keys[k]);
               ids.add(id);
            }
         }
         return ids;
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Collection<Bindings> getAll(String type) {
      String sql = "select * from " + type;
      try { return getRows(statement.executeQuery(sql)); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Bindings getOne(String type, Object id) {
      Object[] params = configuration.getConverter().convert(id, Object[].class);
      String sql = "select * from " + type +  getFilter(getMap(primaryKeys.get(type), params));
      try {
         Collection<Bindings>  entities = getRows(statement.executeQuery(sql));
         return entities.isEmpty() ? null : entities.iterator().next();
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Bindings save(String type, Bindings entity) {
      String filter = getFilter(getKeys(primaryKeys.get(type), entity));
      try {
         String sql = filter.isEmpty()
            ? "insert into " + type + " ("
            + String.join(",", entity.keySet())
            + ") values " + printValue(entity.values(), false)
            : "update " + type + getUpdate(entity) + filter;
         int nb = statement.executeUpdate(sql);
         return (nb == 0) ? null : entity;
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public boolean delete(String type, Object id) {
      Object[] params = configuration.getConverter().convert(id, Object[].class);
      String sql = "delete from " + type + getFilter(getMap(primaryKeys.get(type), params));
      try { return statement.executeUpdate(sql) > 0; }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public void close() throws IOException {
      if (statement != null) {
         try { statement.close(); }
         catch (Exception e) {}
         statement = null;
      }
      if (connection != null) {
         try { connection.close(); }
         catch (Exception e) {}
         connection = null;
      }
   }

   private Bindings getMap(String[] keys, Object[] values) {
      Bindings map = new SimpleBindings();
      for (int k = 0; k < keys.length; k++) {
         Object value = (k >= values.length) ? null : values[k];
         map.put(keys[k], value);
      }
      return map;
   }

   private Bindings getKeys(String keys[], Bindings src) {
      Bindings dst = new SimpleBindings();
      for (String key : keys)
         dst.put(key, src.get(key));
      return dst;
   }

   private String getFilter(Bindings filter) {
      String sql = "";
      for (String key : filter.keySet()) {
         Object value = filter.get(key);
         if (value == null)
            return "";
         sql += (sql.isEmpty() ? " where " : " and ")
              + key + printValue(value, true);
      }
      return sql;
   }

   private String getUpdate(Bindings entity) {
      String sql = "";
      for (String key : entity.keySet())
         sql += (sql.isEmpty() ? " set " : ", ") + key + printValue(entity.get(key), false);
      return sql;
   }

   private Collection<Bindings> getRows(ResultSet rs) throws SQLException {
      Collection<Bindings> rows = new ArrayList<>();
      ResultSetMetaData meta = rs.getMetaData();
      int nc = meta.getColumnCount();
      while (rs.next()) {
         Bindings row = new SimpleBindings();
         for (int c = 1; c <= nc; c++) {
            String col = meta.getColumnName(c).toLowerCase();
            Object val = rs.getObject(c);
            row.put(col, val);
         }
      }
      rs.close();
      return rows;
   }

   private String printValue(Object value, boolean printOperator) {
      if (value == null)
         return printOperator ? " is null" : "null";
      if (value instanceof Collection)
         value = ((Collection)value).toArray();
      if (value.getClass().isArray()) {
         StringJoiner joiner = new StringJoiner(", ", printOperator ? " in (" : "(", ")");
         int length = Array.getLength(value);
         for (int i = 0; i < length; i++)
            joiner.add(printValue(Array.get(value, i), false));
         return joiner.toString();
      }
      String op = printOperator ? " = " : "";
      if (value instanceof Boolean)
         return op + (info.getVendor().isHasBoolean() ? value.toString() : ((Boolean)value ? "1" : "0"));
      if (value instanceof Number || value instanceof java.sql.Date
            || value instanceof java.sql.Time || value instanceof java.sql.Timestamp)
         return op + value.toString();
      if (value instanceof Date)
         return op + new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(value);
      return op + "'" + value.toString().replace("'", "''") + "'";
   }

   private void openConnection() throws IOException {
      if (info == null)
         throw new IOException("No connection info has been set");
      String checking = null;
      try {
           checking = "connection";
           if (connection != null && connection.isClosed()) {
              connection = null;
              statement = null;
           }
           checking = "statement";
           if (statement != null && statement.isClosed())
              statement = null;
           checking = null;
      }
      catch (Throwable t) {
         if ("connection".equals(checking))
            connection = null;
         statement = null;
      }

      try {
          if (connection == null)
              connection = DriverManager.getConnection(info.getUrl(), info.getUser(), info.getPassword());
          if (statement == null) {
              try {
                 statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
              }
              catch (Exception e) {
                 statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
              }
          }
      }
      catch (Exception e) {
         throw new IOException("Connection to " + info.getUrl() + " failed: " + e.toString(), e);
      }
   }
}
