package nxt.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger
{
  private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal()
  {
    protected SimpleDateFormat initialValue()
    {
      return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
    }
  };
  public static final boolean debug = System.getProperty("nxt.debug") != null;
  public static final boolean enableStackTraces = System.getProperty("nxt.enableStackTraces") != null;
  
  public static void logMessage(String paramString)
  {
    System.out.println(((SimpleDateFormat)logDateFormat.get()).format(new Date()) + paramString);
  }
  
  public static void logMessage(String paramString, Exception paramException)
  {
    if (enableStackTraces)
    {
      logMessage(paramString);
      paramException.printStackTrace();
    }
    else
    {
      logMessage(paramString + ":\n" + paramException.toString());
    }
  }
  
  public static void logDebugMessage(String paramString)
  {
    if (debug) {
      logMessage("DEBUG: " + paramString);
    }
  }
  
  public static void logDebugMessage(String paramString, Exception paramException)
  {
    if (debug) {
      if (enableStackTraces)
      {
        logMessage("DEBUG: " + paramString);
        paramException.printStackTrace();
      }
      else
      {
        logMessage("DEBUG: " + paramString + ":\n" + paramException.toString());
      }
    }
  }
}