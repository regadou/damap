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
import org.regadou.damai.Action;
import org.regadou.damai.Converter;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.script.OperatorAction;

public class JdbcRepository implements Repository, Closeable {

   private Map<String, String[]> primaryKeys = new LinkedHashMap<>();
   private Map<String, Map<String, Class>> columnTypes = new LinkedHashMap<>();
   private JdbcConnectionInfo info;
   private Converter converter;
   private Connection connection;
   private Statement statement;

   public JdbcRepository(JdbcConnectionInfo info, Converter converter) {
      this.info = info;
      this.converter = converter;
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
   public Collection<Bindings> getAny(String type, Expression filter) {
      String sql = "select * from " + type;
      if (filter != null && filter.getAction() != null)
         sql += " where " + getClause(filter);
      try { return getRows(statement.executeQuery(sql)); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Bindings getOne(String type, Object id) {
      Object[] params = converter.convert(id, Object[].class);
      String sql = "select * from " + type +  getFilter(type, getMap(primaryKeys.get(type), params));
      try {
         Collection<Bindings>  entities = getRows(statement.executeQuery(sql));
         return entities.isEmpty() ? null : entities.iterator().next();
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Bindings save(String type, Bindings entity) {
      String filter = getFilter(type, getKeys(primaryKeys.get(type), entity));
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
      Object[] params = converter.convert(id, Object[].class);
      String sql = "delete from " + type + getFilter(type, getMap(primaryKeys.get(type), params));
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

   private String getFilter(String table, Bindings filter) {
      String sql = "";
      for (String key : filter.keySet()) {
         Object value = filter.get(key);
         if (value == null)
            return "";
         Class type = getColumnType(table, key);
         if (!type.isAssignableFrom(value.getClass()))
            value = converter.convert(value, type);
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
         rows.add(row);
      }
      rs.close();
      return rows;
   }

   private Class getColumnType(String table, String column) {
      Map<String, Class> columns = columnTypes.get(table);
      if (columns == null) {
         try {
            columnTypes.put(table, columns = new LinkedHashMap<>());
            for (Bindings row : getRows(connection.getMetaData().getColumns(info.getDatabase(),null,table,null)))
               columns.put(row.get("column_name").toString(), getJavaType(row.get("data_type")));
         }
         catch (SQLException e) {
            String msg = "Error looking for type of "+table+"."+column;
            throw new RuntimeException(msg, e);
         }
      }
      Class type = columns.get(column);
      return (type == null) ? Object.class : type;
   }

   private String getClause(Expression exp) {
      Reference[] tokens = exp.getTokens();
      if (tokens == null || tokens.length == 0)
         tokens = new Reference[2];
      else if (tokens.length < 2)
         tokens = new Reference[]{tokens[0], null};
      Action action = exp.getAction();
      String sql = "";
      for (Object token : tokens) {
         if (!(token instanceof Expression) && !(token instanceof Property)) {
            while (token instanceof Reference)
               token = ((Reference)token).getValue();
         }
         if (!sql.isEmpty())
            sql += getOperator(action, token);
         sql += printValue(token, false);
      }
      return sql;
   }

   private String printValue(Object value, boolean printOperator) {
      if (value instanceof Expression) {
         Expression exp = (Expression)value;
         if (exp.getAction() != null)
            return (printOperator ? " = " : "") + "(" + getClause(exp) + ")";
         value = exp.getTokens();
      }
      if (value instanceof Property)
         return ((Property)value).getName();
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      if (value instanceof Collection)
         value = ((Collection)value).toArray();
      if (value.getClass().isArray()) {
         int length = Array.getLength(value);
         switch (length) {
            case 0:
               value = null;
               break;
            case 1:
               return printValue(Array.get(value, 0), printOperator);
            default:
               StringJoiner joiner = new StringJoiner(", ", printOperator ? " in (" : "(", ")");
               for (int i = 0; i < length; i++)
                  joiner.add(printValue(Array.get(value, i), false));
               return joiner.toString();
         }
      }
      if (value == null)
         return printOperator ? " IS NULL " : " NULL ";
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

   private Class getJavaType(Object sqlType) {
      switch (Integer.parseInt(sqlType.toString())) {
         case java.sql.Types.ARRAY:
            return Object[].class;
         case java.sql.Types.BIGINT:
            return Long.class;
         case java.sql.Types.INTEGER:
            return Integer.class;
         case java.sql.Types.SMALLINT:
            return Short.class;
         case java.sql.Types.TINYINT:
            return Byte.class;
         case java.sql.Types.DECIMAL:
         case java.sql.Types.DOUBLE:
         case java.sql.Types.FLOAT:
         case java.sql.Types.NUMERIC:
         case java.sql.Types.REAL:
            return Double.class;
         case java.sql.Types.BINARY:
         case java.sql.Types.BLOB:
         case java.sql.Types.VARBINARY:
            return byte[].class;
         case java.sql.Types.BIT:
         case java.sql.Types.BOOLEAN:
            return Boolean.class;
         case java.sql.Types.CHAR:
         case java.sql.Types.CLOB:
         case java.sql.Types.LONGNVARCHAR:
         case java.sql.Types.LONGVARBINARY:
         case java.sql.Types.LONGVARCHAR:
         case java.sql.Types.NCHAR:
         case java.sql.Types.NCLOB:
         case java.sql.Types.NVARCHAR:
         case java.sql.Types.VARCHAR:
            return String.class;
         case java.sql.Types.NULL:
            return Void.class;
         case java.sql.Types.DATE:
            return java.sql.Date.class;
         case java.sql.Types.TIMESTAMP:
         case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
            return java.util.Date.class;
         case java.sql.Types.TIME:
         case java.sql.Types.TIME_WITH_TIMEZONE:
            return java.sql.Time.class;
         case java.sql.Types.DATALINK:
         case java.sql.Types.DISTINCT:
         case java.sql.Types.JAVA_OBJECT:
         case java.sql.Types.OTHER:
         case java.sql.Types.REF:
         case java.sql.Types.REF_CURSOR:
         case java.sql.Types.ROWID:
         case java.sql.Types.SQLXML:
         case java.sql.Types.STRUCT:
         default:
            return Object.class;
      }
   }

   private String getOperator(Action action, Object value) {
      Operator op;
      if (action instanceof Operator)
         op = (Operator)action;
      else if (action instanceof OperatorAction)
         op = ((OperatorAction)action).getOperator();
      else
         op = Operator.valueOf(action.getName().toUpperCase());
      switch (op) {
         case ADD: return " + ";
         case SUBTRACT: return " - ";
         case MULTIPLY: return " * ";
         case DIVIDE: return " / ";
         case MODULO: return " % ";
         case LESSER: return " < ";
         case LESSEQ: return " <= ";
         case GREATER: return " > ";
         case GREATEQ: return " >= ";
         case NOTEQUAL: return " <> ";
         case AND: return " AND ";
         case OR: return " OR ";
         case NOT: return " NOT ";
         case IN: return " IN ";
         case IS:
         case EQUAL:
            return (value == null) ? " IS " : " = ";
         case EXPONANT:
         case ROOT:
         case LOG:
         case FROM:
         case TO:
         case DO:
         case HAVE:
         case IF:
         case ELSE:
         case WHILE:
            throw new RuntimeException("Operator "+op+" is not supported for JDBC");
         default:
            throw new RuntimeException("Unknown operator "+op);
      }
   }
}
