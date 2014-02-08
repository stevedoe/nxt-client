package nxt.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
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
  private static PrintWriter fileLog = null;
  
  static
  {
    try
    {
      fileLog = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream("nxt.log"))), true);
    }
    catch (IOException localIOException)
    {
      System.out.println("Logging to file nxt.log not possible, will log to stdout only");
    }
  }
  
  public static void logMessage(String paramString)
  {
    String str = ((SimpleDateFormat)logDateFormat.get()).format(new Date()) + paramString;
    System.out.println(str);
    if (fileLog != null) {
      fileLog.println(str);
    }
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