package nxt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import nxt.util.Logger;

final class DbVersion
{
  static void init()
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        Statement localStatement = localConnection.createStatement();Object localObject2 = null;
        try
        {
          int i = 1;
          try
          {
            ResultSet localResultSet = localStatement.executeQuery("SELECT next_update FROM version");
            if (!localResultSet.next()) {
              throw new RuntimeException("Invalid version table");
            }
            i = localResultSet.getInt("next_update");
            if (!localResultSet.isLast()) {
              throw new RuntimeException("Invalid version table");
            }
            localResultSet.close();
            Logger.logMessage("Database is at level " + (i - 1));
          }
          catch (SQLException localSQLException2)
          {
            Logger.logMessage("Initializing an empty database");
            localStatement.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
            localStatement.executeUpdate("INSERT INTO version VALUES (1)");
          }
          i = update(i);
          localStatement.executeUpdate("UPDATE version SET next_update=" + i);
          Logger.logMessage("Updated database is at level " + (i - 1));
          localConnection.commit();
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
    catch (SQLException localSQLException1)
    {
      throw new RuntimeException(localSQLException1.toString(), localSQLException1);
    }
  }
  
  private static void apply(String paramString)
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        Statement localStatement = localConnection.createStatement();Object localObject2 = null;
        try
        {
          try
          {
            Logger.logDebugMessage("Will apply sql:\n" + paramString);
            localStatement.executeUpdate(paramString);
            localConnection.commit();
          }
          catch (SQLException localSQLException2)
          {
            localConnection.rollback();
            throw localSQLException2;
          }
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
    catch (SQLException localSQLException1)
    {
      throw new RuntimeException("Database error executing " + paramString, localSQLException1);
    }
  }
  
  private static int update(int paramInt)
  {
    switch (paramInt)
    {
    case 1: 
      apply("CREATE TABLE IF NOT EXISTS block (db_id INT IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, timestamp INT NOT NULL, previous_block_id BIGINT, FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount INT NOT NULL, total_fee INT NOT NULL, payload_length INT NOT NULL, generator_public_key BINARY(32) NOT NULL, previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, index INT NOT NULL, height INT NOT NULL, generation_signature BINARY(64) NOT NULL, block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_account_id BIGINT NOT NULL)");
    case 2: 
      apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
    case 3: 
      apply("CREATE TABLE IF NOT EXISTS transaction (db_id INT IDENTITY, id BIGINT NOT NULL, deadline SMALLINT NOT NULL, sender_public_key BINARY(32) NOT NULL, recipient_id BIGINT NOT NULL, amount INT NOT NULL, fee INT NOT NULL, referenced_transaction_id BIGINT, index INT NOT NULL, height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, sender_account_id BIGINT NOT NULL, attachment OTHER)");
    case 4: 
      apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
    case 5: 
      apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
    case 6: 
      apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_idx ON transaction (timestamp)");
    case 7: 
      apply("CREATE INDEX IF NOT EXISTS block_generator_account_id_idx ON block (generator_account_id)");
    case 8: 
      apply("CREATE INDEX IF NOT EXISTS transaction_sender_account_id_idx ON transaction (sender_account_id)");
    case 9: 
      apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
    case 10: 
      apply("ALTER TABLE block ALTER COLUMN generator_account_id RENAME TO generator_id");
    case 11: 
      apply("ALTER TABLE transaction ALTER COLUMN sender_account_id RENAME TO sender_id");
    case 12: 
      apply("ALTER INDEX block_generator_account_id_idx RENAME TO block_generator_id_idx");
    case 13: 
      apply("ALTER INDEX transaction_sender_account_id_idx RENAME TO transaction_sender_id_idx");
    case 14: 
      apply("ALTER TABLE block DROP COLUMN IF EXISTS index");
    case 15: 
      apply("ALTER TABLE transaction DROP COLUMN IF EXISTS index");
    case 16: 
      return 16;
    }
    throw new RuntimeException("Database inconsistent with code, probably trying to run older code on newer database");
  }
}