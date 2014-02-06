package nxt;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.http.HttpRequestHandler;
import nxt.peer.HttpJSONRequestHandler;
import nxt.peer.Peer;
import nxt.user.User;
import nxt.user.UserRequestHandler;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public final class Nxt
  extends HttpServlet
{
  public static final String VERSION = "0.7.0e";
  public static final int BLOCK_HEADER_LENGTH = 224;
  public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
  public static final int MAX_PAYLOAD_LENGTH = 32640;
  public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
  public static final int ALIAS_SYSTEM_BLOCK = 22000;
  public static final int TRANSPARENT_FORGING_BLOCK = 30000;
  public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
  public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
  public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
  public static final long MAX_BALANCE = 1000000000L;
  public static final long initialBaseTarget = 153722867L;
  public static final long maxBaseTarget = 153722867000000000L;
  public static final long MAX_ASSET_QUANTITY = 1000000000L;
  public static final int ASSET_ISSUANCE_FEE = 1000;
  public static final int MAX_ALIAS_URI_LENGTH = 1000;
  public static final int MAX_ALIAS_LENGTH = 100;
  public static final long epochBeginning;
  public static String myPlatform;
  public static String myScheme;
  public static String myAddress;
  public static String myHallmark;
  public static int myPort;
  public static boolean shareMyAddress;
  public static Set<String> allowedUserHosts;
  public static Set<String> allowedBotHosts;
  public static int blacklistingPeriod;
  public static final int LOGGING_MASK_EXCEPTIONS = 1;
  public static final int LOGGING_MASK_NON200_RESPONSES = 2;
  public static final int LOGGING_MASK_200_RESPONSES = 4;
  public static int communicationLoggingMask;
  public static Set<String> wellKnownPeers;
  public static int maxNumberOfConnectedPublicPeers;
  public static int connectTimeout;
  public static int readTimeout;
  public static boolean enableHallmarkProtection;
  public static int pushThreshold;
  public static int pullThreshold;
  public static int sendToPeersLimit;
  
  static
  {
    Calendar localCalendar = Calendar.getInstance();
    localCalendar.set(15, 0);
    localCalendar.set(1, 2013);
    localCalendar.set(2, 10);
    localCalendar.set(5, 24);
    localCalendar.set(11, 12);
    localCalendar.set(12, 0);
    localCalendar.set(13, 0);
    localCalendar.set(14, 0);
    epochBeginning = localCalendar.getTimeInMillis();
  }
  
  public void init(ServletConfig paramServletConfig)
    throws ServletException
  {
    Logger.logMessage("NRS 0.7.0e starting...");
    if (Logger.debug) {
      Logger.logMessage("DEBUG logging enabled");
    }
    if (Logger.enableStackTraces) {
      Logger.logMessage("logging of exception stack traces enabled");
    }
    try
    {
      myPlatform = paramServletConfig.getInitParameter("myPlatform");
      Logger.logMessage("\"myPlatform\" = \"" + myPlatform + "\"");
      if (myPlatform == null) {
        myPlatform = "PC";
      } else {
        myPlatform = myPlatform.trim();
      }
      myScheme = paramServletConfig.getInitParameter("myScheme");
      Logger.logMessage("\"myScheme\" = \"" + myScheme + "\"");
      if (myScheme == null) {
        myScheme = "http";
      } else {
        myScheme = myScheme.trim();
      }
      String str1 = paramServletConfig.getInitParameter("myPort");
      Logger.logMessage("\"myPort\" = \"" + str1 + "\"");
      try
      {
        myPort = Integer.parseInt(str1);
      }
      catch (NumberFormatException localNumberFormatException1)
      {
        myPort = myScheme.equals("https") ? 7875 : 7874;
        Logger.logMessage("Invalid value for myPort " + str1 + ", using default " + myPort);
      }
      myAddress = paramServletConfig.getInitParameter("myAddress");
      Logger.logMessage("\"myAddress\" = \"" + myAddress + "\"");
      if (myAddress != null) {
        myAddress = myAddress.trim();
      }
      String str2 = paramServletConfig.getInitParameter("shareMyAddress");
      Logger.logMessage("\"shareMyAddress\" = \"" + str2 + "\"");
      shareMyAddress = Boolean.parseBoolean(str2);
      
      myHallmark = paramServletConfig.getInitParameter("myHallmark");
      Logger.logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
      if (myHallmark != null)
      {
        myHallmark = myHallmark.trim();
        try
        {
          Convert.convert(myHallmark);
        }
        catch (NumberFormatException localNumberFormatException2)
        {
          Logger.logMessage("Your hallmark is invalid: " + myHallmark);
          System.exit(1);
        }
      }
      String str3 = paramServletConfig.getInitParameter("wellKnownPeers");
      Logger.logMessage("\"wellKnownPeers\" = \"" + str3 + "\"");
      if (str3 != null)
      {
        localObject1 = new HashSet();
        for (str7 : str3.split(";"))
        {
          str7 = str7.trim();
          if (str7.length() > 0)
          {
            ((Set)localObject1).add(str7);
            Peer.addPeer(str7, str7);
          }
        }
        wellKnownPeers = Collections.unmodifiableSet((Set)localObject1);
      }
      else
      {
        wellKnownPeers = Collections.emptySet();
        Logger.logMessage("No wellKnownPeers defined, it is unlikely to work");
      }
      Object localObject1 = paramServletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
      Logger.logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + (String)localObject1 + "\"");
      try
      {
        maxNumberOfConnectedPublicPeers = Integer.parseInt((String)localObject1);
      }
      catch (NumberFormatException localNumberFormatException3)
      {
        maxNumberOfConnectedPublicPeers = 10;
        Logger.logMessage("Invalid value for maxNumberOfConnectedPublicPeers " + (String)localObject1 + ", using default " + maxNumberOfConnectedPublicPeers);
      }
      String str4 = paramServletConfig.getInitParameter("connectTimeout");
      Logger.logMessage("\"connectTimeout\" = \"" + str4 + "\"");
      try
      {
        connectTimeout = Integer.parseInt(str4);
      }
      catch (NumberFormatException localNumberFormatException4)
      {
        connectTimeout = 1000;
        Logger.logMessage("Invalid value for connectTimeout " + str4 + ", using default " + connectTimeout);
      }
      String str5 = paramServletConfig.getInitParameter("readTimeout");
      Logger.logMessage("\"readTimeout\" = \"" + str5 + "\"");
      try
      {
        readTimeout = Integer.parseInt(str5);
      }
      catch (NumberFormatException localNumberFormatException5)
      {
        readTimeout = 1000;
        Logger.logMessage("Invalid value for readTimeout " + str5 + ", using default " + readTimeout);
      }
      String str6 = paramServletConfig.getInitParameter("enableHallmarkProtection");
      Logger.logMessage("\"enableHallmarkProtection\" = \"" + str6 + "\"");
      enableHallmarkProtection = Boolean.parseBoolean(str6);
      
      String str7 = paramServletConfig.getInitParameter("pushThreshold");
      Logger.logMessage("\"pushThreshold\" = \"" + str7 + "\"");
      try
      {
        pushThreshold = Integer.parseInt(str7);
      }
      catch (NumberFormatException localNumberFormatException6)
      {
        pushThreshold = 0;
        Logger.logMessage("Invalid value for pushThreshold " + str7 + ", using default " + pushThreshold);
      }
      String str8 = paramServletConfig.getInitParameter("pullThreshold");
      Logger.logMessage("\"pullThreshold\" = \"" + str8 + "\"");
      try
      {
        pullThreshold = Integer.parseInt(str8);
      }
      catch (NumberFormatException localNumberFormatException7)
      {
        pullThreshold = 0;
        Logger.logMessage("Invalid value for pullThreshold " + str8 + ", using default " + pullThreshold);
      }
      String str9 = paramServletConfig.getInitParameter("allowedUserHosts");
      Logger.logMessage("\"allowedUserHosts\" = \"" + str9 + "\"");
      if (str9 != null) {
        if (!str9.trim().equals("*"))
        {
          localObject2 = new HashSet();
          for (String str12 : str9.split(";"))
          {
            str12 = str12.trim();
            if (str12.length() > 0) {
              ((Set)localObject2).add(str12);
            }
          }
          allowedUserHosts = Collections.unmodifiableSet((Set)localObject2);
        }
      }
      Object localObject2 = paramServletConfig.getInitParameter("allowedBotHosts");
      Logger.logMessage("\"allowedBotHosts\" = \"" + (String)localObject2 + "\"");
      if (localObject2 != null) {
        if (!((String)localObject2).trim().equals("*"))
        {
          ??? = new HashSet();
          for (String str13 : ((String)localObject2).split(";"))
          {
            str13 = str13.trim();
            if (str13.length() > 0) {
              ((Set)???).add(str13);
            }
          }
          allowedBotHosts = Collections.unmodifiableSet((Set)???);
        }
      }
      ??? = paramServletConfig.getInitParameter("blacklistingPeriod");
      Logger.logMessage("\"blacklistingPeriod\" = \"" + (String)??? + "\"");
      try
      {
        blacklistingPeriod = Integer.parseInt((String)???);
      }
      catch (NumberFormatException localNumberFormatException8)
      {
        blacklistingPeriod = 300000;
        Logger.logMessage("Invalid value for blacklistingPeriod " + (String)??? + ", using default " + blacklistingPeriod);
      }
      String str10 = paramServletConfig.getInitParameter("communicationLoggingMask");
      Logger.logMessage("\"communicationLoggingMask\" = \"" + str10 + "\"");
      try
      {
        communicationLoggingMask = Integer.parseInt(str10);
      }
      catch (NumberFormatException localNumberFormatException9)
      {
        Logger.logMessage("Invalid value for communicationLogginMask " + str10 + ", using default 0");
      }
      String str11 = paramServletConfig.getInitParameter("sendToPeersLimit");
      Logger.logMessage("\"sendToPeersLimit\" = \"" + str11 + "\"");
      try
      {
        sendToPeersLimit = Integer.parseInt(str11);
      }
      catch (NumberFormatException localNumberFormatException10)
      {
        sendToPeersLimit = 10;
        Logger.logMessage("Invalid value for sendToPeersLimit " + str11 + ", using default " + sendToPeersLimit);
      }
      Db.init();
      
      Blockchain.init();
      
      ThreadPools.start();
      
      Logger.logMessage("NRS 0.7.0e started successfully.");
    }
    catch (Exception localException)
    {
      Logger.logMessage("Error initializing Nxt servlet", localException);
      System.exit(1);
    }
  }
  
  public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    paramHttpServletResponse.setHeader("Pragma", "no-cache");
    paramHttpServletResponse.setDateHeader("Expires", 0L);
    
    User localUser = null;
    try
    {
      String str = paramHttpServletRequest.getParameter("user");
      if (str == null)
      {
        HttpRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
        return;
      }
      if ((allowedUserHosts != null) && (!allowedUserHosts.contains(paramHttpServletRequest.getRemoteHost())))
      {
        JSONObject localJSONObject1 = new JSONObject();
        localJSONObject1.put("response", "denyAccess");
        JSONArray localJSONArray = new JSONArray();
        localJSONArray.add(localJSONObject1);
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("responses", localJSONArray);
        paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
        PrintWriter localPrintWriter = paramHttpServletResponse.getWriter();Object localObject1 = null;
        try
        {
          localJSONObject2.writeJSONString(localPrintWriter);
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localPrintWriter != null) {
            if (localObject1 != null) {
              try
              {
                localPrintWriter.close();
              }
              catch (Throwable localThrowable3)
              {
                localObject1.addSuppressed(localThrowable3);
              }
            } else {
              localPrintWriter.close();
            }
          }
        }
        return;
      }
      localUser = User.getUser(str);
      UserRequestHandler.process(paramHttpServletRequest, localUser);
    }
    catch (Exception localException)
    {
      if (localUser != null) {
        Logger.logMessage("Error processing GET request", localException);
      } else {
        Logger.logDebugMessage("Error processing GET request", localException);
      }
    }
    if (localUser != null) {
      localUser.processPendingResponses(paramHttpServletRequest, paramHttpServletResponse);
    }
  }
  
  public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    HttpJSONRequestHandler.process(paramHttpServletRequest, paramHttpServletResponse);
  }
  
  public void destroy()
  {
    ThreadPools.shutdown();
    
    Db.shutdown();
    
    Logger.logMessage("NRS 0.7.0e stopped.");
  }
}