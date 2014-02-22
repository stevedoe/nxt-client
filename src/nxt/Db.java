package nxt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import nxt.util.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

final class Db
{
  private static volatile JdbcConnectionPool cp;
  private static volatile int maxActiveConnections;
  
  static void init()
  {
    long l = Nxt.getIntProperty("nxt.dbCacheKB");
    if (l == 0L) {
      l = Runtime.getRuntime().maxMemory() / 2048L;
    }
    String str = Nxt.getStringProperty("nxt.dbUrl");
    if (!str.contains("CACHE_SIZE=")) {
      str = str + ";CACHE_SIZE=" + l;
    }
    Logger.logDebugMessage("Database jdbc url set to: " + str);
    cp = JdbcConnectionPool.create(str, "sa", "sa");
    cp.setMaxConnections(Nxt.getIntProperty("nxt.maxDbConnections"));
    cp.setLoginTimeout(Nxt.getIntProperty("nxt.dbLoginTimeout"));
    DbVersion.init();
  }
  
  static void shutdown()
  {
    if (cp != null)
    {
      try
      {
        Connection localConnection = cp.getConnection();Object localObject1 = null;
        try
        {
          Statement localStatement = localConnection.createStatement();Object localObject2 = null;
          try
          {
            localStatement.execute("SHUTDOWN COMPACT");
            Logger.logDebugMessage("Database shutdown completed");
          }
          catch (Throwable localThrowable4)
          {
            localObject2 = localThrowable4;throw localThrowable4;
          }
          finally {}
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localConnection != null) {
            if (localObject1 != null) {
              try
              {
                localConnection.close();
              }
              catch (Throwable localThrowable6)
              {
                localObject1.addSuppressed(localThrowable6);
              }
            } else {
              localConnection.close();
            }
          }
        }
      }
      catch (SQLException localSQLException)
      {
        Logger.logDebugMessage(localSQLException.toString(), localSQLException);
      }
      cp = null;
    }
  }
  
  static Connection getConnection()
    throws SQLException
  {
    Connection localConnection = cp.getConnection();
    localConnection.setAutoCommit(false);
    int i = cp.getActiveConnections();
    if (i > maxActiveConnections)
    {
      maxActiveConnections = i;
      Logger.logDebugMessage("Database connection pool current size: " + i);
    }
    return localConnection;
  }
}