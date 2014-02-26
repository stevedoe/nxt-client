package nxt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Properties;
import nxt.http.API;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Logger;
import nxt.util.ThreadPool;

public final class Nxt
{
  public static final String VERSION = "0.8.3";
  public static final int BLOCK_HEADER_LENGTH = 224;
  public static final int MAX_NUMBER_OF_TRANSACTIONS = 255;
  public static final int MAX_PAYLOAD_LENGTH = 32640;
  public static final long MAX_BALANCE = 1000000000L;
  public static final long INITIAL_BASE_TARGET = 153722867L;
  public static final long MAX_BASE_TARGET = 153722867000000000L;
  public static final int MAX_ALIAS_URI_LENGTH = 1000;
  public static final int MAX_ALIAS_LENGTH = 100;
  public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
  public static final long MAX_ASSET_QUANTITY = 1000000000L;
  public static final int ASSET_ISSUANCE_FEE = 1000;
  public static final int MAX_POLL_NAME_LENGTH = 100;
  public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
  public static final int MAX_POLL_OPTION_LENGTH = 100;
  public static final int ALIAS_SYSTEM_BLOCK = 22000;
  public static final int TRANSPARENT_FORGING_BLOCK = 30000;
  public static final int ARBITRARY_MESSAGES_BLOCK = 40000;
  public static final int TRANSPARENT_FORGING_BLOCK_2 = 47000;
  public static final int TRANSPARENT_FORGING_BLOCK_3 = 51000;
  public static final int TRANSPARENT_FORGING_BLOCK_4 = 64000;
  public static final int TRANSPARENT_FORGING_BLOCK_5 = 67000;
  public static final int ASSET_EXCHANGE_BLOCK = 111111;
  public static final int VOTING_SYSTEM_BLOCK = 222222;
  public static final long EPOCH_BEGINNING;
  public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
  private static final Properties defaultProperties;
  private static final Properties properties;
  
  static
  {
    Object localObject1 = Calendar.getInstance();
    ((Calendar)localObject1).set(15, 0);
    ((Calendar)localObject1).set(1, 2013);
    ((Calendar)localObject1).set(2, 10);
    ((Calendar)localObject1).set(5, 24);
    ((Calendar)localObject1).set(11, 12);
    ((Calendar)localObject1).set(12, 0);
    ((Calendar)localObject1).set(13, 0);
    ((Calendar)localObject1).set(14, 0);
    EPOCH_BEGINNING = ((Calendar)localObject1).getTimeInMillis();
    



    defaultProperties = new Properties();
    Object localObject2;
    try
    {
      localObject1 = ClassLoader.getSystemResourceAsStream("nxt-default.properties");localObject2 = null;
      try
      {
        if (localObject1 != null)
        {
          defaultProperties.load((InputStream)localObject1);
        }
        else
        {
          String str = System.getProperty("nxt-default.properties");
          if (str != null) {
            try
            {
              FileInputStream localFileInputStream = new FileInputStream(str);Object localObject3 = null;
              try
              {
                defaultProperties.load(localFileInputStream);
              }
              catch (Throwable localThrowable6)
              {
                localObject3 = localThrowable6;throw localThrowable6;
              }
              finally
              {
                if (localFileInputStream != null) {
                  if (localObject3 != null) {
                    try
                    {
                      localFileInputStream.close();
                    }
                    catch (Throwable localThrowable7)
                    {
                      localObject3.addSuppressed(localThrowable7);
                    }
                  } else {
                    localFileInputStream.close();
                  }
                }
              }
            }
            catch (IOException localIOException3)
            {
              throw new RuntimeException("Error loading nxt-default.properties from " + str);
            }
          } else {
            throw new RuntimeException("nxt-default.properties not in classpath and system property nxt-default.properties not defined either");
          }
        }
      }
      catch (Throwable localThrowable2)
      {
        localObject2 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localObject1 != null) {
          if (localObject2 != null) {
            try
            {
              ((InputStream)localObject1).close();
            }
            catch (Throwable localThrowable8)
            {
              localObject2.addSuppressed(localThrowable8);
            }
          } else {
            ((InputStream)localObject1).close();
          }
        }
      }
    }
    catch (IOException localIOException1)
    {
      throw new RuntimeException("Error loading nxt-default.properties", localIOException1);
    }
    properties = new Properties(defaultProperties);
    try
    {
      InputStream localInputStream = ClassLoader.getSystemResourceAsStream("nxt.properties");localObject2 = null;
      try
      {
        if (localInputStream != null) {
          properties.load(localInputStream);
        }
      }
      catch (Throwable localThrowable4)
      {
        localObject2 = localThrowable4;throw localThrowable4;
      }
      finally
      {
        if (localInputStream != null) {
          if (localObject2 != null) {
            try
            {
              localInputStream.close();
            }
            catch (Throwable localThrowable9)
            {
              localObject2.addSuppressed(localThrowable9);
            }
          } else {
            localInputStream.close();
          }
        }
      }
    }
    catch (IOException localIOException2)
    {
      throw new RuntimeException("Error loading nxt.properties", localIOException2);
    }
  }
  
  public static int getIntProperty(String paramString)
  {
    try
    {
      int i = Integer.parseInt(properties.getProperty(paramString));
      Logger.logMessage(paramString + " = \"" + i + "\"");
      return i;
    }
    catch (NumberFormatException localNumberFormatException)
    {
      Logger.logMessage(paramString + " not defined, assuming 0");
    }
    return 0;
  }
  
  public static String getStringProperty(String paramString)
  {
    String str = properties.getProperty(paramString);
    if ((str != null) && (!"".equals(str = str.trim())))
    {
      Logger.logMessage(paramString + " = \"" + str + "\"");
      return str;
    }
    Logger.logMessage(paramString + " not defined, assuming null");
    return null;
  }
  
  public static Boolean getBooleanProperty(String paramString)
  {
    String str = properties.getProperty(paramString);
    if (Boolean.TRUE.toString().equals(str))
    {
      Logger.logMessage(paramString + " = \"true\"");
      return Boolean.valueOf(true);
    }
    if (Boolean.FALSE.toString().equals(str))
    {
      Logger.logMessage(paramString + " = \"false\"");
      return Boolean.valueOf(false);
    }
    Logger.logMessage(paramString + " not defined, assuming false");
    return Boolean.valueOf(false);
  }
  
  public static Blockchain getBlockchain()
  {
    return BlockchainImpl.getInstance();
  }
  
  public static BlockchainProcessor getBlockchainProcessor()
  {
    return BlockchainProcessorImpl.getInstance();
  }
  
  public static TransactionProcessor getTransactionProcessor()
  {
    return TransactionProcessorImpl.getInstance();
  }
  
  public static void main(String[] paramArrayOfString)
  {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
    {
      public void run() {}
    }));
    init();
  }
  
  public static void init(Properties paramProperties)
  {
    properties.putAll(paramProperties);
    init();
  }
  
  public static void shutdown()
  {
    Peers.shutdown();
    ThreadPool.shutdown();
    Db.shutdown();
    Logger.logMessage("Nxt server 0.8.3 stopped.");
  }
  
  public static void init() {}
  
  private static class Init
  {
    private static void init() {}
    
    static
    {
      System.out.println("Initializing Nxt server version 0.8.3");
      
      long l1 = System.currentTimeMillis();
      
      Logger.logMessage("logging enabled");
      if (!Nxt.getBooleanProperty("nxt.debugJetty").booleanValue())
      {
        System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
        Logger.logDebugMessage("jetty logging disabled");
      }
      Db.init();
      BlockchainProcessorImpl.getInstance();
      TransactionProcessorImpl.getInstance();
      Peers.init();
      Generator.init();
      API.init();
      Users.init();
      ThreadPool.start();
      
      long l2 = System.currentTimeMillis();
      Logger.logDebugMessage("Initialization took " + (l2 - l1) / 1000L + " seconds");
      Logger.logMessage("Nxt server 0.8.3 started successfully.");
    }
  }
}