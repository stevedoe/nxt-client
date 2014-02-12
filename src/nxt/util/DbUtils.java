package nxt.util;

public final class DbUtils
{
  public static void close(AutoCloseable... paramVarArgs)
  {
    for (AutoCloseable localAutoCloseable : paramVarArgs) {
      if (localAutoCloseable != null) {
        try
        {
          localAutoCloseable.close();
        }
        catch (Exception localException) {}
      }
    }
  }
}