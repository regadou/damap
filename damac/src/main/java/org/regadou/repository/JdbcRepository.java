package org.regadou.repository;

import java.sql.Statement;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.regadou.action.DefaultAction;
import org.regadou.collection.ArrayWrapper;
import org.regadou.damai.Action;
import org.regadou.damai.Expression;
import org.regadou.damai.Operator;
import org.regadou.damai.Property;
import org.regadou.damai.Reference;
import org.regadou.damai.Repository;
import org.regadou.collection.PersistableMap;
import org.regadou.damai.Configuration;
import org.regadou.damai.StandardAction;
import org.regadou.expression.SimpleExpression;
import org.regadou.property.GenericProperty;

public class JdbcRepository implements Repository<Map>, Closeable {

   private transient Map<String, String[]> primaryKeys = new LinkedHashMap<>();
   private transient Map<String, Map<String, Class>> keysMap = new LinkedHashMap<>();
   private transient JdbcConnectionInfo info;
   private transient Configuration configuration;
   private transient Connection connection;
   private transient Statement statement;
   private Collection<String> items;

   public JdbcRepository(JdbcConnectionInfo info, Configuration configuration) {
      this.info = info;
      this.configuration = configuration;
      loadItems(true);
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
   public Collection<String> getItems() {
      return items;
   }

   @Override
   public Map<String,Class> getKeys(String item) {
      if (!items.contains(item))
         return Collections.EMPTY_MAP;
      Map<String, Class> keys = keysMap.get(item);
      if (keys == null) {
         try {
            keysMap.put(item, keys = new LinkedHashMap<>());
            for (Map row : getRows(item, connection.getMetaData().getColumns(info.getDatabase(),null,item,null)))
               keys.put(row.get("column_name").toString(), getJavaType(row.get("data_type")));
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
      return keys;
   }

   @Override
   public Collection<String> getPrimaryKeys(String item) {
      String[] keys = primaryKeys.get(item);
      return (keys == null) ? Collections.EMPTY_LIST : Arrays.asList(keys);
   }

   @Override
   public Collection<Object> getIds(String item) {
      Collection<Object> ids = new ArrayList<>();
      String[] keys = primaryKeys.get(item);
      if (keys == null || keys.length == 0)
         return ids;
      String sql = "select " +  String.join(", ", keys) + " from " + item;
      try {
         for (Map row : getRows(item, statement.executeQuery(sql))) {
            if (keys.length == 1)
               ids.add(row.get(keys[0]));
            else {
               Object[] id = new String[keys.length];
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
   public Collection<Map> getAny(String item, Expression filter) {
      if (!items.contains(item))
         return Collections.EMPTY_LIST;
      String sql = "select * from " + item;
      if (filter != null && filter.getAction() != null)
         sql += " where " + getClause(checkListOperation(item, filter));
      try { return getRows(item, statement.executeQuery(sql)); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Map getOne(String item, Object id) {
      if (!items.contains(item))
         return null;
      Object[] params = configuration.getConverter().convert(id, Object[].class);
      String sql = "select * from " + item +  getFilter(item, getMap(primaryKeys.get(item), params));
      try {
         Collection<Map>  entities = getRows(item, statement.executeQuery(sql));
         return entities.isEmpty() ? null : entities.iterator().next();
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Map add(String item, Map entity) {
      if (!items.contains(item))
         return null;
      String[] keys = primaryKeys.get(item);
      if (keys == null)
         keys = new String[0];
      try { return doInsert(item, entity, keys); }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public Map update(String item, Map entity) {
      if (!items.contains(item))
         return null;
      String[] keys = primaryKeys.get(item);
      if (keys == null)
         return null;
      String filter = getFilter(item, getKeys(keys, entity));
      try {
         if (filter.isEmpty())
            return doInsert(item, entity, keys);
         int nb = statement.executeUpdate("update " + item + getUpdate(entity) + filter);
         //TODO: entity might not be complete so we need to load the full object
         if (nb == 0)
            return null;
         String sql = "select * from " + item + filter;
         Collection<Map> entities = getRows(item, statement.executeQuery(sql));
         return entities.isEmpty() ? null : entities.iterator().next();
      }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public boolean remove(String item, Object id) {
      if (!items.contains(item))
         return false;
      Object[] params = configuration.getConverter().convert(id, Object[].class);
      String sql = "delete from " + item + getFilter(item, getMap(primaryKeys.get(item), params));
      try { return statement.executeUpdate(sql) > 0; }
      catch (SQLException e) { throw new RuntimeException(e); }
   }

   @Override
   public void createItem(String item, Object definition) throws IllegalArgumentException {
      if (items.contains(item))
         throw new IllegalArgumentException("Item "+item+" already exists");
      if (!(definition instanceof Map))
         throw new IllegalArgumentException("Item definition must be a Map");
      Map map = (Map)definition;
      if (map.isEmpty())
         throw new IllegalArgumentException("Definition does not contain any field");
      String primaryKey = null;
      String first = null;
      String sql = "create table "+item+" {";
      for (Object key : map.keySet()) {
         String name = key.toString();
         if (first == null)
            first = name;
         else
            sql += ",";
         sql += "\n   "+name+" "+getFieldDefinition(map.get(key));
      }
      try {
         statement.executeUpdate(sql+"\n}");
         loadItems(false);
      }
      catch (SQLException e) { throw new IllegalArgumentException("Cannot create item "+item+": "+e, e); }
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

   private void loadItems(boolean reloadConnection) {
      try {
         if (reloadConnection || connection == null)
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
      finally { items = new TreeSet<>(primaryKeys.keySet()); }
   }

   private Map doInsert(String item, Map entity, String[] keys) throws SQLException {
      String sql = "insert into " + item + " ("
                 + String.join(",", entity.keySet())
                 + ") values " + printValue(entity.values(), false);
      int nb = statement.executeUpdate(sql, keys);
      if (nb == 0)
         return null;
      ResultSet rs = statement.getGeneratedKeys();
      if (rs.next()) {
         entity = new PersistableMap(entity, getUpdateFunction(item));
         int nc = Math.min(keys.length, rs.getMetaData().getColumnCount());
         for (int c = 0; c < nc; c++)
            entity.put(keys[c], rs.getObject(c+1));
      }
      return entity;
   }

   private Map getMap(String[] keys, Object[] values) {
      Map map = new LinkedHashMap();
      for (int k = 0; k < keys.length; k++) {
         Object value = (k >= values.length) ? null : values[k];
         map.put(keys[k], value);
      }
      return map;
   }

   private Map getKeys(String keys[], Map src) {
      Map dst = new LinkedHashMap();
      for (String key : keys)
         dst.put(key, src.get(key));
      return dst;
   }

   private String getFilter(String table, Map filter) {
      String sql = "";
      Map<String,Class> keys = getKeys(table);
      for (Object key : filter.keySet()) {
         Object value = filter.get(key);
         if (value == null)
            return "";
         Class type = keys.get(key.toString());
         if (type != null && !type.isAssignableFrom(value.getClass()))
            value = configuration.getConverter().convert(value, type);
         sql += (sql.isEmpty() ? " where " : " and ")
              + key + printValue(value, true);
      }
      return sql;
   }

   private String getUpdate(Map entity) {
      String sql = "";
      for (Object key : entity.keySet())
         sql += (sql.isEmpty() ? " set " : ", ") + key + " = " + printValue(entity.get(key), false);
      return sql;
   }

   private Collection<Map> getRows(String item, ResultSet rs) throws SQLException {
      Collection<Map> rows = new ArrayList<>();
      ResultSetMetaData meta = rs.getMetaData();
      int nc = meta.getColumnCount();
      while (rs.next()) {
         Map row = new PersistableMap(getUpdateFunction(item));
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

   private String getClause(Expression exp) {
      Operator op = getOperator(exp.getAction());
      Object[] tokens = exp.getArguments();
      if (tokens == null || tokens.length == 0)
         tokens = new Reference[2];
      else if (tokens.length < 2)
         tokens = new Object[]{tokens[0], null};
      String sql = "";
      for (Object token : tokens) {
         while (token instanceof Reference) {
            if (token instanceof Expression || token instanceof Property)
               break;
            token = ((Reference)token).getValue();
         }
         if (!sql.isEmpty())
            sql += printOperator(op, token);
         sql += printValue(token, false);
      }
      return sql;
   }

   private Predicate<Map> getUpdateFunction(String item) {
      return map -> this.update(item, map) != null;
   }

   private Expression checkListOperation(String item, Expression filter) {
      switch (getOperator(filter.getAction())) {
         case JOIN:
         case TO:
         case FROM:
            break;
         default:
            return filter;
      }
      String[] keys = primaryKeys.get(item);
      if (keys == null || keys.length != 1)
         return filter;
      Property prop = new GenericProperty(null, keys[0], null);
      Object value = filter.getValue();
      if (value == null)
         value = Collections.EMPTY_LIST;
      else if (value instanceof Collection || value.getClass().isArray())
         ;
      else
         value = Collections.singletonList(value);
      return new SimpleExpression(configuration, Operator.IN, prop, value);
   }

   private String printValue(Object value, boolean printOperator) {
      if (value instanceof Expression) {
         Expression exp = (Expression)value;
         if (exp.getAction() != null)
            return (printOperator ? " = " : "") + "(" + getClause(exp) + ")";
         value = exp.getArguments();
      }
      if (value instanceof Property)
         return ((Property)value).getId();
      while (value instanceof Reference)
         value = ((Reference)value).getValue();
      if (value == null)
         return printOperator ? " IS NULL " : " NULL ";
      if (value instanceof Collection)
         value = ((Collection)value).toArray();
      if (value.getClass().isArray()) {
         int length = Array.getLength(value);
         StringJoiner joiner = new StringJoiner(", ", printOperator ? " in (" : "(", ")");
         for (int i = 0; i < length; i++)
            joiner.add(printValue(Array.get(value, i), false));
         return joiner.toString();
      }
      String op = printOperator ? " = " : "";
      if (value instanceof Boolean)
         return op + (info.getVendor().isHasBoolean() ? value.toString() : ((Boolean)value ? "1" : "0"));
      if (value instanceof Number)
         return op + value;
      //TODO: check for complex, probability and time which need escaping
      if (value instanceof java.sql.Time || value instanceof java.sql.Date)
         return op + "'" + value + "'";
      if (value instanceof Date)
         return op + "'" + new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(value) + "'";
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

   private String getFieldDefinition(Object def) {
      if (def instanceof Class)
         return getSqlType((Class)def);
      if (def instanceof CharSequence)
         return def.toString();
      if (def == null)
         return "";
      if (def.getClass().isArray())
         def = new ArrayWrapper(def);
      if (def instanceof Collection) {
         StringJoiner joiner = new StringJoiner(" ");
         for (Object e : (Collection)def)
            joiner.add(getFieldDefinition(e));
         return joiner.toString();
      }
      throw new IllegalArgumentException("Invalid definition element type: "+def.getClass().getName());
   }

   private String getSqlType(Class type) {
      if (CharSequence.class.isAssignableFrom(type))
         return "text";
      if (Character.class.isAssignableFrom(type))
         return "char(1)";
      if (Date.class.isAssignableFrom(type)) {
         if (java.sql.Date.class.isAssignableFrom(type))
            return "date";
         if (java.sql.Time.class.isAssignableFrom(type))
            return "time";
         return "timestamp";
      }
      if (Boolean.class.isAssignableFrom(type))
         return info.getVendor().isHasBoolean() ? "boolean" : "tinyint";
      if (Number.class.isAssignableFrom(type)) {
         if (Byte.class.isAssignableFrom(type))
            return "tinyint";
         if (Short.class.isAssignableFrom(type))
            return "smallint";
         if (Integer.class.isAssignableFrom(type))
            return "int";
         if (Long.class.isAssignableFrom(type))
            return "bigint";
         if (Float.class.isAssignableFrom(type))
            return "real";
         if (Double.class.isAssignableFrom(type))
            return "float";
         if (BigInteger.class.isAssignableFrom(type))
            return "bigint";
         //TODO: check for primitives
         return "numeric";
      }
      if (byte[].class.isAssignableFrom(type))
         return "binary";
      if (Blob.class.isAssignableFrom(type))
         return "blob";
      if (Clob.class.isAssignableFrom(type))
         return "clob";
      throw new IllegalArgumentException("Unknown data type: "+type.getName());
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
            return Object.class;
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

   private Operator getOperator(Action action) {
      StandardAction std = action.getStandardAction();
      if (std instanceof Operator)
         return (Operator)std;
      throw new RuntimeException("Action "+std+" is not an operator");
   }

   private String printOperator(Operator op, Object value) {
      switch (op) {
         case ADD: return " + ";
         case SUBTRACT: return " - ";
         case MULTIPLY: return " * ";
         case DIVIDE: return " / ";
         case MODULO: return " % ";
         case LESS: return " < ";
         case LESSEQUAL: return " <= ";
         case MORE: return " > ";
         case MOREQUAL: return " >= ";
         case NOTEQUAL: return " <> ";
         case AND: return " AND ";
         case OR: return " OR ";
         case NOT: return " NOT ";
         case IN: return " IN ";
         case IS:
         case EQUAL:
            return (value == null) ? " IS " : " = ";
         case FROM:
         case TO:
         case JOIN:
            throw new RuntimeException("Operator "+op+" can only be at the top level for JDBC");
         case HAVE:
            throw new RuntimeException("Subqueries not supported yet for JDBC");
         case POWER:
         case ROOT:
         case LOGARITHM:
            throw new RuntimeException("Operator "+op+" not supported yet for JDBC");
         case DO:
         case CASE:
         case WHILE:
            throw new RuntimeException("Operator "+op+" is not supported for JDBC");
         default:
            throw new RuntimeException("Unknown operator "+op);
      }
   }
}
