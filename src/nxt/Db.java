package nxt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
    return localConnection;
  }
}