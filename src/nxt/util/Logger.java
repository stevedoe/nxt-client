package nxt.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import nxt.Nxt;

public final class Logger
{
  private static final boolean debug;
  private static final boolean enableStackTraces;
  
  public static enum Event
  {
    MESSAGE,  EXCEPTION;
    
    private Event() {}
  }
  
  private static final ThreadLocal<SimpleDateFormat> logDateFormat = new ThreadLocal()
  {
    protected SimpleDateFormat initialValue()
    {
      return new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ");
    }
  };
  private static final Listeners<String, Event> messageListeners = new Listeners();
  private static final Listeners<Exception, Event> exceptionListeners = new Listeners();
  private static final PrintWriter fileLog;
  
  static
  {
    debug = Nxt.getBooleanProperty("nxt.debug").booleanValue();
    enableStackTraces = Nxt.getBooleanProperty("nxt.enableStackTraces").booleanValue();
    PrintWriter localPrintWriter = null;
    try
    {
      localPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Nxt.getStringProperty("nxt.log")))), true);
    }
    catch (IOException|RuntimeException localIOException)
    {
      logMessage("Logging to file not possible, will log to stdout only", localIOException);
    }
    fileLog = localPrintWriter;
    logMessage("Debug logging " + (debug ? "enabled" : "disabled"));
    logMessage("Exception stack traces " + (enableStackTraces ? "enabled" : "disabled"));
  }
  
  public static boolean addMessageListener(Listener<String> paramListener, Event paramEvent)
  {
    return messageListeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean addExceptionListener(Listener<Exception> paramListener, Event paramEvent)
  {
    return exceptionListeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean removeMessageListener(Listener<String> paramListener, Event paramEvent)
  {
    return messageListeners.removeListener(paramListener, paramEvent);
  }
  
  public static boolean removeExceptionListener(Listener<Exception> paramListener, Event paramEvent)
  {
    return exceptionListeners.removeListener(paramListener, paramEvent);
  }
  
  public static void logMessage(String paramString)
  {
    String str = ((SimpleDateFormat)logDateFormat.get()).format(new Date()) + paramString;
    System.out.println(str);
    if (fileLog != null) {
      fileLog.println(str);
    }
    messageListeners.notify(paramString, Event.MESSAGE);
  }
  
  public static void logMessage(String paramString, Exception paramException)
  {
    if (enableStackTraces)
    {
      logMessage(paramString);
      paramException.printStackTrace(System.out);
      if (fileLog != null) {
        paramException.printStackTrace(fileLog);
      }
    }
    else
    {
      logMessage(paramString + ":\n" + paramException.toString());
    }
    exceptionListeners.notify(paramException, Event.EXCEPTION);
  }
  
  public static void logDebugMessage(String paramString)
  {
    if (debug) {
      logMessage("DEBUG: " + paramString);
    }
  }
  
  public static void logDebugMessage(String paramString, Exception paramException)
  {
    if (enableStackTraces)
    {
      logMessage("DEBUG: " + paramString);
      paramException.printStackTrace(System.out);
      if (fileLog != null) {
        paramException.printStackTrace(fileLog);
      }
    }
    else if (debug)
    {
      logMessage("DEBUG: " + paramString + ":\n" + paramException.toString());
    }
    exceptionListeners.notify(paramException, Event.EXCEPTION);
  }
}