package nxt;

import java.sql.Connection;
import java.sql.SQLException;
import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

final class Db
{
  private static JdbcConnectionPool cp;
  
  static void init()
  {
    long l = Runtime.getRuntime().maxMemory() / 2048L;
    Logger.logDebugMessage("Database cache size set to " + l + " kB");
    cp = JdbcConnectionPool.create("jdbc:h2:nxt_db/nxt;DB_CLOSE_DELAY=10;DB_CLOSE_ON_EXIT=FALSE;CACHE_SIZE=" + l, "sa", "sa");
    cp.setMaxConnections(200);
    cp.setLoginTimeout(70);
    DbVersion.init();
  }
  
  static void shutdown()
  {
    if (cp != null) {
      cp.dispose();
    }
  }
  
  static Connection getConnection()
    throws SQLException
  {
    Connection localConnection = cp.getConnection();
    localConnection.setAutoCommit(false);
    return localConnection;
  }
}