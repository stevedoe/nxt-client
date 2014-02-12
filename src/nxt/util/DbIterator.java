package nxt.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public final class DbIterator<T>
  implements Iterator<T>, AutoCloseable
{
  private final Connection con;
  private final PreparedStatement pstmt;
  private final ResultSetReader<T> rsReader;
  private final ResultSet rs;
  private boolean hasNext;
  
  public DbIterator(Connection paramConnection, PreparedStatement paramPreparedStatement, ResultSetReader<T> paramResultSetReader)
  {
    this.con = paramConnection;
    this.pstmt = paramPreparedStatement;
    this.rsReader = paramResultSetReader;
    try
    {
      this.rs = paramPreparedStatement.executeQuery();
      this.hasNext = this.rs.next();
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { paramPreparedStatement, paramConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  public boolean hasNext()
  {
    if (!this.hasNext) {
      DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
    }
    return this.hasNext;
  }
  
  public T next()
  {
    if (!this.hasNext)
    {
      DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
      return null;
    }
    try
    {
      Object localObject = this.rsReader.get(this.con, this.rs);
      this.hasNext = this.rs.next();
      return localObject;
    }
    catch (Exception localException)
    {
      DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
      throw new RuntimeException(localException.toString(), localException);
    }
  }
  
  public void remove()
  {
    throw new UnsupportedOperationException("Removal not suported");
  }
  
  public void close()
  {
    DbUtils.close(new AutoCloseable[] { this.rs, this.pstmt, this.con });
  }
  
  public static abstract interface ResultSetReader<T>
  {
    public abstract T get(Connection paramConnection, ResultSet paramResultSet)
      throws Exception;
  }
}