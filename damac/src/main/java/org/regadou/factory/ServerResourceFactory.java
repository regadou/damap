package org.regadou.factory;

import org.regadou.repository.JdbcVendor;
import org.regadou.repository.JdbcRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import org.regadou.damai.Configuration;
import org.regadou.damai.Repository;
import org.regadou.damai.Resource;
import org.regadou.damai.ResourceFactory;
import org.regadou.damai.ResourceManager;
import org.regadou.repository.JdbcConnectionInfo;
import org.regadou.resource.GenericResource;
import org.regadou.system.TcpServer;

public class ServerResourceFactory implements ResourceFactory {

   private final Map<String,JdbcVendor> vendorMap = new TreeMap<>();
   private Configuration configuration;
   private ResourceManager resourceManager;
   private List<String> schemes = Arrays.asList("jdbc", "tcp");

   @Inject
   public ServerResourceFactory(ResourceManager resourceManager, Configuration configuration) {
      this.resourceManager = resourceManager;
      this.configuration = configuration;
      for (JdbcVendor vendor : new JdbcVendor[]{
         new JdbcVendor("derby",       true,  "org.apache.derby.jdbc.EmbeddedDriver",                  "",   "",   "ALTER COLUMN",  false),
         new JdbcVendor("hsqldb",      false, "org.hsqldb.jdbcDriver",                                 "",   "",   "ALTER COLUMN",  true),
         new JdbcVendor("mysql",       true,  "com.mysql.jdbc.Driver",                                 "`",  "`",  "MODIFY COLUMN", true),
         new JdbcVendor("oracle",      false, "oracle.jdbc.driver.OracleDriver",                       "",   "",   "MODIFY COLUMN", false),
         new JdbcVendor("postgresql",  true,  "org.postgresql.Driver",                                 "\"", "\"", "ALTER COLUMN",  true),
         new JdbcVendor("access",      true,  "org.regadou.jmdb.MDBDriver",                            "[",  "]",  "ALTER COLUMN",  true),
         new JdbcVendor("sqlserver",   true,  "com.microsoft.sqlserver.jdbc.SQLServerDriver",          "\"", "\"", "ALTER COLUMN",  true),
         new JdbcVendor("sqlite",      true,  "org.sqlite.JDBC",                                       "\"", "\"", null,            true),
         new JdbcVendor("cassandra",   true,  "com.github.adejanovski.cassandra.jdbc.CassandraDriver", "",    "",  null,            true),
         new JdbcVendor("c*",          true,  "com.github.cassandra.jdbc.CassandraDriver",             "",    "",  null,            true),
      }) {
         vendorMap.put(vendor.getName(), vendor);
      }
   }

   @Override
   public Resource getResource(String id) {
      switch(id.substring(0, id.indexOf(':'))) {
         case "jdbc":
            JdbcConnectionInfo info = new JdbcConnectionInfo(id, vendorMap);
            Repository repo = new JdbcRepository(info, configuration);
            return new GenericResource(info.getUrl(), repo, null, true, configuration);
         case "tcp":
            TcpServer server = new TcpServer(id, configuration.getEngineManager(), configuration.getContextFactory());
            return new GenericResource(id, server, null, true, configuration);
      }
      return null;
   }

   @Override
   public String[] getSchemes() {
      return schemes.toArray(new String[schemes.size()]);
   }

   @Override
   public ResourceManager getResourceManager() {
      return resourceManager;
   }

   public void registerVendor(JdbcVendor vendor) {
      vendorMap.put(vendor.getName(), vendor);
   }
}
