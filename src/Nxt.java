import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Nxt
  extends HttpServlet
{
  static final String VERSION = "0.4.7e";
  static final long GENESIS_BLOCK_ID = 2680262203532249785L;
  static final long CREATOR_ID = 1739068987193023818L;
  static final int BLOCK_HEADER_LENGTH = 224;
  static final int MAX_PAYLOAD_LENGTH = 32640;
  static final long initialBaseTarget = 153722867L;
  static final long maxBaseTarget = 153722867000000000L;
  static final BigInteger two64 = new BigInteger("18446744073709551616");
  static long epochBeginning;
  static final String alphabet = "0123456789abcdefghijklmnopqrstuvwxyz";
  static FileChannel blockchainChannel;
  static String myScheme;
  static String myAddress;
  static String myHallmark;
  static int myPort;
  static boolean shareMyAddress;
  static HashSet<String> allowedUserHosts;
  static HashSet<String> allowedBotHosts;
  static int blacklistingPeriod;
  static final int LOGGING_MASK_EXCEPTIONS = 1;
  static final int LOGGING_MASK_NON200_RESPONSES = 2;
  static final int LOGGING_MASK_200_RESPONSES = 4;
  static int communicationLoggingMask;
  static int transactionCounter;
  static HashMap<Long, Nxt.Transaction> transactions;
  static ConcurrentHashMap<Long, Nxt.Transaction> unconfirmedTransactions = new ConcurrentHashMap();
  static ConcurrentHashMap<Long, Nxt.Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
  static HashSet<String> wellKnownPeers = new HashSet();
  static int maxNumberOfConnectedPublicPeers;
  static int connectTimeout;
  static int readTimeout;
  static boolean enableHallmarkProtection;
  static int pushThreshold;
  static int pullThreshold;
  static int peerCounter;
  static HashMap<String, Nxt.Peer> peers = new HashMap();
  static int blockCounter;
  static HashMap<Long, Nxt.Block> blocks;
  static long lastBlock;
  static Nxt.Peer lastBlockchainFeeder;
  static HashMap<Long, Nxt.Account> accounts = new HashMap();
  static HashMap<String, Nxt.Alias> aliases = new HashMap();
  static HashMap<Long, Nxt.Alias> aliasIdToAliasMappings = new HashMap();
  static HashMap<Long, Nxt.Asset> assets = new HashMap();
  static HashMap<String, Long> assetNameToIdMappings = new HashMap();
  static HashMap<Long, Nxt.AskOrder> askOrders = new HashMap();
  static HashMap<Long, Nxt.BidOrder> bidOrders = new HashMap();
  static HashMap<Long, TreeSet<Nxt.AskOrder>> sortedAskOrders = new HashMap();
  static HashMap<Long, TreeSet<Nxt.BidOrder>> sortedBidOrders = new HashMap();
  static ConcurrentHashMap<String, Nxt.User> users = new ConcurrentHashMap();
  static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
  static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(7);
  static HashMap<Nxt.Account, Nxt.Block> lastBlocks = new HashMap();
  static HashMap<Nxt.Account, BigInteger> hits = new HashMap();
  
  static int getEpochTime(long paramLong)
  {
    return (int)((paramLong - epochBeginning + 500L) / 1000L);
  }
  
  static void logMessage(String paramString)
  {
    System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ").format(new Date()) + paramString);
  }
  
  static byte[] convert(String paramString)
  {
    byte[] arrayOfByte = new byte[paramString.length() / 2];
    for (int i = 0; i < arrayOfByte.length; i++) {
      arrayOfByte[i] = ((byte)Integer.parseInt(paramString.substring(i * 2, i * 2 + 2), 16));
    }
    return arrayOfByte;
  }
  
  static String convert(byte[] paramArrayOfByte)
  {
    StringBuilder localStringBuilder = new StringBuilder();
    for (int i = 0; i < paramArrayOfByte.length; i++)
    {
      int j;
      localStringBuilder.append("0123456789abcdefghijklmnopqrstuvwxyz".charAt((j = paramArrayOfByte[i] & 0xFF) >> 4)).append("0123456789abcdefghijklmnopqrstuvwxyz".charAt(j & 0xF));
    }
    return localStringBuilder.toString();
  }
  
  static String convert(long paramLong)
  {
    BigInteger localBigInteger = BigInteger.valueOf(paramLong);
    if (paramLong < 0L) {
      localBigInteger = localBigInteger.add(two64);
    }
    return localBigInteger.toString();
  }
  
  static void matchOrders(long paramLong)
    throws Exception
  {
    TreeSet localTreeSet1 = (TreeSet)sortedAskOrders.get(Long.valueOf(paramLong));
    TreeSet localTreeSet2 = (TreeSet)sortedBidOrders.get(Long.valueOf(paramLong));
    synchronized (askOrders)
    {
      synchronized (bidOrders)
      {
        do
        {
          Nxt.AskOrder localAskOrder = (Nxt.AskOrder)localTreeSet1.first();
          Nxt.BidOrder localBidOrder = (Nxt.BidOrder)localTreeSet2.first();
          if (localAskOrder.price > localBidOrder.price) {
            break;
          }
          int i = localAskOrder.quantity < localBidOrder.quantity ? localAskOrder.quantity : localBidOrder.quantity;
          long l = (localAskOrder.height < localBidOrder.height) || ((localAskOrder.height == localBidOrder.height) && (localAskOrder.id < localBidOrder.id)) ? localAskOrder.price : localBidOrder.price;
          if (localAskOrder.quantity -= i == 0)
          {
            askOrders.remove(Long.valueOf(localAskOrder.id));
            localTreeSet1.remove(localAskOrder);
          }
          synchronized (localAskOrder.account)
          {
            localAskOrder.account.setBalance(localAskOrder.account.balance + i * l);
            localAskOrder.account.setUnconfirmedBalance(localAskOrder.account.unconfirmedBalance + i * l);
          }
          if (localBidOrder.quantity -= i == 0)
          {
            bidOrders.remove(Long.valueOf(localBidOrder.id));
            localTreeSet2.remove(localBidOrder);
          }
          synchronized (localBidOrder.account)
          {
            Integer localInteger = (Integer)localBidOrder.account.assetBalances.get(Long.valueOf(paramLong));
            if (localInteger == null)
            {
              localBidOrder.account.assetBalances.put(Long.valueOf(paramLong), Integer.valueOf(i));
              localBidOrder.account.unconfirmedAssetBalances.put(Long.valueOf(paramLong), Integer.valueOf(i));
            }
            else
            {
              localBidOrder.account.assetBalances.put(Long.valueOf(paramLong), Integer.valueOf(localInteger.intValue() + i));
              localBidOrder.account.unconfirmedAssetBalances.put(Long.valueOf(paramLong), Integer.valueOf(((Integer)localBidOrder.account.unconfirmedAssetBalances.get(Long.valueOf(paramLong))).intValue() + i));
            }
          }
          if (localTreeSet1.isEmpty()) {
            break;
          }
        } while (!localTreeSet2.isEmpty());
      }
    }
  }
  
  public void init(ServletConfig paramServletConfig)
    throws ServletException
  {
    logMessage("Nxt 0.4.7e started.");
    try
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
      String str1 = paramServletConfig.getInitParameter("blockchainStoragePath");
      logMessage("\"blockchainStoragePath\" = \"" + str1 + "\"");
      blockchainChannel = FileChannel.open(Paths.get(str1, new String[0]), new OpenOption[] { StandardOpenOption.READ, StandardOpenOption.WRITE });
      myScheme = paramServletConfig.getInitParameter("myScheme");
      logMessage("\"myScheme\" = \"" + myScheme + "\"");
      if (myScheme == null) {
        myScheme = "http";
      } else {
        myScheme = myScheme.trim();
      }
      String str2 = paramServletConfig.getInitParameter("myPort");
      logMessage("\"myPort\" = \"" + str2 + "\"");
      try
      {
        myPort = Integer.parseInt(str2);
      }
      catch (Exception localException2)
      {
        myPort = myScheme.equals("https") ? 7875 : 7874;
      }
      myAddress = paramServletConfig.getInitParameter("myAddress");
      logMessage("\"myAddress\" = \"" + myAddress + "\"");
      if (myAddress != null) {
        myAddress = myAddress.trim();
      }
      String str3 = paramServletConfig.getInitParameter("shareMyAddress");
      logMessage("\"shareMyAddress\" = \"" + str3 + "\"");
      try
      {
        shareMyAddress = Boolean.parseBoolean(str3);
      }
      catch (Exception localException3)
      {
        shareMyAddress = true;
      }
      myHallmark = paramServletConfig.getInitParameter("myHallmark");
      logMessage("\"myHallmark\" = \"" + myHallmark + "\"");
      if (myHallmark != null) {
        myHallmark = myHallmark.trim();
      }
      String str4 = paramServletConfig.getInitParameter("wellKnownPeers");
      logMessage("\"wellKnownPeers\" = \"" + str4 + "\"");
      if (str4 != null) {
        for (str5 : str4.split(";"))
        {
          str5 = str5.trim();
          if (str5.length() > 0)
          {
            wellKnownPeers.add(str5);
            Nxt.Peer.addPeer(str5, str5);
          }
        }
      }
      String str5 = paramServletConfig.getInitParameter("maxNumberOfConnectedPublicPeers");
      logMessage("\"maxNumberOfConnectedPublicPeers\" = \"" + str5 + "\"");
      try
      {
        maxNumberOfConnectedPublicPeers = Integer.parseInt(str5);
      }
      catch (Exception localException4)
      {
        maxNumberOfConnectedPublicPeers = 10;
      }
      String str6 = paramServletConfig.getInitParameter("connectTimeout");
      logMessage("\"connectTimeout\" = \"" + str6 + "\"");
      try
      {
        connectTimeout = Integer.parseInt(str6);
      }
      catch (Exception localException5)
      {
        connectTimeout = 1000;
      }
      String str7 = paramServletConfig.getInitParameter("readTimeout");
      logMessage("\"readTimeout\" = \"" + str7 + "\"");
      try
      {
        readTimeout = Integer.parseInt(str7);
      }
      catch (Exception localException6)
      {
        readTimeout = 1000;
      }
      ??? = paramServletConfig.getInitParameter("enableHallmarkProtection");
      logMessage("\"enableHallmarkProtection\" = \"" + (String)??? + "\"");
      try
      {
        enableHallmarkProtection = Boolean.parseBoolean((String)???);
      }
      catch (Exception localException7)
      {
        enableHallmarkProtection = true;
      }
      String str8 = paramServletConfig.getInitParameter("pushThreshold");
      logMessage("\"pushThreshold\" = \"" + str8 + "\"");
      try
      {
        pushThreshold = Integer.parseInt(str8);
      }
      catch (Exception localException8)
      {
        pushThreshold = 0;
      }
      String str9 = paramServletConfig.getInitParameter("pullThreshold");
      logMessage("\"pullThreshold\" = \"" + str9 + "\"");
      try
      {
        pullThreshold = Integer.parseInt(str9);
      }
      catch (Exception localException9)
      {
        pullThreshold = 0;
      }
      String str10 = paramServletConfig.getInitParameter("allowedUserHosts");
      logMessage("\"allowedUserHosts\" = \"" + str10 + "\"");
      if ((str10 != null) && (!str10.trim().equals("*")))
      {
        allowedUserHosts = new HashSet();
        for (str11 : str10.split(";"))
        {
          str11 = str11.trim();
          if (str11.length() > 0) {
            allowedUserHosts.add(str11);
          }
        }
      }
      String str11 = paramServletConfig.getInitParameter("allowedBotHosts");
      logMessage("\"allowedBotHosts\" = \"" + str11 + "\"");
      if ((str11 != null) && (!str11.trim().equals("*")))
      {
        allowedBotHosts = new HashSet();
        for (str12 : str11.split(";"))
        {
          str12 = str12.trim();
          if (str12.length() > 0) {
            allowedBotHosts.add(str12);
          }
        }
      }
      String str12 = paramServletConfig.getInitParameter("blacklistingPeriod");
      logMessage("\"blacklistingPeriod\" = \"" + str12 + "\"");
      try
      {
        blacklistingPeriod = Integer.parseInt(str12);
      }
      catch (Exception localException10)
      {
        blacklistingPeriod = 300000;
      }
      String str13 = paramServletConfig.getInitParameter("communicationLoggingMask");
      logMessage("\"communicationLoggingMask\" = \"" + str13 + "\"");
      try
      {
        communicationLoggingMask = Integer.parseInt(str13);
      }
      catch (Exception localException11) {}
      Object localObject5;
      Object localObject4;
      try
      {
        logMessage("Loading transactions...");
        Nxt.Transaction.loadTransactions("transactions.nxt");
        logMessage("...Done");
      }
      catch (FileNotFoundException localFileNotFoundException1)
      {
        transactions = new HashMap();
        localObject2 = new long[] { new BigInteger("163918645372308887").longValue(), new BigInteger("620741658595224146").longValue(), new BigInteger("723492359641172834").longValue(), new BigInteger("818877006463198736").longValue(), new BigInteger("1264744488939798088").longValue(), new BigInteger("1600633904360147460").longValue(), new BigInteger("1796652256451468602").longValue(), new BigInteger("1814189588814307776").longValue(), new BigInteger("1965151371996418680").longValue(), new BigInteger("2175830371415049383").longValue(), new BigInteger("2401730748874927467").longValue(), new BigInteger("2584657662098653454").longValue(), new BigInteger("2694765945307858403").longValue(), new BigInteger("3143507805486077020").longValue(), new BigInteger("3684449848581573439").longValue(), new BigInteger("4071545868996394636").longValue(), new BigInteger("4277298711855908797").longValue(), new BigInteger("4454381633636789149").longValue(), new BigInteger("4747512364439223888").longValue(), new BigInteger("4777958973882919649").longValue(), new BigInteger("4803826772380379922").longValue(), new BigInteger("5095742298090230979").longValue(), new BigInteger("5271441507933314159").longValue(), new BigInteger("5430757907205901788").longValue(), new BigInteger("5491587494620055787").longValue(), new BigInteger("5622658764175897611").longValue(), new BigInteger("5982846390354787993").longValue(), new BigInteger("6290807999797358345").longValue(), new BigInteger("6785084810899231190").longValue(), new BigInteger("6878906112724074600").longValue(), new BigInteger("7017504655955743955").longValue(), new BigInteger("7085298282228890923").longValue(), new BigInteger("7446331133773682477").longValue(), new BigInteger("7542917420413518667").longValue(), new BigInteger("7549995577397145669").longValue(), new BigInteger("7577840883495855927").longValue(), new BigInteger("7579216551136708118").longValue(), new BigInteger("8278234497743900807").longValue(), new BigInteger("8517842408878875334").longValue(), new BigInteger("8870453786186409991").longValue(), new BigInteger("9037328626462718729").longValue(), new BigInteger("9161949457233564608").longValue(), new BigInteger("9230759115816986914").longValue(), new BigInteger("9306550122583806885").longValue(), new BigInteger("9433259657262176905").longValue(), new BigInteger("9988839211066715803").longValue(), new BigInteger("10105875265190846103").longValue(), new BigInteger("10339765764359265796").longValue(), new BigInteger("10738613957974090819").longValue(), new BigInteger("10890046632913063215").longValue(), new BigInteger("11494237785755831723").longValue(), new BigInteger("11541844302056663007").longValue(), new BigInteger("11706312660844961581").longValue(), new BigInteger("12101431510634235443").longValue(), new BigInteger("12186190861869148835").longValue(), new BigInteger("12558748907112364526").longValue(), new BigInteger("13138516747685979557").longValue(), new BigInteger("13330279748251018740").longValue(), new BigInteger("14274119416917666908").longValue(), new BigInteger("14557384677985343260").longValue(), new BigInteger("14748294830376619968").longValue(), new BigInteger("14839596582718854826").longValue(), new BigInteger("15190676494543480574").longValue(), new BigInteger("15253761794338766759").longValue(), new BigInteger("15558257163011348529").longValue(), new BigInteger("15874940801139996458").longValue(), new BigInteger("16516270647696160902").longValue(), new BigInteger("17156841960446798306").longValue(), new BigInteger("17228894143802851995").longValue(), new BigInteger("17240396975291969151").longValue(), new BigInteger("17491178046969559641").longValue(), new BigInteger("18345202375028346230").longValue(), new BigInteger("18388669820699395594").longValue() };
        ??? = new int[] { 36742, 1970092, 349130, 24880020, 2867856, 9975150, 2690963, 7648, 5486333, 34913026, 997515, 30922966, 6650, 44888, 2468850, 49875751, 49875751, 9476393, 49875751, 14887912, 528683, 583546, 7315, 19925363, 29856290, 5320, 4987575, 5985, 24912938, 49875751, 2724712, 1482474, 200999, 1476156, 498758, 987540, 16625250, 5264386, 15487585, 2684479, 14962725, 34913026, 5033128, 2916900, 49875751, 4962637, 170486123, 8644631, 22166945, 6668388, 233751, 4987575, 11083556, 1845403, 49876, 3491, 3491, 9476, 49876, 6151, 682633, 49875751, 482964, 4988, 49875751, 4988, 9144, 503745, 49875751, 52370, 29437998, 585375, 9975150 };
        byte[][] arrayOfByte = { { 41, 115, -41, 7, 37, 21, -3, -41, 120, 119, 63, -101, 108, 48, -117, 1, -43, 32, 85, 95, 65, 42, 92, -22, 123, -36, 6, -99, -61, -53, 93, 7, 23, 8, -30, 65, 57, -127, -2, 42, -92, -104, 11, 72, -66, 108, 17, 113, 99, -117, -75, 123, 110, 107, 119, -25, 67, 64, 32, 117, 111, 54, 82, -14 }, { 118, 43, 84, -91, -110, -102, 100, -40, -33, -47, -13, -7, -88, 2, -42, -66, -38, -22, 105, -42, -69, 78, 51, -55, -48, 49, -89, 116, -96, -104, -114, 14, 94, 58, -115, -8, 111, -44, 76, -104, 54, -15, 126, 31, 6, -80, 65, 6, 124, 37, -73, 92, 4, 122, 122, -108, 1, -54, 31, -38, -117, -1, -52, -56 }, { 79, 100, -101, 107, -6, -61, 40, 32, -98, 32, 80, -59, -76, -23, -62, 38, 4, 105, -106, -105, -121, -85, 13, -98, -77, 126, -125, 103, 12, -41, 1, 2, 45, -62, -69, 102, 116, -61, 101, -14, -68, -31, 9, 110, 18, 2, 33, -98, -37, -128, 17, -19, 124, 125, -63, 92, -70, 96, 96, 125, 91, 8, -65, -12 }, { 58, -99, 14, -97, -75, -10, 110, -102, 119, -3, -2, -12, -82, -33, -27, 118, -19, 55, -109, 6, 110, -10, 108, 30, 94, -113, -5, -98, 19, 12, -125, 14, -77, 33, -128, -21, 36, -120, -12, -81, 64, 95, 67, -3, 100, 122, -47, 127, -92, 114, 68, 72, 2, -40, 80, 117, -17, -56, 103, 37, -119, 3, 22, 23 }, { 76, 22, 121, -4, -77, -127, 18, -102, 7, 94, -73, -96, 108, -11, 81, -18, -37, -85, -75, 86, -119, 94, 126, 95, 47, -36, -16, -50, -9, 95, 60, 15, 14, 93, -108, -83, -67, 29, 2, -53, 10, -118, -51, -46, 92, -23, -56, 60, 46, -90, -84, 126, 60, 78, 12, 53, 61, 121, -6, 77, 112, 60, 40, 63 }, { 64, 121, -73, 68, 4, -103, 81, 55, -41, -81, -63, 10, 117, -74, 54, -13, -85, 79, 21, 116, -25, -12, 21, 120, -36, -80, 53, -78, 103, 25, 55, 6, -75, 96, 80, -125, -11, -103, -20, -41, 121, -61, -40, 63, 24, -81, -125, 90, -12, -40, -52, -1, -114, 14, -44, -112, -80, 83, -63, 88, -107, -10, -114, -86 }, { -81, 126, -41, -34, 66, -114, -114, 114, 39, 32, -125, -19, -95, -50, -111, -51, -33, 51, 99, -127, 58, 50, -110, 44, 80, -94, -96, 68, -69, 34, 86, 3, -82, -69, 28, 20, -111, 69, 18, -41, -23, 27, -118, 20, 72, 21, -112, 53, -87, -81, -47, -101, 123, -80, 99, -15, 33, -120, -8, 82, 80, -8, -10, -45 }, { 92, 77, 53, -87, 26, 13, -121, -39, -62, -42, 47, 4, 7, 108, -15, 112, 103, 38, -50, -74, 60, 56, -63, 43, -116, 49, -106, 69, 118, 65, 17, 12, 31, 127, -94, 55, -117, -29, -117, 31, -95, -110, -2, 63, -73, -106, -88, -41, -19, 69, 60, -17, -16, 61, 32, -23, 88, -106, -96, 37, -96, 114, -19, -99 }, { 68, -26, 57, -56, -30, 108, 61, 24, 106, -56, -92, 99, -59, 107, 25, -110, -57, 80, 79, -92, -107, 90, 54, -73, -40, -39, 78, 109, -57, -62, -17, 6, -25, -29, 37, 90, -24, -27, -61, -69, 44, 121, 107, -72, -57, 108, 32, -69, -21, -41, 126, 91, 118, 11, -91, 50, -11, 116, 126, -96, -39, 110, 105, -52 }, { 48, 108, 123, 50, -50, -58, 33, 14, 59, 102, 17, -18, -119, 4, 10, -29, 36, -56, -31, 43, -71, -48, -14, 87, 119, -119, 40, 104, -44, -76, -24, 2, 48, -96, -7, 16, -119, -3, 108, 78, 125, 88, 61, -53, -3, -16, 20, -83, 74, 124, -47, -17, -15, -21, -23, -119, -47, 105, -4, 115, -20, 77, 57, 88 }, { 33, 101, 79, -35, 32, -119, 20, 120, 34, -80, -41, 90, -22, 93, -20, -45, 9, 24, -46, 80, -55, -9, -24, -78, -124, 27, -120, -36, -51, 59, -38, 7, 113, 125, 68, 109, 24, -121, 111, 37, -71, 100, -111, 78, -43, -14, -76, -44, 64, 103, 16, -28, -44, -103, 74, 81, -118, -74, 47, -77, -65, 8, 42, -100 }, { -63, -96, -95, -111, -85, -98, -85, 42, 87, 29, -62, -57, 57, 48, 9, -39, -110, 63, -103, -114, -48, -11, -92, 105, -26, -79, -11, 78, -118, 14, -39, 1, -115, 74, 70, -41, -119, -68, -39, -60, 64, 31, 25, -111, -16, -20, 61, -22, 17, -13, 57, -110, 24, 61, -104, 21, -72, -69, 56, 116, -117, 93, -1, -123 }, { -18, -70, 12, 112, -111, 10, 22, 31, -120, 26, 53, 14, 10, 69, 51, 45, -50, -127, -22, 95, 54, 17, -8, 54, -115, 36, -79, 12, -79, 82, 4, 1, 92, 59, 23, -13, -85, -87, -110, -58, 84, -31, -48, -105, -101, -92, -9, 28, -109, 77, -47, 100, -48, -83, 106, -102, 70, -95, 94, -1, -99, -15, 21, 99 }, { 109, 123, 54, 40, -120, 32, -118, 49, -52, 0, -103, 103, 101, -9, 32, 78, 124, -56, 88, -19, 101, -32, 70, 67, -41, 85, -103, 1, 1, -105, -51, 10, 4, 51, -26, -19, 39, -43, 63, -41, -101, 80, 106, -66, 125, 47, -117, -120, -93, -120, 99, -113, -17, 61, 102, -2, 72, 9, -124, 123, -128, 78, 43, 96 }, { -22, -63, 20, 65, 5, -89, -123, -61, 14, 34, 83, -113, 34, 85, 26, -21, 1, 16, 88, 55, -92, -111, 14, -31, -37, -67, -8, 85, 39, -112, -33, 8, 28, 16, 107, -29, 1, 3, 100, -53, 2, 81, 52, -94, -14, 36, -123, -82, -6, -118, 104, 75, -99, -82, -100, 7, 30, -66, 0, -59, 108, -54, 31, 20 }, { 0, 13, -74, 28, -54, -12, 45, 36, -24, 55, 43, -110, -72, 117, -110, -56, -72, 85, 79, -89, -92, 65, -67, -34, -24, 38, 67, 42, 84, -94, 91, 13, 100, 89, 20, -95, -76, 2, 116, 34, 67, 52, -80, -101, -22, -32, 51, 32, -76, 44, -93, 11, 42, -69, -12, 7, -52, -55, 122, -10, 48, 21, 92, 110 }, { -115, 19, 115, 28, -56, 118, 111, 26, 18, 123, 111, -96, -115, 120, 105, 62, -123, -124, 101, 51, 3, 18, -89, 127, 48, -27, 39, -78, -118, 5, -2, 6, -105, 17, 123, 26, 25, -62, -37, 49, 117, 3, 10, 97, -7, 54, 121, -90, -51, -49, 11, 104, -66, 11, -6, 57, 5, -64, -8, 59, 82, -126, 26, -113 }, { 16, -53, 94, 99, -46, -29, 64, -89, -59, 116, -21, 53, 14, -77, -71, 95, 22, -121, -51, 125, -14, -96, 95, 95, 32, 96, 79, 41, -39, -128, 79, 0, 5, 6, -115, 104, 103, 77, -92, 93, -109, 58, 96, 97, -22, 116, -62, 11, 30, -122, 14, 28, 69, 124, 63, -119, 19, 80, -36, -116, -76, -58, 36, 87 }, { 109, -82, 33, -119, 17, 109, -109, -16, 98, 108, 111, 5, 98, 1, -15, -32, 22, 46, -65, 117, -78, 119, 35, -35, -3, 41, 23, -97, 55, 69, 58, 9, 20, -113, -121, -13, -41, -48, 22, -73, -1, -44, -73, 3, -10, -122, 19, -103, 10, -26, -128, 62, 34, 55, 54, -43, 35, -30, 115, 64, -80, -20, -25, 67 }, { -16, -74, -116, -128, 52, 96, -75, 17, -22, 72, -43, 22, -95, -16, 32, -72, 98, 46, -4, 83, 34, -58, -108, 18, 17, -58, -123, 53, -108, 116, 18, 2, 7, -94, -126, -45, 72, -69, -65, -89, 64, 31, -78, 78, -115, 106, 67, 55, -123, 104, -128, 36, -23, -90, -14, -87, 78, 19, 18, -128, 39, 73, 35, 120 }, { 20, -30, 15, 111, -82, 39, -108, 57, -80, 98, -19, -27, 100, -18, 47, 77, -41, 95, 80, -113, -128, -88, -76, 115, 65, -53, 83, 115, 7, 2, -104, 3, 120, 115, 14, -116, 33, -15, -120, 22, -56, -8, -69, 5, -75, 94, 124, 12, -126, -48, 51, -105, 22, -66, -93, 16, -63, -74, 32, 114, -54, -3, -47, -126 }, { 56, -101, 55, -1, 64, 4, -64, 95, 31, -15, 72, 46, 67, -9, 68, -43, -55, 28, -63, -17, -16, 65, 11, -91, -91, 32, 88, 41, 60, 67, 105, 8, 58, 102, -79, -5, -113, -113, -67, 82, 50, -26, 116, -78, -103, 107, 102, 23, -74, -47, 115, -50, -35, 63, -80, -32, 72, 117, 47, 68, 86, -20, -35, 8 }, { 21, 27, 20, -59, 117, -102, -42, 22, -10, 121, 41, -59, 115, 15, -43, 54, -79, -62, -16, 58, 116, 15, 88, 108, 114, 67, 3, -30, -99, 78, 103, 11, 49, 63, -4, -110, -27, 41, 70, -57, -69, -18, 70, 30, -21, 66, -104, -27, 3, 53, 50, 100, -33, 54, -3, -78, 92, 85, -78, 54, 19, 32, 95, 9 }, { -93, 65, -64, -79, 82, 85, -34, -90, 122, -29, -40, 3, -80, -40, 32, 26, 102, -73, 17, 53, -93, -29, 122, 86, 107, -100, 50, 56, -28, 124, 90, 14, 93, -88, 97, 101, -85, -50, 46, -109, -88, -127, -112, 63, -89, 24, -34, -9, -116, -59, -87, -86, -12, 111, -111, 87, -87, -13, -73, -124, -47, 7, 1, 9 }, { 60, -99, -77, -20, 112, -75, -34, 100, -4, -96, 81, 71, -116, -62, 38, -68, 105, 7, -126, 21, -125, -25, -56, -11, -59, 95, 117, 108, 32, -38, -65, 13, 46, 65, -46, -89, 0, 120, 5, 23, 40, 110, 114, 79, 111, -70, 8, 16, -49, -52, -82, -18, 108, -43, 81, 96, 72, -65, 70, 7, -37, 58, 46, -14 }, { -95, -32, 85, 78, 74, -53, 93, -102, -26, -110, 86, 1, -93, -50, -23, -108, -37, 97, 19, 103, 94, -65, -127, -21, 60, 98, -51, -118, 82, -31, 27, 7, -112, -45, 79, 95, -20, 90, -4, -40, 117, 100, -6, 19, -47, 53, 53, 48, 105, 91, -70, -34, -5, -87, -57, -103, -112, -108, -40, 87, -25, 13, -76, -116 }, { 44, -122, -70, 125, -60, -32, 38, 69, -77, -103, 49, -124, -4, 75, -41, -84, 68, 74, 118, 15, -13, 115, 117, -78, 42, 89, 0, -20, -12, -58, -97, 10, -48, 95, 81, 101, 23, -67, -23, 74, -79, 21, 97, 123, 103, 101, -50, -115, 116, 112, 51, 50, -124, 27, 76, 40, 74, 10, 65, -49, 102, 95, 5, 35 }, { -6, 57, 71, 5, -61, -100, -21, -9, 47, -60, 59, 108, -75, 105, 56, 41, -119, 31, 37, 27, -86, 120, -125, -108, 121, 104, -21, -70, -57, -104, 2, 11, 118, 104, 68, 6, 7, -90, -70, -61, -16, 77, -8, 88, 31, -26, 35, -44, 8, 50, 51, -88, -62, -103, 54, -41, -2, 117, 98, -34, 49, -123, 83, -58 }, { 54, 21, -36, 126, -50, -72, 82, -5, -122, -116, 72, -19, -18, -68, -71, -27, 97, -22, 53, -94, 47, -6, 15, -92, -55, 127, 13, 13, -69, 81, -82, 8, -50, 10, 84, 110, -87, -44, 61, -78, -65, 84, -32, 48, -8, -105, 35, 116, -68, -116, -6, 75, -77, 120, -95, 74, 73, 105, 39, -87, 98, -53, 47, 10 }, { -113, 116, 37, -1, 95, -89, -93, 113, 36, -70, -57, -99, 94, 52, -81, -118, 98, 58, -36, 73, 82, -67, -80, 46, 83, -127, -8, 73, 66, -27, 43, 7, 108, 32, 73, 1, -56, -108, 41, -98, -15, 49, 1, 107, 65, 44, -68, 126, -28, -53, 120, -114, 126, -79, -14, -105, -33, 53, 5, -119, 67, 52, 35, -29 }, { 98, 23, 23, 83, 78, -89, 13, 55, -83, 97, -30, -67, 99, 24, 47, -4, -117, -34, -79, -97, 95, 74, 4, 21, 66, -26, 15, 80, 60, -25, -118, 14, 36, -55, -41, -124, 90, -1, 84, 52, 31, 88, 83, 121, -47, -59, -10, 17, 51, -83, 23, 108, 19, 104, 32, 29, -66, 24, 21, 110, 104, -71, -23, -103 }, { 12, -23, 60, 35, 6, -52, -67, 96, 15, -128, -47, -15, 40, 3, 54, -81, 3, 94, 3, -98, -94, -13, -74, -101, 40, -92, 90, -64, -98, 68, -25, 2, -62, -43, 112, 32, -78, -123, 26, -80, 126, 120, -88, -92, 126, -128, 73, -43, 87, -119, 81, 111, 95, -56, -128, -14, 51, -40, -42, 102, 46, 106, 6, 6 }, { -38, -120, -11, -114, -7, -105, -98, 74, 114, 49, 64, -100, 4, 40, 110, -21, 25, 6, -92, -40, -61, 48, 94, -116, -71, -87, 75, -31, 13, -119, 1, 5, 33, -69, -16, -125, -79, -46, -36, 3, 40, 1, -88, -118, -107, 95, -23, -107, -49, 44, -39, 2, 108, -23, 39, 50, -51, -59, -4, -42, -10, 60, 10, -103 }, { 67, -53, 55, -32, -117, 3, 94, 52, -115, -127, -109, 116, -121, -27, -115, -23, 98, 90, -2, 48, -54, -76, 108, -56, 99, 30, -35, -18, -59, 25, -122, 3, 43, -13, -109, 34, -10, 123, 117, 113, -112, -85, -119, -62, -78, -114, -96, 101, 72, -98, 28, 89, -98, -121, 20, 115, 89, -20, 94, -55, 124, 27, -76, 94 }, { 15, -101, 98, -21, 8, 5, -114, -64, 74, 123, 99, 28, 125, -33, 22, 23, -2, -56, 13, 91, 27, -105, -113, 19, 60, -7, -67, 107, 70, 103, -107, 13, -38, -108, -77, -29, 2, 9, -12, 21, 12, 65, 108, -16, 69, 77, 96, -54, 55, -78, -7, 41, -48, 124, -12, 64, 113, -45, -21, -119, -113, 88, -116, 113 }, { -17, 77, 10, 84, -57, -12, 101, 21, -91, 92, 17, -32, -26, 77, 70, 46, 81, -55, 40, 44, 118, -35, -97, 47, 5, 125, 41, -127, -72, 66, -18, 2, 115, -13, -74, 126, 86, 80, 11, -122, -29, -68, 113, 54, -117, 107, -75, -107, -54, 72, -44, 98, -111, -33, -56, -40, 93, -47, 84, -43, -45, 86, 65, -84 }, { -126, 60, -56, 121, 31, -124, -109, 100, -118, -29, 106, 94, 5, 27, 13, -79, 91, -111, -38, -42, 18, 61, -100, 118, -18, -4, -60, 121, 46, -22, 6, 4, -37, -20, 124, -43, 51, -57, -49, -44, -24, -38, 81, 60, -14, -97, -109, -11, -5, -85, 75, -17, -124, -96, -53, 52, 64, 100, -118, -120, 6, 60, 76, -110 }, { -12, -40, 115, -41, 68, 85, 20, 91, -44, -5, 73, -105, -81, 32, 116, 32, -28, 69, 88, -54, 29, -53, -51, -83, 54, 93, -102, -102, -23, 7, 110, 15, 34, 122, 84, 52, -121, 37, -103, -91, 37, -77, -101, 64, -18, 63, -27, -75, -112, -11, 1, -69, -25, -123, -99, -31, 116, 11, 4, -42, -124, 98, -2, 53 }, { -128, -69, -16, -33, -8, -112, 39, -57, 113, -76, -29, -37, 4, 121, -63, 12, -54, -66, -121, 13, -4, -44, 50, 27, 103, 101, 44, -115, 12, -4, -8, 10, 53, 108, 90, -47, 46, -113, 5, -3, -111, 8, -66, -73, 57, 72, 90, -33, 47, 99, 50, -55, 53, -4, 96, 87, 57, 26, 53, -45, -83, 39, -17, 45 }, { -121, 125, 60, -9, -79, -128, -19, 113, 54, 77, -23, -89, 105, -5, 47, 114, -120, -88, 31, 25, -96, -75, -6, 76, 9, -83, 75, -109, -126, -47, -6, 2, -59, 64, 3, 74, 100, 110, -96, 66, -3, 10, -124, -6, 8, 50, 109, 14, -109, 79, 73, 77, 67, 63, -50, 10, 86, -63, -125, -86, 35, -26, 7, 83 }, { 36, 31, -77, 126, 106, 97, 63, 81, -37, -126, 69, -127, -22, -69, 104, -111, 93, -49, 77, -3, -38, -112, 47, -55, -23, -68, -8, 78, -127, -28, -59, 10, 22, -61, -127, -13, -72, -14, -87, 14, 61, 76, -89, 81, -97, -97, -105, 94, -93, -9, -3, -104, -83, 59, 104, 121, -30, 106, -2, 62, -51, -72, -63, 55 }, { 81, -88, -8, -96, -31, 118, -23, -38, -94, 80, 35, -20, -93, -102, 124, 93, 0, 15, 36, -127, -41, -19, 6, -124, 94, -49, 44, 26, -69, 43, -58, 9, -18, -3, -2, 60, -122, -30, -47, 124, 71, 47, -74, -68, 4, -101, -16, 77, -120, -15, 45, -12, 68, -77, -74, 63, -113, 44, -71, 56, 122, -59, 53, -44 }, { 122, 30, 27, -79, 32, 115, -104, -28, -53, 109, 120, 121, -115, -65, -87, 101, 23, 10, 122, 101, 29, 32, 56, 63, -23, -48, -51, 51, 16, -124, -41, 6, -71, 49, -20, 26, -57, 65, 49, 45, 7, 49, -126, 54, -122, -43, 1, -5, 111, 117, 104, 117, 126, 114, -77, 66, -127, -50, 69, 14, 70, 73, 60, 112 }, { 104, -117, 105, -118, 35, 16, -16, 105, 27, -87, -43, -59, -13, -23, 5, 8, -112, -28, 18, -1, 48, 94, -82, 55, 32, 16, 59, -117, 108, -89, 101, 9, -35, 58, 70, 62, 65, 91, 14, -43, -104, 97, 1, -72, 16, -24, 79, 79, -85, -51, -79, -55, -128, 23, 109, -95, 17, 92, -38, 109, 65, -50, 46, -114 }, { 44, -3, 102, -60, -85, 66, 121, -119, 9, 82, -47, -117, 67, -28, 108, 57, -47, -52, -24, -82, 65, -13, 85, 107, -21, 16, -24, -85, 102, -92, 73, 5, 7, 21, 41, 47, -118, 72, 43, 51, -5, -64, 100, -34, -25, 53, -45, -115, 30, -72, -114, 126, 66, 60, -24, -67, 44, 48, 22, 117, -10, -33, -89, -108 }, { -7, 71, -93, -66, 3, 30, -124, -116, -48, -76, -7, -62, 125, -122, -60, -104, -30, 71, 36, -110, 34, -126, 31, 10, 108, 102, -53, 56, 104, -56, -48, 12, 25, 21, 19, -90, 45, -122, -73, -112, 97, 96, 115, 71, 127, -7, -46, 84, -24, 102, -104, -96, 28, 8, 37, -84, -13, -65, -6, 61, -85, -117, -30, 70 }, { -112, 39, -39, -24, 127, -115, 68, -1, -111, -43, 101, 20, -12, 39, -70, 67, -50, 68, 105, 69, -91, -106, 91, 4, -52, 75, 64, -121, 46, -117, 31, 10, -125, 77, 51, -3, -93, 58, 79, 121, 126, -29, 56, -101, 1, -28, 49, 16, -80, 92, -62, 83, 33, 17, 106, 89, -9, 60, 79, 38, -74, -48, 119, 24 }, { 105, -118, 34, 52, 111, 30, 38, -73, 125, -116, 90, 69, 2, 126, -34, -25, -41, -67, -23, -105, -12, -75, 10, 69, -51, -95, -101, 92, -80, -73, -120, 2, 71, 46, 11, -85, -18, 125, 81, 117, 33, -89, -42, 118, 51, 60, 89, 110, 97, -118, -111, -36, 75, 112, -4, -8, -36, -49, -55, 35, 92, 70, -37, 36 }, { 71, 4, -113, 13, -48, 29, -56, 82, 115, -38, -20, -79, -8, 126, -111, 5, -12, -56, -107, 98, 111, 19, 127, -10, -42, 24, -38, -123, 59, 51, -64, 3, 47, -1, -83, -127, -58, 86, 33, -76, 5, 71, -80, -50, -62, 116, 75, 20, -126, 23, -31, -21, 24, -83, -19, 114, -17, 1, 110, -70, -119, 126, 82, -83 }, { -77, -69, -45, -78, -78, 69, 35, 85, 84, 25, -66, -25, 53, -38, -2, 125, -38, 103, 88, 31, -9, -43, 15, -93, 69, -22, -13, -20, 73, 3, -100, 7, 26, -18, 123, -14, -78, 113, 79, -57, -109, -118, 105, -104, 75, -88, -24, -109, 73, -126, 9, 55, 98, -120, 93, 114, 74, 0, -86, -68, 47, 29, 75, 67 }, { -104, 11, -85, 16, -124, -91, 66, -91, 18, -67, -122, -57, -114, 88, 79, 11, -60, -119, 89, 64, 57, 120, -11, 8, 52, -18, -67, -127, 26, -19, -69, 2, -82, -56, 11, -90, -104, 110, -10, -68, 87, 21, 28, 87, -5, -74, -21, -84, 120, 70, -17, 102, 72, -116, -69, 108, -86, -79, -74, 115, -78, -67, 6, 45 }, { -6, -101, -17, 38, -25, -7, -93, 112, 13, -33, 121, 71, -79, -122, -95, 22, 47, -51, 16, 84, 55, -39, -26, 37, -36, -18, 11, 119, 106, -57, 42, 8, -1, 23, 7, -63, -9, -50, 30, 35, -125, 83, 9, -60, -94, -15, -76, 120, 18, -103, -70, 95, 26, 48, -103, -95, 10, 113, 66, 54, -96, -4, 37, 111 }, { -124, -53, 43, -59, -73, 99, 71, -36, -31, 61, -25, -14, -71, 48, 17, 10, -26, -21, -22, 104, 64, -128, 27, -40, 111, -70, -90, 91, -81, -88, -10, 11, -62, 127, -124, -2, -67, -69, 65, 73, 40, 82, 112, -112, 100, -26, 30, 86, 30, 1, -105, 45, 103, -47, -124, 58, 105, 24, 20, 108, -101, 84, -34, 80 }, { 28, -1, 84, 111, 43, 109, 57, -23, 52, -95, 110, -50, 77, 15, 80, 85, 125, -117, -10, 8, 59, -58, 18, 97, -58, 45, 92, -3, 56, 24, -117, 9, -73, -9, 48, -99, 50, -24, -3, -41, -43, 48, -77, -8, -89, -42, 126, 73, 28, -65, -108, 54, 6, 34, 32, 2, -73, -123, -106, -52, -73, -106, -112, 109 }, { 73, -76, -7, 49, 67, -34, 124, 80, 111, -91, -22, -121, -74, 42, -4, -18, 84, -3, 38, 126, 31, 54, -120, 65, -122, -14, -38, -80, -124, 90, 37, 1, 51, 123, 69, 48, 109, -112, -63, 27, 67, -127, 29, 79, -26, 99, -24, -100, 51, 103, -105, 13, 85, 74, 12, -37, 43, 80, -113, 6, 70, -107, -5, -80 }, { 110, -54, 109, 21, -124, 98, 90, -26, 69, -44, 17, 117, 78, -91, -7, -18, -81, -43, 20, 80, 48, -109, 117, 125, -67, 19, -15, 69, -28, 47, 15, 4, 34, -54, 51, -128, 18, 61, -77, -122, 100, -58, -118, -36, 5, 32, 43, 15, 60, -55, 120, 123, -77, -76, -121, 77, 93, 16, -73, 54, 46, -83, -39, 125 }, { 115, -15, -42, 111, -124, 52, 29, -124, -10, -23, 41, -128, 65, -60, -121, 6, -42, 14, 98, -80, 80, -46, -38, 64, 16, 84, -50, 47, -97, 11, -88, 12, 68, -127, -92, 87, -22, 54, -49, 33, -4, -68, 21, -7, -45, 84, 107, 57, 8, -106, 0, -87, -104, 93, -43, -98, -92, -72, 110, -14, -66, 119, 14, -68 }, { -19, 7, 7, 66, -94, 18, 36, 8, -58, -31, 21, -113, -124, -5, -12, 105, 40, -62, 57, -56, 25, 117, 49, 17, -33, 49, 105, 113, -26, 78, 97, 2, -22, -84, 49, 67, -6, 33, 89, 28, 30, 12, -3, -23, -45, 7, -4, -39, -20, 25, -91, 55, 53, 21, -94, 17, -54, 109, 125, 124, 122, 117, -125, 60 }, { -28, -104, -46, -22, 71, -79, 100, 48, -90, -57, -30, -23, -24, 1, 2, -31, 85, -6, -113, -116, 105, -31, -109, 106, 1, 78, -3, 103, -6, 100, -44, 15, -100, 97, 59, -42, 22, 83, 113, -118, 112, -57, 80, -45, -86, 72, 77, -26, -106, 50, 28, -24, 41, 22, -73, 108, 18, -93, 30, 8, -11, -16, 124, 106 }, { 16, -119, -109, 115, 67, 36, 28, 74, 101, -58, -82, 91, 4, -97, 111, -77, -37, -125, 126, 3, 10, -99, -115, 91, -66, -83, -81, 10, 7, 92, 26, 6, -45, 66, -26, 118, -77, 13, -91, 20, -18, -33, -103, 43, 75, -100, -5, -64, 117, 30, 5, -100, -90, 13, 18, -52, 26, 24, -10, 24, -31, 53, 88, 112 }, { 7, -90, 46, 109, -42, 108, -84, 124, -28, -63, 34, -19, -76, 88, -121, 23, 54, -73, -15, -52, 84, -119, 64, 20, 92, -91, -58, -121, -117, -90, -102, 1, 49, 21, 3, -85, -3, 38, 117, 73, -38, -71, -37, 40, -2, -50, -47, -46, 75, -105, 125, 126, -13, 68, 50, -81, -43, -93, 85, -79, 52, 98, 118, 50 }, { -104, 65, -61, 12, 68, 106, 37, -64, 40, -114, 61, 73, 74, 61, -113, -79, 57, 47, -57, -21, -68, -62, 23, -18, 93, -7, -55, -88, -106, 104, -126, 5, 53, 97, 100, -67, -112, -88, 41, 24, 95, 15, 25, -67, 79, -69, 53, 21, -128, -101, 73, 17, 7, -98, 5, -2, 33, -113, 99, -72, 125, 7, 18, -105 }, { -17, 28, 79, 34, 110, 86, 43, 27, -114, -112, -126, -98, -121, 126, -21, 111, 58, -114, -123, 75, 117, -116, 7, 107, 90, 80, -75, -121, 116, -11, -76, 0, -117, -52, 76, -116, 115, -117, 61, -7, 55, -34, 38, 101, 86, -19, -36, -92, -94, 61, 88, -128, -121, -103, 84, 19, -83, -102, 122, -111, 62, 112, 20, 3 }, { -127, -90, 28, -77, -48, -56, -10, 84, -41, 59, -115, 68, -74, -104, -119, -49, -37, -90, -57, 66, 108, 110, -62, -107, 88, 90, 29, -65, 74, -38, 95, 8, 120, 88, 96, -65, -109, 68, -63, -4, -16, 90, 7, 39, -56, -110, -100, 86, -39, -53, -89, -35, 127, -42, -48, -36, 53, -66, 109, -51, 51, -23, -12, 73 }, { -12, 78, 81, 30, 124, 22, 56, -112, 58, -99, 30, -98, 103, 66, 89, 92, -52, -20, 26, 82, -92, -18, 96, 7, 38, 21, -9, -25, -17, 4, 43, 15, 111, 103, -48, -50, -83, 52, 59, 103, 102, 83, -105, 87, 20, -120, 35, -7, -39, -24, 29, -39, -35, -87, 88, 120, 126, 19, 108, 34, -59, -20, 86, 47 }, { 19, -70, 36, 55, -42, -49, 33, 100, 105, -5, 89, 43, 3, -85, 60, -96, 43, -46, 86, -33, 120, -123, -99, -100, -34, 48, 82, -37, 34, 78, 127, 12, -39, -76, -26, 117, 74, -60, -68, -2, -37, -56, -6, 94, -27, 81, 32, -96, -19, -32, -77, 22, -56, -49, -38, -60, 45, -69, 40, 106, -106, -34, 101, -75 }, { 57, -92, -44, 8, -79, -88, -82, 58, -116, 93, 103, -127, 87, -121, -28, 31, -108, -14, -23, 38, 57, -83, -33, -110, 24, 6, 68, 124, -89, -35, -127, 5, -118, -78, -127, -35, 112, -34, 30, 24, -70, -71, 126, 39, -124, 122, -35, -97, -18, 25, 119, 79, 119, -65, 59, -20, -84, 120, -47, 4, -106, -125, -38, -113 }, { 18, -93, 34, -80, -43, 127, 57, -118, 24, -119, 25, 71, 59, -29, -108, -99, -122, 58, 44, 0, 42, -111, 25, 94, -36, 41, -64, -53, -78, -119, 85, 7, -45, -70, 81, -84, 71, -61, -68, -79, 112, 117, 19, 18, 70, 95, 108, -58, 48, 116, -89, 43, 66, 55, 37, -37, -60, 104, 47, -19, -56, 97, 73, 26 }, { 78, 4, -111, -36, 120, 111, -64, 46, 99, 125, -5, 97, -126, -21, 60, -78, -33, 89, 25, -60, 0, -49, 59, -118, 18, 3, -60, 30, 105, -92, -101, 15, 63, 50, 25, 2, -116, 78, -5, -25, -59, 74, -116, 64, -55, -121, 1, 69, 51, -119, 43, -6, -81, 14, 5, 84, -67, -73, 67, 24, 82, -37, 109, -93 }, { -44, -30, -64, -68, -21, 74, 124, 122, 114, -89, -91, -51, 89, 32, 96, -1, -101, -112, -94, 98, -24, -31, -50, 100, -72, 56, 24, 30, 105, 115, 15, 3, -67, 107, -18, 111, -38, -93, -11, 24, 36, 73, -23, 108, 14, -41, -65, 32, 51, 22, 95, 41, 85, -121, -35, -107, 0, 105, -112, 59, 48, -22, -84, 46 }, { 4, 38, 54, -84, -78, 24, -48, 8, -117, 78, -95, 24, 25, -32, -61, 26, -97, -74, 46, -120, -125, 27, 73, 107, -17, -21, -6, -52, 47, -68, 66, 5, -62, -12, -102, -127, 48, -69, -91, -81, -33, -13, -9, -12, -44, -73, 40, -58, 120, -120, 108, 101, 18, -14, -17, -93, 113, 49, 76, -4, -113, -91, -93, -52 }, { 28, -48, 70, -35, 123, -31, 16, -52, 72, 84, -51, 78, 104, 59, -102, -112, 29, 28, 25, 66, 12, 75, 26, -85, 56, -12, -4, -92, 49, 86, -27, 12, 44, -63, 108, 82, -76, -97, -41, 95, -48, -95, -115, 1, 64, -49, -97, 90, 65, 46, -114, -127, -92, 79, 100, 49, 116, -58, -106, 9, 117, -7, -91, 96 }, { 58, 26, 18, 76, 127, -77, -58, -87, -116, -44, 60, -32, -4, -76, -124, 4, -60, 82, -5, -100, -95, 18, 2, -53, -50, -96, -126, 105, 93, 19, 74, 13, 87, 125, -72, -10, 42, 14, 91, 44, 78, 52, 60, -59, -27, -37, -57, 17, -85, 31, -46, 113, 100, -117, 15, 108, -42, 12, 47, 63, 1, 11, -122, -3 } };
        for (int i2 = 0; i2 < localObject2.length; i2++)
        {
          localObject5 = new Nxt.Transaction((byte)0, (byte)0, 0, (short)0, new byte[] { 18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27 }, localObject2[i2], ???[i2], 0, 0L, arrayOfByte[i2]);
          transactions.put(Long.valueOf(((Nxt.Transaction)localObject5).getId()), localObject5);
        }
        localObject5 = transactions.values().iterator();
        while (((Iterator)localObject5).hasNext())
        {
          localObject4 = (Nxt.Transaction)((Iterator)localObject5).next();
          ((Nxt.Transaction)localObject4).index = (++transactionCounter);
          ((Nxt.Transaction)localObject4).block = 2680262203532249785L;
        }
        Nxt.Transaction.saveTransactions("transactions.nxt");
      }
      try
      {
        logMessage("Loading blocks...");
        Nxt.Block.loadBlocks("blocks.nxt");
        logMessage("...Done");
      }
      catch (FileNotFoundException localFileNotFoundException2)
      {
        blocks = new HashMap();
        localObject2 = new Nxt.Block(-1, 0, 0L, transactions.size(), 1000000000, 0, transactions.size() * 128, null, new byte[] { 18, 89, -20, 33, -45, 26, 48, -119, -115, 124, -47, 96, -97, -128, -39, 102, -117, 71, 120, -29, -39, 126, -108, 16, 68, -77, -97, 12, 68, -46, -27, 27 }, new byte[64], new byte[] { 105, -44, 38, -60, -104, -73, 10, -58, -47, 103, -127, -128, 53, 101, 39, -63, -2, -32, 48, -83, 115, 47, -65, 118, 114, -62, 38, 109, 22, 106, 76, 8, -49, -113, -34, -76, 82, 79, -47, -76, -106, -69, -54, -85, 3, -6, 110, 103, 118, 15, 109, -92, 82, 37, 20, 2, 36, -112, 21, 72, 108, 72, 114, 17 });
        ((Nxt.Block)localObject2).index = (++blockCounter);
        blocks.put(Long.valueOf(2680262203532249785L), localObject2);
        ((Nxt.Block)localObject2).transactions = new long[((Nxt.Block)localObject2).numberOfTransactions];
        int i1 = 0;
        localObject5 = transactions.keySet().iterator();
        while (((Iterator)localObject5).hasNext())
        {
          long l2 = ((Long)((Iterator)localObject5).next()).longValue();
          ((Nxt.Block)localObject2).transactions[(i1++)] = l2;
        }
        Arrays.sort(((Nxt.Block)localObject2).transactions);
        MessageDigest localMessageDigest = MessageDigest.getInstance("SHA-256");
        for (i1 = 0; i1 < ((Nxt.Block)localObject2).numberOfTransactions; i1++) {
          localMessageDigest.update(((Nxt.Transaction)transactions.get(Long.valueOf(localObject2.transactions[i1]))).getBytes());
        }
        ((Nxt.Block)localObject2).payloadHash = localMessageDigest.digest();
        ((Nxt.Block)localObject2).baseTarget = 153722867L;
        lastBlock = 2680262203532249785L;
        ((Nxt.Block)localObject2).cumulativeDifficulty = BigInteger.ZERO;
        Nxt.Block.saveBlocks("blocks.nxt", false);
      }
      logMessage("Scanning blockchain...");
      Object localObject2 = blocks;
      blocks = new HashMap();
      lastBlock = 2680262203532249785L;
      long l1 = 2680262203532249785L;
      do
      {
        localObject4 = (Nxt.Block)((HashMap)localObject2).get(Long.valueOf(l1));
        long l3 = ((Nxt.Block)localObject4).nextBlock;
        ((Nxt.Block)localObject4).analyze();
        l1 = l3;
      } while (l1 != 0L);
      logMessage("...Done");
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            if (Nxt.Peer.getNumberOfConnectedPublicPeers() < Nxt.maxNumberOfConnectedPublicPeers)
            {
              Nxt.Peer localPeer = Nxt.Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? 0 : 2, false);
              if (localPeer != null) {
                localPeer.connect();
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 5L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            long l = System.currentTimeMillis();
            Collection localCollection;
            synchronized (Nxt.peers)
            {
              localCollection = ((HashMap)Nxt.peers.clone()).values();
            }
            Iterator localIterator = localCollection.iterator();
            while (localIterator.hasNext())
            {
              ??? = (Nxt.Peer)localIterator.next();
              if ((((Nxt.Peer)???).blacklistingTime > 0L) && (((Nxt.Peer)???).blacklistingTime + Nxt.blacklistingPeriod <= l)) {
                ((Nxt.Peer)???).removeBlacklistedStatus();
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 1L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            Nxt.Peer localPeer = Nxt.Peer.getAnyPeer(1, true);
            if (localPeer != null)
            {
              JSONObject localJSONObject1 = new JSONObject();
              localJSONObject1.put("requestType", "getPeers");
              JSONObject localJSONObject2 = localPeer.send(localJSONObject1);
              if (localJSONObject2 != null)
              {
                JSONArray localJSONArray = (JSONArray)localJSONObject2.get("peers");
                for (int i = 0; i < localJSONArray.size(); i++)
                {
                  String str = ((String)localJSONArray.get(i)).trim();
                  if (str.length() > 0) {
                    Nxt.Peer.addPeer(str, str);
                  }
                }
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 5L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            Nxt.Peer localPeer = Nxt.Peer.getAnyPeer(1, true);
            if (localPeer != null)
            {
              JSONObject localJSONObject1 = new JSONObject();
              localJSONObject1.put("requestType", "getUnconfirmedTransactions");
              JSONObject localJSONObject2 = localPeer.send(localJSONObject1);
              if (localJSONObject2 != null) {
                Nxt.Transaction.processTransactions(localJSONObject2, "unconfirmedTransactions");
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 5L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            int i = Nxt.getEpochTime(System.currentTimeMillis());
            synchronized (Nxt.transactions)
            {
              JSONArray localJSONArray = new JSONArray();
              Iterator localIterator = Nxt.unconfirmedTransactions.values().iterator();
              Object localObject1;
              Object localObject2;
              while (localIterator.hasNext())
              {
                localObject1 = (Nxt.Transaction)localIterator.next();
                if ((((Nxt.Transaction)localObject1).timestamp + ((Nxt.Transaction)localObject1).deadline * 60 < i) || (!((Nxt.Transaction)localObject1).validateAttachment()))
                {
                  localIterator.remove();
                  localObject2 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(((Nxt.Transaction)localObject1).senderPublicKey)));
                  synchronized (localObject2)
                  {
                    ((Nxt.Account)localObject2).setUnconfirmedBalance(((Nxt.Account)localObject2).unconfirmedBalance + (((Nxt.Transaction)localObject1).amount + ((Nxt.Transaction)localObject1).fee) * 100L);
                  }
                  ??? = new JSONObject();
                  ((JSONObject)???).put("index", Integer.valueOf(((Nxt.Transaction)localObject1).index));
                  localJSONArray.add(???);
                }
              }
              if (localJSONArray.size() > 0)
              {
                localObject1 = new JSONObject();
                ((JSONObject)localObject1).put("response", "processNewData");
                ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray);
                ??? = Nxt.users.values().iterator();
                while (((Iterator)???).hasNext())
                {
                  localObject2 = (Nxt.User)((Iterator)???).next();
                  ((Nxt.User)localObject2).send((JSONObject)localObject1);
                }
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 1L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            Nxt.Peer localPeer = Nxt.Peer.getAnyPeer(1, true);
            if (localPeer != null)
            {
              Nxt.lastBlockchainFeeder = localPeer;
              JSONObject localJSONObject1 = new JSONObject();
              localJSONObject1.put("requestType", "getCumulativeDifficulty");
              JSONObject localJSONObject2 = localPeer.send(localJSONObject1);
              if (localJSONObject2 != null)
              {
                BigInteger localBigInteger1 = Nxt.Block.getLastBlock().cumulativeDifficulty;
                BigInteger localBigInteger2 = new BigInteger((String)localJSONObject2.get("cumulativeDifficulty"));
                if (localBigInteger2.compareTo(localBigInteger1) > 0)
                {
                  localJSONObject1 = new JSONObject();
                  localJSONObject1.put("requestType", "getMilestoneBlockIds");
                  localJSONObject2 = localPeer.send(localJSONObject1);
                  if (localJSONObject2 != null)
                  {
                    long l1 = 2680262203532249785L;
                    JSONArray localJSONArray1 = (JSONArray)localJSONObject2.get("milestoneBlockIds");
                    for (int i = 0; i < localJSONArray1.size(); i++)
                    {
                      long l2 = new BigInteger((String)localJSONArray1.get(i)).longValue();
                      Nxt.Block localBlock = (Nxt.Block)Nxt.blocks.get(Long.valueOf(l2));
                      if (localBlock != null)
                      {
                        l1 = l2;
                        break;
                      }
                    }
                    int j;
                    do
                    {
                      localJSONObject1 = new JSONObject();
                      localJSONObject1.put("requestType", "getNextBlockIds");
                      localJSONObject1.put("blockId", Nxt.convert(l1));
                      localJSONObject2 = localPeer.send(localJSONObject1);
                      if (localJSONObject2 == null) {
                        return;
                      }
                      JSONArray localJSONArray2 = (JSONArray)localJSONObject2.get("nextBlockIds");
                      j = localJSONArray2.size();
                      if (j == 0) {
                        return;
                      }
                      for (i = 0; i < j; i++)
                      {
                        long l4 = new BigInteger((String)localJSONArray2.get(i)).longValue();
                        if (Nxt.blocks.get(Long.valueOf(l4)) == null) {
                          break;
                        }
                        l1 = l4;
                      }
                    } while (i == j);
                    if (Nxt.Block.getLastBlock().height - ((Nxt.Block)Nxt.blocks.get(Long.valueOf(l1))).height < 720)
                    {
                      long l3 = l1;
                      LinkedList localLinkedList = new LinkedList();
                      HashMap localHashMap = new HashMap();
                      Object localObject1;
                      Object localObject2;
                      int k;
                      for (;;)
                      {
                        localJSONObject1 = new JSONObject();
                        localJSONObject1.put("requestType", "getNextBlocks");
                        localJSONObject1.put("blockId", Nxt.convert(l3));
                        localJSONObject2 = localPeer.send(localJSONObject1);
                        if (localJSONObject2 == null) {
                          break;
                        }
                        JSONArray localJSONArray3 = (JSONArray)localJSONObject2.get("nextBlocks");
                        j = localJSONArray3.size();
                        if (j == 0) {
                          break;
                        }
                        for (i = 0; i < j; i++)
                        {
                          localObject1 = (JSONObject)localJSONArray3.get(i);
                          localObject2 = Nxt.Block.getBlock((JSONObject)localObject1);
                          l3 = ((Nxt.Block)localObject2).getId();
                          synchronized (Nxt.blocks)
                          {
                            k = 0;
                            Object localObject3;
                            if (((Nxt.Block)localObject2).previousBlock == Nxt.lastBlock)
                            {
                              localObject3 = ByteBuffer.allocate(224 + ((Nxt.Block)localObject2).payloadLength);
                              ((ByteBuffer)localObject3).order(ByteOrder.LITTLE_ENDIAN);
                              ((ByteBuffer)localObject3).put(((Nxt.Block)localObject2).getBytes());
                              JSONArray localJSONArray4 = (JSONArray)((JSONObject)localObject1).get("transactions");
                              for (int n = 0; n < localJSONArray4.size(); n++) {
                                ((ByteBuffer)localObject3).put(Nxt.Transaction.getTransaction((JSONObject)localJSONArray4.get(n)).getBytes());
                              }
                              if (Nxt.Block.pushBlock((ByteBuffer)localObject3, false))
                              {
                                k = 1;
                              }
                              else
                              {
                                localPeer.blacklist();
                                return;
                              }
                            }
                            if ((k == 0) && (Nxt.blocks.get(Long.valueOf(((Nxt.Block)localObject2).getId())) == null))
                            {
                              localLinkedList.add(localObject2);
                              ((Nxt.Block)localObject2).transactions = new long[((Nxt.Block)localObject2).numberOfTransactions];
                              localObject3 = (JSONArray)((JSONObject)localObject1).get("transactions");
                              for (int m = 0; m < ((Nxt.Block)localObject2).numberOfTransactions; m++)
                              {
                                Nxt.Transaction localTransaction = Nxt.Transaction.getTransaction((JSONObject)((JSONArray)localObject3).get(m));
                                ((Nxt.Block)localObject2).transactions[m] = localTransaction.getId();
                                localHashMap.put(Long.valueOf(localObject2.transactions[m]), localTransaction);
                              }
                            }
                          }
                        }
                      }
                      if ((!localLinkedList.isEmpty()) && (Nxt.Block.getLastBlock().height - ((Nxt.Block)Nxt.blocks.get(Long.valueOf(l1))).height < 720)) {
                        synchronized (Nxt.blocks)
                        {
                          Nxt.Block.saveBlocks("blocks.nxt.bak", true);
                          Nxt.Transaction.saveTransactions("transactions.nxt.bak");
                          localBigInteger1 = Nxt.Block.getLastBlock().cumulativeDifficulty;
                          while ((Nxt.lastBlock != l1) && (Nxt.Block.popLastBlock())) {}
                          if (Nxt.lastBlock == l1)
                          {
                            localObject2 = localLinkedList.iterator();
                            while (((Iterator)localObject2).hasNext())
                            {
                              localObject1 = (Nxt.Block)((Iterator)localObject2).next();
                              if (((Nxt.Block)localObject1).previousBlock == Nxt.lastBlock)
                              {
                                ??? = ByteBuffer.allocate(224 + ((Nxt.Block)localObject1).payloadLength);
                                ((ByteBuffer)???).order(ByteOrder.LITTLE_ENDIAN);
                                ((ByteBuffer)???).put(((Nxt.Block)localObject1).getBytes());
                                for (k = 0; k < ((Nxt.Block)localObject1).transactions.length; k++) {
                                  ((ByteBuffer)???).put(((Nxt.Transaction)localHashMap.get(Long.valueOf(localObject1.transactions[k]))).getBytes());
                                }
                                if (!Nxt.Block.pushBlock((ByteBuffer)???, false)) {
                                  break;
                                }
                              }
                            }
                          }
                          if (Nxt.Block.getLastBlock().cumulativeDifficulty.compareTo(localBigInteger1) < 0)
                          {
                            Nxt.Block.loadBlocks("blocks.nxt.bak");
                            Nxt.Transaction.loadTransactions("transactions.nxt.bak");
                            localPeer.blacklist();
                          }
                        }
                      }
                      Nxt.Block.saveBlocks("blocks.nxt", false);
                      Nxt.Transaction.saveTransactions("transactions.nxt");
                    }
                  }
                }
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 1L, TimeUnit.SECONDS);
      scheduledThreadPool.scheduleWithFixedDelay(new Runnable()
      {
        public void run()
        {
          try
          {
            HashMap localHashMap = new HashMap();
            Iterator localIterator = Nxt.users.values().iterator();
            Object localObject1;
            Nxt.Account localAccount;
            while (localIterator.hasNext())
            {
              localObject1 = (Nxt.User)localIterator.next();
              if (((Nxt.User)localObject1).secretPhrase != null)
              {
                localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(Nxt.Crypto.getPublicKey(((Nxt.User)localObject1).secretPhrase))));
                if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
                  localHashMap.put(localAccount, localObject1);
                }
              }
            }
            localIterator = localHashMap.entrySet().iterator();
            while (localIterator.hasNext())
            {
              localObject1 = (Map.Entry)localIterator.next();
              localAccount = (Nxt.Account)((Map.Entry)localObject1).getKey();
              Nxt.User localUser = (Nxt.User)((Map.Entry)localObject1).getValue();
              Nxt.Block localBlock = Nxt.Block.getLastBlock();
              Object localObject2;
              if (Nxt.lastBlocks.get(localAccount) != localBlock)
              {
                byte[] arrayOfByte = Nxt.Crypto.sign(localBlock.generationSignature, localUser.secretPhrase);
                localObject2 = MessageDigest.getInstance("SHA-256").digest(arrayOfByte);
                BigInteger localBigInteger = new BigInteger(1, new byte[] { localObject2[7], localObject2[6], localObject2[5], localObject2[4], localObject2[3], localObject2[2], localObject2[1], localObject2[0] });
                Nxt.lastBlocks.put(localAccount, localBlock);
                Nxt.hits.put(localAccount, localBigInteger);
                JSONObject localJSONObject = new JSONObject();
                localJSONObject.put("response", "setBlockGenerationDeadline");
                localJSONObject.put("deadline", Long.valueOf(localBigInteger.divide(BigInteger.valueOf(Nxt.Block.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance()))).longValue() - (Nxt.getEpochTime(System.currentTimeMillis()) - localBlock.timestamp)));
                localUser.send(localJSONObject);
              }
              int i = Nxt.getEpochTime(System.currentTimeMillis()) - localBlock.timestamp;
              if (i > 0)
              {
                localObject2 = BigInteger.valueOf(Nxt.Block.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
                if (((BigInteger)Nxt.hits.get(localAccount)).compareTo((BigInteger)localObject2) < 0) {
                  localAccount.generateBlock(localUser.secretPhrase);
                }
              }
            }
          }
          catch (Exception localException) {}
        }
      }, 0L, 1L, TimeUnit.SECONDS);
    }
    catch (Exception localException1)
    {
      logMessage("10: " + localException1.toString());
    }
  }
  
  /* Error */
  public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    // Byte code:
    //   0: aconst_null
    //   1: astore_3
    //   2: aload_1
    //   3: ldc_w 904
    //   6: invokeinterface 906 2 0
    //   11: astore 4
    //   13: aload 4
    //   15: ifnonnull +8002 -> 8017
    //   18: new 911	org/json/simple/JSONObject
    //   21: dup
    //   22: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   25: astore 5
    //   27: getstatic 506	Nxt:allowedBotHosts	Ljava/util/HashSet;
    //   30: ifnull +47 -> 77
    //   33: getstatic 506	Nxt:allowedBotHosts	Ljava/util/HashSet;
    //   36: aload_1
    //   37: invokeinterface 914 1 0
    //   42: invokevirtual 917	java/util/HashSet:contains	(Ljava/lang/Object;)Z
    //   45: ifne +32 -> 77
    //   48: aload 5
    //   50: ldc_w 920
    //   53: bipush 7
    //   55: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   58: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   61: pop
    //   62: aload 5
    //   64: ldc_w 923
    //   67: ldc_w 925
    //   70: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   73: pop
    //   74: goto +7904 -> 7978
    //   77: aload_1
    //   78: ldc_w 927
    //   81: invokeinterface 906 2 0
    //   86: astore 6
    //   88: aload 6
    //   90: ifnonnull +31 -> 121
    //   93: aload 5
    //   95: ldc_w 920
    //   98: iconst_1
    //   99: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   102: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   105: pop
    //   106: aload 5
    //   108: ldc_w 923
    //   111: ldc_w 929
    //   114: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   117: pop
    //   118: goto +7860 -> 7978
    //   121: aload 6
    //   123: dup
    //   124: astore 7
    //   126: invokevirtual 931	java/lang/String:hashCode	()I
    //   129: lookupswitch	default:+7824->7953, -1836634766:+203->332, -1835768118:+217->346, -1594290433:+231->360, -1415951151:+245->374, -996573277:+259->388, -944172977:+273->402, -907924844:+287->416, -502897730:+301->430, -502886798:+315->444, -431881575:+329->458, -75245096:+343->472, -75121853:+357->486, 9950744:+371->500, 62209885:+385->514, 246104597:+399->528, 635655024:+413->542, 697674406:+427->556, 1183136939:+441->570, 1500977576:+455->584, 1708941985:+469->598, 1948728474:+483->612, 1949657815:+497->626, 1962369435:+511->640, 1965583067:+525->654
    //   333: iconst_4
    //   334: ldc_w 934
    //   337: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   340: ifne +2272 -> 2612
    //   343: goto +7610 -> 7953
    //   346: aload 7
    //   348: ldc_w 936
    //   351: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   354: ifne +6055 -> 6409
    //   357: goto +7596 -> 7953
    //   360: aload 7
    //   362: ldc_w 938
    //   365: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   368: ifne +2395 -> 2763
    //   371: goto +7582 -> 7953
    //   374: aload 7
    //   376: ldc_w 940
    //   379: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   382: ifne +2545 -> 2927
    //   385: goto +7568 -> 7953
    //   388: aload 7
    //   390: ldc_w 942
    //   393: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   396: ifne +5771 -> 6167
    //   399: goto +7554 -> 7953
    //   402: aload 7
    //   404: ldc_w 944
    //   407: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   410: ifne +6074 -> 6484
    //   413: goto +7540 -> 7953
    //   416: aload 7
    //   418: ldc_w 946
    //   421: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   424: ifne +1303 -> 1727
    //   427: goto +7526 -> 7953
    //   430: aload 7
    //   432: ldc_w 948
    //   435: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   438: ifne +3080 -> 3518
    //   441: goto +7512 -> 7953
    //   444: aload 7
    //   446: ldc_w 950
    //   449: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   452: ifne +3260 -> 3712
    //   455: goto +7498 -> 7953
    //   458: aload 7
    //   460: ldc_w 952
    //   463: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   466: ifne +4037 -> 4503
    //   469: goto +7484 -> 7953
    //   472: aload 7
    //   474: ldc_w 954
    //   477: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   480: ifne +4789 -> 5269
    //   483: goto +7470 -> 7953
    //   486: aload 7
    //   488: ldc_w 956
    //   491: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   494: ifne +5373 -> 5867
    //   497: goto +7456 -> 7953
    //   500: aload 7
    //   502: ldc_w 958
    //   505: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   508: ifne +6742 -> 7250
    //   511: goto +7442 -> 7953
    //   514: aload 7
    //   516: ldc_w 960
    //   519: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   522: ifne +1029 -> 1551
    //   525: goto +7428 -> 7953
    //   528: aload 7
    //   530: ldc_w 962
    //   533: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   536: ifne +6204 -> 6740
    //   539: goto +7414 -> 7953
    //   542: aload 7
    //   544: ldc_w 964
    //   547: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   550: ifne +4686 -> 5236
    //   553: goto +7400 -> 7953
    //   556: aload 7
    //   558: ldc_w 966
    //   561: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   564: ifne +3269 -> 3833
    //   567: goto +7386 -> 7953
    //   570: aload 7
    //   572: ldc_w 968
    //   575: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   578: ifne +1599 -> 2177
    //   581: goto +7372 -> 7953
    //   584: aload 7
    //   586: ldc_w 970
    //   589: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   592: ifne +5296 -> 5888
    //   595: goto +7358 -> 7953
    //   598: aload 7
    //   600: ldc_w 972
    //   603: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   606: ifne +62 -> 668
    //   609: goto +7344 -> 7953
    //   612: aload 7
    //   614: ldc_w 974
    //   617: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   620: ifne +2683 -> 3303
    //   623: goto +7330 -> 7953
    //   626: aload 7
    //   628: ldc_w 976
    //   631: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   634: ifne +3417 -> 4051
    //   637: goto +7316 -> 7953
    //   640: aload 7
    //   642: ldc_w 978
    //   645: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   648: ifne +4846 -> 5494
    //   651: goto +7302 -> 7953
    //   654: aload 7
    //   656: ldc_w 980
    //   659: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   662: ifne +4928 -> 5590
    //   665: goto +7288 -> 7953
    //   668: aload_1
    //   669: ldc_w 982
    //   672: invokeinterface 906 2 0
    //   677: astore 8
    //   679: aload_1
    //   680: ldc_w 984
    //   683: invokeinterface 906 2 0
    //   688: astore 9
    //   690: aload_1
    //   691: ldc_w 986
    //   694: invokeinterface 906 2 0
    //   699: astore 10
    //   701: aload_1
    //   702: ldc_w 988
    //   705: invokeinterface 906 2 0
    //   710: astore 11
    //   712: aload_1
    //   713: ldc_w 990
    //   716: invokeinterface 906 2 0
    //   721: astore 12
    //   723: aload_1
    //   724: ldc_w 992
    //   727: invokeinterface 906 2 0
    //   732: astore 13
    //   734: aload 8
    //   736: ifnonnull +31 -> 767
    //   739: aload 5
    //   741: ldc_w 920
    //   744: iconst_3
    //   745: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   748: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   751: pop
    //   752: aload 5
    //   754: ldc_w 923
    //   757: ldc_w 994
    //   760: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   763: pop
    //   764: goto +7214 -> 7978
    //   767: aload 9
    //   769: ifnonnull +31 -> 800
    //   772: aload 5
    //   774: ldc_w 920
    //   777: iconst_3
    //   778: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   781: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   784: pop
    //   785: aload 5
    //   787: ldc_w 923
    //   790: ldc_w 996
    //   793: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   796: pop
    //   797: goto +7181 -> 7978
    //   800: aload 10
    //   802: ifnonnull +31 -> 833
    //   805: aload 5
    //   807: ldc_w 920
    //   810: iconst_3
    //   811: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   814: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   817: pop
    //   818: aload 5
    //   820: ldc_w 923
    //   823: ldc_w 998
    //   826: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   829: pop
    //   830: goto +7148 -> 7978
    //   833: aload 11
    //   835: ifnonnull +31 -> 866
    //   838: aload 5
    //   840: ldc_w 920
    //   843: iconst_3
    //   844: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   847: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   850: pop
    //   851: aload 5
    //   853: ldc_w 923
    //   856: ldc_w 1000
    //   859: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   862: pop
    //   863: goto +7115 -> 7978
    //   866: aload 12
    //   868: ifnonnull +31 -> 899
    //   871: aload 5
    //   873: ldc_w 920
    //   876: iconst_3
    //   877: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   880: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   883: pop
    //   884: aload 5
    //   886: ldc_w 923
    //   889: ldc_w 1002
    //   892: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   895: pop
    //   896: goto +7082 -> 7978
    //   899: aload 9
    //   901: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   904: astore 9
    //   906: aload 9
    //   908: invokevirtual 222	java/lang/String:length	()I
    //   911: ifeq +13 -> 924
    //   914: aload 9
    //   916: invokevirtual 222	java/lang/String:length	()I
    //   919: bipush 100
    //   921: if_icmple +31 -> 952
    //   924: aload 5
    //   926: ldc_w 920
    //   929: iconst_4
    //   930: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   933: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   936: pop
    //   937: aload 5
    //   939: ldc_w 923
    //   942: ldc_w 1004
    //   945: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   948: pop
    //   949: goto +7029 -> 7978
    //   952: aload 9
    //   954: invokevirtual 1006	java/lang/String:toLowerCase	()Ljava/lang/String;
    //   957: astore 14
    //   959: iconst_0
    //   960: istore 15
    //   962: goto +24 -> 986
    //   965: ldc 32
    //   967: aload 14
    //   969: iload 15
    //   971: invokevirtual 243	java/lang/String:charAt	(I)C
    //   974: invokevirtual 1009	java/lang/String:indexOf	(I)I
    //   977: ifge +6 -> 983
    //   980: goto +16 -> 996
    //   983: iinc 15 1
    //   986: iload 15
    //   988: aload 14
    //   990: invokevirtual 222	java/lang/String:length	()I
    //   993: if_icmplt -28 -> 965
    //   996: iload 15
    //   998: aload 14
    //   1000: invokevirtual 222	java/lang/String:length	()I
    //   1003: if_icmpeq +31 -> 1034
    //   1006: aload 5
    //   1008: ldc_w 920
    //   1011: iconst_4
    //   1012: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1015: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1018: pop
    //   1019: aload 5
    //   1021: ldc_w 923
    //   1024: ldc_w 1013
    //   1027: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1030: pop
    //   1031: goto +6947 -> 7978
    //   1034: aload 10
    //   1036: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   1039: astore 10
    //   1041: aload 10
    //   1043: invokevirtual 222	java/lang/String:length	()I
    //   1046: sipush 1000
    //   1049: if_icmple +31 -> 1080
    //   1052: aload 5
    //   1054: ldc_w 920
    //   1057: iconst_4
    //   1058: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1061: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1064: pop
    //   1065: aload 5
    //   1067: ldc_w 923
    //   1070: ldc_w 1015
    //   1073: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1076: pop
    //   1077: goto +6901 -> 7978
    //   1080: aload 11
    //   1082: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   1085: istore 16
    //   1087: iload 16
    //   1089: ifle +11 -> 1100
    //   1092: iload 16
    //   1094: ldc_w 780
    //   1097: if_icmplt +11 -> 1108
    //   1100: new 263	java/lang/Exception
    //   1103: dup
    //   1104: invokespecial 1017	java/lang/Exception:<init>	()V
    //   1107: athrow
    //   1108: aload 12
    //   1110: invokestatic 1018	java/lang/Short:parseShort	(Ljava/lang/String;)S
    //   1113: istore 17
    //   1115: iload 17
    //   1117: iconst_1
    //   1118: if_icmpge +11 -> 1129
    //   1121: new 263	java/lang/Exception
    //   1124: dup
    //   1125: invokespecial 1017	java/lang/Exception:<init>	()V
    //   1128: athrow
    //   1129: aload 13
    //   1131: ifnonnull +7 -> 1138
    //   1134: lconst_0
    //   1135: goto +15 -> 1150
    //   1138: new 110	java/math/BigInteger
    //   1141: dup
    //   1142: aload 13
    //   1144: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   1147: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   1150: lstore 18
    //   1152: aload 8
    //   1154: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   1157: astore 20
    //   1159: aload 20
    //   1161: invokestatic 1029	Nxt$Account:getId	([B)J
    //   1164: lstore 21
    //   1166: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   1169: lload 21
    //   1171: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   1174: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1177: checkcast 311	Nxt$Account
    //   1180: astore 23
    //   1182: aload 23
    //   1184: ifnonnull +32 -> 1216
    //   1187: aload 5
    //   1189: ldc_w 920
    //   1192: bipush 6
    //   1194: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1197: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1200: pop
    //   1201: aload 5
    //   1203: ldc_w 923
    //   1206: ldc_w 1032
    //   1209: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1212: pop
    //   1213: goto +6765 -> 7978
    //   1216: iload 16
    //   1218: i2l
    //   1219: ldc2_w 1034
    //   1222: lmul
    //   1223: aload 23
    //   1225: getfield 318	Nxt$Account:unconfirmedBalance	J
    //   1228: lcmp
    //   1229: ifle +32 -> 1261
    //   1232: aload 5
    //   1234: ldc_w 920
    //   1237: bipush 6
    //   1239: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1242: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1245: pop
    //   1246: aload 5
    //   1248: ldc_w 923
    //   1251: ldc_w 1032
    //   1254: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1257: pop
    //   1258: goto +6720 -> 7978
    //   1261: getstatic 140	Nxt:aliases	Ljava/util/HashMap;
    //   1264: dup
    //   1265: astore 25
    //   1267: monitorenter
    //   1268: getstatic 140	Nxt:aliases	Ljava/util/HashMap;
    //   1271: aload 14
    //   1273: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   1276: checkcast 1036	Nxt$Alias
    //   1279: astore 24
    //   1281: aload 25
    //   1283: monitorexit
    //   1284: goto +7 -> 1291
    //   1287: aload 25
    //   1289: monitorexit
    //   1290: athrow
    //   1291: aload 24
    //   1293: ifnull +63 -> 1356
    //   1296: aload 24
    //   1298: getfield 1038	Nxt$Alias:account	LNxt$Account;
    //   1301: aload 23
    //   1303: if_acmpeq +53 -> 1356
    //   1306: aload 5
    //   1308: ldc_w 920
    //   1311: bipush 8
    //   1313: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1316: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1319: pop
    //   1320: aload 5
    //   1322: ldc_w 923
    //   1325: new 192	java/lang/StringBuilder
    //   1328: dup
    //   1329: ldc_w 379
    //   1332: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   1335: aload 9
    //   1337: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1340: ldc_w 1039
    //   1343: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   1346: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   1349: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1352: pop
    //   1353: goto +6625 -> 7978
    //   1356: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   1359: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   1362: istore 25
    //   1364: new 524	Nxt$Transaction
    //   1367: dup
    //   1368: iconst_1
    //   1369: iconst_1
    //   1370: iload 25
    //   1372: iload 17
    //   1374: aload 20
    //   1376: ldc2_w 15
    //   1379: iconst_0
    //   1380: iload 16
    //   1382: lload 18
    //   1384: bipush 64
    //   1386: newarray byte
    //   1388: invokespecial 731	Nxt$Transaction:<init>	(BBIS[BJIIJ[B)V
    //   1391: astore 26
    //   1393: aload 26
    //   1395: new 1046	Nxt$Transaction$MessagingAliasAssignmentAttachment
    //   1398: dup
    //   1399: aload 9
    //   1401: aload 10
    //   1403: invokespecial 1048	Nxt$Transaction$MessagingAliasAssignmentAttachment:<init>	(Ljava/lang/String;Ljava/lang/String;)V
    //   1406: putfield 1051	Nxt$Transaction:attachment	LNxt$Transaction$Attachment;
    //   1409: aload 26
    //   1411: aload 8
    //   1413: invokevirtual 1055	Nxt$Transaction:sign	(Ljava/lang/String;)V
    //   1416: new 911	org/json/simple/JSONObject
    //   1419: dup
    //   1420: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   1423: astore 27
    //   1425: aload 27
    //   1427: ldc_w 927
    //   1430: ldc_w 1058
    //   1433: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1436: pop
    //   1437: new 1060	org/json/simple/JSONArray
    //   1440: dup
    //   1441: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   1444: astore 28
    //   1446: aload 28
    //   1448: aload 26
    //   1450: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   1453: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   1456: pop
    //   1457: aload 27
    //   1459: ldc_w 1068
    //   1462: aload 28
    //   1464: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1467: pop
    //   1468: aload 27
    //   1470: invokestatic 1069	Nxt$Peer:sendToAllPeers	(Lorg/json/simple/JSONObject;)V
    //   1473: aload 5
    //   1475: ldc_w 1073
    //   1478: aload 26
    //   1480: invokevirtual 734	Nxt$Transaction:getId	()J
    //   1483: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   1486: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1489: pop
    //   1490: goto +6488 -> 7978
    //   1493: pop
    //   1494: aload 5
    //   1496: ldc_w 920
    //   1499: iconst_4
    //   1500: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1503: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1506: pop
    //   1507: aload 5
    //   1509: ldc_w 923
    //   1512: ldc_w 1077
    //   1515: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1518: pop
    //   1519: goto +6459 -> 7978
    //   1522: pop
    //   1523: aload 5
    //   1525: ldc_w 920
    //   1528: iconst_4
    //   1529: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1532: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1535: pop
    //   1536: aload 5
    //   1538: ldc_w 923
    //   1541: ldc_w 1079
    //   1544: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1547: pop
    //   1548: goto +6430 -> 7978
    //   1551: aload_1
    //   1552: ldc_w 1081
    //   1555: invokeinterface 906 2 0
    //   1560: astore 8
    //   1562: aload 8
    //   1564: ifnonnull +31 -> 1595
    //   1567: aload 5
    //   1569: ldc_w 920
    //   1572: iconst_3
    //   1573: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1576: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1579: pop
    //   1580: aload 5
    //   1582: ldc_w 923
    //   1585: ldc_w 1083
    //   1588: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1591: pop
    //   1592: goto +6386 -> 7978
    //   1595: aload 8
    //   1597: invokestatic 1085	Nxt:convert	(Ljava/lang/String;)[B
    //   1600: invokestatic 1087	java/nio/ByteBuffer:wrap	([B)Ljava/nio/ByteBuffer;
    //   1603: astore 9
    //   1605: aload 9
    //   1607: getstatic 1093	java/nio/ByteOrder:LITTLE_ENDIAN	Ljava/nio/ByteOrder;
    //   1610: invokevirtual 1099	java/nio/ByteBuffer:order	(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;
    //   1613: pop
    //   1614: aload 9
    //   1616: invokestatic 1103	Nxt$Transaction:getTransaction	(Ljava/nio/ByteBuffer;)LNxt$Transaction;
    //   1619: astore 10
    //   1621: new 911	org/json/simple/JSONObject
    //   1624: dup
    //   1625: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   1628: astore 11
    //   1630: aload 11
    //   1632: ldc_w 927
    //   1635: ldc_w 1058
    //   1638: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1641: pop
    //   1642: new 1060	org/json/simple/JSONArray
    //   1645: dup
    //   1646: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   1649: astore 12
    //   1651: aload 12
    //   1653: aload 10
    //   1655: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   1658: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   1661: pop
    //   1662: aload 11
    //   1664: ldc_w 1068
    //   1667: aload 12
    //   1669: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1672: pop
    //   1673: aload 11
    //   1675: invokestatic 1069	Nxt$Peer:sendToAllPeers	(Lorg/json/simple/JSONObject;)V
    //   1678: aload 5
    //   1680: ldc_w 1073
    //   1683: aload 10
    //   1685: invokevirtual 734	Nxt$Transaction:getId	()J
    //   1688: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   1691: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1694: pop
    //   1695: goto +6283 -> 7978
    //   1698: pop
    //   1699: aload 5
    //   1701: ldc_w 920
    //   1704: iconst_4
    //   1705: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1708: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1711: pop
    //   1712: aload 5
    //   1714: ldc_w 923
    //   1717: ldc_w 1106
    //   1720: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1723: pop
    //   1724: goto +6254 -> 7978
    //   1727: aload_1
    //   1728: ldc_w 1108
    //   1731: invokeinterface 906 2 0
    //   1736: astore 8
    //   1738: aload 8
    //   1740: ifnonnull +31 -> 1771
    //   1743: aload 5
    //   1745: ldc_w 920
    //   1748: iconst_3
    //   1749: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1752: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1755: pop
    //   1756: aload 5
    //   1758: ldc_w 923
    //   1761: ldc_w 1110
    //   1764: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1767: pop
    //   1768: goto +6210 -> 7978
    //   1771: aload 8
    //   1773: invokestatic 1085	Nxt:convert	(Ljava/lang/String;)[B
    //   1776: astore 9
    //   1778: aload 9
    //   1780: invokestatic 1087	java/nio/ByteBuffer:wrap	([B)Ljava/nio/ByteBuffer;
    //   1783: astore 10
    //   1785: aload 10
    //   1787: getstatic 1093	java/nio/ByteOrder:LITTLE_ENDIAN	Ljava/nio/ByteOrder;
    //   1790: invokevirtual 1099	java/nio/ByteBuffer:order	(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;
    //   1793: pop
    //   1794: bipush 32
    //   1796: newarray byte
    //   1798: astore 11
    //   1800: aload 10
    //   1802: aload 11
    //   1804: invokevirtual 1112	java/nio/ByteBuffer:get	([B)Ljava/nio/ByteBuffer;
    //   1807: pop
    //   1808: aload 10
    //   1810: invokevirtual 1114	java/nio/ByteBuffer:getShort	()S
    //   1813: istore 12
    //   1815: iload 12
    //   1817: newarray byte
    //   1819: astore 13
    //   1821: aload 10
    //   1823: aload 13
    //   1825: invokevirtual 1112	java/nio/ByteBuffer:get	([B)Ljava/nio/ByteBuffer;
    //   1828: pop
    //   1829: new 223	java/lang/String
    //   1832: dup
    //   1833: aload 13
    //   1835: ldc_w 1118
    //   1838: invokespecial 1120	java/lang/String:<init>	([BLjava/lang/String;)V
    //   1841: astore 14
    //   1843: aload 10
    //   1845: invokevirtual 1123	java/nio/ByteBuffer:getInt	()I
    //   1848: istore 15
    //   1850: aload 10
    //   1852: invokevirtual 1123	java/nio/ByteBuffer:getInt	()I
    //   1855: istore 16
    //   1857: aload 10
    //   1859: invokevirtual 1126	java/nio/ByteBuffer:get	()B
    //   1862: pop
    //   1863: bipush 64
    //   1865: newarray byte
    //   1867: astore 17
    //   1869: aload 10
    //   1871: aload 17
    //   1873: invokevirtual 1112	java/nio/ByteBuffer:get	([B)Ljava/nio/ByteBuffer;
    //   1876: pop
    //   1877: aload 5
    //   1879: ldc_w 1129
    //   1882: aload 11
    //   1884: invokestatic 1029	Nxt$Account:getId	([B)J
    //   1887: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   1890: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1893: pop
    //   1894: aload 5
    //   1896: ldc_w 1130
    //   1899: aload 14
    //   1901: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1904: pop
    //   1905: aload 5
    //   1907: ldc_w 1132
    //   1910: iload 15
    //   1912: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   1915: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1918: pop
    //   1919: iload 16
    //   1921: sipush 10000
    //   1924: idiv
    //   1925: istore 18
    //   1927: iload 16
    //   1929: sipush 10000
    //   1932: irem
    //   1933: bipush 100
    //   1935: idiv
    //   1936: istore 19
    //   1938: iload 16
    //   1940: bipush 100
    //   1942: irem
    //   1943: istore 20
    //   1945: aload 5
    //   1947: ldc_w 1134
    //   1950: new 192	java/lang/StringBuilder
    //   1953: dup
    //   1954: iload 18
    //   1956: bipush 10
    //   1958: if_icmpge +9 -> 1967
    //   1961: ldc_w 1136
    //   1964: goto +33 -> 1997
    //   1967: iload 18
    //   1969: bipush 100
    //   1971: if_icmpge +9 -> 1980
    //   1974: ldc_w 1138
    //   1977: goto +20 -> 1997
    //   1980: iload 18
    //   1982: sipush 1000
    //   1985: if_icmpge +9 -> 1994
    //   1988: ldc_w 1140
    //   1991: goto +6 -> 1997
    //   1994: ldc_w 1142
    //   1997: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   2000: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   2003: iload 18
    //   2005: invokevirtual 1147	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   2008: ldc_w 1150
    //   2011: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   2014: iload 19
    //   2016: bipush 10
    //   2018: if_icmpge +9 -> 2027
    //   2021: ldc_w 1140
    //   2024: goto +6 -> 2030
    //   2027: ldc_w 1142
    //   2030: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   2033: iload 19
    //   2035: invokevirtual 1147	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   2038: ldc_w 1150
    //   2041: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   2044: iload 20
    //   2046: bipush 10
    //   2048: if_icmpge +9 -> 2057
    //   2051: ldc_w 1140
    //   2054: goto +6 -> 2060
    //   2057: ldc_w 1142
    //   2060: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   2063: iload 20
    //   2065: invokevirtual 1147	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   2068: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   2071: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2074: pop
    //   2075: aload 9
    //   2077: arraylength
    //   2078: bipush 64
    //   2080: isub
    //   2081: newarray byte
    //   2083: astore 21
    //   2085: aload 9
    //   2087: iconst_0
    //   2088: aload 21
    //   2090: iconst_0
    //   2091: aload 21
    //   2093: arraylength
    //   2094: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   2097: aload 5
    //   2099: ldc_w 1156
    //   2102: aload 14
    //   2104: invokevirtual 222	java/lang/String:length	()I
    //   2107: bipush 100
    //   2109: if_icmpgt +16 -> 2125
    //   2112: iload 15
    //   2114: ifle +11 -> 2125
    //   2117: iload 15
    //   2119: ldc_w 780
    //   2122: if_icmple +7 -> 2129
    //   2125: iconst_0
    //   2126: goto +12 -> 2138
    //   2129: aload 17
    //   2131: aload 21
    //   2133: aload 11
    //   2135: invokestatic 1158	Nxt$Crypto:verify	([B[B[B)Z
    //   2138: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   2141: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2144: pop
    //   2145: goto +5833 -> 7978
    //   2148: pop
    //   2149: aload 5
    //   2151: ldc_w 920
    //   2154: iconst_4
    //   2155: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2158: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2161: pop
    //   2162: aload 5
    //   2164: ldc_w 923
    //   2167: ldc_w 1165
    //   2170: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2173: pop
    //   2174: goto +5804 -> 7978
    //   2177: aload_1
    //   2178: ldc_w 1167
    //   2181: invokeinterface 906 2 0
    //   2186: astore 8
    //   2188: aload_1
    //   2189: ldc_w 1169
    //   2192: invokeinterface 906 2 0
    //   2197: astore 9
    //   2199: aload 8
    //   2201: ifnonnull +31 -> 2232
    //   2204: aload 5
    //   2206: ldc_w 920
    //   2209: iconst_3
    //   2210: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2213: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2216: pop
    //   2217: aload 5
    //   2219: ldc_w 923
    //   2222: ldc_w 1171
    //   2225: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2228: pop
    //   2229: goto +5749 -> 7978
    //   2232: aload 9
    //   2234: ifnonnull +31 -> 2265
    //   2237: aload 5
    //   2239: ldc_w 920
    //   2242: iconst_3
    //   2243: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2246: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2249: pop
    //   2250: aload 5
    //   2252: ldc_w 923
    //   2255: ldc_w 1173
    //   2258: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2261: pop
    //   2262: goto +5716 -> 7978
    //   2265: aload 8
    //   2267: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   2270: ldc_w 1118
    //   2273: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   2276: astore 10
    //   2278: bipush 100
    //   2280: newarray byte
    //   2282: astore 11
    //   2284: iconst_0
    //   2285: istore 12
    //   2287: iconst_0
    //   2288: istore 13
    //   2290: goto +93 -> 2383
    //   2293: aload 9
    //   2295: iload 12
    //   2297: iload 12
    //   2299: bipush 8
    //   2301: iadd
    //   2302: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   2305: bipush 32
    //   2307: invokestatic 1177	java/lang/Long:parseLong	(Ljava/lang/String;I)J
    //   2310: lstore 14
    //   2312: aload 11
    //   2314: iload 13
    //   2316: lload 14
    //   2318: l2i
    //   2319: i2b
    //   2320: bastore
    //   2321: aload 11
    //   2323: iload 13
    //   2325: iconst_1
    //   2326: iadd
    //   2327: lload 14
    //   2329: bipush 8
    //   2331: lshr
    //   2332: l2i
    //   2333: i2b
    //   2334: bastore
    //   2335: aload 11
    //   2337: iload 13
    //   2339: iconst_2
    //   2340: iadd
    //   2341: lload 14
    //   2343: bipush 16
    //   2345: lshr
    //   2346: l2i
    //   2347: i2b
    //   2348: bastore
    //   2349: aload 11
    //   2351: iload 13
    //   2353: iconst_3
    //   2354: iadd
    //   2355: lload 14
    //   2357: bipush 24
    //   2359: lshr
    //   2360: l2i
    //   2361: i2b
    //   2362: bastore
    //   2363: aload 11
    //   2365: iload 13
    //   2367: iconst_4
    //   2368: iadd
    //   2369: lload 14
    //   2371: bipush 32
    //   2373: lshr
    //   2374: l2i
    //   2375: i2b
    //   2376: bastore
    //   2377: iinc 12 8
    //   2380: iinc 13 5
    //   2383: iload 12
    //   2385: aload 9
    //   2387: invokevirtual 222	java/lang/String:length	()I
    //   2390: if_icmplt -97 -> 2293
    //   2393: goto +4 -> 2397
    //   2396: pop
    //   2397: iload 12
    //   2399: sipush 160
    //   2402: if_icmpeq +31 -> 2433
    //   2405: aload 5
    //   2407: ldc_w 920
    //   2410: iconst_4
    //   2411: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2414: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2417: pop
    //   2418: aload 5
    //   2420: ldc_w 923
    //   2423: ldc_w 1181
    //   2426: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2429: pop
    //   2430: goto +5548 -> 7978
    //   2433: bipush 32
    //   2435: newarray byte
    //   2437: astore 14
    //   2439: aload 11
    //   2441: iconst_0
    //   2442: aload 14
    //   2444: iconst_0
    //   2445: bipush 32
    //   2447: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   2450: aload 11
    //   2452: bipush 32
    //   2454: baload
    //   2455: sipush 255
    //   2458: iand
    //   2459: aload 11
    //   2461: bipush 33
    //   2463: baload
    //   2464: sipush 255
    //   2467: iand
    //   2468: bipush 8
    //   2470: ishl
    //   2471: ior
    //   2472: aload 11
    //   2474: bipush 34
    //   2476: baload
    //   2477: sipush 255
    //   2480: iand
    //   2481: bipush 16
    //   2483: ishl
    //   2484: ior
    //   2485: aload 11
    //   2487: bipush 35
    //   2489: baload
    //   2490: sipush 255
    //   2493: iand
    //   2494: bipush 24
    //   2496: ishl
    //   2497: ior
    //   2498: istore 15
    //   2500: bipush 64
    //   2502: newarray byte
    //   2504: astore 16
    //   2506: aload 11
    //   2508: bipush 36
    //   2510: aload 16
    //   2512: iconst_0
    //   2513: bipush 64
    //   2515: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   2518: aload 10
    //   2520: arraylength
    //   2521: bipush 36
    //   2523: iadd
    //   2524: newarray byte
    //   2526: astore 17
    //   2528: aload 10
    //   2530: iconst_0
    //   2531: aload 17
    //   2533: iconst_0
    //   2534: aload 10
    //   2536: arraylength
    //   2537: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   2540: aload 11
    //   2542: iconst_0
    //   2543: aload 17
    //   2545: aload 10
    //   2547: arraylength
    //   2548: bipush 36
    //   2550: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   2553: aload 16
    //   2555: aload 17
    //   2557: aload 14
    //   2559: invokestatic 1158	Nxt$Crypto:verify	([B[B[B)Z
    //   2562: istore 18
    //   2564: aload 5
    //   2566: ldc_w 1129
    //   2569: aload 14
    //   2571: invokestatic 1029	Nxt$Account:getId	([B)J
    //   2574: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   2577: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2580: pop
    //   2581: aload 5
    //   2583: ldc_w 1183
    //   2586: iload 15
    //   2588: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2591: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2594: pop
    //   2595: aload 5
    //   2597: ldc_w 1156
    //   2600: iload 18
    //   2602: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   2605: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2608: pop
    //   2609: goto +5369 -> 7978
    //   2612: aload_1
    //   2613: ldc_w 982
    //   2616: invokeinterface 906 2 0
    //   2621: astore 8
    //   2623: aload 8
    //   2625: ifnonnull +31 -> 2656
    //   2628: aload 5
    //   2630: ldc_w 920
    //   2633: iconst_3
    //   2634: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2637: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2640: pop
    //   2641: aload 5
    //   2643: ldc_w 923
    //   2646: ldc_w 994
    //   2649: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2652: pop
    //   2653: goto +5325 -> 7978
    //   2656: ldc_w 807
    //   2659: invokestatic 809	java/security/MessageDigest:getInstance	(Ljava/lang/String;)Ljava/security/MessageDigest;
    //   2662: aload 8
    //   2664: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   2667: invokevirtual 1185	java/security/MessageDigest:digest	([B)[B
    //   2670: astore 9
    //   2672: new 110	java/math/BigInteger
    //   2675: dup
    //   2676: iconst_1
    //   2677: bipush 8
    //   2679: newarray byte
    //   2681: dup
    //   2682: iconst_0
    //   2683: aload 9
    //   2685: bipush 7
    //   2687: baload
    //   2688: bastore
    //   2689: dup
    //   2690: iconst_1
    //   2691: aload 9
    //   2693: bipush 6
    //   2695: baload
    //   2696: bastore
    //   2697: dup
    //   2698: iconst_2
    //   2699: aload 9
    //   2701: iconst_5
    //   2702: baload
    //   2703: bastore
    //   2704: dup
    //   2705: iconst_3
    //   2706: aload 9
    //   2708: iconst_4
    //   2709: baload
    //   2710: bastore
    //   2711: dup
    //   2712: iconst_4
    //   2713: aload 9
    //   2715: iconst_3
    //   2716: baload
    //   2717: bastore
    //   2718: dup
    //   2719: iconst_5
    //   2720: aload 9
    //   2722: iconst_2
    //   2723: baload
    //   2724: bastore
    //   2725: dup
    //   2726: bipush 6
    //   2728: aload 9
    //   2730: iconst_1
    //   2731: baload
    //   2732: bastore
    //   2733: dup
    //   2734: bipush 7
    //   2736: aload 9
    //   2738: iconst_0
    //   2739: baload
    //   2740: bastore
    //   2741: invokespecial 1188	java/math/BigInteger:<init>	(I[B)V
    //   2744: astore 10
    //   2746: aload 5
    //   2748: ldc_w 1191
    //   2751: aload 10
    //   2753: invokevirtual 259	java/math/BigInteger:toString	()Ljava/lang/String;
    //   2756: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2759: pop
    //   2760: goto +5218 -> 7978
    //   2763: aload_1
    //   2764: ldc_w 1129
    //   2767: invokeinterface 906 2 0
    //   2772: astore 8
    //   2774: aload 8
    //   2776: ifnonnull +31 -> 2807
    //   2779: aload 5
    //   2781: ldc_w 920
    //   2784: iconst_3
    //   2785: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2788: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2791: pop
    //   2792: aload 5
    //   2794: ldc_w 923
    //   2797: ldc_w 1193
    //   2800: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2803: pop
    //   2804: goto +5174 -> 7978
    //   2807: new 110	java/math/BigInteger
    //   2810: dup
    //   2811: aload 8
    //   2813: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   2816: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   2819: lstore 9
    //   2821: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   2824: lload 9
    //   2826: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   2829: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   2832: checkcast 311	Nxt$Account
    //   2835: astore 11
    //   2837: aload 11
    //   2839: ifnonnull +31 -> 2870
    //   2842: aload 5
    //   2844: ldc_w 920
    //   2847: iconst_5
    //   2848: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2851: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2854: pop
    //   2855: aload 5
    //   2857: ldc_w 923
    //   2860: ldc_w 1195
    //   2863: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2866: pop
    //   2867: goto +5111 -> 7978
    //   2870: aload 11
    //   2872: getfield 1197	Nxt$Account:publicKey	[B
    //   2875: ifnull +5103 -> 7978
    //   2878: aload 5
    //   2880: ldc_w 1200
    //   2883: aload 11
    //   2885: getfield 1197	Nxt$Account:publicKey	[B
    //   2888: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   2891: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2894: pop
    //   2895: goto +5083 -> 7978
    //   2898: pop
    //   2899: aload 5
    //   2901: ldc_w 920
    //   2904: iconst_4
    //   2905: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2908: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2911: pop
    //   2912: aload 5
    //   2914: ldc_w 923
    //   2917: ldc_w 1203
    //   2920: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2923: pop
    //   2924: goto +5054 -> 7978
    //   2927: aload_1
    //   2928: ldc_w 1129
    //   2931: invokeinterface 906 2 0
    //   2936: astore 8
    //   2938: aload_1
    //   2939: ldc_w 1183
    //   2942: invokeinterface 906 2 0
    //   2947: astore 9
    //   2949: aload 8
    //   2951: ifnonnull +31 -> 2982
    //   2954: aload 5
    //   2956: ldc_w 920
    //   2959: iconst_3
    //   2960: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2963: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2966: pop
    //   2967: aload 5
    //   2969: ldc_w 923
    //   2972: ldc_w 1193
    //   2975: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2978: pop
    //   2979: goto +4999 -> 7978
    //   2982: aload 9
    //   2984: ifnonnull +31 -> 3015
    //   2987: aload 5
    //   2989: ldc_w 920
    //   2992: iconst_3
    //   2993: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   2996: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2999: pop
    //   3000: aload 5
    //   3002: ldc_w 923
    //   3005: ldc_w 1205
    //   3008: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3011: pop
    //   3012: goto +4966 -> 7978
    //   3015: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   3018: new 110	java/math/BigInteger
    //   3021: dup
    //   3022: aload 8
    //   3024: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   3027: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   3030: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3033: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   3036: checkcast 311	Nxt$Account
    //   3039: astore 10
    //   3041: aload 10
    //   3043: ifnonnull +31 -> 3074
    //   3046: aload 5
    //   3048: ldc_w 920
    //   3051: iconst_5
    //   3052: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3055: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3058: pop
    //   3059: aload 5
    //   3061: ldc_w 923
    //   3064: ldc_w 1195
    //   3067: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3070: pop
    //   3071: goto +4907 -> 7978
    //   3074: aload 9
    //   3076: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   3079: istore 11
    //   3081: iload 11
    //   3083: ifge +11 -> 3094
    //   3086: new 263	java/lang/Exception
    //   3089: dup
    //   3090: invokespecial 1017	java/lang/Exception:<init>	()V
    //   3093: athrow
    //   3094: new 1060	org/json/simple/JSONArray
    //   3097: dup
    //   3098: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   3101: astore 12
    //   3103: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   3106: invokevirtual 1207	java/util/HashMap:entrySet	()Ljava/util/Set;
    //   3109: invokeinterface 797 1 0
    //   3114: astore 14
    //   3116: goto +105 -> 3221
    //   3119: aload 14
    //   3121: invokeinterface 747 1 0
    //   3126: checkcast 1210	java/util/Map$Entry
    //   3129: astore 13
    //   3131: aload 13
    //   3133: invokeinterface 1212 1 0
    //   3138: checkcast 524	Nxt$Transaction
    //   3141: astore 15
    //   3143: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   3146: aload 15
    //   3148: getfield 757	Nxt$Transaction:block	J
    //   3151: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3154: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   3157: checkcast 771	Nxt$Block
    //   3160: getfield 1215	Nxt$Block:timestamp	I
    //   3163: iload 11
    //   3165: if_icmplt +56 -> 3221
    //   3168: aload 15
    //   3170: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   3173: invokestatic 1029	Nxt$Account:getId	([B)J
    //   3176: aload 10
    //   3178: getfield 1220	Nxt$Account:id	J
    //   3181: lcmp
    //   3182: ifeq +17 -> 3199
    //   3185: aload 15
    //   3187: getfield 1221	Nxt$Transaction:recipient	J
    //   3190: aload 10
    //   3192: getfield 1220	Nxt$Account:id	J
    //   3195: lcmp
    //   3196: ifne +25 -> 3221
    //   3199: aload 12
    //   3201: aload 13
    //   3203: invokeinterface 1224 1 0
    //   3208: checkcast 266	java/lang/Long
    //   3211: invokevirtual 800	java/lang/Long:longValue	()J
    //   3214: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   3217: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   3220: pop
    //   3221: aload 14
    //   3223: invokeinterface 760 1 0
    //   3228: ifne -109 -> 3119
    //   3231: aload 5
    //   3233: ldc_w 1227
    //   3236: aload 12
    //   3238: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3241: pop
    //   3242: goto +4736 -> 7978
    //   3245: pop
    //   3246: aload 5
    //   3248: ldc_w 920
    //   3251: iconst_4
    //   3252: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3255: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3258: pop
    //   3259: aload 5
    //   3261: ldc_w 923
    //   3264: ldc_w 1229
    //   3267: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3270: pop
    //   3271: goto +4707 -> 7978
    //   3274: pop
    //   3275: aload 5
    //   3277: ldc_w 920
    //   3280: iconst_4
    //   3281: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3284: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3287: pop
    //   3288: aload 5
    //   3290: ldc_w 923
    //   3293: ldc_w 1203
    //   3296: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3299: pop
    //   3300: goto +4678 -> 7978
    //   3303: aload_1
    //   3304: ldc_w 984
    //   3307: invokeinterface 906 2 0
    //   3312: astore 8
    //   3314: aload 8
    //   3316: ifnonnull +31 -> 3347
    //   3319: aload 5
    //   3321: ldc_w 920
    //   3324: iconst_3
    //   3325: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3328: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3331: pop
    //   3332: aload 5
    //   3334: ldc_w 923
    //   3337: ldc_w 996
    //   3340: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3343: pop
    //   3344: goto +4634 -> 7978
    //   3347: new 110	java/math/BigInteger
    //   3350: dup
    //   3351: aload 8
    //   3353: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   3356: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   3359: lstore 9
    //   3361: getstatic 142	Nxt:aliasIdToAliasMappings	Ljava/util/HashMap;
    //   3364: lload 9
    //   3366: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3369: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   3372: checkcast 1036	Nxt$Alias
    //   3375: astore 11
    //   3377: aload 11
    //   3379: ifnonnull +31 -> 3410
    //   3382: aload 5
    //   3384: ldc_w 920
    //   3387: iconst_5
    //   3388: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3391: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3394: pop
    //   3395: aload 5
    //   3397: ldc_w 923
    //   3400: ldc_w 1231
    //   3403: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3406: pop
    //   3407: goto +4571 -> 7978
    //   3410: aload 5
    //   3412: ldc_w 1129
    //   3415: aload 11
    //   3417: getfield 1038	Nxt$Alias:account	LNxt$Account;
    //   3420: getfield 1220	Nxt$Account:id	J
    //   3423: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   3426: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3429: pop
    //   3430: aload 5
    //   3432: ldc_w 984
    //   3435: aload 11
    //   3437: getfield 1233	Nxt$Alias:alias	Ljava/lang/String;
    //   3440: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3443: pop
    //   3444: aload 11
    //   3446: getfield 1235	Nxt$Alias:uri	Ljava/lang/String;
    //   3449: invokevirtual 222	java/lang/String:length	()I
    //   3452: ifle +17 -> 3469
    //   3455: aload 5
    //   3457: ldc_w 986
    //   3460: aload 11
    //   3462: getfield 1235	Nxt$Alias:uri	Ljava/lang/String;
    //   3465: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3468: pop
    //   3469: aload 5
    //   3471: ldc_w 1183
    //   3474: aload 11
    //   3476: getfield 1237	Nxt$Alias:timestamp	I
    //   3479: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3482: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3485: pop
    //   3486: goto +4492 -> 7978
    //   3489: pop
    //   3490: aload 5
    //   3492: ldc_w 920
    //   3495: iconst_4
    //   3496: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3499: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3502: pop
    //   3503: aload 5
    //   3505: ldc_w 923
    //   3508: ldc_w 1238
    //   3511: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3514: pop
    //   3515: goto +4463 -> 7978
    //   3518: aload_1
    //   3519: ldc_w 1183
    //   3522: invokeinterface 906 2 0
    //   3527: astore 8
    //   3529: aload 8
    //   3531: ifnonnull +31 -> 3562
    //   3534: aload 5
    //   3536: ldc_w 920
    //   3539: iconst_3
    //   3540: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3543: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3546: pop
    //   3547: aload 5
    //   3549: ldc_w 923
    //   3552: ldc_w 1205
    //   3555: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3558: pop
    //   3559: goto +4419 -> 7978
    //   3562: aload 8
    //   3564: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   3567: istore 9
    //   3569: iload 9
    //   3571: ifge +11 -> 3582
    //   3574: new 263	java/lang/Exception
    //   3577: dup
    //   3578: invokespecial 1017	java/lang/Exception:<init>	()V
    //   3581: athrow
    //   3582: new 1060	org/json/simple/JSONArray
    //   3585: dup
    //   3586: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   3589: astore 10
    //   3591: getstatic 142	Nxt:aliasIdToAliasMappings	Ljava/util/HashMap;
    //   3594: invokevirtual 1207	java/util/HashMap:entrySet	()Ljava/util/Set;
    //   3597: invokeinterface 797 1 0
    //   3602: astore 12
    //   3604: goto +55 -> 3659
    //   3607: aload 12
    //   3609: invokeinterface 747 1 0
    //   3614: checkcast 1210	java/util/Map$Entry
    //   3617: astore 11
    //   3619: aload 11
    //   3621: invokeinterface 1212 1 0
    //   3626: checkcast 1036	Nxt$Alias
    //   3629: getfield 1237	Nxt$Alias:timestamp	I
    //   3632: iload 9
    //   3634: if_icmplt +25 -> 3659
    //   3637: aload 10
    //   3639: aload 11
    //   3641: invokeinterface 1224 1 0
    //   3646: checkcast 266	java/lang/Long
    //   3649: invokevirtual 800	java/lang/Long:longValue	()J
    //   3652: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   3655: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   3658: pop
    //   3659: aload 12
    //   3661: invokeinterface 760 1 0
    //   3666: ifne -59 -> 3607
    //   3669: aload 5
    //   3671: ldc_w 1240
    //   3674: aload 10
    //   3676: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3679: pop
    //   3680: goto +4298 -> 7978
    //   3683: pop
    //   3684: aload 5
    //   3686: ldc_w 920
    //   3689: iconst_4
    //   3690: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3693: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3696: pop
    //   3697: aload 5
    //   3699: ldc_w 923
    //   3702: ldc_w 1229
    //   3705: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3708: pop
    //   3709: goto +4269 -> 7978
    //   3712: aload_1
    //   3713: ldc_w 984
    //   3716: invokeinterface 906 2 0
    //   3721: astore 8
    //   3723: aload 8
    //   3725: ifnonnull +31 -> 3756
    //   3728: aload 5
    //   3730: ldc_w 920
    //   3733: iconst_3
    //   3734: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3737: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3740: pop
    //   3741: aload 5
    //   3743: ldc_w 923
    //   3746: ldc_w 996
    //   3749: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3752: pop
    //   3753: goto +4225 -> 7978
    //   3756: getstatic 140	Nxt:aliases	Ljava/util/HashMap;
    //   3759: aload 8
    //   3761: invokevirtual 1006	java/lang/String:toLowerCase	()Ljava/lang/String;
    //   3764: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   3767: checkcast 1036	Nxt$Alias
    //   3770: astore 9
    //   3772: aload 9
    //   3774: ifnonnull +31 -> 3805
    //   3777: aload 5
    //   3779: ldc_w 920
    //   3782: iconst_5
    //   3783: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3786: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3789: pop
    //   3790: aload 5
    //   3792: ldc_w 923
    //   3795: ldc_w 1231
    //   3798: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3801: pop
    //   3802: goto +4176 -> 7978
    //   3805: aload 9
    //   3807: getfield 1235	Nxt$Alias:uri	Ljava/lang/String;
    //   3810: invokevirtual 222	java/lang/String:length	()I
    //   3813: ifle +4165 -> 7978
    //   3816: aload 5
    //   3818: ldc_w 986
    //   3821: aload 9
    //   3823: getfield 1235	Nxt$Alias:uri	Ljava/lang/String;
    //   3826: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3829: pop
    //   3830: goto +4148 -> 7978
    //   3833: aload_1
    //   3834: ldc_w 1129
    //   3837: invokeinterface 906 2 0
    //   3842: astore 8
    //   3844: aload 8
    //   3846: ifnonnull +31 -> 3877
    //   3849: aload 5
    //   3851: ldc_w 920
    //   3854: iconst_3
    //   3855: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3858: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3861: pop
    //   3862: aload 5
    //   3864: ldc_w 923
    //   3867: ldc_w 1193
    //   3870: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3873: pop
    //   3874: goto +4104 -> 7978
    //   3877: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   3880: new 110	java/math/BigInteger
    //   3883: dup
    //   3884: aload 8
    //   3886: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   3889: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   3892: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3895: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   3898: checkcast 311	Nxt$Account
    //   3901: astore 9
    //   3903: aload 9
    //   3905: ifnonnull +45 -> 3950
    //   3908: aload 5
    //   3910: ldc_w 1242
    //   3913: iconst_0
    //   3914: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3917: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3920: pop
    //   3921: aload 5
    //   3923: ldc_w 1243
    //   3926: iconst_0
    //   3927: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3930: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3933: pop
    //   3934: aload 5
    //   3936: ldc_w 1244
    //   3939: iconst_0
    //   3940: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   3943: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3946: pop
    //   3947: goto +4031 -> 7978
    //   3950: aload 9
    //   3952: dup
    //   3953: astore 10
    //   3955: monitorenter
    //   3956: aload 5
    //   3958: ldc_w 1242
    //   3961: aload 9
    //   3963: getfield 310	Nxt$Account:balance	J
    //   3966: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3969: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3972: pop
    //   3973: aload 5
    //   3975: ldc_w 1243
    //   3978: aload 9
    //   3980: getfield 318	Nxt$Account:unconfirmedBalance	J
    //   3983: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   3986: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   3989: pop
    //   3990: aload 5
    //   3992: ldc_w 1244
    //   3995: aload 9
    //   3997: invokevirtual 1246	Nxt$Account:getEffectiveBalance	()I
    //   4000: i2l
    //   4001: ldc2_w 1034
    //   4004: lmul
    //   4005: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   4008: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4011: pop
    //   4012: aload 10
    //   4014: monitorexit
    //   4015: goto +3963 -> 7978
    //   4018: aload 10
    //   4020: monitorexit
    //   4021: athrow
    //   4022: pop
    //   4023: aload 5
    //   4025: ldc_w 920
    //   4028: iconst_4
    //   4029: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4032: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4035: pop
    //   4036: aload 5
    //   4038: ldc_w 923
    //   4041: ldc_w 1203
    //   4044: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4047: pop
    //   4048: goto +3930 -> 7978
    //   4051: aload_1
    //   4052: ldc_w 1249
    //   4055: invokeinterface 906 2 0
    //   4060: astore 8
    //   4062: aload 8
    //   4064: ifnonnull +31 -> 4095
    //   4067: aload 5
    //   4069: ldc_w 920
    //   4072: iconst_3
    //   4073: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4076: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4079: pop
    //   4080: aload 5
    //   4082: ldc_w 923
    //   4085: ldc_w 1250
    //   4088: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4091: pop
    //   4092: goto +3886 -> 7978
    //   4095: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   4098: new 110	java/math/BigInteger
    //   4101: dup
    //   4102: aload 8
    //   4104: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   4107: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   4110: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   4113: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   4116: checkcast 771	Nxt$Block
    //   4119: astore 9
    //   4121: aload 9
    //   4123: ifnonnull +31 -> 4154
    //   4126: aload 5
    //   4128: ldc_w 920
    //   4131: iconst_5
    //   4132: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4135: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4138: pop
    //   4139: aload 5
    //   4141: ldc_w 923
    //   4144: ldc_w 1252
    //   4147: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4150: pop
    //   4151: goto +3827 -> 7978
    //   4154: aload 5
    //   4156: ldc_w 1254
    //   4159: aload 9
    //   4161: getfield 1255	Nxt$Block:height	I
    //   4164: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4167: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4170: pop
    //   4171: aload 5
    //   4173: ldc_w 1257
    //   4176: aload 9
    //   4178: getfield 1259	Nxt$Block:generatorPublicKey	[B
    //   4181: invokestatic 1029	Nxt$Account:getId	([B)J
    //   4184: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   4187: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4190: pop
    //   4191: aload 5
    //   4193: ldc_w 1183
    //   4196: aload 9
    //   4198: getfield 1215	Nxt$Block:timestamp	I
    //   4201: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4204: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4207: pop
    //   4208: aload 5
    //   4210: ldc_w 1262
    //   4213: aload 9
    //   4215: getfield 787	Nxt$Block:numberOfTransactions	I
    //   4218: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4221: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4224: pop
    //   4225: aload 5
    //   4227: ldc_w 1263
    //   4230: aload 9
    //   4232: getfield 1265	Nxt$Block:totalAmount	I
    //   4235: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4238: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4241: pop
    //   4242: aload 5
    //   4244: ldc_w 1267
    //   4247: aload 9
    //   4249: getfield 1269	Nxt$Block:totalFee	I
    //   4252: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4255: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4258: pop
    //   4259: aload 5
    //   4261: ldc_w 1271
    //   4264: aload 9
    //   4266: getfield 1273	Nxt$Block:payloadLength	I
    //   4269: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4272: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4275: pop
    //   4276: aload 5
    //   4278: ldc_w 1275
    //   4281: aload 9
    //   4283: getfield 1277	Nxt$Block:version	I
    //   4286: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4289: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4292: pop
    //   4293: aload 5
    //   4295: ldc_w 1279
    //   4298: aload 9
    //   4300: getfield 828	Nxt$Block:baseTarget	J
    //   4303: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   4306: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4309: pop
    //   4310: aload 9
    //   4312: getfield 1280	Nxt$Block:previousBlock	J
    //   4315: lconst_0
    //   4316: lcmp
    //   4317: ifeq +20 -> 4337
    //   4320: aload 5
    //   4322: ldc_w 1283
    //   4325: aload 9
    //   4327: getfield 1280	Nxt$Block:previousBlock	J
    //   4330: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   4333: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4336: pop
    //   4337: aload 9
    //   4339: getfield 845	Nxt$Block:nextBlock	J
    //   4342: lconst_0
    //   4343: lcmp
    //   4344: ifeq +20 -> 4364
    //   4347: aload 5
    //   4349: ldc_w 1284
    //   4352: aload 9
    //   4354: getfield 845	Nxt$Block:nextBlock	J
    //   4357: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   4360: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4363: pop
    //   4364: aload 5
    //   4366: ldc_w 1285
    //   4369: aload 9
    //   4371: getfield 825	Nxt$Block:payloadHash	[B
    //   4374: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   4377: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4380: pop
    //   4381: aload 5
    //   4383: ldc_w 1286
    //   4386: aload 9
    //   4388: getfield 1288	Nxt$Block:generationSignature	[B
    //   4391: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   4394: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4397: pop
    //   4398: aload 5
    //   4400: ldc_w 1290
    //   4403: aload 9
    //   4405: getfield 1292	Nxt$Block:blockSignature	[B
    //   4408: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   4411: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4414: pop
    //   4415: new 1060	org/json/simple/JSONArray
    //   4418: dup
    //   4419: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   4422: astore 10
    //   4424: iconst_0
    //   4425: istore 11
    //   4427: goto +23 -> 4450
    //   4430: aload 10
    //   4432: aload 9
    //   4434: getfield 790	Nxt$Block:transactions	[J
    //   4437: iload 11
    //   4439: laload
    //   4440: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   4443: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4446: pop
    //   4447: iinc 11 1
    //   4450: iload 11
    //   4452: aload 9
    //   4454: getfield 787	Nxt$Block:numberOfTransactions	I
    //   4457: if_icmplt -27 -> 4430
    //   4460: aload 5
    //   4462: ldc_w 1068
    //   4465: aload 10
    //   4467: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4470: pop
    //   4471: goto +3507 -> 7978
    //   4474: pop
    //   4475: aload 5
    //   4477: ldc_w 920
    //   4480: iconst_4
    //   4481: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   4484: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4487: pop
    //   4488: aload 5
    //   4490: ldc_w 923
    //   4493: ldc_w 1294
    //   4496: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4499: pop
    //   4500: goto +3478 -> 7978
    //   4503: new 1060	org/json/simple/JSONArray
    //   4506: dup
    //   4507: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   4510: astore 8
    //   4512: new 911	org/json/simple/JSONObject
    //   4515: dup
    //   4516: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4519: astore 9
    //   4521: aload 9
    //   4523: ldc_w 1296
    //   4526: iconst_0
    //   4527: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4530: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4533: pop
    //   4534: aload 9
    //   4536: ldc_w 1303
    //   4539: ldc_w 1305
    //   4542: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4545: pop
    //   4546: new 1060	org/json/simple/JSONArray
    //   4549: dup
    //   4550: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   4553: astore 10
    //   4555: new 911	org/json/simple/JSONObject
    //   4558: dup
    //   4559: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4562: astore 11
    //   4564: aload 11
    //   4566: ldc_w 1296
    //   4569: iconst_0
    //   4570: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4573: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4576: pop
    //   4577: aload 11
    //   4579: ldc_w 1303
    //   4582: ldc_w 1307
    //   4585: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4588: pop
    //   4589: aload 10
    //   4591: aload 11
    //   4593: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4596: pop
    //   4597: aload 9
    //   4599: ldc_w 1309
    //   4602: aload 10
    //   4604: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4607: pop
    //   4608: aload 8
    //   4610: aload 9
    //   4612: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4615: pop
    //   4616: new 911	org/json/simple/JSONObject
    //   4619: dup
    //   4620: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4623: astore 9
    //   4625: aload 9
    //   4627: ldc_w 1296
    //   4630: iconst_1
    //   4631: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4634: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4637: pop
    //   4638: aload 9
    //   4640: ldc_w 1303
    //   4643: ldc_w 1311
    //   4646: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4649: pop
    //   4650: new 1060	org/json/simple/JSONArray
    //   4653: dup
    //   4654: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   4657: astore 10
    //   4659: new 911	org/json/simple/JSONObject
    //   4662: dup
    //   4663: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4666: astore 11
    //   4668: aload 11
    //   4670: ldc_w 1296
    //   4673: iconst_0
    //   4674: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4677: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4680: pop
    //   4681: aload 11
    //   4683: ldc_w 1303
    //   4686: ldc_w 1313
    //   4689: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4692: pop
    //   4693: aload 10
    //   4695: aload 11
    //   4697: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4700: pop
    //   4701: new 911	org/json/simple/JSONObject
    //   4704: dup
    //   4705: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4708: astore 11
    //   4710: aload 11
    //   4712: ldc_w 1296
    //   4715: iconst_1
    //   4716: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4719: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4722: pop
    //   4723: aload 11
    //   4725: ldc_w 1303
    //   4728: ldc_w 1315
    //   4731: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4734: pop
    //   4735: aload 10
    //   4737: aload 11
    //   4739: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4742: pop
    //   4743: aload 9
    //   4745: ldc_w 1309
    //   4748: aload 10
    //   4750: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4753: pop
    //   4754: aload 8
    //   4756: aload 9
    //   4758: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4761: pop
    //   4762: new 911	org/json/simple/JSONObject
    //   4765: dup
    //   4766: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4769: astore 9
    //   4771: aload 9
    //   4773: ldc_w 1296
    //   4776: iconst_2
    //   4777: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4780: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4783: pop
    //   4784: aload 9
    //   4786: ldc_w 1303
    //   4789: ldc_w 1317
    //   4792: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4795: pop
    //   4796: new 1060	org/json/simple/JSONArray
    //   4799: dup
    //   4800: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   4803: astore 10
    //   4805: new 911	org/json/simple/JSONObject
    //   4808: dup
    //   4809: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4812: astore 11
    //   4814: aload 11
    //   4816: ldc_w 1296
    //   4819: iconst_0
    //   4820: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4823: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4826: pop
    //   4827: aload 11
    //   4829: ldc_w 1303
    //   4832: ldc_w 1319
    //   4835: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4838: pop
    //   4839: aload 10
    //   4841: aload 11
    //   4843: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4846: pop
    //   4847: new 911	org/json/simple/JSONObject
    //   4850: dup
    //   4851: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4854: astore 11
    //   4856: aload 11
    //   4858: ldc_w 1296
    //   4861: iconst_1
    //   4862: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4865: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4868: pop
    //   4869: aload 11
    //   4871: ldc_w 1303
    //   4874: ldc_w 1321
    //   4877: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4880: pop
    //   4881: aload 10
    //   4883: aload 11
    //   4885: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4888: pop
    //   4889: new 911	org/json/simple/JSONObject
    //   4892: dup
    //   4893: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4896: astore 11
    //   4898: aload 11
    //   4900: ldc_w 1296
    //   4903: iconst_2
    //   4904: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4907: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4910: pop
    //   4911: aload 11
    //   4913: ldc_w 1303
    //   4916: ldc_w 1323
    //   4919: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4922: pop
    //   4923: aload 10
    //   4925: aload 11
    //   4927: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4930: pop
    //   4931: new 911	org/json/simple/JSONObject
    //   4934: dup
    //   4935: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4938: astore 11
    //   4940: aload 11
    //   4942: ldc_w 1296
    //   4945: iconst_3
    //   4946: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4949: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4952: pop
    //   4953: aload 11
    //   4955: ldc_w 1303
    //   4958: ldc_w 1325
    //   4961: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4964: pop
    //   4965: aload 10
    //   4967: aload 11
    //   4969: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   4972: pop
    //   4973: new 911	org/json/simple/JSONObject
    //   4976: dup
    //   4977: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   4980: astore 11
    //   4982: aload 11
    //   4984: ldc_w 1296
    //   4987: iconst_4
    //   4988: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   4991: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4994: pop
    //   4995: aload 11
    //   4997: ldc_w 1303
    //   5000: ldc_w 1327
    //   5003: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5006: pop
    //   5007: aload 10
    //   5009: aload 11
    //   5011: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5014: pop
    //   5015: new 911	org/json/simple/JSONObject
    //   5018: dup
    //   5019: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   5022: astore 11
    //   5024: aload 11
    //   5026: ldc_w 1296
    //   5029: iconst_5
    //   5030: invokestatic 1298	java/lang/Byte:valueOf	(B)Ljava/lang/Byte;
    //   5033: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5036: pop
    //   5037: aload 11
    //   5039: ldc_w 1303
    //   5042: ldc_w 1329
    //   5045: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5048: pop
    //   5049: aload 10
    //   5051: aload 11
    //   5053: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5056: pop
    //   5057: aload 9
    //   5059: ldc_w 1309
    //   5062: aload 10
    //   5064: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5067: pop
    //   5068: aload 8
    //   5070: aload 9
    //   5072: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5075: pop
    //   5076: aload 5
    //   5078: ldc_w 1331
    //   5081: aload 8
    //   5083: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5086: pop
    //   5087: new 1060	org/json/simple/JSONArray
    //   5090: dup
    //   5091: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   5094: astore 12
    //   5096: new 911	org/json/simple/JSONObject
    //   5099: dup
    //   5100: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   5103: astore 13
    //   5105: aload 13
    //   5107: ldc_w 1296
    //   5110: iconst_0
    //   5111: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5114: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5117: pop
    //   5118: aload 13
    //   5120: ldc_w 1303
    //   5123: ldc_w 1333
    //   5126: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5129: pop
    //   5130: aload 12
    //   5132: aload 13
    //   5134: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5137: pop
    //   5138: new 911	org/json/simple/JSONObject
    //   5141: dup
    //   5142: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   5145: astore 13
    //   5147: aload 13
    //   5149: ldc_w 1296
    //   5152: iconst_1
    //   5153: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5156: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5159: pop
    //   5160: aload 13
    //   5162: ldc_w 1303
    //   5165: ldc_w 1335
    //   5168: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5171: pop
    //   5172: aload 12
    //   5174: aload 13
    //   5176: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5179: pop
    //   5180: new 911	org/json/simple/JSONObject
    //   5183: dup
    //   5184: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   5187: astore 13
    //   5189: aload 13
    //   5191: ldc_w 1296
    //   5194: iconst_2
    //   5195: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5198: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5201: pop
    //   5202: aload 13
    //   5204: ldc_w 1303
    //   5207: ldc_w 1337
    //   5210: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5213: pop
    //   5214: aload 12
    //   5216: aload 13
    //   5218: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5221: pop
    //   5222: aload 5
    //   5224: ldc_w 1339
    //   5227: aload 12
    //   5229: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5232: pop
    //   5233: goto +2745 -> 7978
    //   5236: aload 5
    //   5238: ldc_w 1130
    //   5241: aload_1
    //   5242: invokeinterface 914 1 0
    //   5247: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5250: pop
    //   5251: aload 5
    //   5253: ldc_w 1341
    //   5256: aload_1
    //   5257: invokeinterface 1343 1 0
    //   5262: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5265: pop
    //   5266: goto +2712 -> 7978
    //   5269: aload_1
    //   5270: ldc_w 1346
    //   5273: invokeinterface 906 2 0
    //   5278: astore 8
    //   5280: aload 8
    //   5282: ifnonnull +31 -> 5313
    //   5285: aload 5
    //   5287: ldc_w 920
    //   5290: iconst_3
    //   5291: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5294: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5297: pop
    //   5298: aload 5
    //   5300: ldc_w 923
    //   5303: ldc_w 1348
    //   5306: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5309: pop
    //   5310: goto +2668 -> 7978
    //   5313: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   5316: aload 8
    //   5318: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   5321: checkcast 461	Nxt$Peer
    //   5324: astore 9
    //   5326: aload 9
    //   5328: ifnonnull +31 -> 5359
    //   5331: aload 5
    //   5333: ldc_w 920
    //   5336: iconst_5
    //   5337: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5340: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5343: pop
    //   5344: aload 5
    //   5346: ldc_w 923
    //   5349: ldc_w 1350
    //   5352: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5355: pop
    //   5356: goto +2622 -> 7978
    //   5359: aload 5
    //   5361: ldc_w 1352
    //   5364: aload 9
    //   5366: getfield 1354	Nxt$Peer:state	I
    //   5369: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5372: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5375: pop
    //   5376: aload 5
    //   5378: ldc_w 1356
    //   5381: aload 9
    //   5383: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   5386: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5389: pop
    //   5390: aload 9
    //   5392: getfield 1360	Nxt$Peer:hallmark	Ljava/lang/String;
    //   5395: ifnull +17 -> 5412
    //   5398: aload 5
    //   5400: ldc_w 1108
    //   5403: aload 9
    //   5405: getfield 1360	Nxt$Peer:hallmark	Ljava/lang/String;
    //   5408: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5411: pop
    //   5412: aload 5
    //   5414: ldc_w 1132
    //   5417: aload 9
    //   5419: invokevirtual 1362	Nxt$Peer:getWeight	()I
    //   5422: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5425: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5428: pop
    //   5429: aload 5
    //   5431: ldc_w 1365
    //   5434: aload 9
    //   5436: getfield 1367	Nxt$Peer:downloadedVolume	J
    //   5439: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5442: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5445: pop
    //   5446: aload 5
    //   5448: ldc_w 1369
    //   5451: aload 9
    //   5453: getfield 1371	Nxt$Peer:uploadedVolume	J
    //   5456: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5459: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5462: pop
    //   5463: aload 5
    //   5465: ldc_w 1373
    //   5468: aload 9
    //   5470: getfield 1375	Nxt$Peer:application	Ljava/lang/String;
    //   5473: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5476: pop
    //   5477: aload 5
    //   5479: ldc_w 1275
    //   5482: aload 9
    //   5484: getfield 1377	Nxt$Peer:version	Ljava/lang/String;
    //   5487: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5490: pop
    //   5491: goto +2487 -> 7978
    //   5494: new 1060	org/json/simple/JSONArray
    //   5497: dup
    //   5498: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   5501: astore 8
    //   5503: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   5506: dup
    //   5507: astore 10
    //   5509: monitorenter
    //   5510: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   5513: invokevirtual 1379	java/util/HashMap:clone	()Ljava/lang/Object;
    //   5516: checkcast 133	java/util/HashMap
    //   5519: invokevirtual 793	java/util/HashMap:keySet	()Ljava/util/Set;
    //   5522: astore 9
    //   5524: aload 10
    //   5526: monitorexit
    //   5527: goto +7 -> 5534
    //   5530: aload 10
    //   5532: monitorexit
    //   5533: athrow
    //   5534: aload 9
    //   5536: invokeinterface 797 1 0
    //   5541: astore 11
    //   5543: goto +23 -> 5566
    //   5546: aload 11
    //   5548: invokeinterface 747 1 0
    //   5553: checkcast 223	java/lang/String
    //   5556: astore 10
    //   5558: aload 8
    //   5560: aload 10
    //   5562: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   5565: pop
    //   5566: aload 11
    //   5568: invokeinterface 760 1 0
    //   5573: ifne -27 -> 5546
    //   5576: aload 5
    //   5578: ldc_w 1382
    //   5581: aload 8
    //   5583: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5586: pop
    //   5587: goto +2391 -> 7978
    //   5590: aload 5
    //   5592: ldc_w 1275
    //   5595: ldc 8
    //   5597: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5600: pop
    //   5601: aload 5
    //   5603: ldc_w 1383
    //   5606: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   5609: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   5612: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5615: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5618: pop
    //   5619: aload 5
    //   5621: ldc_w 1385
    //   5624: getstatic 831	Nxt:lastBlock	J
    //   5627: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   5630: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5633: pop
    //   5634: aload 5
    //   5636: ldc_w 1386
    //   5639: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   5642: invokevirtual 777	java/util/HashMap:size	()I
    //   5645: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5648: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5651: pop
    //   5652: aload 5
    //   5654: ldc_w 1262
    //   5657: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   5660: invokevirtual 777	java/util/HashMap:size	()I
    //   5663: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5666: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5669: pop
    //   5670: aload 5
    //   5672: ldc_w 1388
    //   5675: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   5678: invokevirtual 777	java/util/HashMap:size	()I
    //   5681: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5684: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5687: pop
    //   5688: aload 5
    //   5690: ldc_w 1390
    //   5693: getstatic 144	Nxt:assets	Ljava/util/HashMap;
    //   5696: invokevirtual 777	java/util/HashMap:size	()I
    //   5699: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5702: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5705: pop
    //   5706: aload 5
    //   5708: ldc_w 1392
    //   5711: getstatic 148	Nxt:askOrders	Ljava/util/HashMap;
    //   5714: invokevirtual 777	java/util/HashMap:size	()I
    //   5717: getstatic 150	Nxt:bidOrders	Ljava/util/HashMap;
    //   5720: invokevirtual 777	java/util/HashMap:size	()I
    //   5723: iadd
    //   5724: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5727: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5730: pop
    //   5731: aload 5
    //   5733: ldc_w 1394
    //   5736: getstatic 140	Nxt:aliases	Ljava/util/HashMap;
    //   5739: invokevirtual 777	java/util/HashMap:size	()I
    //   5742: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5745: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5748: pop
    //   5749: aload 5
    //   5751: ldc_w 1396
    //   5754: getstatic 156	Nxt:users	Ljava/util/concurrent/ConcurrentHashMap;
    //   5757: invokevirtual 1398	java/util/concurrent/ConcurrentHashMap:size	()I
    //   5760: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5763: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5766: pop
    //   5767: aload 5
    //   5769: ldc_w 1399
    //   5772: getstatic 1400	Nxt:lastBlockchainFeeder	LNxt$Peer;
    //   5775: ifnonnull +7 -> 5782
    //   5778: aconst_null
    //   5779: goto +9 -> 5788
    //   5782: getstatic 1400	Nxt:lastBlockchainFeeder	LNxt$Peer;
    //   5785: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   5788: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5791: pop
    //   5792: aload 5
    //   5794: ldc_w 1402
    //   5797: invokestatic 1404	java/lang/Runtime:getRuntime	()Ljava/lang/Runtime;
    //   5800: invokevirtual 1410	java/lang/Runtime:availableProcessors	()I
    //   5803: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5806: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5809: pop
    //   5810: aload 5
    //   5812: ldc_w 1412
    //   5815: invokestatic 1404	java/lang/Runtime:getRuntime	()Ljava/lang/Runtime;
    //   5818: invokevirtual 1414	java/lang/Runtime:maxMemory	()J
    //   5821: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5824: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5827: pop
    //   5828: aload 5
    //   5830: ldc_w 1416
    //   5833: invokestatic 1404	java/lang/Runtime:getRuntime	()Ljava/lang/Runtime;
    //   5836: invokevirtual 1418	java/lang/Runtime:totalMemory	()J
    //   5839: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5842: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5845: pop
    //   5846: aload 5
    //   5848: ldc_w 1420
    //   5851: invokestatic 1404	java/lang/Runtime:getRuntime	()Ljava/lang/Runtime;
    //   5854: invokevirtual 1422	java/lang/Runtime:freeMemory	()J
    //   5857: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5860: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5863: pop
    //   5864: goto +2114 -> 7978
    //   5867: aload 5
    //   5869: ldc_w 1383
    //   5872: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   5875: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   5878: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5881: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5884: pop
    //   5885: goto +2093 -> 7978
    //   5888: aload_1
    //   5889: ldc_w 1073
    //   5892: invokeinterface 906 2 0
    //   5897: astore 8
    //   5899: aload 8
    //   5901: ifnonnull +31 -> 5932
    //   5904: aload 5
    //   5906: ldc_w 920
    //   5909: iconst_3
    //   5910: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5913: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5916: pop
    //   5917: aload 5
    //   5919: ldc_w 923
    //   5922: ldc_w 1424
    //   5925: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   5928: pop
    //   5929: goto +2049 -> 7978
    //   5932: new 110	java/math/BigInteger
    //   5935: dup
    //   5936: aload 8
    //   5938: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   5941: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   5944: lstore 9
    //   5946: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   5949: lload 9
    //   5951: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5954: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   5957: checkcast 524	Nxt$Transaction
    //   5960: astore 11
    //   5962: aload 11
    //   5964: ifnonnull +82 -> 6046
    //   5967: getstatic 124	Nxt:unconfirmedTransactions	Ljava/util/concurrent/ConcurrentHashMap;
    //   5970: lload 9
    //   5972: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   5975: invokevirtual 1426	java/util/concurrent/ConcurrentHashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   5978: checkcast 524	Nxt$Transaction
    //   5981: astore 11
    //   5983: aload 11
    //   5985: ifnonnull +31 -> 6016
    //   5988: aload 5
    //   5990: ldc_w 920
    //   5993: iconst_5
    //   5994: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   5997: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6000: pop
    //   6001: aload 5
    //   6003: ldc_w 923
    //   6006: ldc_w 1427
    //   6009: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6012: pop
    //   6013: goto +1965 -> 7978
    //   6016: aload 11
    //   6018: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   6021: astore 5
    //   6023: aload 5
    //   6025: ldc_w 1429
    //   6028: aload 11
    //   6030: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   6033: invokestatic 1029	Nxt$Account:getId	([B)J
    //   6036: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   6039: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6042: pop
    //   6043: goto +1935 -> 7978
    //   6046: aload 11
    //   6048: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   6051: astore 5
    //   6053: aload 5
    //   6055: ldc_w 1429
    //   6058: aload 11
    //   6060: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   6063: invokestatic 1029	Nxt$Account:getId	([B)J
    //   6066: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   6069: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6072: pop
    //   6073: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   6076: aload 11
    //   6078: getfield 757	Nxt$Transaction:block	J
    //   6081: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   6084: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   6087: checkcast 771	Nxt$Block
    //   6090: astore 12
    //   6092: aload 5
    //   6094: ldc_w 1249
    //   6097: aload 12
    //   6099: invokevirtual 1431	Nxt$Block:getId	()J
    //   6102: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   6105: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6108: pop
    //   6109: aload 5
    //   6111: ldc_w 1432
    //   6114: invokestatic 1434	Nxt$Block:getLastBlock	()LNxt$Block;
    //   6117: getfield 1255	Nxt$Block:height	I
    //   6120: aload 12
    //   6122: getfield 1255	Nxt$Block:height	I
    //   6125: isub
    //   6126: iconst_1
    //   6127: iadd
    //   6128: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6131: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6134: pop
    //   6135: goto +1843 -> 7978
    //   6138: pop
    //   6139: aload 5
    //   6141: ldc_w 920
    //   6144: iconst_4
    //   6145: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6148: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6151: pop
    //   6152: aload 5
    //   6154: ldc_w 923
    //   6157: ldc_w 1438
    //   6160: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6163: pop
    //   6164: goto +1814 -> 7978
    //   6167: aload_1
    //   6168: ldc_w 1073
    //   6171: invokeinterface 906 2 0
    //   6176: astore 8
    //   6178: aload 8
    //   6180: ifnonnull +31 -> 6211
    //   6183: aload 5
    //   6185: ldc_w 920
    //   6188: iconst_3
    //   6189: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6192: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6195: pop
    //   6196: aload 5
    //   6198: ldc_w 923
    //   6201: ldc_w 1424
    //   6204: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6207: pop
    //   6208: goto +1770 -> 7978
    //   6211: new 110	java/math/BigInteger
    //   6214: dup
    //   6215: aload 8
    //   6217: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   6220: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   6223: lstore 9
    //   6225: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   6228: lload 9
    //   6230: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   6233: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   6236: checkcast 524	Nxt$Transaction
    //   6239: astore 11
    //   6241: aload 11
    //   6243: ifnonnull +72 -> 6315
    //   6246: getstatic 124	Nxt:unconfirmedTransactions	Ljava/util/concurrent/ConcurrentHashMap;
    //   6249: lload 9
    //   6251: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   6254: invokevirtual 1426	java/util/concurrent/ConcurrentHashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   6257: checkcast 524	Nxt$Transaction
    //   6260: astore 11
    //   6262: aload 11
    //   6264: ifnonnull +31 -> 6295
    //   6267: aload 5
    //   6269: ldc_w 920
    //   6272: iconst_5
    //   6273: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6276: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6279: pop
    //   6280: aload 5
    //   6282: ldc_w 923
    //   6285: ldc_w 1427
    //   6288: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6291: pop
    //   6292: goto +1686 -> 7978
    //   6295: aload 5
    //   6297: ldc_w 1440
    //   6300: aload 11
    //   6302: invokevirtual 814	Nxt$Transaction:getBytes	()[B
    //   6305: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   6308: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6311: pop
    //   6312: goto +1666 -> 7978
    //   6315: aload 5
    //   6317: ldc_w 1440
    //   6320: aload 11
    //   6322: invokevirtual 814	Nxt$Transaction:getBytes	()[B
    //   6325: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   6328: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6331: pop
    //   6332: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   6335: aload 11
    //   6337: getfield 757	Nxt$Transaction:block	J
    //   6340: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   6343: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   6346: checkcast 771	Nxt$Block
    //   6349: astore 12
    //   6351: aload 5
    //   6353: ldc_w 1432
    //   6356: invokestatic 1434	Nxt$Block:getLastBlock	()LNxt$Block;
    //   6359: getfield 1255	Nxt$Block:height	I
    //   6362: aload 12
    //   6364: getfield 1255	Nxt$Block:height	I
    //   6367: isub
    //   6368: iconst_1
    //   6369: iadd
    //   6370: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6373: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6376: pop
    //   6377: goto +1601 -> 7978
    //   6380: pop
    //   6381: aload 5
    //   6383: ldc_w 920
    //   6386: iconst_4
    //   6387: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6390: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6393: pop
    //   6394: aload 5
    //   6396: ldc_w 923
    //   6399: ldc_w 1438
    //   6402: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6405: pop
    //   6406: goto +1572 -> 7978
    //   6409: new 1060	org/json/simple/JSONArray
    //   6412: dup
    //   6413: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   6416: astore 8
    //   6418: getstatic 124	Nxt:unconfirmedTransactions	Ljava/util/concurrent/ConcurrentHashMap;
    //   6421: invokevirtual 1442	java/util/concurrent/ConcurrentHashMap:keySet	()Ljava/util/Set;
    //   6424: invokeinterface 797 1 0
    //   6429: astore 10
    //   6431: goto +29 -> 6460
    //   6434: aload 10
    //   6436: invokeinterface 747 1 0
    //   6441: checkcast 266	java/lang/Long
    //   6444: astore 9
    //   6446: aload 8
    //   6448: aload 9
    //   6450: invokevirtual 800	java/lang/Long:longValue	()J
    //   6453: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   6456: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   6459: pop
    //   6460: aload 10
    //   6462: invokeinterface 760 1 0
    //   6467: ifne -33 -> 6434
    //   6470: aload 5
    //   6472: ldc_w 1443
    //   6475: aload 8
    //   6477: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6480: pop
    //   6481: goto +1497 -> 7978
    //   6484: aload_1
    //   6485: ldc_w 1129
    //   6488: invokeinterface 906 2 0
    //   6493: astore 8
    //   6495: aload 8
    //   6497: ifnonnull +31 -> 6528
    //   6500: aload 5
    //   6502: ldc_w 920
    //   6505: iconst_3
    //   6506: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6509: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6512: pop
    //   6513: aload 5
    //   6515: ldc_w 923
    //   6518: ldc_w 1193
    //   6521: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6524: pop
    //   6525: goto +1453 -> 7978
    //   6528: new 110	java/math/BigInteger
    //   6531: dup
    //   6532: aload 8
    //   6534: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   6537: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   6540: lstore 9
    //   6542: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   6545: lload 9
    //   6547: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   6550: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   6553: checkcast 311	Nxt$Account
    //   6556: astore 11
    //   6558: aload 11
    //   6560: ifnonnull +31 -> 6591
    //   6563: aload 5
    //   6565: ldc_w 920
    //   6568: iconst_5
    //   6569: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6572: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6575: pop
    //   6576: aload 5
    //   6578: ldc_w 923
    //   6581: ldc_w 1195
    //   6584: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6587: pop
    //   6588: goto +1390 -> 7978
    //   6591: new 1060	org/json/simple/JSONArray
    //   6594: dup
    //   6595: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   6598: astore 12
    //   6600: getstatic 140	Nxt:aliases	Ljava/util/HashMap;
    //   6603: invokevirtual 737	java/util/HashMap:values	()Ljava/util/Collection;
    //   6606: invokeinterface 741 1 0
    //   6611: astore 14
    //   6613: goto +74 -> 6687
    //   6616: aload 14
    //   6618: invokeinterface 747 1 0
    //   6623: checkcast 1036	Nxt$Alias
    //   6626: astore 13
    //   6628: aload 13
    //   6630: getfield 1038	Nxt$Alias:account	LNxt$Account;
    //   6633: getfield 1220	Nxt$Account:id	J
    //   6636: lload 9
    //   6638: lcmp
    //   6639: ifne +48 -> 6687
    //   6642: new 911	org/json/simple/JSONObject
    //   6645: dup
    //   6646: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   6649: astore 15
    //   6651: aload 15
    //   6653: ldc_w 984
    //   6656: aload 13
    //   6658: getfield 1233	Nxt$Alias:alias	Ljava/lang/String;
    //   6661: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6664: pop
    //   6665: aload 15
    //   6667: ldc_w 986
    //   6670: aload 13
    //   6672: getfield 1235	Nxt$Alias:uri	Ljava/lang/String;
    //   6675: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6678: pop
    //   6679: aload 12
    //   6681: aload 15
    //   6683: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   6686: pop
    //   6687: aload 14
    //   6689: invokeinterface 760 1 0
    //   6694: ifne -78 -> 6616
    //   6697: aload 5
    //   6699: ldc_w 1445
    //   6702: aload 12
    //   6704: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6707: pop
    //   6708: goto +1270 -> 7978
    //   6711: pop
    //   6712: aload 5
    //   6714: ldc_w 920
    //   6717: iconst_4
    //   6718: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6721: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6724: pop
    //   6725: aload 5
    //   6727: ldc_w 923
    //   6730: ldc_w 1203
    //   6733: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6736: pop
    //   6737: goto +1241 -> 7978
    //   6740: aload_1
    //   6741: ldc_w 982
    //   6744: invokeinterface 906 2 0
    //   6749: astore 8
    //   6751: aload_1
    //   6752: ldc_w 1130
    //   6755: invokeinterface 906 2 0
    //   6760: astore 9
    //   6762: aload_1
    //   6763: ldc_w 1132
    //   6766: invokeinterface 906 2 0
    //   6771: astore 10
    //   6773: aload_1
    //   6774: ldc_w 1134
    //   6777: invokeinterface 906 2 0
    //   6782: astore 11
    //   6784: aload 8
    //   6786: ifnonnull +31 -> 6817
    //   6789: aload 5
    //   6791: ldc_w 920
    //   6794: iconst_3
    //   6795: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6798: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6801: pop
    //   6802: aload 5
    //   6804: ldc_w 923
    //   6807: ldc_w 994
    //   6810: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6813: pop
    //   6814: goto +1164 -> 7978
    //   6817: aload 9
    //   6819: ifnonnull +31 -> 6850
    //   6822: aload 5
    //   6824: ldc_w 920
    //   6827: iconst_3
    //   6828: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6831: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6834: pop
    //   6835: aload 5
    //   6837: ldc_w 923
    //   6840: ldc_w 1446
    //   6843: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6846: pop
    //   6847: goto +1131 -> 7978
    //   6850: aload 10
    //   6852: ifnonnull +31 -> 6883
    //   6855: aload 5
    //   6857: ldc_w 920
    //   6860: iconst_3
    //   6861: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6864: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6867: pop
    //   6868: aload 5
    //   6870: ldc_w 923
    //   6873: ldc_w 1448
    //   6876: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6879: pop
    //   6880: goto +1098 -> 7978
    //   6883: aload 11
    //   6885: ifnonnull +31 -> 6916
    //   6888: aload 5
    //   6890: ldc_w 920
    //   6893: iconst_3
    //   6894: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6897: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6900: pop
    //   6901: aload 5
    //   6903: ldc_w 923
    //   6906: ldc_w 1450
    //   6909: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6912: pop
    //   6913: goto +1065 -> 7978
    //   6916: aload 9
    //   6918: invokevirtual 222	java/lang/String:length	()I
    //   6921: bipush 100
    //   6923: if_icmple +31 -> 6954
    //   6926: aload 5
    //   6928: ldc_w 920
    //   6931: iconst_4
    //   6932: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   6935: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6938: pop
    //   6939: aload 5
    //   6941: ldc_w 923
    //   6944: ldc_w 1452
    //   6947: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   6950: pop
    //   6951: goto +1027 -> 7978
    //   6954: aload 10
    //   6956: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   6959: istore 12
    //   6961: iload 12
    //   6963: ifle +11 -> 6974
    //   6966: iload 12
    //   6968: ldc_w 780
    //   6971: if_icmple +11 -> 6982
    //   6974: new 263	java/lang/Exception
    //   6977: dup
    //   6978: invokespecial 1017	java/lang/Exception:<init>	()V
    //   6981: athrow
    //   6982: aload 11
    //   6984: iconst_0
    //   6985: iconst_4
    //   6986: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   6989: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   6992: sipush 10000
    //   6995: imul
    //   6996: aload 11
    //   6998: iconst_5
    //   6999: bipush 7
    //   7001: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   7004: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   7007: bipush 100
    //   7009: imul
    //   7010: iadd
    //   7011: aload 11
    //   7013: bipush 8
    //   7015: bipush 10
    //   7017: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   7020: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   7023: iadd
    //   7024: istore 13
    //   7026: aload 8
    //   7028: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   7031: astore 14
    //   7033: aload 9
    //   7035: ldc_w 1118
    //   7038: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   7041: astore 15
    //   7043: bipush 34
    //   7045: aload 15
    //   7047: arraylength
    //   7048: iadd
    //   7049: iconst_4
    //   7050: iadd
    //   7051: iconst_4
    //   7052: iadd
    //   7053: iconst_1
    //   7054: iadd
    //   7055: invokestatic 1454	java/nio/ByteBuffer:allocate	(I)Ljava/nio/ByteBuffer;
    //   7058: astore 16
    //   7060: aload 16
    //   7062: getstatic 1093	java/nio/ByteOrder:LITTLE_ENDIAN	Ljava/nio/ByteOrder;
    //   7065: invokevirtual 1099	java/nio/ByteBuffer:order	(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;
    //   7068: pop
    //   7069: aload 16
    //   7071: aload 14
    //   7073: invokevirtual 1458	java/nio/ByteBuffer:put	([B)Ljava/nio/ByteBuffer;
    //   7076: pop
    //   7077: aload 16
    //   7079: aload 15
    //   7081: arraylength
    //   7082: i2s
    //   7083: invokevirtual 1460	java/nio/ByteBuffer:putShort	(S)Ljava/nio/ByteBuffer;
    //   7086: pop
    //   7087: aload 16
    //   7089: aload 15
    //   7091: invokevirtual 1458	java/nio/ByteBuffer:put	([B)Ljava/nio/ByteBuffer;
    //   7094: pop
    //   7095: aload 16
    //   7097: iload 12
    //   7099: invokevirtual 1464	java/nio/ByteBuffer:putInt	(I)Ljava/nio/ByteBuffer;
    //   7102: pop
    //   7103: aload 16
    //   7105: iload 13
    //   7107: invokevirtual 1464	java/nio/ByteBuffer:putInt	(I)Ljava/nio/ByteBuffer;
    //   7110: pop
    //   7111: aload 16
    //   7113: invokevirtual 1467	java/nio/ByteBuffer:array	()[B
    //   7116: astore 17
    //   7118: aload 17
    //   7120: aload 17
    //   7122: arraylength
    //   7123: iconst_1
    //   7124: isub
    //   7125: invokestatic 1470	java/util/concurrent/ThreadLocalRandom:current	()Ljava/util/concurrent/ThreadLocalRandom;
    //   7128: invokevirtual 1476	java/util/concurrent/ThreadLocalRandom:nextInt	()I
    //   7131: i2b
    //   7132: bastore
    //   7133: aload 17
    //   7135: aload 8
    //   7137: invokestatic 1479	Nxt$Crypto:sign	([BLjava/lang/String;)[B
    //   7140: astore 18
    //   7142: aload 18
    //   7144: aload 17
    //   7146: aload 14
    //   7148: invokestatic 1158	Nxt$Crypto:verify	([B[B[B)Z
    //   7151: ifeq -33 -> 7118
    //   7154: aload 5
    //   7156: ldc_w 1108
    //   7159: new 192	java/lang/StringBuilder
    //   7162: dup
    //   7163: aload 17
    //   7165: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   7168: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   7171: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   7174: aload 18
    //   7176: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   7179: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   7182: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   7185: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7188: pop
    //   7189: goto +789 -> 7978
    //   7192: pop
    //   7193: aload 5
    //   7195: ldc_w 920
    //   7198: iconst_4
    //   7199: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7202: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7205: pop
    //   7206: aload 5
    //   7208: ldc_w 923
    //   7211: ldc_w 1482
    //   7214: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7217: pop
    //   7218: goto +760 -> 7978
    //   7221: pop
    //   7222: aload 5
    //   7224: ldc_w 920
    //   7227: iconst_4
    //   7228: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7231: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7234: pop
    //   7235: aload 5
    //   7237: ldc_w 923
    //   7240: ldc_w 1484
    //   7243: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7246: pop
    //   7247: goto +731 -> 7978
    //   7250: aload_1
    //   7251: ldc_w 982
    //   7254: invokeinterface 906 2 0
    //   7259: astore 8
    //   7261: aload_1
    //   7262: ldc_w 1486
    //   7265: invokeinterface 906 2 0
    //   7270: astore 9
    //   7272: aload_1
    //   7273: ldc_w 1487
    //   7276: invokeinterface 906 2 0
    //   7281: astore 10
    //   7283: aload_1
    //   7284: ldc_w 988
    //   7287: invokeinterface 906 2 0
    //   7292: astore 11
    //   7294: aload_1
    //   7295: ldc_w 990
    //   7298: invokeinterface 906 2 0
    //   7303: astore 12
    //   7305: aload_1
    //   7306: ldc_w 992
    //   7309: invokeinterface 906 2 0
    //   7314: astore 13
    //   7316: aload 8
    //   7318: ifnonnull +31 -> 7349
    //   7321: aload 5
    //   7323: ldc_w 920
    //   7326: iconst_3
    //   7327: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7330: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7333: pop
    //   7334: aload 5
    //   7336: ldc_w 923
    //   7339: ldc_w 994
    //   7342: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7345: pop
    //   7346: goto +632 -> 7978
    //   7349: aload 9
    //   7351: ifnonnull +31 -> 7382
    //   7354: aload 5
    //   7356: ldc_w 920
    //   7359: iconst_3
    //   7360: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7363: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7366: pop
    //   7367: aload 5
    //   7369: ldc_w 923
    //   7372: ldc_w 1489
    //   7375: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7378: pop
    //   7379: goto +599 -> 7978
    //   7382: aload 10
    //   7384: ifnonnull +31 -> 7415
    //   7387: aload 5
    //   7389: ldc_w 920
    //   7392: iconst_3
    //   7393: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7396: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7399: pop
    //   7400: aload 5
    //   7402: ldc_w 923
    //   7405: ldc_w 1491
    //   7408: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7411: pop
    //   7412: goto +566 -> 7978
    //   7415: aload 11
    //   7417: ifnonnull +31 -> 7448
    //   7420: aload 5
    //   7422: ldc_w 920
    //   7425: iconst_3
    //   7426: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7429: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7432: pop
    //   7433: aload 5
    //   7435: ldc_w 923
    //   7438: ldc_w 1000
    //   7441: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7444: pop
    //   7445: goto +533 -> 7978
    //   7448: aload 12
    //   7450: ifnonnull +31 -> 7481
    //   7453: aload 5
    //   7455: ldc_w 920
    //   7458: iconst_3
    //   7459: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7462: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7465: pop
    //   7466: aload 5
    //   7468: ldc_w 923
    //   7471: ldc_w 1002
    //   7474: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7477: pop
    //   7478: goto +500 -> 7978
    //   7481: new 110	java/math/BigInteger
    //   7484: dup
    //   7485: aload 9
    //   7487: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   7490: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   7493: lstore 14
    //   7495: aload 10
    //   7497: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   7500: istore 16
    //   7502: iload 16
    //   7504: ifle +11 -> 7515
    //   7507: iload 16
    //   7509: ldc_w 780
    //   7512: if_icmplt +11 -> 7523
    //   7515: new 263	java/lang/Exception
    //   7518: dup
    //   7519: invokespecial 1017	java/lang/Exception:<init>	()V
    //   7522: athrow
    //   7523: aload 11
    //   7525: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   7528: istore 17
    //   7530: iload 17
    //   7532: ifle +11 -> 7543
    //   7535: iload 17
    //   7537: ldc_w 780
    //   7540: if_icmplt +11 -> 7551
    //   7543: new 263	java/lang/Exception
    //   7546: dup
    //   7547: invokespecial 1017	java/lang/Exception:<init>	()V
    //   7550: athrow
    //   7551: aload 12
    //   7553: invokestatic 1018	java/lang/Short:parseShort	(Ljava/lang/String;)S
    //   7556: istore 18
    //   7558: iload 18
    //   7560: iconst_1
    //   7561: if_icmpge +11 -> 7572
    //   7564: new 263	java/lang/Exception
    //   7567: dup
    //   7568: invokespecial 1017	java/lang/Exception:<init>	()V
    //   7571: athrow
    //   7572: aload 13
    //   7574: ifnonnull +7 -> 7581
    //   7577: lconst_0
    //   7578: goto +15 -> 7593
    //   7581: new 110	java/math/BigInteger
    //   7584: dup
    //   7585: aload 13
    //   7587: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   7590: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   7593: lstore 19
    //   7595: aload 8
    //   7597: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   7600: astore 21
    //   7602: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   7605: aload 21
    //   7607: invokestatic 1029	Nxt$Account:getId	([B)J
    //   7610: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   7613: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   7616: checkcast 311	Nxt$Account
    //   7619: astore 22
    //   7621: aload 22
    //   7623: ifnonnull +32 -> 7655
    //   7626: aload 5
    //   7628: ldc_w 920
    //   7631: bipush 6
    //   7633: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7636: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7639: pop
    //   7640: aload 5
    //   7642: ldc_w 923
    //   7645: ldc_w 1032
    //   7648: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7651: pop
    //   7652: goto +326 -> 7978
    //   7655: iload 16
    //   7657: iload 17
    //   7659: iadd
    //   7660: i2l
    //   7661: ldc2_w 1034
    //   7664: lmul
    //   7665: aload 22
    //   7667: getfield 318	Nxt$Account:unconfirmedBalance	J
    //   7670: lcmp
    //   7671: ifle +32 -> 7703
    //   7674: aload 5
    //   7676: ldc_w 920
    //   7679: bipush 6
    //   7681: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7684: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7687: pop
    //   7688: aload 5
    //   7690: ldc_w 923
    //   7693: ldc_w 1032
    //   7696: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7699: pop
    //   7700: goto +278 -> 7978
    //   7703: new 524	Nxt$Transaction
    //   7706: dup
    //   7707: iconst_0
    //   7708: iconst_0
    //   7709: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   7712: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   7715: iload 18
    //   7717: aload 21
    //   7719: lload 14
    //   7721: iload 16
    //   7723: iload 17
    //   7725: lload 19
    //   7727: bipush 64
    //   7729: newarray byte
    //   7731: invokespecial 731	Nxt$Transaction:<init>	(BBIS[BJIIJ[B)V
    //   7734: astore 23
    //   7736: aload 23
    //   7738: aload 8
    //   7740: invokevirtual 1055	Nxt$Transaction:sign	(Ljava/lang/String;)V
    //   7743: new 911	org/json/simple/JSONObject
    //   7746: dup
    //   7747: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   7750: astore 24
    //   7752: aload 24
    //   7754: ldc_w 927
    //   7757: ldc_w 1058
    //   7760: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7763: pop
    //   7764: new 1060	org/json/simple/JSONArray
    //   7767: dup
    //   7768: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   7771: astore 25
    //   7773: aload 25
    //   7775: aload 23
    //   7777: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   7780: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   7783: pop
    //   7784: aload 24
    //   7786: ldc_w 1068
    //   7789: aload 25
    //   7791: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7794: pop
    //   7795: aload 24
    //   7797: invokestatic 1069	Nxt$Peer:sendToAllPeers	(Lorg/json/simple/JSONObject;)V
    //   7800: aload 5
    //   7802: ldc_w 1073
    //   7805: aload 23
    //   7807: invokevirtual 734	Nxt$Transaction:getId	()J
    //   7810: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   7813: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7816: pop
    //   7817: aload 5
    //   7819: ldc_w 1440
    //   7822: aload 23
    //   7824: invokevirtual 814	Nxt$Transaction:getBytes	()[B
    //   7827: invokestatic 1201	Nxt:convert	([B)Ljava/lang/String;
    //   7830: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7833: pop
    //   7834: goto +144 -> 7978
    //   7837: pop
    //   7838: aload 5
    //   7840: ldc_w 920
    //   7843: iconst_4
    //   7844: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7847: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7850: pop
    //   7851: aload 5
    //   7853: ldc_w 923
    //   7856: ldc_w 1077
    //   7859: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7862: pop
    //   7863: goto +115 -> 7978
    //   7866: pop
    //   7867: aload 5
    //   7869: ldc_w 920
    //   7872: iconst_4
    //   7873: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7876: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7879: pop
    //   7880: aload 5
    //   7882: ldc_w 923
    //   7885: ldc_w 1079
    //   7888: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7891: pop
    //   7892: goto +86 -> 7978
    //   7895: pop
    //   7896: aload 5
    //   7898: ldc_w 920
    //   7901: iconst_4
    //   7902: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7905: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7908: pop
    //   7909: aload 5
    //   7911: ldc_w 923
    //   7914: ldc_w 1493
    //   7917: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7920: pop
    //   7921: goto +57 -> 7978
    //   7924: pop
    //   7925: aload 5
    //   7927: ldc_w 920
    //   7930: iconst_4
    //   7931: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7934: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7937: pop
    //   7938: aload 5
    //   7940: ldc_w 923
    //   7943: ldc_w 1495
    //   7946: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7949: pop
    //   7950: goto +28 -> 7978
    //   7953: aload 5
    //   7955: ldc_w 920
    //   7958: iconst_1
    //   7959: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   7962: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7965: pop
    //   7966: aload 5
    //   7968: ldc_w 923
    //   7971: ldc_w 929
    //   7974: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   7977: pop
    //   7978: aload_2
    //   7979: ldc_w 1497
    //   7982: invokeinterface 1499 2 0
    //   7987: aload_2
    //   7988: invokeinterface 1504 1 0
    //   7993: astore 6
    //   7995: aload 6
    //   7997: aload 5
    //   7999: invokevirtual 1508	org/json/simple/JSONObject:toString	()Ljava/lang/String;
    //   8002: ldc_w 1118
    //   8005: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   8008: invokevirtual 1509	javax/servlet/ServletOutputStream:write	([B)V
    //   8011: aload 6
    //   8013: invokevirtual 1514	javax/servlet/ServletOutputStream:close	()V
    //   8016: return
    //   8017: getstatic 501	Nxt:allowedUserHosts	Ljava/util/HashSet;
    //   8020: ifnull +115 -> 8135
    //   8023: getstatic 501	Nxt:allowedUserHosts	Ljava/util/HashSet;
    //   8026: aload_1
    //   8027: invokeinterface 914 1 0
    //   8032: invokevirtual 917	java/util/HashSet:contains	(Ljava/lang/Object;)Z
    //   8035: ifne +100 -> 8135
    //   8038: new 911	org/json/simple/JSONObject
    //   8041: dup
    //   8042: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   8045: astore 5
    //   8047: aload 5
    //   8049: ldc_w 1517
    //   8052: ldc_w 1519
    //   8055: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   8058: pop
    //   8059: new 1060	org/json/simple/JSONArray
    //   8062: dup
    //   8063: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   8066: astore 6
    //   8068: aload 6
    //   8070: aload 5
    //   8072: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   8075: pop
    //   8076: new 911	org/json/simple/JSONObject
    //   8079: dup
    //   8080: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   8083: astore 7
    //   8085: aload 7
    //   8087: ldc_w 1521
    //   8090: aload 6
    //   8092: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   8095: pop
    //   8096: aload_2
    //   8097: ldc_w 1497
    //   8100: invokeinterface 1499 2 0
    //   8105: aload_2
    //   8106: invokeinterface 1504 1 0
    //   8111: astore 8
    //   8113: aload 8
    //   8115: aload 7
    //   8117: invokevirtual 1508	org/json/simple/JSONObject:toString	()Ljava/lang/String;
    //   8120: ldc_w 1118
    //   8123: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   8126: invokevirtual 1509	javax/servlet/ServletOutputStream:write	([B)V
    //   8129: aload 8
    //   8131: invokevirtual 1514	javax/servlet/ServletOutputStream:close	()V
    //   8134: return
    //   8135: getstatic 156	Nxt:users	Ljava/util/concurrent/ConcurrentHashMap;
    //   8138: aload 4
    //   8140: invokevirtual 1426	java/util/concurrent/ConcurrentHashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   8143: checkcast 1523	Nxt$User
    //   8146: astore_3
    //   8147: aload_3
    //   8148: ifnonnull +21 -> 8169
    //   8151: new 1523	Nxt$User
    //   8154: dup
    //   8155: invokespecial 1525	Nxt$User:<init>	()V
    //   8158: astore_3
    //   8159: getstatic 156	Nxt:users	Ljava/util/concurrent/ConcurrentHashMap;
    //   8162: aload 4
    //   8164: aload_3
    //   8165: invokevirtual 1526	java/util/concurrent/ConcurrentHashMap:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   8168: pop
    //   8169: aload_1
    //   8170: ldc_w 927
    //   8173: invokeinterface 906 2 0
    //   8178: dup
    //   8179: astore 5
    //   8181: invokevirtual 931	java/lang/String:hashCode	()I
    //   8184: lookupswitch	default:+4961->13145, -1695267198:+84->8268, -1413383884:+98->8282, -1215991508:+112->8296, -632255711:+126->8310, -349483447:+140->8324, 9950744:+154->8338, 94341973:+168->8352, 592625624:+182->8366, 892719322:+196->8380
    //   8269: iconst_2
    //   8270: ldc_w 1527
    //   8273: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8276: ifne +2179 -> 10455
    //   8279: goto +4866 -> 13145
    //   8282: aload 5
    //   8284: ldc_w 1529
    //   8287: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8290: ifne +4952 -> 13242
    //   8293: goto +4852 -> 13145
    //   8296: aload 5
    //   8298: ldc_w 1531
    //   8301: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8304: ifne +2189 -> 10493
    //   8307: goto +4838 -> 13145
    //   8310: aload 5
    //   8312: ldc_w 1533
    //   8315: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8318: ifne +2483 -> 10801
    //   8321: goto +4824 -> 13145
    //   8324: aload 5
    //   8326: ldc_w 1535
    //   8329: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8332: ifne +3373 -> 11705
    //   8335: goto +4810 -> 13145
    //   8338: aload 5
    //   8340: ldc_w 958
    //   8343: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8346: ifne +2595 -> 10941
    //   8349: goto +4796 -> 13145
    //   8352: aload 5
    //   8354: ldc_w 1537
    //   8357: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8360: ifne +34 -> 8394
    //   8363: goto +4782 -> 13145
    //   8366: aload 5
    //   8368: ldc_w 1539
    //   8371: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8374: ifne +602 -> 8976
    //   8377: goto +4768 -> 13145
    //   8380: aload 5
    //   8382: ldc_w 1541
    //   8385: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   8388: ifne +2263 -> 10651
    //   8391: goto +4754 -> 13145
    //   8394: aload_1
    //   8395: ldc_w 1167
    //   8398: invokeinterface 906 2 0
    //   8403: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   8406: ldc_w 1118
    //   8409: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   8412: astore 6
    //   8414: aload 6
    //   8416: arraylength
    //   8417: bipush 32
    //   8419: iadd
    //   8420: iconst_4
    //   8421: iadd
    //   8422: newarray byte
    //   8424: astore 7
    //   8426: aload 6
    //   8428: iconst_0
    //   8429: aload 7
    //   8431: iconst_0
    //   8432: aload 6
    //   8434: arraylength
    //   8435: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   8438: aload_3
    //   8439: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   8442: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   8445: iconst_0
    //   8446: aload 7
    //   8448: aload 6
    //   8450: arraylength
    //   8451: bipush 32
    //   8453: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   8456: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   8459: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   8462: istore 8
    //   8464: aload 7
    //   8466: aload 6
    //   8468: arraylength
    //   8469: bipush 32
    //   8471: iadd
    //   8472: iload 8
    //   8474: i2b
    //   8475: bastore
    //   8476: aload 7
    //   8478: aload 6
    //   8480: arraylength
    //   8481: bipush 32
    //   8483: iadd
    //   8484: iconst_1
    //   8485: iadd
    //   8486: iload 8
    //   8488: bipush 8
    //   8490: ishr
    //   8491: i2b
    //   8492: bastore
    //   8493: aload 7
    //   8495: aload 6
    //   8497: arraylength
    //   8498: bipush 32
    //   8500: iadd
    //   8501: iconst_2
    //   8502: iadd
    //   8503: iload 8
    //   8505: bipush 16
    //   8507: ishr
    //   8508: i2b
    //   8509: bastore
    //   8510: aload 7
    //   8512: aload 6
    //   8514: arraylength
    //   8515: bipush 32
    //   8517: iadd
    //   8518: iconst_3
    //   8519: iadd
    //   8520: iload 8
    //   8522: bipush 24
    //   8524: ishr
    //   8525: i2b
    //   8526: bastore
    //   8527: bipush 100
    //   8529: newarray byte
    //   8531: astore 9
    //   8533: aload 7
    //   8535: aload 6
    //   8537: arraylength
    //   8538: aload 9
    //   8540: iconst_0
    //   8541: bipush 36
    //   8543: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   8546: aload 7
    //   8548: aload_3
    //   8549: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   8552: invokestatic 1479	Nxt$Crypto:sign	([BLjava/lang/String;)[B
    //   8555: iconst_0
    //   8556: aload 9
    //   8558: bipush 36
    //   8560: bipush 64
    //   8562: invokestatic 1152	java/lang/System:arraycopy	(Ljava/lang/Object;ILjava/lang/Object;II)V
    //   8565: ldc_w 1142
    //   8568: astore 10
    //   8570: iconst_0
    //   8571: istore 11
    //   8573: goto +351 -> 8924
    //   8576: aload 9
    //   8578: iload 11
    //   8580: baload
    //   8581: sipush 255
    //   8584: iand
    //   8585: i2l
    //   8586: aload 9
    //   8588: iload 11
    //   8590: iconst_1
    //   8591: iadd
    //   8592: baload
    //   8593: sipush 255
    //   8596: iand
    //   8597: i2l
    //   8598: bipush 8
    //   8600: lshl
    //   8601: lor
    //   8602: aload 9
    //   8604: iload 11
    //   8606: iconst_2
    //   8607: iadd
    //   8608: baload
    //   8609: sipush 255
    //   8612: iand
    //   8613: i2l
    //   8614: bipush 16
    //   8616: lshl
    //   8617: lor
    //   8618: aload 9
    //   8620: iload 11
    //   8622: iconst_3
    //   8623: iadd
    //   8624: baload
    //   8625: sipush 255
    //   8628: iand
    //   8629: i2l
    //   8630: bipush 24
    //   8632: lshl
    //   8633: lor
    //   8634: aload 9
    //   8636: iload 11
    //   8638: iconst_4
    //   8639: iadd
    //   8640: baload
    //   8641: sipush 255
    //   8644: iand
    //   8645: i2l
    //   8646: bipush 32
    //   8648: lshl
    //   8649: lor
    //   8650: lstore 12
    //   8652: lload 12
    //   8654: ldc2_w 1545
    //   8657: lcmp
    //   8658: ifge +29 -> 8687
    //   8661: new 192	java/lang/StringBuilder
    //   8664: dup
    //   8665: aload 10
    //   8667: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8670: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8673: ldc_w 1547
    //   8676: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8679: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8682: astore 10
    //   8684: goto +210 -> 8894
    //   8687: lload 12
    //   8689: ldc2_w 1549
    //   8692: lcmp
    //   8693: ifge +29 -> 8722
    //   8696: new 192	java/lang/StringBuilder
    //   8699: dup
    //   8700: aload 10
    //   8702: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8705: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8708: ldc_w 1551
    //   8711: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8714: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8717: astore 10
    //   8719: goto +175 -> 8894
    //   8722: lload 12
    //   8724: ldc2_w 1553
    //   8727: lcmp
    //   8728: ifge +29 -> 8757
    //   8731: new 192	java/lang/StringBuilder
    //   8734: dup
    //   8735: aload 10
    //   8737: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8740: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8743: ldc_w 1555
    //   8746: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8749: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8752: astore 10
    //   8754: goto +140 -> 8894
    //   8757: lload 12
    //   8759: ldc2_w 1557
    //   8762: lcmp
    //   8763: ifge +29 -> 8792
    //   8766: new 192	java/lang/StringBuilder
    //   8769: dup
    //   8770: aload 10
    //   8772: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8775: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8778: ldc_w 1559
    //   8781: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8784: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8787: astore 10
    //   8789: goto +105 -> 8894
    //   8792: lload 12
    //   8794: ldc2_w 1561
    //   8797: lcmp
    //   8798: ifge +29 -> 8827
    //   8801: new 192	java/lang/StringBuilder
    //   8804: dup
    //   8805: aload 10
    //   8807: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8810: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8813: ldc_w 1136
    //   8816: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8819: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8822: astore 10
    //   8824: goto +70 -> 8894
    //   8827: lload 12
    //   8829: ldc2_w 1563
    //   8832: lcmp
    //   8833: ifge +29 -> 8862
    //   8836: new 192	java/lang/StringBuilder
    //   8839: dup
    //   8840: aload 10
    //   8842: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8845: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8848: ldc_w 1138
    //   8851: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8854: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8857: astore 10
    //   8859: goto +35 -> 8894
    //   8862: lload 12
    //   8864: ldc2_w 1565
    //   8867: lcmp
    //   8868: ifge +26 -> 8894
    //   8871: new 192	java/lang/StringBuilder
    //   8874: dup
    //   8875: aload 10
    //   8877: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8880: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8883: ldc_w 1140
    //   8886: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8889: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8892: astore 10
    //   8894: new 192	java/lang/StringBuilder
    //   8897: dup
    //   8898: aload 10
    //   8900: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   8903: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   8906: lload 12
    //   8908: bipush 32
    //   8910: invokestatic 1567	java/lang/Long:toString	(JI)Ljava/lang/String;
    //   8913: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   8916: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   8919: astore 10
    //   8921: iinc 11 5
    //   8924: iload 11
    //   8926: bipush 100
    //   8928: if_icmplt -352 -> 8576
    //   8931: new 911	org/json/simple/JSONObject
    //   8934: dup
    //   8935: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   8938: astore 11
    //   8940: aload 11
    //   8942: ldc_w 1517
    //   8945: ldc_w 1570
    //   8948: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   8951: pop
    //   8952: aload 11
    //   8954: ldc_w 1169
    //   8957: aload 10
    //   8959: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   8962: pop
    //   8963: aload_3
    //   8964: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   8967: aload 11
    //   8969: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   8972: pop
    //   8973: goto +4269 -> 13242
    //   8976: new 1060	org/json/simple/JSONArray
    //   8979: dup
    //   8980: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   8983: astore 6
    //   8985: new 1060	org/json/simple/JSONArray
    //   8988: dup
    //   8989: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   8992: astore 7
    //   8994: new 1060	org/json/simple/JSONArray
    //   8997: dup
    //   8998: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   9001: astore 8
    //   9003: new 1060	org/json/simple/JSONArray
    //   9006: dup
    //   9007: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   9010: astore 9
    //   9012: new 1060	org/json/simple/JSONArray
    //   9015: dup
    //   9016: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   9019: astore 10
    //   9021: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   9024: dup
    //   9025: astore 11
    //   9027: monitorenter
    //   9028: getstatic 124	Nxt:unconfirmedTransactions	Ljava/util/concurrent/ConcurrentHashMap;
    //   9031: invokevirtual 1581	java/util/concurrent/ConcurrentHashMap:values	()Ljava/util/Collection;
    //   9034: invokeinterface 741 1 0
    //   9039: astore 13
    //   9041: goto +154 -> 9195
    //   9044: aload 13
    //   9046: invokeinterface 747 1 0
    //   9051: checkcast 524	Nxt$Transaction
    //   9054: astore 12
    //   9056: new 911	org/json/simple/JSONObject
    //   9059: dup
    //   9060: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   9063: astore 14
    //   9065: aload 14
    //   9067: ldc_w 1582
    //   9070: aload 12
    //   9072: getfield 754	Nxt$Transaction:index	I
    //   9075: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9078: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9081: pop
    //   9082: aload 14
    //   9084: ldc_w 1183
    //   9087: aload 12
    //   9089: getfield 1583	Nxt$Transaction:timestamp	I
    //   9092: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9095: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9098: pop
    //   9099: aload 14
    //   9101: ldc_w 990
    //   9104: aload 12
    //   9106: getfield 1584	Nxt$Transaction:deadline	S
    //   9109: invokestatic 1587	java/lang/Short:valueOf	(S)Ljava/lang/Short;
    //   9112: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9115: pop
    //   9116: aload 14
    //   9118: ldc_w 1486
    //   9121: aload 12
    //   9123: getfield 1221	Nxt$Transaction:recipient	J
    //   9126: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   9129: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9132: pop
    //   9133: aload 14
    //   9135: ldc_w 1487
    //   9138: aload 12
    //   9140: getfield 1590	Nxt$Transaction:amount	I
    //   9143: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9146: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9149: pop
    //   9150: aload 14
    //   9152: ldc_w 988
    //   9155: aload 12
    //   9157: getfield 1592	Nxt$Transaction:fee	I
    //   9160: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9163: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9166: pop
    //   9167: aload 14
    //   9169: ldc_w 1429
    //   9172: aload 12
    //   9174: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   9177: invokestatic 1029	Nxt$Account:getId	([B)J
    //   9180: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   9183: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9186: pop
    //   9187: aload 6
    //   9189: aload 14
    //   9191: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   9194: pop
    //   9195: aload 13
    //   9197: invokeinterface 760 1 0
    //   9202: ifne -158 -> 9044
    //   9205: aload 11
    //   9207: monitorexit
    //   9208: goto +7 -> 9215
    //   9211: aload 11
    //   9213: monitorexit
    //   9214: athrow
    //   9215: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   9218: dup
    //   9219: astore 11
    //   9221: monitorenter
    //   9222: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   9225: invokevirtual 1207	java/util/HashMap:entrySet	()Ljava/util/Set;
    //   9228: invokeinterface 797 1 0
    //   9233: astore 13
    //   9235: goto +757 -> 9992
    //   9238: aload 13
    //   9240: invokeinterface 747 1 0
    //   9245: checkcast 1210	java/util/Map$Entry
    //   9248: astore 12
    //   9250: aload 12
    //   9252: invokeinterface 1224 1 0
    //   9257: checkcast 223	java/lang/String
    //   9260: astore 14
    //   9262: aload 12
    //   9264: invokeinterface 1212 1 0
    //   9269: checkcast 461	Nxt$Peer
    //   9272: astore 15
    //   9274: aload 15
    //   9276: getfield 1594	Nxt$Peer:blacklistingTime	J
    //   9279: lconst_0
    //   9280: lcmp
    //   9281: ifle +178 -> 9459
    //   9284: new 911	org/json/simple/JSONObject
    //   9287: dup
    //   9288: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   9291: astore 16
    //   9293: aload 16
    //   9295: ldc_w 1582
    //   9298: aload 15
    //   9300: getfield 1597	Nxt$Peer:index	I
    //   9303: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9306: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9309: pop
    //   9310: aload 16
    //   9312: ldc_w 1356
    //   9315: aload 15
    //   9317: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9320: invokevirtual 222	java/lang/String:length	()I
    //   9323: ifle +57 -> 9380
    //   9326: aload 15
    //   9328: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9331: invokevirtual 222	java/lang/String:length	()I
    //   9334: bipush 30
    //   9336: if_icmple +36 -> 9372
    //   9339: new 192	java/lang/StringBuilder
    //   9342: dup
    //   9343: aload 15
    //   9345: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9348: iconst_0
    //   9349: bipush 30
    //   9351: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   9354: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   9357: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   9360: ldc_w 1598
    //   9363: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9366: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   9369: goto +13 -> 9382
    //   9372: aload 15
    //   9374: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9377: goto +5 -> 9382
    //   9380: aload 14
    //   9382: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9385: pop
    //   9386: getstatic 131	Nxt:wellKnownPeers	Ljava/util/HashSet;
    //   9389: invokevirtual 1600	java/util/HashSet:iterator	()Ljava/util/Iterator;
    //   9392: astore 18
    //   9394: goto +44 -> 9438
    //   9397: aload 18
    //   9399: invokeinterface 747 1 0
    //   9404: checkcast 223	java/lang/String
    //   9407: astore 17
    //   9409: aload 15
    //   9411: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9414: aload 17
    //   9416: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   9419: ifeq +19 -> 9438
    //   9422: aload 16
    //   9424: ldc_w 1601
    //   9427: iconst_1
    //   9428: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   9431: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9434: pop
    //   9435: goto +13 -> 9448
    //   9438: aload 18
    //   9440: invokeinterface 760 1 0
    //   9445: ifne -48 -> 9397
    //   9448: aload 9
    //   9450: aload 16
    //   9452: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   9455: pop
    //   9456: goto +536 -> 9992
    //   9459: aload 15
    //   9461: getfield 1354	Nxt$Peer:state	I
    //   9464: ifne +173 -> 9637
    //   9467: aload 15
    //   9469: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9472: invokevirtual 222	java/lang/String:length	()I
    //   9475: ifle +517 -> 9992
    //   9478: new 911	org/json/simple/JSONObject
    //   9481: dup
    //   9482: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   9485: astore 16
    //   9487: aload 16
    //   9489: ldc_w 1582
    //   9492: aload 15
    //   9494: getfield 1597	Nxt$Peer:index	I
    //   9497: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9500: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9503: pop
    //   9504: aload 16
    //   9506: ldc_w 1356
    //   9509: aload 15
    //   9511: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9514: invokevirtual 222	java/lang/String:length	()I
    //   9517: bipush 30
    //   9519: if_icmple +36 -> 9555
    //   9522: new 192	java/lang/StringBuilder
    //   9525: dup
    //   9526: aload 15
    //   9528: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9531: iconst_0
    //   9532: bipush 30
    //   9534: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   9537: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   9540: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   9543: ldc_w 1598
    //   9546: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9549: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   9552: goto +8 -> 9560
    //   9555: aload 15
    //   9557: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9560: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9563: pop
    //   9564: getstatic 131	Nxt:wellKnownPeers	Ljava/util/HashSet;
    //   9567: invokevirtual 1600	java/util/HashSet:iterator	()Ljava/util/Iterator;
    //   9570: astore 18
    //   9572: goto +44 -> 9616
    //   9575: aload 18
    //   9577: invokeinterface 747 1 0
    //   9582: checkcast 223	java/lang/String
    //   9585: astore 17
    //   9587: aload 15
    //   9589: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9592: aload 17
    //   9594: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   9597: ifeq +19 -> 9616
    //   9600: aload 16
    //   9602: ldc_w 1601
    //   9605: iconst_1
    //   9606: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   9609: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9612: pop
    //   9613: goto +13 -> 9626
    //   9616: aload 18
    //   9618: invokeinterface 760 1 0
    //   9623: ifne -48 -> 9575
    //   9626: aload 8
    //   9628: aload 16
    //   9630: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   9633: pop
    //   9634: goto +358 -> 9992
    //   9637: new 911	org/json/simple/JSONObject
    //   9640: dup
    //   9641: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   9644: astore 16
    //   9646: aload 16
    //   9648: ldc_w 1582
    //   9651: aload 15
    //   9653: getfield 1597	Nxt$Peer:index	I
    //   9656: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9659: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9662: pop
    //   9663: aload 15
    //   9665: getfield 1354	Nxt$Peer:state	I
    //   9668: iconst_2
    //   9669: if_icmpne +16 -> 9685
    //   9672: aload 16
    //   9674: ldc_w 1603
    //   9677: iconst_1
    //   9678: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   9681: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9684: pop
    //   9685: aload 16
    //   9687: ldc_w 1341
    //   9690: aload 14
    //   9692: invokevirtual 222	java/lang/String:length	()I
    //   9695: bipush 30
    //   9697: if_icmple +33 -> 9730
    //   9700: new 192	java/lang/StringBuilder
    //   9703: dup
    //   9704: aload 14
    //   9706: iconst_0
    //   9707: bipush 30
    //   9709: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   9712: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   9715: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   9718: ldc_w 1598
    //   9721: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9724: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   9727: goto +5 -> 9732
    //   9730: aload 14
    //   9732: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9735: pop
    //   9736: aload 16
    //   9738: ldc_w 1356
    //   9741: aload 15
    //   9743: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9746: invokevirtual 222	java/lang/String:length	()I
    //   9749: bipush 30
    //   9751: if_icmple +36 -> 9787
    //   9754: new 192	java/lang/StringBuilder
    //   9757: dup
    //   9758: aload 15
    //   9760: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9763: iconst_0
    //   9764: bipush 30
    //   9766: invokevirtual 228	java/lang/String:substring	(II)Ljava/lang/String;
    //   9769: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   9772: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   9775: ldc_w 1598
    //   9778: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9781: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   9784: goto +8 -> 9792
    //   9787: aload 15
    //   9789: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9792: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9795: pop
    //   9796: aload 16
    //   9798: ldc_w 1132
    //   9801: aload 15
    //   9803: invokevirtual 1362	Nxt$Peer:getWeight	()I
    //   9806: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   9809: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9812: pop
    //   9813: aload 16
    //   9815: ldc_w 1605
    //   9818: aload 15
    //   9820: getfield 1367	Nxt$Peer:downloadedVolume	J
    //   9823: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   9826: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9829: pop
    //   9830: aload 16
    //   9832: ldc_w 1607
    //   9835: aload 15
    //   9837: getfield 1371	Nxt$Peer:uploadedVolume	J
    //   9840: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   9843: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9846: pop
    //   9847: aload 16
    //   9849: ldc_w 1609
    //   9852: new 192	java/lang/StringBuilder
    //   9855: dup
    //   9856: aload 15
    //   9858: getfield 1375	Nxt$Peer:application	Ljava/lang/String;
    //   9861: ifnonnull +9 -> 9870
    //   9864: ldc_w 1611
    //   9867: goto +8 -> 9875
    //   9870: aload 15
    //   9872: getfield 1375	Nxt$Peer:application	Ljava/lang/String;
    //   9875: invokestatic 1144	java/lang/String:valueOf	(Ljava/lang/Object;)Ljava/lang/String;
    //   9878: invokespecial 206	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   9881: ldc_w 1613
    //   9884: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9887: aload 15
    //   9889: getfield 1377	Nxt$Peer:version	Ljava/lang/String;
    //   9892: ifnonnull +9 -> 9901
    //   9895: ldc_w 1611
    //   9898: goto +8 -> 9906
    //   9901: aload 15
    //   9903: getfield 1377	Nxt$Peer:version	Ljava/lang/String;
    //   9906: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9909: ldc_w 1615
    //   9912: invokevirtual 207	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   9915: invokevirtual 211	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   9918: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9921: pop
    //   9922: getstatic 131	Nxt:wellKnownPeers	Ljava/util/HashSet;
    //   9925: invokevirtual 1600	java/util/HashSet:iterator	()Ljava/util/Iterator;
    //   9928: astore 18
    //   9930: goto +44 -> 9974
    //   9933: aload 18
    //   9935: invokeinterface 747 1 0
    //   9940: checkcast 223	java/lang/String
    //   9943: astore 17
    //   9945: aload 15
    //   9947: getfield 1358	Nxt$Peer:announcedAddress	Ljava/lang/String;
    //   9950: aload 17
    //   9952: invokevirtual 425	java/lang/String:equals	(Ljava/lang/Object;)Z
    //   9955: ifeq +19 -> 9974
    //   9958: aload 16
    //   9960: ldc_w 1601
    //   9963: iconst_1
    //   9964: invokestatic 1162	java/lang/Boolean:valueOf	(Z)Ljava/lang/Boolean;
    //   9967: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   9970: pop
    //   9971: goto +13 -> 9984
    //   9974: aload 18
    //   9976: invokeinterface 760 1 0
    //   9981: ifne -48 -> 9933
    //   9984: aload 7
    //   9986: aload 16
    //   9988: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   9991: pop
    //   9992: aload 13
    //   9994: invokeinterface 760 1 0
    //   9999: ifne -761 -> 9238
    //   10002: aload 11
    //   10004: monitorexit
    //   10005: goto +7 -> 10012
    //   10008: aload 11
    //   10010: monitorexit
    //   10011: athrow
    //   10012: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   10015: dup
    //   10016: astore 11
    //   10018: monitorenter
    //   10019: getstatic 831	Nxt:lastBlock	J
    //   10022: lstore 12
    //   10024: invokestatic 1434	Nxt$Block:getLastBlock	()LNxt$Block;
    //   10027: getfield 1255	Nxt$Block:height	I
    //   10030: istore 14
    //   10032: iconst_0
    //   10033: istore 15
    //   10035: goto +263 -> 10298
    //   10038: iinc 15 1
    //   10041: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   10044: lload 12
    //   10046: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   10049: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   10052: checkcast 771	Nxt$Block
    //   10055: astore 16
    //   10057: new 911	org/json/simple/JSONObject
    //   10060: dup
    //   10061: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10064: astore 17
    //   10066: aload 17
    //   10068: ldc_w 1582
    //   10071: aload 16
    //   10073: getfield 786	Nxt$Block:index	I
    //   10076: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10079: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10082: pop
    //   10083: aload 17
    //   10085: ldc_w 1183
    //   10088: aload 16
    //   10090: getfield 1215	Nxt$Block:timestamp	I
    //   10093: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10096: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10099: pop
    //   10100: aload 17
    //   10102: ldc_w 1262
    //   10105: aload 16
    //   10107: getfield 787	Nxt$Block:numberOfTransactions	I
    //   10110: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10113: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10116: pop
    //   10117: aload 17
    //   10119: ldc_w 1263
    //   10122: aload 16
    //   10124: getfield 1265	Nxt$Block:totalAmount	I
    //   10127: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10130: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10133: pop
    //   10134: aload 17
    //   10136: ldc_w 1267
    //   10139: aload 16
    //   10141: getfield 1269	Nxt$Block:totalFee	I
    //   10144: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10147: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10150: pop
    //   10151: aload 17
    //   10153: ldc_w 1271
    //   10156: aload 16
    //   10158: getfield 1273	Nxt$Block:payloadLength	I
    //   10161: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10164: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10167: pop
    //   10168: aload 17
    //   10170: ldc_w 1257
    //   10173: aload 16
    //   10175: getfield 1259	Nxt$Block:generatorPublicKey	[B
    //   10178: invokestatic 1029	Nxt$Account:getId	([B)J
    //   10181: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   10184: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10187: pop
    //   10188: aload 17
    //   10190: ldc_w 1254
    //   10193: iload 14
    //   10195: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10198: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10201: pop
    //   10202: aload 17
    //   10204: ldc_w 1275
    //   10207: aload 16
    //   10209: getfield 1277	Nxt$Block:version	I
    //   10212: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   10215: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10218: pop
    //   10219: aload 17
    //   10221: ldc_w 1249
    //   10224: lload 12
    //   10226: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   10229: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10232: pop
    //   10233: aload 17
    //   10235: ldc_w 1279
    //   10238: aload 16
    //   10240: getfield 828	Nxt$Block:baseTarget	J
    //   10243: invokestatic 251	java/math/BigInteger:valueOf	(J)Ljava/math/BigInteger;
    //   10246: ldc2_w 1617
    //   10249: invokestatic 251	java/math/BigInteger:valueOf	(J)Ljava/math/BigInteger;
    //   10252: invokevirtual 1619	java/math/BigInteger:multiply	(Ljava/math/BigInteger;)Ljava/math/BigInteger;
    //   10255: ldc2_w 23
    //   10258: invokestatic 251	java/math/BigInteger:valueOf	(J)Ljava/math/BigInteger;
    //   10261: invokevirtual 1622	java/math/BigInteger:divide	(Ljava/math/BigInteger;)Ljava/math/BigInteger;
    //   10264: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10267: pop
    //   10268: aload 10
    //   10270: aload 17
    //   10272: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   10275: pop
    //   10276: lload 12
    //   10278: ldc2_w 12
    //   10281: lcmp
    //   10282: ifne +6 -> 10288
    //   10285: goto +20 -> 10305
    //   10288: aload 16
    //   10290: getfield 1280	Nxt$Block:previousBlock	J
    //   10293: lstore 12
    //   10295: iinc 14 255
    //   10298: iload 15
    //   10300: bipush 60
    //   10302: if_icmplt -264 -> 10038
    //   10305: aload 11
    //   10307: monitorexit
    //   10308: goto +7 -> 10315
    //   10311: aload 11
    //   10313: monitorexit
    //   10314: athrow
    //   10315: new 911	org/json/simple/JSONObject
    //   10318: dup
    //   10319: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10322: astore 11
    //   10324: aload 11
    //   10326: ldc_w 1517
    //   10329: ldc_w 1625
    //   10332: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10335: pop
    //   10336: aload 11
    //   10338: ldc_w 1275
    //   10341: ldc 8
    //   10343: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10346: pop
    //   10347: aload 6
    //   10349: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   10352: ifle +14 -> 10366
    //   10355: aload 11
    //   10357: ldc_w 1628
    //   10360: aload 6
    //   10362: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10365: pop
    //   10366: aload 7
    //   10368: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   10371: ifle +14 -> 10385
    //   10374: aload 11
    //   10376: ldc_w 1629
    //   10379: aload 7
    //   10381: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10384: pop
    //   10385: aload 8
    //   10387: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   10390: ifle +14 -> 10404
    //   10393: aload 11
    //   10395: ldc_w 1631
    //   10398: aload 8
    //   10400: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10403: pop
    //   10404: aload 9
    //   10406: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   10409: ifle +14 -> 10423
    //   10412: aload 11
    //   10414: ldc_w 1633
    //   10417: aload 9
    //   10419: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10422: pop
    //   10423: aload 10
    //   10425: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   10428: ifle +14 -> 10442
    //   10431: aload 11
    //   10433: ldc_w 1635
    //   10436: aload 10
    //   10438: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10441: pop
    //   10442: aload_3
    //   10443: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   10446: aload 11
    //   10448: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   10451: pop
    //   10452: goto +2790 -> 13242
    //   10455: aload_3
    //   10456: invokevirtual 1637	Nxt$User:deinitializeKeyPair	()V
    //   10459: new 911	org/json/simple/JSONObject
    //   10462: dup
    //   10463: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10466: astore 6
    //   10468: aload 6
    //   10470: ldc_w 1517
    //   10473: ldc_w 1527
    //   10476: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10479: pop
    //   10480: aload_3
    //   10481: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   10484: aload 6
    //   10486: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   10489: pop
    //   10490: goto +2752 -> 13242
    //   10493: getstatic 501	Nxt:allowedUserHosts	Ljava/util/HashSet;
    //   10496: ifnonnull +64 -> 10560
    //   10499: aload_1
    //   10500: invokeinterface 1343 1 0
    //   10505: invokestatic 1640	java/net/InetAddress:getByName	(Ljava/lang/String;)Ljava/net/InetAddress;
    //   10508: invokevirtual 1646	java/net/InetAddress:isLoopbackAddress	()Z
    //   10511: ifne +49 -> 10560
    //   10514: new 911	org/json/simple/JSONObject
    //   10517: dup
    //   10518: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10521: astore 6
    //   10523: aload 6
    //   10525: ldc_w 1517
    //   10528: ldc_w 1649
    //   10531: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10534: pop
    //   10535: aload 6
    //   10537: ldc_w 1651
    //   10540: ldc_w 1653
    //   10543: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10546: pop
    //   10547: aload_3
    //   10548: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   10551: aload 6
    //   10553: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   10556: pop
    //   10557: goto +2685 -> 13242
    //   10560: aload_1
    //   10561: ldc_w 1346
    //   10564: invokeinterface 906 2 0
    //   10569: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   10572: istore 6
    //   10574: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   10577: invokevirtual 737	java/util/HashMap:values	()Ljava/util/Collection;
    //   10580: invokeinterface 741 1 0
    //   10585: astore 8
    //   10587: goto +51 -> 10638
    //   10590: aload 8
    //   10592: invokeinterface 747 1 0
    //   10597: checkcast 461	Nxt$Peer
    //   10600: astore 7
    //   10602: aload 7
    //   10604: getfield 1597	Nxt$Peer:index	I
    //   10607: iload 6
    //   10609: if_icmpne +29 -> 10638
    //   10612: aload 7
    //   10614: getfield 1594	Nxt$Peer:blacklistingTime	J
    //   10617: lconst_0
    //   10618: lcmp
    //   10619: ifne +2623 -> 13242
    //   10622: aload 7
    //   10624: getfield 1354	Nxt$Peer:state	I
    //   10627: ifeq +2615 -> 13242
    //   10630: aload 7
    //   10632: invokevirtual 1655	Nxt$Peer:deactivate	()V
    //   10635: goto +2607 -> 13242
    //   10638: aload 8
    //   10640: invokeinterface 760 1 0
    //   10645: ifne -55 -> 10590
    //   10648: goto +2594 -> 13242
    //   10651: getstatic 501	Nxt:allowedUserHosts	Ljava/util/HashSet;
    //   10654: ifnonnull +64 -> 10718
    //   10657: aload_1
    //   10658: invokeinterface 1343 1 0
    //   10663: invokestatic 1640	java/net/InetAddress:getByName	(Ljava/lang/String;)Ljava/net/InetAddress;
    //   10666: invokevirtual 1646	java/net/InetAddress:isLoopbackAddress	()Z
    //   10669: ifne +49 -> 10718
    //   10672: new 911	org/json/simple/JSONObject
    //   10675: dup
    //   10676: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10679: astore 6
    //   10681: aload 6
    //   10683: ldc_w 1517
    //   10686: ldc_w 1649
    //   10689: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10692: pop
    //   10693: aload 6
    //   10695: ldc_w 1651
    //   10698: ldc_w 1653
    //   10701: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10704: pop
    //   10705: aload_3
    //   10706: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   10709: aload 6
    //   10711: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   10714: pop
    //   10715: goto +2527 -> 13242
    //   10718: aload_1
    //   10719: ldc_w 1346
    //   10722: invokeinterface 906 2 0
    //   10727: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   10730: istore 6
    //   10732: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   10735: invokevirtual 737	java/util/HashMap:values	()Ljava/util/Collection;
    //   10738: invokeinterface 741 1 0
    //   10743: astore 8
    //   10745: goto +43 -> 10788
    //   10748: aload 8
    //   10750: invokeinterface 747 1 0
    //   10755: checkcast 461	Nxt$Peer
    //   10758: astore 7
    //   10760: aload 7
    //   10762: getfield 1597	Nxt$Peer:index	I
    //   10765: iload 6
    //   10767: if_icmpne +21 -> 10788
    //   10770: aload 7
    //   10772: getfield 1594	Nxt$Peer:blacklistingTime	J
    //   10775: lconst_0
    //   10776: lcmp
    //   10777: ifle +2465 -> 13242
    //   10780: aload 7
    //   10782: invokevirtual 1658	Nxt$Peer:removeBlacklistedStatus	()V
    //   10785: goto +2457 -> 13242
    //   10788: aload 8
    //   10790: invokeinterface 760 1 0
    //   10795: ifne -47 -> 10748
    //   10798: goto +2444 -> 13242
    //   10801: getstatic 501	Nxt:allowedUserHosts	Ljava/util/HashSet;
    //   10804: ifnonnull +64 -> 10868
    //   10807: aload_1
    //   10808: invokeinterface 1343 1 0
    //   10813: invokestatic 1640	java/net/InetAddress:getByName	(Ljava/lang/String;)Ljava/net/InetAddress;
    //   10816: invokevirtual 1646	java/net/InetAddress:isLoopbackAddress	()Z
    //   10819: ifne +49 -> 10868
    //   10822: new 911	org/json/simple/JSONObject
    //   10825: dup
    //   10826: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   10829: astore 6
    //   10831: aload 6
    //   10833: ldc_w 1517
    //   10836: ldc_w 1649
    //   10839: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10842: pop
    //   10843: aload 6
    //   10845: ldc_w 1651
    //   10848: ldc_w 1653
    //   10851: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   10854: pop
    //   10855: aload_3
    //   10856: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   10859: aload 6
    //   10861: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   10864: pop
    //   10865: goto +2377 -> 13242
    //   10868: aload_1
    //   10869: ldc_w 1346
    //   10872: invokeinterface 906 2 0
    //   10877: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   10880: istore 6
    //   10882: getstatic 136	Nxt:peers	Ljava/util/HashMap;
    //   10885: invokevirtual 737	java/util/HashMap:values	()Ljava/util/Collection;
    //   10888: invokeinterface 741 1 0
    //   10893: astore 8
    //   10895: goto +33 -> 10928
    //   10898: aload 8
    //   10900: invokeinterface 747 1 0
    //   10905: checkcast 461	Nxt$Peer
    //   10908: astore 7
    //   10910: aload 7
    //   10912: getfield 1597	Nxt$Peer:index	I
    //   10915: iload 6
    //   10917: if_icmpne +11 -> 10928
    //   10920: aload 7
    //   10922: invokevirtual 1661	Nxt$Peer:removePeer	()V
    //   10925: goto +2317 -> 13242
    //   10928: aload 8
    //   10930: invokeinterface 760 1 0
    //   10935: ifne -37 -> 10898
    //   10938: goto +2304 -> 13242
    //   10941: aload_3
    //   10942: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   10945: ifnull +2297 -> 13242
    //   10948: aload_1
    //   10949: ldc_w 1486
    //   10952: invokeinterface 906 2 0
    //   10957: astore 6
    //   10959: aload_1
    //   10960: ldc_w 1487
    //   10963: invokeinterface 906 2 0
    //   10968: astore 7
    //   10970: aload_1
    //   10971: ldc_w 988
    //   10974: invokeinterface 906 2 0
    //   10979: astore 8
    //   10981: aload_1
    //   10982: ldc_w 990
    //   10985: invokeinterface 906 2 0
    //   10990: astore 9
    //   10992: iconst_0
    //   10993: istore 12
    //   10995: iconst_0
    //   10996: istore 13
    //   10998: iconst_0
    //   10999: istore 14
    //   11001: new 110	java/math/BigInteger
    //   11004: dup
    //   11005: aload 6
    //   11007: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   11010: invokespecial 114	java/math/BigInteger:<init>	(Ljava/lang/String;)V
    //   11013: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   11016: lstore 10
    //   11018: aload 7
    //   11020: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   11023: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   11026: istore 12
    //   11028: aload 8
    //   11030: invokevirtual 412	java/lang/String:trim	()Ljava/lang/String;
    //   11033: invokestatic 418	java/lang/Integer:parseInt	(Ljava/lang/String;)I
    //   11036: istore 13
    //   11038: aload 9
    //   11040: invokestatic 1664	java/lang/Double:parseDouble	(Ljava/lang/String;)D
    //   11043: ldc2_w 1670
    //   11046: dmul
    //   11047: d2i
    //   11048: i2s
    //   11049: istore 14
    //   11051: goto +94 -> 11145
    //   11054: pop
    //   11055: new 911	org/json/simple/JSONObject
    //   11058: dup
    //   11059: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11062: astore 15
    //   11064: aload 15
    //   11066: ldc_w 1517
    //   11069: ldc_w 1672
    //   11072: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11075: pop
    //   11076: aload 15
    //   11078: ldc_w 1651
    //   11081: ldc_w 1674
    //   11084: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11087: pop
    //   11088: aload 15
    //   11090: ldc_w 1486
    //   11093: aload 6
    //   11095: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11098: pop
    //   11099: aload 15
    //   11101: ldc_w 1487
    //   11104: aload 7
    //   11106: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11109: pop
    //   11110: aload 15
    //   11112: ldc_w 988
    //   11115: aload 8
    //   11117: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11120: pop
    //   11121: aload 15
    //   11123: ldc_w 990
    //   11126: aload 9
    //   11128: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11131: pop
    //   11132: aload_3
    //   11133: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11136: aload 15
    //   11138: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11141: pop
    //   11142: goto +2100 -> 13242
    //   11145: iload 12
    //   11147: ifgt +93 -> 11240
    //   11150: new 911	org/json/simple/JSONObject
    //   11153: dup
    //   11154: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11157: astore 15
    //   11159: aload 15
    //   11161: ldc_w 1517
    //   11164: ldc_w 1672
    //   11167: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11170: pop
    //   11171: aload 15
    //   11173: ldc_w 1651
    //   11176: ldc_w 1676
    //   11179: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11182: pop
    //   11183: aload 15
    //   11185: ldc_w 1486
    //   11188: aload 6
    //   11190: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11193: pop
    //   11194: aload 15
    //   11196: ldc_w 1487
    //   11199: aload 7
    //   11201: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11204: pop
    //   11205: aload 15
    //   11207: ldc_w 988
    //   11210: aload 8
    //   11212: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11215: pop
    //   11216: aload 15
    //   11218: ldc_w 990
    //   11221: aload 9
    //   11223: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11226: pop
    //   11227: aload_3
    //   11228: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11231: aload 15
    //   11233: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11236: pop
    //   11237: goto +2005 -> 13242
    //   11240: iload 13
    //   11242: ifgt +93 -> 11335
    //   11245: new 911	org/json/simple/JSONObject
    //   11248: dup
    //   11249: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11252: astore 15
    //   11254: aload 15
    //   11256: ldc_w 1517
    //   11259: ldc_w 1672
    //   11262: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11265: pop
    //   11266: aload 15
    //   11268: ldc_w 1651
    //   11271: ldc_w 1678
    //   11274: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11277: pop
    //   11278: aload 15
    //   11280: ldc_w 1486
    //   11283: aload 6
    //   11285: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11288: pop
    //   11289: aload 15
    //   11291: ldc_w 1487
    //   11294: aload 7
    //   11296: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11299: pop
    //   11300: aload 15
    //   11302: ldc_w 988
    //   11305: aload 8
    //   11307: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11310: pop
    //   11311: aload 15
    //   11313: ldc_w 990
    //   11316: aload 9
    //   11318: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11321: pop
    //   11322: aload_3
    //   11323: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11326: aload 15
    //   11328: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11331: pop
    //   11332: goto +1910 -> 13242
    //   11335: iload 14
    //   11337: iconst_1
    //   11338: if_icmpge +93 -> 11431
    //   11341: new 911	org/json/simple/JSONObject
    //   11344: dup
    //   11345: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11348: astore 15
    //   11350: aload 15
    //   11352: ldc_w 1517
    //   11355: ldc_w 1672
    //   11358: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11361: pop
    //   11362: aload 15
    //   11364: ldc_w 1651
    //   11367: ldc_w 1680
    //   11370: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11373: pop
    //   11374: aload 15
    //   11376: ldc_w 1486
    //   11379: aload 6
    //   11381: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11384: pop
    //   11385: aload 15
    //   11387: ldc_w 1487
    //   11390: aload 7
    //   11392: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11395: pop
    //   11396: aload 15
    //   11398: ldc_w 988
    //   11401: aload 8
    //   11403: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11406: pop
    //   11407: aload 15
    //   11409: ldc_w 990
    //   11412: aload 9
    //   11414: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11417: pop
    //   11418: aload_3
    //   11419: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11422: aload 15
    //   11424: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11427: pop
    //   11428: goto +1814 -> 13242
    //   11431: aload_3
    //   11432: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   11435: invokestatic 1024	Nxt$Crypto:getPublicKey	(Ljava/lang/String;)[B
    //   11438: astore 15
    //   11440: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   11443: aload 15
    //   11445: invokestatic 1029	Nxt$Account:getId	([B)J
    //   11448: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   11451: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   11454: checkcast 311	Nxt$Account
    //   11457: astore 16
    //   11459: aload 16
    //   11461: ifnull +22 -> 11483
    //   11464: iload 12
    //   11466: iload 13
    //   11468: iadd
    //   11469: i2l
    //   11470: ldc2_w 1034
    //   11473: lmul
    //   11474: aload 16
    //   11476: getfield 318	Nxt$Account:unconfirmedBalance	J
    //   11479: lcmp
    //   11480: ifle +93 -> 11573
    //   11483: new 911	org/json/simple/JSONObject
    //   11486: dup
    //   11487: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11490: astore 17
    //   11492: aload 17
    //   11494: ldc_w 1517
    //   11497: ldc_w 1672
    //   11500: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11503: pop
    //   11504: aload 17
    //   11506: ldc_w 1651
    //   11509: ldc_w 1682
    //   11512: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11515: pop
    //   11516: aload 17
    //   11518: ldc_w 1486
    //   11521: aload 6
    //   11523: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11526: pop
    //   11527: aload 17
    //   11529: ldc_w 1487
    //   11532: aload 7
    //   11534: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11537: pop
    //   11538: aload 17
    //   11540: ldc_w 988
    //   11543: aload 8
    //   11545: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11548: pop
    //   11549: aload 17
    //   11551: ldc_w 990
    //   11554: aload 9
    //   11556: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11559: pop
    //   11560: aload_3
    //   11561: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11564: aload 17
    //   11566: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11569: pop
    //   11570: goto +1672 -> 13242
    //   11573: new 524	Nxt$Transaction
    //   11576: dup
    //   11577: iconst_0
    //   11578: iconst_0
    //   11579: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   11582: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   11585: iload 14
    //   11587: aload 15
    //   11589: lload 10
    //   11591: iload 12
    //   11593: iload 13
    //   11595: lconst_0
    //   11596: bipush 64
    //   11598: newarray byte
    //   11600: invokespecial 731	Nxt$Transaction:<init>	(BBIS[BJIIJ[B)V
    //   11603: astore 17
    //   11605: aload 17
    //   11607: aload_3
    //   11608: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   11611: invokevirtual 1055	Nxt$Transaction:sign	(Ljava/lang/String;)V
    //   11614: new 911	org/json/simple/JSONObject
    //   11617: dup
    //   11618: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11621: astore 18
    //   11623: aload 18
    //   11625: ldc_w 927
    //   11628: ldc_w 1058
    //   11631: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11634: pop
    //   11635: new 1060	org/json/simple/JSONArray
    //   11638: dup
    //   11639: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   11642: astore 19
    //   11644: aload 19
    //   11646: aload 17
    //   11648: invokevirtual 1063	Nxt$Transaction:getJSONObject	()Lorg/json/simple/JSONObject;
    //   11651: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   11654: pop
    //   11655: aload 18
    //   11657: ldc_w 1068
    //   11660: aload 19
    //   11662: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11665: pop
    //   11666: aload 18
    //   11668: invokestatic 1069	Nxt$Peer:sendToAllPeers	(Lorg/json/simple/JSONObject;)V
    //   11671: new 911	org/json/simple/JSONObject
    //   11674: dup
    //   11675: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11678: astore 20
    //   11680: aload 20
    //   11682: ldc_w 1517
    //   11685: ldc_w 1684
    //   11688: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11691: pop
    //   11692: aload_3
    //   11693: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   11696: aload 20
    //   11698: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   11701: pop
    //   11702: goto +1540 -> 13242
    //   11705: aload_1
    //   11706: ldc_w 982
    //   11709: invokeinterface 906 2 0
    //   11714: astore 6
    //   11716: aload_3
    //   11717: aload 6
    //   11719: invokevirtual 1686	Nxt$User:initializeKeyPair	(Ljava/lang/String;)Ljava/math/BigInteger;
    //   11722: astore 7
    //   11724: new 911	org/json/simple/JSONObject
    //   11727: dup
    //   11728: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11731: astore 8
    //   11733: aload 8
    //   11735: ldc_w 1517
    //   11738: ldc_w 1535
    //   11741: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11744: pop
    //   11745: aload 8
    //   11747: ldc_w 1129
    //   11750: aload 7
    //   11752: invokevirtual 259	java/math/BigInteger:toString	()Ljava/lang/String;
    //   11755: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11758: pop
    //   11759: aload 6
    //   11761: invokevirtual 222	java/lang/String:length	()I
    //   11764: bipush 30
    //   11766: if_icmpge +19 -> 11785
    //   11769: aload 8
    //   11771: ldc_w 1690
    //   11774: iconst_1
    //   11775: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   11778: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11781: pop
    //   11782: goto +16 -> 11798
    //   11785: aload 8
    //   11787: ldc_w 1690
    //   11790: iconst_5
    //   11791: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   11794: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11797: pop
    //   11798: getstatic 138	Nxt:accounts	Ljava/util/HashMap;
    //   11801: aload 7
    //   11803: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   11806: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   11809: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   11812: checkcast 311	Nxt$Account
    //   11815: astore 9
    //   11817: aload 9
    //   11819: ifnonnull +19 -> 11838
    //   11822: aload 8
    //   11824: ldc_w 1242
    //   11827: iconst_0
    //   11828: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   11831: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11834: pop
    //   11835: goto +1297 -> 13132
    //   11838: aload 8
    //   11840: ldc_w 1242
    //   11843: aload 9
    //   11845: getfield 318	Nxt$Account:unconfirmedBalance	J
    //   11848: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   11851: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11854: pop
    //   11855: aload 9
    //   11857: invokevirtual 1246	Nxt$Account:getEffectiveBalance	()I
    //   11860: ifle +192 -> 12052
    //   11863: new 911	org/json/simple/JSONObject
    //   11866: dup
    //   11867: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   11870: astore 10
    //   11872: aload 10
    //   11874: ldc_w 1517
    //   11877: ldc_w 1692
    //   11880: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   11883: pop
    //   11884: invokestatic 1434	Nxt$Block:getLastBlock	()LNxt$Block;
    //   11887: astore 11
    //   11889: aload 11
    //   11891: getfield 1288	Nxt$Block:generationSignature	[B
    //   11894: aload_3
    //   11895: getfield 1543	Nxt$User:secretPhrase	Ljava/lang/String;
    //   11898: invokestatic 1479	Nxt$Crypto:sign	([BLjava/lang/String;)[B
    //   11901: astore 12
    //   11903: ldc_w 807
    //   11906: invokestatic 809	java/security/MessageDigest:getInstance	(Ljava/lang/String;)Ljava/security/MessageDigest;
    //   11909: aload 12
    //   11911: invokevirtual 1185	java/security/MessageDigest:digest	([B)[B
    //   11914: astore 13
    //   11916: new 110	java/math/BigInteger
    //   11919: dup
    //   11920: iconst_1
    //   11921: bipush 8
    //   11923: newarray byte
    //   11925: dup
    //   11926: iconst_0
    //   11927: aload 13
    //   11929: bipush 7
    //   11931: baload
    //   11932: bastore
    //   11933: dup
    //   11934: iconst_1
    //   11935: aload 13
    //   11937: bipush 6
    //   11939: baload
    //   11940: bastore
    //   11941: dup
    //   11942: iconst_2
    //   11943: aload 13
    //   11945: iconst_5
    //   11946: baload
    //   11947: bastore
    //   11948: dup
    //   11949: iconst_3
    //   11950: aload 13
    //   11952: iconst_4
    //   11953: baload
    //   11954: bastore
    //   11955: dup
    //   11956: iconst_4
    //   11957: aload 13
    //   11959: iconst_3
    //   11960: baload
    //   11961: bastore
    //   11962: dup
    //   11963: iconst_5
    //   11964: aload 13
    //   11966: iconst_2
    //   11967: baload
    //   11968: bastore
    //   11969: dup
    //   11970: bipush 6
    //   11972: aload 13
    //   11974: iconst_1
    //   11975: baload
    //   11976: bastore
    //   11977: dup
    //   11978: bipush 7
    //   11980: aload 13
    //   11982: iconst_0
    //   11983: baload
    //   11984: bastore
    //   11985: invokespecial 1188	java/math/BigInteger:<init>	(I[B)V
    //   11988: astore 14
    //   11990: aload 10
    //   11992: ldc_w 990
    //   11995: aload 14
    //   11997: invokestatic 1694	Nxt$Block:getBaseTarget	()J
    //   12000: invokestatic 251	java/math/BigInteger:valueOf	(J)Ljava/math/BigInteger;
    //   12003: aload 9
    //   12005: invokevirtual 1246	Nxt$Account:getEffectiveBalance	()I
    //   12008: i2l
    //   12009: invokestatic 251	java/math/BigInteger:valueOf	(J)Ljava/math/BigInteger;
    //   12012: invokevirtual 1619	java/math/BigInteger:multiply	(Ljava/math/BigInteger;)Ljava/math/BigInteger;
    //   12015: invokevirtual 1622	java/math/BigInteger:divide	(Ljava/math/BigInteger;)Ljava/math/BigInteger;
    //   12018: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12021: invokestatic 1041	java/lang/System:currentTimeMillis	()J
    //   12024: invokestatic 1044	Nxt:getEpochTime	(J)I
    //   12027: aload 11
    //   12029: getfield 1215	Nxt$Block:timestamp	I
    //   12032: isub
    //   12033: i2l
    //   12034: lsub
    //   12035: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   12038: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12041: pop
    //   12042: aload_3
    //   12043: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   12046: aload 10
    //   12048: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   12051: pop
    //   12052: new 1060	org/json/simple/JSONArray
    //   12055: dup
    //   12056: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   12059: astore 10
    //   12061: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   12064: dup
    //   12065: astore 11
    //   12067: monitorenter
    //   12068: getstatic 124	Nxt:unconfirmedTransactions	Ljava/util/concurrent/ConcurrentHashMap;
    //   12071: invokevirtual 1581	java/util/concurrent/ConcurrentHashMap:values	()Ljava/util/Collection;
    //   12074: invokeinterface 741 1 0
    //   12079: astore 13
    //   12081: goto +381 -> 12462
    //   12084: aload 13
    //   12086: invokeinterface 747 1 0
    //   12091: checkcast 524	Nxt$Transaction
    //   12094: astore 12
    //   12096: aload 12
    //   12098: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   12101: invokestatic 1029	Nxt$Account:getId	([B)J
    //   12104: aload 7
    //   12106: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12109: lcmp
    //   12110: ifne +186 -> 12296
    //   12113: new 911	org/json/simple/JSONObject
    //   12116: dup
    //   12117: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   12120: astore 14
    //   12122: aload 14
    //   12124: ldc_w 1582
    //   12127: aload 12
    //   12129: getfield 754	Nxt$Transaction:index	I
    //   12132: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12135: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12138: pop
    //   12139: aload 14
    //   12141: ldc_w 1697
    //   12144: aload 12
    //   12146: getfield 1583	Nxt$Transaction:timestamp	I
    //   12149: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12152: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12155: pop
    //   12156: aload 14
    //   12158: ldc_w 990
    //   12161: aload 12
    //   12163: getfield 1584	Nxt$Transaction:deadline	S
    //   12166: invokestatic 1587	java/lang/Short:valueOf	(S)Ljava/lang/Short;
    //   12169: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12172: pop
    //   12173: aload 14
    //   12175: ldc_w 1129
    //   12178: aload 12
    //   12180: getfield 1221	Nxt$Transaction:recipient	J
    //   12183: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12186: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12189: pop
    //   12190: aload 14
    //   12192: ldc_w 1699
    //   12195: aload 12
    //   12197: getfield 1590	Nxt$Transaction:amount	I
    //   12200: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12203: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12206: pop
    //   12207: aload 12
    //   12209: getfield 1221	Nxt$Transaction:recipient	J
    //   12212: aload 7
    //   12214: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12217: lcmp
    //   12218: ifne +20 -> 12238
    //   12221: aload 14
    //   12223: ldc_w 1701
    //   12226: aload 12
    //   12228: getfield 1590	Nxt$Transaction:amount	I
    //   12231: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12234: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12237: pop
    //   12238: aload 14
    //   12240: ldc_w 988
    //   12243: aload 12
    //   12245: getfield 1592	Nxt$Transaction:fee	I
    //   12248: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12251: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12254: pop
    //   12255: aload 14
    //   12257: ldc_w 1703
    //   12260: iconst_0
    //   12261: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12264: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12267: pop
    //   12268: aload 14
    //   12270: ldc_w 1705
    //   12273: aload 12
    //   12275: invokevirtual 734	Nxt$Transaction:getId	()J
    //   12278: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12281: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12284: pop
    //   12285: aload 10
    //   12287: aload 14
    //   12289: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   12292: pop
    //   12293: goto +169 -> 12462
    //   12296: aload 12
    //   12298: getfield 1221	Nxt$Transaction:recipient	J
    //   12301: aload 7
    //   12303: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12306: lcmp
    //   12307: ifne +155 -> 12462
    //   12310: new 911	org/json/simple/JSONObject
    //   12313: dup
    //   12314: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   12317: astore 14
    //   12319: aload 14
    //   12321: ldc_w 1582
    //   12324: aload 12
    //   12326: getfield 754	Nxt$Transaction:index	I
    //   12329: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12332: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12335: pop
    //   12336: aload 14
    //   12338: ldc_w 1697
    //   12341: aload 12
    //   12343: getfield 1583	Nxt$Transaction:timestamp	I
    //   12346: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12349: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12352: pop
    //   12353: aload 14
    //   12355: ldc_w 990
    //   12358: aload 12
    //   12360: getfield 1584	Nxt$Transaction:deadline	S
    //   12363: invokestatic 1587	java/lang/Short:valueOf	(S)Ljava/lang/Short;
    //   12366: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12369: pop
    //   12370: aload 14
    //   12372: ldc_w 1129
    //   12375: aload 12
    //   12377: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   12380: invokestatic 1029	Nxt$Account:getId	([B)J
    //   12383: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12386: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12389: pop
    //   12390: aload 14
    //   12392: ldc_w 1701
    //   12395: aload 12
    //   12397: getfield 1590	Nxt$Transaction:amount	I
    //   12400: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12403: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12406: pop
    //   12407: aload 14
    //   12409: ldc_w 988
    //   12412: aload 12
    //   12414: getfield 1592	Nxt$Transaction:fee	I
    //   12417: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12420: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12423: pop
    //   12424: aload 14
    //   12426: ldc_w 1703
    //   12429: iconst_0
    //   12430: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12433: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12436: pop
    //   12437: aload 14
    //   12439: ldc_w 1705
    //   12442: aload 12
    //   12444: invokevirtual 734	Nxt$Transaction:getId	()J
    //   12447: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12450: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12453: pop
    //   12454: aload 10
    //   12456: aload 14
    //   12458: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   12461: pop
    //   12462: aload 13
    //   12464: invokeinterface 760 1 0
    //   12469: ifne -385 -> 12084
    //   12472: aload 11
    //   12474: monitorexit
    //   12475: goto +7 -> 12482
    //   12478: aload 11
    //   12480: monitorexit
    //   12481: athrow
    //   12482: getstatic 831	Nxt:lastBlock	J
    //   12485: lstore 11
    //   12487: iconst_1
    //   12488: istore 13
    //   12490: goto +581 -> 13071
    //   12493: getstatic 775	Nxt:blocks	Ljava/util/HashMap;
    //   12496: lload 11
    //   12498: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   12501: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   12504: checkcast 771	Nxt$Block
    //   12507: astore 14
    //   12509: aload 14
    //   12511: getfield 1259	Nxt$Block:generatorPublicKey	[B
    //   12514: invokestatic 1029	Nxt$Account:getId	([B)J
    //   12517: aload 7
    //   12519: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12522: lcmp
    //   12523: ifne +116 -> 12639
    //   12526: aload 14
    //   12528: getfield 1269	Nxt$Block:totalFee	I
    //   12531: ifle +108 -> 12639
    //   12534: new 911	org/json/simple/JSONObject
    //   12537: dup
    //   12538: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   12541: astore 15
    //   12543: aload 15
    //   12545: ldc_w 1582
    //   12548: lload 11
    //   12550: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12553: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12556: pop
    //   12557: aload 15
    //   12559: ldc_w 1706
    //   12562: aload 14
    //   12564: getfield 1215	Nxt$Block:timestamp	I
    //   12567: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12570: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12573: pop
    //   12574: aload 15
    //   12576: ldc_w 1249
    //   12579: lload 11
    //   12581: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12584: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12587: pop
    //   12588: aload 15
    //   12590: ldc_w 1708
    //   12593: aload 14
    //   12595: getfield 1269	Nxt$Block:totalFee	I
    //   12598: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12601: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12604: pop
    //   12605: aload 15
    //   12607: ldc_w 1703
    //   12610: iload 13
    //   12612: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12615: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12618: pop
    //   12619: aload 15
    //   12621: ldc_w 1705
    //   12624: ldc_w 1150
    //   12627: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12630: pop
    //   12631: aload 10
    //   12633: aload 15
    //   12635: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   12638: pop
    //   12639: iconst_0
    //   12640: istore 15
    //   12642: goto +396 -> 13038
    //   12645: getstatic 530	Nxt:transactions	Ljava/util/HashMap;
    //   12648: aload 14
    //   12650: getfield 790	Nxt$Block:transactions	[J
    //   12653: iload 15
    //   12655: laload
    //   12656: invokestatic 265	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   12659: invokevirtual 270	java/util/HashMap:get	(Ljava/lang/Object;)Ljava/lang/Object;
    //   12662: checkcast 524	Nxt$Transaction
    //   12665: astore 16
    //   12667: aload 16
    //   12669: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   12672: invokestatic 1029	Nxt$Account:getId	([B)J
    //   12675: aload 7
    //   12677: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12680: lcmp
    //   12681: ifne +187 -> 12868
    //   12684: new 911	org/json/simple/JSONObject
    //   12687: dup
    //   12688: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   12691: astore 17
    //   12693: aload 17
    //   12695: ldc_w 1582
    //   12698: aload 16
    //   12700: getfield 754	Nxt$Transaction:index	I
    //   12703: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12706: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12709: pop
    //   12710: aload 17
    //   12712: ldc_w 1706
    //   12715: aload 14
    //   12717: getfield 1215	Nxt$Block:timestamp	I
    //   12720: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12723: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12726: pop
    //   12727: aload 17
    //   12729: ldc_w 1697
    //   12732: aload 16
    //   12734: getfield 1583	Nxt$Transaction:timestamp	I
    //   12737: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12740: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12743: pop
    //   12744: aload 17
    //   12746: ldc_w 1129
    //   12749: aload 16
    //   12751: getfield 1221	Nxt$Transaction:recipient	J
    //   12754: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12757: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12760: pop
    //   12761: aload 17
    //   12763: ldc_w 1699
    //   12766: aload 16
    //   12768: getfield 1590	Nxt$Transaction:amount	I
    //   12771: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12774: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12777: pop
    //   12778: aload 16
    //   12780: getfield 1221	Nxt$Transaction:recipient	J
    //   12783: aload 7
    //   12785: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12788: lcmp
    //   12789: ifne +20 -> 12809
    //   12792: aload 17
    //   12794: ldc_w 1701
    //   12797: aload 16
    //   12799: getfield 1590	Nxt$Transaction:amount	I
    //   12802: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12805: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12808: pop
    //   12809: aload 17
    //   12811: ldc_w 988
    //   12814: aload 16
    //   12816: getfield 1592	Nxt$Transaction:fee	I
    //   12819: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12822: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12825: pop
    //   12826: aload 17
    //   12828: ldc_w 1703
    //   12831: iload 13
    //   12833: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12836: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12839: pop
    //   12840: aload 17
    //   12842: ldc_w 1705
    //   12845: aload 16
    //   12847: invokevirtual 734	Nxt$Transaction:getId	()J
    //   12850: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12853: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12856: pop
    //   12857: aload 10
    //   12859: aload 17
    //   12861: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   12864: pop
    //   12865: goto +170 -> 13035
    //   12868: aload 16
    //   12870: getfield 1221	Nxt$Transaction:recipient	J
    //   12873: aload 7
    //   12875: invokevirtual 534	java/math/BigInteger:longValue	()J
    //   12878: lcmp
    //   12879: ifne +156 -> 13035
    //   12882: new 911	org/json/simple/JSONObject
    //   12885: dup
    //   12886: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   12889: astore 17
    //   12891: aload 17
    //   12893: ldc_w 1582
    //   12896: aload 16
    //   12898: getfield 754	Nxt$Transaction:index	I
    //   12901: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12904: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12907: pop
    //   12908: aload 17
    //   12910: ldc_w 1706
    //   12913: aload 14
    //   12915: getfield 1215	Nxt$Block:timestamp	I
    //   12918: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12921: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12924: pop
    //   12925: aload 17
    //   12927: ldc_w 1697
    //   12930: aload 16
    //   12932: getfield 1583	Nxt$Transaction:timestamp	I
    //   12935: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12938: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12941: pop
    //   12942: aload 17
    //   12944: ldc_w 1129
    //   12947: aload 16
    //   12949: getfield 1217	Nxt$Transaction:senderPublicKey	[B
    //   12952: invokestatic 1029	Nxt$Account:getId	([B)J
    //   12955: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   12958: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12961: pop
    //   12962: aload 17
    //   12964: ldc_w 1701
    //   12967: aload 16
    //   12969: getfield 1590	Nxt$Transaction:amount	I
    //   12972: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12975: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12978: pop
    //   12979: aload 17
    //   12981: ldc_w 988
    //   12984: aload 16
    //   12986: getfield 1592	Nxt$Transaction:fee	I
    //   12989: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   12992: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   12995: pop
    //   12996: aload 17
    //   12998: ldc_w 1703
    //   13001: iload 13
    //   13003: invokestatic 328	java/lang/Integer:valueOf	(I)Ljava/lang/Integer;
    //   13006: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13009: pop
    //   13010: aload 17
    //   13012: ldc_w 1705
    //   13015: aload 16
    //   13017: invokevirtual 734	Nxt$Transaction:getId	()J
    //   13020: invokestatic 1075	Nxt:convert	(J)Ljava/lang/String;
    //   13023: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13026: pop
    //   13027: aload 10
    //   13029: aload 17
    //   13031: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   13034: pop
    //   13035: iinc 15 1
    //   13038: iload 15
    //   13040: aload 14
    //   13042: getfield 790	Nxt$Block:transactions	[J
    //   13045: arraylength
    //   13046: if_icmplt -401 -> 12645
    //   13049: lload 11
    //   13051: ldc2_w 12
    //   13054: lcmp
    //   13055: ifne +6 -> 13061
    //   13058: goto +24 -> 13082
    //   13061: aload 14
    //   13063: getfield 1280	Nxt$Block:previousBlock	J
    //   13066: lstore 11
    //   13068: iinc 13 1
    //   13071: aload 10
    //   13073: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   13076: sipush 1000
    //   13079: if_icmplt -586 -> 12493
    //   13082: aload 10
    //   13084: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   13087: ifle +45 -> 13132
    //   13090: new 911	org/json/simple/JSONObject
    //   13093: dup
    //   13094: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   13097: astore 14
    //   13099: aload 14
    //   13101: ldc_w 1517
    //   13104: ldc_w 1710
    //   13107: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13110: pop
    //   13111: aload 14
    //   13113: ldc_w 1712
    //   13116: aload 10
    //   13118: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13121: pop
    //   13122: aload_3
    //   13123: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   13126: aload 14
    //   13128: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   13131: pop
    //   13132: aload_3
    //   13133: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   13136: aload 8
    //   13138: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   13141: pop
    //   13142: goto +100 -> 13242
    //   13145: new 911	org/json/simple/JSONObject
    //   13148: dup
    //   13149: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   13152: astore 6
    //   13154: aload 6
    //   13156: ldc_w 1517
    //   13159: ldc_w 1649
    //   13162: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13165: pop
    //   13166: aload 6
    //   13168: ldc_w 1651
    //   13171: ldc_w 1714
    //   13174: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13177: pop
    //   13178: aload_3
    //   13179: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   13182: aload 6
    //   13184: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   13187: pop
    //   13188: goto +54 -> 13242
    //   13191: astore 4
    //   13193: aload_3
    //   13194: ifnull +48 -> 13242
    //   13197: new 911	org/json/simple/JSONObject
    //   13200: dup
    //   13201: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   13204: astore 5
    //   13206: aload 5
    //   13208: ldc_w 1517
    //   13211: ldc_w 1649
    //   13214: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13217: pop
    //   13218: aload 5
    //   13220: ldc_w 1651
    //   13223: aload 4
    //   13225: invokevirtual 890	java/lang/Exception:toString	()Ljava/lang/String;
    //   13228: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13231: pop
    //   13232: aload_3
    //   13233: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   13236: aload 5
    //   13238: invokevirtual 1576	java/util/concurrent/ConcurrentLinkedQueue:offer	(Ljava/lang/Object;)Z
    //   13241: pop
    //   13242: aload_3
    //   13243: ifnull +349 -> 13592
    //   13246: aload_3
    //   13247: dup
    //   13248: astore 4
    //   13250: monitorenter
    //   13251: new 1060	org/json/simple/JSONArray
    //   13254: dup
    //   13255: invokespecial 1062	org/json/simple/JSONArray:<init>	()V
    //   13258: astore 5
    //   13260: goto +11 -> 13271
    //   13263: aload 5
    //   13265: aload 6
    //   13267: invokevirtual 1067	org/json/simple/JSONArray:add	(Ljava/lang/Object;)Z
    //   13270: pop
    //   13271: aload_3
    //   13272: getfield 1572	Nxt$User:pendingResponses	Ljava/util/concurrent/ConcurrentLinkedQueue;
    //   13275: invokevirtual 1716	java/util/concurrent/ConcurrentLinkedQueue:poll	()Ljava/lang/Object;
    //   13278: checkcast 911	org/json/simple/JSONObject
    //   13281: dup
    //   13282: astore 6
    //   13284: ifnonnull -21 -> 13263
    //   13287: aload 5
    //   13289: invokevirtual 1627	org/json/simple/JSONArray:size	()I
    //   13292: ifle +176 -> 13468
    //   13295: new 911	org/json/simple/JSONObject
    //   13298: dup
    //   13299: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   13302: astore 7
    //   13304: aload 7
    //   13306: ldc_w 1521
    //   13309: aload 5
    //   13311: invokevirtual 922	org/json/simple/JSONObject:put	(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   13314: pop
    //   13315: aload_3
    //   13316: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13319: ifnull +108 -> 13427
    //   13322: aload_3
    //   13323: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13326: invokeinterface 1723 1 0
    //   13331: ldc_w 1497
    //   13334: invokeinterface 1729 2 0
    //   13339: aload_3
    //   13340: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13343: invokeinterface 1723 1 0
    //   13348: invokeinterface 1732 1 0
    //   13353: astore 8
    //   13355: aload 8
    //   13357: aload 7
    //   13359: invokevirtual 1508	org/json/simple/JSONObject:toString	()Ljava/lang/String;
    //   13362: ldc_w 1118
    //   13365: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   13368: invokevirtual 1509	javax/servlet/ServletOutputStream:write	([B)V
    //   13371: aload 8
    //   13373: invokevirtual 1514	javax/servlet/ServletOutputStream:close	()V
    //   13376: aload_3
    //   13377: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13380: invokeinterface 1733 1 0
    //   13385: aload_3
    //   13386: aload_1
    //   13387: invokeinterface 1736 1 0
    //   13392: putfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13395: aload_3
    //   13396: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13399: new 1740	Nxt$UserAsyncListener
    //   13402: dup
    //   13403: aload_3
    //   13404: invokespecial 1742	Nxt$UserAsyncListener:<init>	(LNxt$User;)V
    //   13407: invokeinterface 1745 2 0
    //   13412: aload_3
    //   13413: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13416: ldc2_w 1749
    //   13419: invokeinterface 1751 3 0
    //   13424: goto +158 -> 13582
    //   13427: aload_2
    //   13428: ldc_w 1497
    //   13431: invokeinterface 1499 2 0
    //   13436: aload_2
    //   13437: invokeinterface 1504 1 0
    //   13442: astore 8
    //   13444: aload 8
    //   13446: aload 7
    //   13448: invokevirtual 1508	org/json/simple/JSONObject:toString	()Ljava/lang/String;
    //   13451: ldc_w 1118
    //   13454: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   13457: invokevirtual 1509	javax/servlet/ServletOutputStream:write	([B)V
    //   13460: aload 8
    //   13462: invokevirtual 1514	javax/servlet/ServletOutputStream:close	()V
    //   13465: goto +117 -> 13582
    //   13468: aload_3
    //   13469: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13472: ifnull +71 -> 13543
    //   13475: aload_3
    //   13476: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13479: invokeinterface 1723 1 0
    //   13484: ldc_w 1497
    //   13487: invokeinterface 1729 2 0
    //   13492: aload_3
    //   13493: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13496: invokeinterface 1723 1 0
    //   13501: invokeinterface 1732 1 0
    //   13506: astore 7
    //   13508: aload 7
    //   13510: new 911	org/json/simple/JSONObject
    //   13513: dup
    //   13514: invokespecial 913	org/json/simple/JSONObject:<init>	()V
    //   13517: invokevirtual 1508	org/json/simple/JSONObject:toString	()Ljava/lang/String;
    //   13520: ldc_w 1118
    //   13523: invokevirtual 1175	java/lang/String:getBytes	(Ljava/lang/String;)[B
    //   13526: invokevirtual 1509	javax/servlet/ServletOutputStream:write	([B)V
    //   13529: aload 7
    //   13531: invokevirtual 1514	javax/servlet/ServletOutputStream:close	()V
    //   13534: aload_3
    //   13535: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13538: invokeinterface 1733 1 0
    //   13543: aload_3
    //   13544: aload_1
    //   13545: invokeinterface 1736 1 0
    //   13550: putfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13553: aload_3
    //   13554: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13557: new 1740	Nxt$UserAsyncListener
    //   13560: dup
    //   13561: aload_3
    //   13562: invokespecial 1742	Nxt$UserAsyncListener:<init>	(LNxt$User;)V
    //   13565: invokeinterface 1745 2 0
    //   13570: aload_3
    //   13571: getfield 1719	Nxt$User:asyncContext	Ljavax/servlet/AsyncContext;
    //   13574: ldc2_w 1749
    //   13577: invokeinterface 1751 3 0
    //   13582: aload 4
    //   13584: monitorexit
    //   13585: goto +7 -> 13592
    //   13588: aload 4
    //   13590: monitorexit
    //   13591: athrow
    //   13592: return
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	13593	0	this	Nxt
    //   0	13593	1	paramHttpServletRequest	HttpServletRequest
    //   0	13593	2	paramHttpServletResponse	HttpServletResponse
    //   1	13570	3	localUser	Nxt.User
    //   11	8152	4	str1	String
    //   13191	33	4	localException1	Exception
    //   25	13285	5	localObject1	Object
    //   86	10466	6	localObject2	Object
    //   10572	38	6	i	int
    //   10679	31	6	localJSONObject1	JSONObject
    //   10730	38	6	j	int
    //   10829	31	6	localJSONObject2	JSONObject
    //   10880	38	6	k	int
    //   10957	2326	6	localObject3	Object
    //   124	13406	7	localObject4	Object
    //   677	7453	8	localObject5	Object
    //   8462	63	8	m	int
    //   9001	4460	8	localObject6	Object
    //   688	2049	9	localObject7	Object
    //   2819	6	9	l1	long
    //   2947	128	9	str2	String
    //   3359	6	9	l2	long
    //   3567	68	9	n	int
    //   3770	1765	9	localObject8	Object
    //   5944	306	9	l3	long
    //   6444	5	9	localLong	Long
    //   6540	97	9	l4	long
    //   6760	5244	9	localObject9	Object
    //   699	2976	10	localObject10	Object
    //   3953	6484	10	Ljava/lang/Object;	Object
    //   11016	574	10	l5	long
    //   11870	1247	10	localObject11	Object
    //   710	2174	11	localObject12	Object
    //   3079	87	11	i1	int
    //   3375	265	11	localObject13	Object
    //   4425	33	11	i2	int
    //   4562	2962	11	localObject14	Object
    //   8571	358	11	i3	int
    //   8938	30	11	localJSONObject3	JSONObject
    //   12485	582	11	l6	long
    //   721	947	12	localObject15	Object
    //   1813	590	12	i4	int
    //   3101	3602	12	localObject16	Object
    //   6959	139	12	i5	int
    //   7303	249	12	str3	String
    //   8650	257	12	l7	long
    //   9054	209	12	localObject17	Object
    //   10022	272	12	l8	long
    //   10993	599	12	i6	int
    //   11901	542	12	localObject18	Object
    //   732	1102	13	localObject19	Object
    //   2288	93	13	i7	int
    //   3129	3542	13	localObject20	Object
    //   7024	82	13	i8	int
    //   7314	2679	13	localObject21	Object
    //   10996	598	13	i9	int
    //   11914	549	13	localObject22	Object
    //   12488	581	13	i10	int
    //   957	1146	14	str4	String
    //   2310	60	14	l9	long
    //   2437	4710	14	localObject23	Object
    //   7493	227	14	l10	long
    //   9063	668	14	localObject24	Object
    //   10030	1556	14	i11	int
    //   11988	1139	14	localObject25	Object
    //   960	1627	15	i12	int
    //   3141	6805	15	localObject26	Object
    //   10033	270	15	i13	int
    //   11062	1572	15	localObject27	Object
    //   12640	407	15	i14	int
    //   1085	858	16	i15	int
    //   2504	4608	16	localObject28	Object
    //   7500	222	16	i16	int
    //   9291	3725	16	localObject29	Object
    //   1113	260	17	s1	short
    //   1867	5297	17	arrayOfByte1	byte[]
    //   7528	196	17	i17	int
    //   9407	3623	17	localObject30	Object
    //   1150	233	18	l11	long
    //   1925	79	18	i18	int
    //   2562	39	18	bool	boolean
    //   7140	35	18	arrayOfByte2	byte[]
    //   7556	160	18	s2	short
    //   9392	2275	18	localObject31	Object
    //   1936	98	19	i19	int
    //   7593	133	19	l12	long
    //   11642	19	19	localJSONArray1	JSONArray
    //   1157	218	20	arrayOfByte3	byte[]
    //   1943	121	20	i20	int
    //   11678	19	20	localJSONObject4	JSONObject
    //   1164	6	21	l13	long
    //   2083	5635	21	arrayOfByte4	byte[]
    //   7619	47	22	localAccount	Nxt.Account
    //   1180	6643	23	localObject32	Object
    //   1279	6517	24	localObject33	Object
    //   1265	23	25	Ljava/lang/Object;	Object
    //   1362	9	25	i21	int
    //   7771	19	25	localJSONArray2	JSONArray
    //   1391	88	26	localTransaction	Nxt.Transaction
    //   1423	46	27	localJSONObject5	JSONObject
    //   1444	19	28	localJSONArray3	JSONArray
    //   1493	1	103	localException2	Exception
    //   1522	1	104	localException3	Exception
    //   1698	1	105	localException4	Exception
    //   2148	1	106	localException5	Exception
    //   2396	1	107	localException6	Exception
    //   2898	1	108	localException7	Exception
    //   3245	1	109	localException8	Exception
    //   3274	1	110	localException9	Exception
    //   3489	1	111	localException10	Exception
    //   3683	1	112	localException11	Exception
    //   4022	1	113	localException12	Exception
    //   4474	1	114	localException13	Exception
    //   6138	1	115	localException14	Exception
    //   6380	1	116	localException15	Exception
    //   6711	1	117	localException16	Exception
    //   7192	1	118	localException17	Exception
    //   7221	1	119	localException18	Exception
    //   7837	1	120	localException19	Exception
    //   7866	1	121	localException20	Exception
    //   7895	1	122	localException21	Exception
    //   7924	1	123	localException22	Exception
    //   11054	1	124	localException23	Exception
    // Exception table:
    //   from	to	target	type
    //   1268	1284	1287	finally
    //   1287	1290	1287	finally
    //   1108	1490	1493	java/lang/Exception
    //   1080	1519	1522	java/lang/Exception
    //   1595	1695	1698	java/lang/Exception
    //   1771	2145	2148	java/lang/Exception
    //   2290	2393	2396	java/lang/Exception
    //   2807	2895	2898	java/lang/Exception
    //   3074	3242	3245	java/lang/Exception
    //   3015	3271	3274	java/lang/Exception
    //   3347	3486	3489	java/lang/Exception
    //   3562	3680	3683	java/lang/Exception
    //   3956	4015	4018	finally
    //   4018	4021	4018	finally
    //   3877	4022	4022	java/lang/Exception
    //   4095	4471	4474	java/lang/Exception
    //   5510	5527	5530	finally
    //   5530	5533	5530	finally
    //   5932	6135	6138	java/lang/Exception
    //   6211	6377	6380	java/lang/Exception
    //   6528	6708	6711	java/lang/Exception
    //   6982	7189	7192	java/lang/Exception
    //   6954	7218	7221	java/lang/Exception
    //   7551	7834	7837	java/lang/Exception
    //   7523	7863	7866	java/lang/Exception
    //   7495	7892	7895	java/lang/Exception
    //   7481	7921	7924	java/lang/Exception
    //   9028	9208	9211	finally
    //   9211	9214	9211	finally
    //   9222	10005	10008	finally
    //   10008	10011	10008	finally
    //   10019	10308	10311	finally
    //   10311	10314	10311	finally
    //   11001	11051	11054	java/lang/Exception
    //   12068	12475	12478	finally
    //   12478	12481	12478	finally
    //   2	8016	13191	java/lang/Exception
    //   8017	8134	13191	java/lang/Exception
    //   8135	13188	13191	java/lang/Exception
    //   13251	13585	13588	finally
    //   13588	13591	13588	finally
  }
  
  public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    Nxt.Peer localPeer = null;
    JSONObject localJSONObject1 = new JSONObject();
    try
    {
      localObject1 = paramHttpServletRequest.getInputStream();
      Object localObject2 = new ByteArrayOutputStream();
      Object localObject3 = new byte[65536];
      int m;
      while ((m = ((InputStream)localObject1).read((byte[])localObject3)) > 0) {
        ((ByteArrayOutputStream)localObject2).write((byte[])localObject3, 0, m);
      }
      ((InputStream)localObject1).close();
      JSONObject localJSONObject2 = (JSONObject)JSONValue.parse(((ByteArrayOutputStream)localObject2).toString("UTF-8"));
      localPeer = Nxt.Peer.addPeer(paramHttpServletRequest.getRemoteHost(), "");
      if (localPeer != null)
      {
        if (localPeer.state == 2) {
          localPeer.setState(1);
        }
        localPeer.updateDownloadedVolume(((ByteArrayOutputStream)localObject2).size());
      }
      if (((Long)localJSONObject2.get("protocol")).longValue() == 1L)
      {
        switch ((localObject1 = (String)localJSONObject2.get("requestType")).hashCode())
        {
        case -2055947697: 
          if (((String)localObject1).equals("getNextBlocks")) {}
          break;
        case -1195538491: 
          if (((String)localObject1).equals("getMilestoneBlockIds")) {}
          break;
        case -80817804: 
          if (((String)localObject1).equals("getNextBlockIds")) {}
          break;
        case -75444956: 
          if (((String)localObject1).equals("getInfo")) {}
          break;
        case 382446885: 
          if (((String)localObject1).equals("getUnconfirmedTransactions")) {}
          break;
        case 1172622692: 
          if (((String)localObject1).equals("processTransactions")) {}
          break;
        case 1608811908: 
          if (((String)localObject1).equals("getCumulativeDifficulty")) {
            break;
          }
          break;
        case 1962369435: 
          if (((String)localObject1).equals("getPeers")) {}
          break;
        case 1966367582: 
          int i2;
          int i;
          if (!((String)localObject1).equals("processBlock"))
          {
            break label1601;
            localJSONObject1.put("cumulativeDifficulty", Nxt.Block.getLastBlock().cumulativeDifficulty.toString());
            break label1647;
            localObject2 = (String)localJSONObject2.get("announcedAddress");
            if (localObject2 != null)
            {
              localObject2 = ((String)localObject2).trim();
              if (((String)localObject2).length() > 0) {
                localPeer.announcedAddress = ((String)localObject2);
              }
            }
            if (localPeer != null)
            {
              localObject3 = (String)localJSONObject2.get("application");
              if (localObject3 == null)
              {
                localObject3 = "?";
              }
              else
              {
                localObject3 = ((String)localObject3).trim();
                if (((String)localObject3).length() > 20) {
                  localObject3 = ((String)localObject3).substring(0, 20) + "...";
                }
              }
              localPeer.application = ((String)localObject3);
              String str = (String)localJSONObject2.get("version");
              if (str == null)
              {
                str = "?";
              }
              else
              {
                str = str.trim();
                if (str.length() > 10) {
                  str = str.substring(0, 10) + "...";
                }
              }
              localPeer.version = str;
              if (localPeer.analyzeHallmark(paramHttpServletRequest.getRemoteHost(), (String)localJSONObject2.get("hallmark"))) {
                localPeer.setState(1);
              } else {
                localPeer.blacklist();
              }
            }
            if ((myHallmark != null) && (myHallmark.length() > 0)) {
              localJSONObject1.put("hallmark", myHallmark);
            }
            localJSONObject1.put("application", "NRS");
            localJSONObject1.put("version", "0.4.7e");
            break label1647;
            localObject2 = new JSONArray();
            localObject3 = Nxt.Block.getLastBlock();
            int n = ((Nxt.Block)localObject3).height * 4 / 1461 + 1;
            int i1;
            while (((Nxt.Block)localObject3).height > 0)
            {
              ((JSONArray)localObject2).add(convert(((Nxt.Block)localObject3).getId()));
              for (i1 = 0; (i1 < n) && (((Nxt.Block)localObject3).height > 0); i1++) {
                localObject3 = (Nxt.Block)blocks.get(Long.valueOf(((Nxt.Block)localObject3).previousBlock));
              }
            }
            localJSONObject1.put("milestoneBlockIds", localObject2);
            break label1647;
            localObject2 = new JSONArray();
            localObject3 = (Nxt.Block)blocks.get(Long.valueOf(new BigInteger((String)localJSONObject2.get("blockId")).longValue()));
            while ((localObject3 != null) && (((JSONArray)localObject2).size() < 1440))
            {
              localObject3 = (Nxt.Block)blocks.get(Long.valueOf(((Nxt.Block)localObject3).nextBlock));
              if (localObject3 != null) {
                ((JSONArray)localObject2).add(convert(((Nxt.Block)localObject3).getId()));
              }
            }
            localJSONObject1.put("nextBlockIds", localObject2);
            break label1647;
            localObject2 = new LinkedList();
            int j = 0;
            Object localObject5 = (Nxt.Block)blocks.get(Long.valueOf(new BigInteger((String)localJSONObject2.get("blockId")).longValue()));
            while (localObject5 != null)
            {
              localObject5 = (Nxt.Block)blocks.get(Long.valueOf(((Nxt.Block)localObject5).nextBlock));
              if (localObject5 != null)
              {
                i1 = 224 + ((Nxt.Block)localObject5).payloadLength;
                if (j + i1 > 1048576) {
                  break;
                }
                ((LinkedList)localObject2).add(localObject5);
                j += i1;
              }
            }
            Object localObject6 = new JSONArray();
            for (i2 = 0; i2 < ((LinkedList)localObject2).size(); i2++) {
              ((JSONArray)localObject6).add(((Nxt.Block)((LinkedList)localObject2).get(i2)).getJSONObject(transactions));
            }
            localJSONObject1.put("nextBlocks", localObject6);
            break label1647;
            localObject2 = new JSONArray();
            localObject5 = peers.values().iterator();
            while (((Iterator)localObject5).hasNext())
            {
              localObject4 = (Nxt.Peer)((Iterator)localObject5).next();
              if ((((Nxt.Peer)localObject4).blacklistingTime == 0L) && (((Nxt.Peer)localObject4).announcedAddress.length() > 0)) {
                ((JSONArray)localObject2).add(((Nxt.Peer)localObject4).announcedAddress);
              }
            }
            localJSONObject1.put("peers", localObject2);
            break label1647;
            i = 0;
            Object localObject4 = new JSONArray();
            localObject6 = unconfirmedTransactions.values().iterator();
            while (((Iterator)localObject6).hasNext())
            {
              localObject5 = (Nxt.Transaction)((Iterator)localObject6).next();
              ((JSONArray)localObject4).add(((Nxt.Transaction)localObject5).getJSONObject());
              i++;
              if (i >= 255) {
                break;
              }
            }
            localJSONObject1.put("unconfirmedTransactions", localObject4);
            break label1647;
          }
          else
          {
            i = ((Long)localJSONObject2.get("version")).intValue();
            int k = ((Long)localJSONObject2.get("timestamp")).intValue();
            long l = new BigInteger((String)localJSONObject2.get("previousBlock")).longValue();
            i2 = ((Long)localJSONObject2.get("numberOfTransactions")).intValue();
            int i3 = ((Long)localJSONObject2.get("totalAmount")).intValue();
            int i4 = ((Long)localJSONObject2.get("totalFee")).intValue();
            int i5 = ((Long)localJSONObject2.get("payloadLength")).intValue();
            byte[] arrayOfByte2 = convert((String)localJSONObject2.get("payloadHash"));
            byte[] arrayOfByte3 = convert((String)localJSONObject2.get("generatorPublicKey"));
            byte[] arrayOfByte4 = convert((String)localJSONObject2.get("generationSignature"));
            byte[] arrayOfByte5 = convert((String)localJSONObject2.get("blockSignature"));
            Nxt.Block localBlock = new Nxt.Block(i, k, l, i2, i3, i4, i5, arrayOfByte2, arrayOfByte3, arrayOfByte4, arrayOfByte5);
            ByteBuffer localByteBuffer = ByteBuffer.allocate(224 + i5);
            localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            localByteBuffer.put(localBlock.getBytes());
            JSONArray localJSONArray = (JSONArray)localJSONObject2.get("transactions");
            for (int i6 = 0; i6 < localJSONArray.size(); i6++) {
              localByteBuffer.put(Nxt.Transaction.getTransaction((JSONObject)localJSONArray.get(i6)).getBytes());
            }
            boolean bool = Nxt.Block.pushBlock(localByteBuffer, true);
            localJSONObject1.put("accepted", Boolean.valueOf(bool));
            break label1647;
            Nxt.Transaction.processTransactions(localJSONObject2, "transactions");
          }
          break;
        }
        label1601:
        localJSONObject1.put("error", "Unsupported request type!");
      }
      else
      {
        localJSONObject1.put("error", "Unsupported protocol!");
      }
    }
    catch (Exception localException)
    {
      localJSONObject1.put("error", localException.toString());
    }
    label1647:
    paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
    byte[] arrayOfByte1 = localJSONObject1.toString().getBytes("UTF-8");
    Object localObject1 = paramHttpServletResponse.getOutputStream();
    ((ServletOutputStream)localObject1).write(arrayOfByte1);
    ((ServletOutputStream)localObject1).close();
    if (localPeer != null) {
      localPeer.updateUploadedVolume(arrayOfByte1.length);
    }
  }
  
  public void destroy()
  {
    scheduledThreadPool.shutdown();
    cachedThreadPool.shutdown();
    try
    {
      Nxt.Block.saveBlocks("blocks.nxt", true);
    }
    catch (Exception localException1) {}
    try
    {
      Nxt.Transaction.saveTransactions("transactions.nxt");
    }
    catch (Exception localException2) {}
    try
    {
      blockchainChannel.close();
    }
    catch (Exception localException3) {}
    logMessage("Nxt stopped.");
  }
  
  static class Account
  {
    long id;
    long balance;
    int height;
    byte[] publicKey;
    HashMap<Long, Integer> assetBalances;
    long unconfirmedBalance;
    HashMap<Long, Integer> unconfirmedAssetBalances;
    
    Account(long paramLong)
    {
      this.id = paramLong;
      this.height = Nxt.Block.getLastBlock().height;
      this.assetBalances = new HashMap();
      this.unconfirmedAssetBalances = new HashMap();
    }
    
    static Account addAccount(long paramLong)
    {
      synchronized (Nxt.accounts)
      {
        Account localAccount = new Account(paramLong);
        Nxt.accounts.put(Long.valueOf(paramLong), localAccount);
        return localAccount;
      }
    }
    
    void generateBlock(String paramString)
      throws Exception
    {
      Object localObject1;
      synchronized (Nxt.transactions)
      {
        localObject1 = (Nxt.Transaction[])Nxt.unconfirmedTransactions.values().toArray(new Nxt.Transaction[0]);
        while (localObject1.length > 0)
        {
          for (int i = 0; i < localObject1.length; i++)
          {
            localHashMap = localObject1[i];
            if ((localHashMap.referencedTransaction != 0L) && (Nxt.transactions.get(Long.valueOf(localHashMap.referencedTransaction)) == null))
            {
              localObject1[i] = localObject1[(localObject1.length - 1)];
              Nxt.Transaction[] arrayOfTransaction = new Nxt.Transaction[localObject1.length - 1];
              System.arraycopy(localObject1, 0, arrayOfTransaction, 0, arrayOfTransaction.length);
              localObject1 = arrayOfTransaction;
              break;
            }
          }
          if (i == localObject1.length) {
            break;
          }
        }
      }
      Arrays.sort((Object[])localObject1);
      ??? = new HashMap();
      HashSet localHashSet = new HashSet();
      HashMap localHashMap = new HashMap();
      int j = 0;
      while (j <= 32640)
      {
        int k = ((HashMap)???).size();
        for (m = 0; m < localObject1.length; m++)
        {
          localObject2 = localObject1[m];
          int n = ((Nxt.Transaction)localObject2).getBytes().length;
          if ((((HashMap)???).get(Long.valueOf(((Nxt.Transaction)localObject2).getId())) == null) && (j + n <= 32640))
          {
            long l1 = getId(((Nxt.Transaction)localObject2).senderPublicKey);
            Long localLong = (Long)localHashMap.get(Long.valueOf(l1));
            if (localLong == null) {
              localLong = new Long(0L);
            }
            long l2 = (((Nxt.Transaction)localObject2).amount + ((Nxt.Transaction)localObject2).fee) * 100L;
            if ((localLong.longValue() + l2 <= ((Account)Nxt.accounts.get(Long.valueOf(l1))).balance) && (((Nxt.Transaction)localObject2).validateAttachment())) {
              switch (((Nxt.Transaction)localObject2).type)
              {
              case 1: 
                switch (((Nxt.Transaction)localObject2).subtype)
                {
                case 1: 
                  if (!localHashSet.add(((Nxt.Transaction.MessagingAliasAssignmentAttachment)((Nxt.Transaction)localObject2).attachment).alias.toLowerCase())) {}
                  break;
                }
              default: 
                localHashMap.put(Long.valueOf(l1), Long.valueOf(localLong.longValue() + l2));
                ((HashMap)???).put(Long.valueOf(((Nxt.Transaction)localObject2).getId()), localObject2);
                j += n;
              }
            }
          }
        }
        if (((HashMap)???).size() == k) {
          break;
        }
      }
      Nxt.Block localBlock = new Nxt.Block(1, Nxt.getEpochTime(System.currentTimeMillis()), Nxt.lastBlock, ((HashMap)???).size(), 0, 0, 0, null, Nxt.Crypto.getPublicKey(paramString), null, new byte[64]);
      localBlock.transactions = new long[localBlock.numberOfTransactions];
      int m = 0;
      Object localObject3 = ((HashMap)???).entrySet().iterator();
      while (((Iterator)localObject3).hasNext())
      {
        localObject2 = (Map.Entry)((Iterator)localObject3).next();
        localObject4 = (Nxt.Transaction)((Map.Entry)localObject2).getValue();
        localBlock.totalAmount += ((Nxt.Transaction)localObject4).amount;
        localBlock.totalFee += ((Nxt.Transaction)localObject4).fee;
        localBlock.payloadLength += ((Nxt.Transaction)localObject4).getBytes().length;
        localBlock.transactions[(m++)] = ((Long)((Map.Entry)localObject2).getKey()).longValue();
      }
      Arrays.sort(localBlock.transactions);
      Object localObject2 = MessageDigest.getInstance("SHA-256");
      for (m = 0; m < localBlock.numberOfTransactions; m++) {
        ((MessageDigest)localObject2).update(((Nxt.Transaction)((HashMap)???).get(Long.valueOf(localBlock.transactions[m]))).getBytes());
      }
      localBlock.payloadHash = ((MessageDigest)localObject2).digest();
      localBlock.generationSignature = Nxt.Crypto.sign(Nxt.Block.getLastBlock().generationSignature, paramString);
      localObject3 = localBlock.getBytes();
      Object localObject4 = new byte[localObject3.length - 64];
      System.arraycopy(localObject3, 0, localObject4, 0, localObject4.length);
      localBlock.blockSignature = Nxt.Crypto.sign((byte[])localObject4, paramString);
      JSONObject localJSONObject = localBlock.getJSONObject((HashMap)???);
      localJSONObject.put("requestType", "processBlock");
      if ((localBlock.verifyBlockSignature()) && (localBlock.verifyGenerationSignature())) {
        Nxt.Peer.sendToAllPeers(localJSONObject);
      } else {
        Nxt.logMessage("Generated an incorrect block. Waiting for the next one...");
      }
    }
    
    int getEffectiveBalance()
    {
      if (this.height == 0) {
        return (int)(this.balance / 100L);
      }
      if (Nxt.Block.getLastBlock().height - this.height < 1440) {
        return 0;
      }
      int i = 0;
      for (long l : Nxt.Block.getLastBlock().transactions)
      {
        Nxt.Transaction localTransaction = (Nxt.Transaction)Nxt.transactions.get(Long.valueOf(l));
        if (localTransaction.recipient == this.id) {
          i += localTransaction.amount;
        }
      }
      return (int)(this.balance / 100L) - i;
    }
    
    static long getId(byte[] paramArrayOfByte)
      throws Exception
    {
      byte[] arrayOfByte = MessageDigest.getInstance("SHA-256").digest(paramArrayOfByte);
      BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      return localBigInteger.longValue();
    }
    
    void setBalance(long paramLong)
      throws Exception
    {
      this.balance = paramLong;
      Iterator localIterator = Nxt.peers.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.Peer localPeer = (Nxt.Peer)localIterator.next();
        if ((localPeer.accountId == this.id) && (localPeer.adjustedWeight > 0L)) {
          localPeer.updateWeight();
        }
      }
    }
    
    void setUnconfirmedBalance(long paramLong)
      throws Exception
    {
      this.unconfirmedBalance = paramLong;
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("response", "setBalance");
      localJSONObject.put("balance", Long.valueOf(paramLong));
      Iterator localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.User localUser = (Nxt.User)localIterator.next();
        if ((localUser.secretPhrase != null) && (getId(Nxt.Crypto.getPublicKey(localUser.secretPhrase)) == this.id)) {
          localUser.send(localJSONObject);
        }
      }
    }
  }
  
  static class Alias
  {
    Nxt.Account account;
    String alias;
    String uri;
    int timestamp;
    
    Alias(Nxt.Account paramAccount, String paramString1, String paramString2, int paramInt)
    {
      this.account = paramAccount;
      this.alias = paramString1;
      this.uri = paramString2;
      this.timestamp = paramInt;
    }
  }
  
  static class AskOrder
    implements Comparable<AskOrder>
  {
    long id;
    long height;
    Nxt.Account account;
    long asset;
    int quantity;
    long price;
    
    AskOrder(long paramLong1, Nxt.Account paramAccount, long paramLong2, int paramInt, long paramLong3)
    {
      this.id = paramLong1;
      this.height = Nxt.Block.getLastBlock().height;
      this.account = paramAccount;
      this.asset = paramLong2;
      this.quantity = paramInt;
      this.price = paramLong3;
    }
    
    public int compareTo(AskOrder paramAskOrder)
    {
      if (this.price < paramAskOrder.price) {
        return -1;
      }
      if (this.price > paramAskOrder.price) {
        return 1;
      }
      if (this.height < paramAskOrder.height) {
        return -1;
      }
      if (this.height > paramAskOrder.height) {
        return 1;
      }
      if (this.id < paramAskOrder.id) {
        return -1;
      }
      if (this.id > paramAskOrder.id) {
        return 1;
      }
      return 0;
    }
  }
  
  static class Asset
  {
    long accountId;
    String name;
    String description;
    int quantity;
    
    Asset(long paramLong, String paramString1, String paramString2, int paramInt)
    {
      this.accountId = paramLong;
      this.name = paramString1;
      this.description = paramString2;
      this.quantity = paramInt;
    }
  }
  
  static class BidOrder
    implements Comparable<BidOrder>
  {
    long id;
    long height;
    Nxt.Account account;
    long asset;
    int quantity;
    long price;
    
    BidOrder(long paramLong1, Nxt.Account paramAccount, long paramLong2, int paramInt, long paramLong3)
    {
      this.id = paramLong1;
      this.height = Nxt.Block.getLastBlock().height;
      this.account = paramAccount;
      this.asset = paramLong2;
      this.quantity = paramInt;
      this.price = paramLong3;
    }
    
    public int compareTo(BidOrder paramBidOrder)
    {
      if (this.price > paramBidOrder.price) {
        return -1;
      }
      if (this.price < paramBidOrder.price) {
        return 1;
      }
      if (this.height < paramBidOrder.height) {
        return -1;
      }
      if (this.height > paramBidOrder.height) {
        return 1;
      }
      if (this.id < paramBidOrder.id) {
        return -1;
      }
      if (this.id > paramBidOrder.id) {
        return 1;
      }
      return 0;
    }
  }
  
  static class Block
    implements Serializable
  {
    static final long serialVersionUID = 0L;
    int version;
    int timestamp;
    long previousBlock;
    int numberOfTransactions;
    int totalAmount;
    int totalFee;
    int payloadLength;
    byte[] payloadHash;
    byte[] generatorPublicKey;
    byte[] generationSignature;
    byte[] blockSignature;
    int index;
    long[] transactions;
    long baseTarget;
    int height;
    long nextBlock;
    BigInteger cumulativeDifficulty;
    long prevBlockPtr;
    
    Block(int paramInt1, int paramInt2, long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    {
      this.version = paramInt1;
      this.timestamp = paramInt2;
      this.previousBlock = paramLong;
      this.numberOfTransactions = paramInt3;
      this.totalAmount = paramInt4;
      this.totalFee = paramInt5;
      this.payloadLength = paramInt6;
      this.payloadHash = paramArrayOfByte1;
      this.generatorPublicKey = paramArrayOfByte2;
      this.generationSignature = paramArrayOfByte3;
      this.blockSignature = paramArrayOfByte4;
    }
    
    void analyze()
      throws Exception
    {
      if (this.previousBlock == 0L)
      {
        Nxt.lastBlock = 2680262203532249785L;
        Nxt.blocks.put(Long.valueOf(Nxt.lastBlock), this);
        this.baseTarget = 153722867L;
        this.cumulativeDifficulty = BigInteger.ZERO;
        Nxt.Account.addAccount(1739068987193023818L);
      }
      else
      {
        getLastBlock().nextBlock = getId();
        this.height = (getLastBlock().height + 1);
        Nxt.lastBlock = getId();
        Nxt.blocks.put(Long.valueOf(Nxt.lastBlock), this);
        this.baseTarget = getBaseTarget();
        this.cumulativeDifficulty = ((Block)Nxt.blocks.get(Long.valueOf(this.previousBlock))).cumulativeDifficulty.add(Nxt.two64.divide(BigInteger.valueOf(this.baseTarget)));
        Nxt.Account localAccount1 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(this.generatorPublicKey)));
        synchronized (localAccount1)
        {
          localAccount1.setBalance(localAccount1.balance + this.totalFee * 100L);
          localAccount1.setUnconfirmedBalance(localAccount1.unconfirmedBalance + this.totalFee * 100L);
        }
      }
      synchronized (Nxt.transactions)
      {
        for (int i = 0; i < this.numberOfTransactions; i++)
        {
          Nxt.Transaction localTransaction = (Nxt.Transaction)Nxt.transactions.get(Long.valueOf(this.transactions[i]));
          long l1 = Nxt.Account.getId(localTransaction.senderPublicKey);
          Nxt.Account localAccount2 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(l1));
          synchronized (localAccount2)
          {
            localAccount2.setBalance(localAccount2.balance - (localTransaction.amount + localTransaction.fee) * 100L);
            localAccount2.setUnconfirmedBalance(localAccount2.unconfirmedBalance - (localTransaction.amount + localTransaction.fee) * 100L);
            if (localAccount2.publicKey == null) {
              localAccount2.publicKey = localTransaction.senderPublicKey;
            }
          }
          ??? = (Nxt.Account)Nxt.accounts.get(Long.valueOf(localTransaction.recipient));
          if (??? == null) {
            ??? = Nxt.Account.addAccount(localTransaction.recipient);
          }
          Object localObject1;
          switch (localTransaction.type)
          {
          case 0: 
            switch (localTransaction.subtype)
            {
            case 0: 
              synchronized (???)
              {
                ((Nxt.Account)???).setBalance(((Nxt.Account)???).balance + localTransaction.amount * 100L);
                ((Nxt.Account)???).setUnconfirmedBalance(((Nxt.Account)???).unconfirmedBalance + localTransaction.amount * 100L);
              }
            }
            break;
          case 1: 
            switch (localTransaction.subtype)
            {
            case 1: 
              ??? = (Nxt.Transaction.MessagingAliasAssignmentAttachment)localTransaction.attachment;
              String str = ((Nxt.Transaction.MessagingAliasAssignmentAttachment)???).alias.toLowerCase();
              synchronized (Nxt.aliases)
              {
                localObject1 = (Nxt.Alias)Nxt.aliases.get(str);
                if (localObject1 == null)
                {
                  localObject1 = new Nxt.Alias(localAccount2, ((Nxt.Transaction.MessagingAliasAssignmentAttachment)???).alias, ((Nxt.Transaction.MessagingAliasAssignmentAttachment)???).uri, this.timestamp);
                  Nxt.aliases.put(str, localObject1);
                  Nxt.aliasIdToAliasMappings.put(Long.valueOf(localTransaction.getId()), localObject1);
                }
                else
                {
                  ((Nxt.Alias)localObject1).uri = ((Nxt.Transaction.MessagingAliasAssignmentAttachment)???).uri;
                  ((Nxt.Alias)localObject1).timestamp = this.timestamp;
                }
              }
            }
            break;
          case 2: 
            switch (localTransaction.subtype)
            {
            case 0: 
              ??? = (Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)localTransaction.attachment;
              long l2 = localTransaction.getId();
              localObject1 = new Nxt.Asset(l1, ((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).name, ((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).description, ((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).quantity);
              synchronized (Nxt.assets)
              {
                Nxt.assets.put(Long.valueOf(l2), localObject1);
                Nxt.assetNameToIdMappings.put(((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).name.toLowerCase(), Long.valueOf(l2));
              }
              synchronized (Nxt.askOrders)
              {
                Nxt.sortedAskOrders.put(Long.valueOf(l2), new TreeSet());
              }
              synchronized (Nxt.bidOrders)
              {
                Nxt.sortedBidOrders.put(Long.valueOf(l2), new TreeSet());
              }
              synchronized (localAccount2)
              {
                localAccount2.assetBalances.put(Long.valueOf(l2), Integer.valueOf(((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).quantity));
                localAccount2.unconfirmedAssetBalances.put(Long.valueOf(l2), Integer.valueOf(((Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment)???).quantity));
              }
            case 1: 
              ??? = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localTransaction.attachment;
              synchronized (localAccount2)
              {
                localAccount2.assetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(((Integer)localAccount2.assetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
                localAccount2.unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(((Integer)localAccount2.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
              }
              synchronized (???)
              {
                ??? = (Integer)((Nxt.Account)???).assetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset));
                if (??? == null)
                {
                  ((Nxt.Account)???).assetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
                  ((Nxt.Account)???).unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
                }
                else
                {
                  ((Nxt.Account)???).assetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(???.intValue() + ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
                  ((Nxt.Account)???).unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset), Integer.valueOf(((Integer)((Nxt.Account)???).unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).asset))).intValue() + ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)???).quantity));
                }
              }
            case 2: 
              ??? = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localTransaction.attachment;
              ??? = new Nxt.AskOrder(localTransaction.getId(), localAccount2, ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset, ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).quantity, ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).price);
              synchronized (localAccount2)
              {
                localAccount2.assetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset), Integer.valueOf(((Integer)localAccount2.assetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).quantity));
                localAccount2.unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset), Integer.valueOf(((Integer)localAccount2.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).quantity));
              }
              synchronized (Nxt.askOrders)
              {
                Nxt.askOrders.put(Long.valueOf(((Nxt.AskOrder)???).id), ???);
                ((TreeSet)Nxt.sortedAskOrders.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset))).add(???);
              }
              Nxt.matchOrders(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)???).asset);
              break;
            case 3: 
              ??? = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localTransaction.attachment;
              ??? = new Nxt.BidOrder(localTransaction.getId(), localAccount2, ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).asset, ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).quantity, ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).price);
              synchronized (localAccount2)
              {
                localAccount2.setBalance(localAccount2.balance - ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).quantity * ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).price);
                localAccount2.setUnconfirmedBalance(localAccount2.unconfirmedBalance - ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).quantity * ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).price);
              }
              synchronized (Nxt.bidOrders)
              {
                Nxt.bidOrders.put(Long.valueOf(((Nxt.BidOrder)???).id), ???);
                ((TreeSet)Nxt.sortedBidOrders.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).asset))).add(???);
              }
              Nxt.matchOrders(((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)???).asset);
              break;
            case 4: 
              ??? = (Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment)localTransaction.attachment;
              synchronized (Nxt.askOrders)
              {
                ??? = (Nxt.AskOrder)Nxt.askOrders.remove(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment)???).order));
                ((TreeSet)Nxt.sortedAskOrders.get(Long.valueOf(((Nxt.AskOrder)???).asset))).remove(???);
              }
              synchronized (localAccount2)
              {
                localAccount2.assetBalances.put(Long.valueOf(((Nxt.AskOrder)???).asset), Integer.valueOf(((Integer)localAccount2.assetBalances.get(Long.valueOf(((Nxt.AskOrder)???).asset))).intValue() + ((Nxt.AskOrder)???).quantity));
                localAccount2.unconfirmedAssetBalances.put(Long.valueOf(((Nxt.AskOrder)???).asset), Integer.valueOf(((Integer)localAccount2.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.AskOrder)???).asset))).intValue() + ((Nxt.AskOrder)???).quantity));
              }
            case 5: 
              ??? = (Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment)localTransaction.attachment;
              synchronized (Nxt.bidOrders)
              {
                ??? = (Nxt.BidOrder)Nxt.bidOrders.remove(Long.valueOf(((Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment)???).order));
                ((TreeSet)Nxt.sortedBidOrders.get(Long.valueOf(((Nxt.BidOrder)???).asset))).remove(???);
              }
              synchronized (localAccount2)
              {
                localAccount2.setBalance(localAccount2.balance + ((Nxt.BidOrder)???).quantity * ((Nxt.BidOrder)???).price);
                localAccount2.setUnconfirmedBalance(localAccount2.unconfirmedBalance + ((Nxt.BidOrder)???).quantity * ((Nxt.BidOrder)???).price);
              }
            }
            break;
          }
        }
      }
    }
    
    static long getBaseTarget()
      throws Exception
    {
      if (Nxt.lastBlock == 2680262203532249785L) {
        return ((Block)Nxt.blocks.get(Long.valueOf(2680262203532249785L))).baseTarget;
      }
      Block localBlock1 = getLastBlock();
      Block localBlock2 = (Block)Nxt.blocks.get(Long.valueOf(localBlock1.previousBlock));
      long l1 = localBlock2.baseTarget;
      long l2 = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(localBlock1.timestamp - localBlock2.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
      if ((l2 < 0L) || (l2 > 153722867000000000L)) {
        l2 = 153722867000000000L;
      }
      if (l2 < l1 / 2L) {
        l2 = l1 / 2L;
      }
      if (l2 == 0L) {
        l2 = 1L;
      }
      long l3 = l1 * 2L;
      if (l3 < 0L) {
        l3 = 153722867000000000L;
      }
      if (l2 > l3) {
        l2 = l3;
      }
      return l2;
    }
    
    static Block getBlock(JSONObject paramJSONObject)
    {
      int i = ((Long)paramJSONObject.get("version")).intValue();
      int j = ((Long)paramJSONObject.get("timestamp")).intValue();
      long l = new BigInteger((String)paramJSONObject.get("previousBlock")).longValue();
      int k = ((Long)paramJSONObject.get("numberOfTransactions")).intValue();
      int m = ((Long)paramJSONObject.get("totalAmount")).intValue();
      int n = ((Long)paramJSONObject.get("totalFee")).intValue();
      int i1 = ((Long)paramJSONObject.get("payloadLength")).intValue();
      byte[] arrayOfByte1 = Nxt.convert((String)paramJSONObject.get("payloadHash"));
      byte[] arrayOfByte2 = Nxt.convert((String)paramJSONObject.get("generatorPublicKey"));
      byte[] arrayOfByte3 = Nxt.convert((String)paramJSONObject.get("generationSignature"));
      byte[] arrayOfByte4 = Nxt.convert((String)paramJSONObject.get("blockSignature"));
      return new Block(i, j, l, k, m, n, i1, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4);
    }
    
    byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(224);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putInt(this.version);
      localByteBuffer.putInt(this.timestamp);
      localByteBuffer.putLong(this.previousBlock);
      localByteBuffer.putInt(this.numberOfTransactions);
      localByteBuffer.putInt(this.totalAmount);
      localByteBuffer.putInt(this.totalFee);
      localByteBuffer.putInt(this.payloadLength);
      localByteBuffer.put(this.payloadHash);
      localByteBuffer.put(this.generatorPublicKey);
      localByteBuffer.put(this.generationSignature);
      localByteBuffer.put(this.blockSignature);
      return localByteBuffer.array();
    }
    
    long getId()
      throws Exception
    {
      byte[] arrayOfByte = MessageDigest.getInstance("SHA-256").digest(getBytes());
      BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      return localBigInteger.longValue();
    }
    
    JSONObject getJSONObject(HashMap<Long, Nxt.Transaction> paramHashMap)
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("version", Integer.valueOf(this.version));
      localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
      localJSONObject.put("previousBlock", Nxt.convert(this.previousBlock));
      localJSONObject.put("numberOfTransactions", Integer.valueOf(this.numberOfTransactions));
      localJSONObject.put("totalAmount", Integer.valueOf(this.totalAmount));
      localJSONObject.put("totalFee", Integer.valueOf(this.totalFee));
      localJSONObject.put("payloadLength", Integer.valueOf(this.payloadLength));
      localJSONObject.put("payloadHash", Nxt.convert(this.payloadHash));
      localJSONObject.put("generatorPublicKey", Nxt.convert(this.generatorPublicKey));
      localJSONObject.put("generationSignature", Nxt.convert(this.generationSignature));
      localJSONObject.put("blockSignature", Nxt.convert(this.blockSignature));
      JSONArray localJSONArray = new JSONArray();
      for (int i = 0; i < this.numberOfTransactions; i++) {
        localJSONArray.add(((Nxt.Transaction)paramHashMap.get(Long.valueOf(this.transactions[i]))).getJSONObject());
      }
      localJSONObject.put("transactions", localJSONArray);
      return localJSONObject;
    }
    
    static Block getLastBlock()
    {
      return (Block)Nxt.blocks.get(Long.valueOf(Nxt.lastBlock));
    }
    
    static void loadBlocks(String paramString)
      throws Exception
    {
      FileInputStream localFileInputStream = new FileInputStream(paramString);
      ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);
      Nxt.blockCounter = localObjectInputStream.readInt();
      Nxt.blocks = (HashMap)localObjectInputStream.readObject();
      Nxt.lastBlock = localObjectInputStream.readLong();
      localObjectInputStream.close();
      localFileInputStream.close();
    }
    
    static boolean popLastBlock()
    {
      if (Nxt.lastBlock == 2680262203532249785L) {
        return false;
      }
      try
      {
        JSONObject localJSONObject1 = new JSONObject();
        localJSONObject1.put("response", "processNewData");
        JSONArray localJSONArray = new JSONArray();
        synchronized (Nxt.blocks)
        {
          localObject1 = getLastBlock();
          Nxt.Account localAccount1 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(((Block)localObject1).generatorPublicKey)));
          synchronized (localAccount1)
          {
            localAccount1.setBalance(localAccount1.balance - ((Block)localObject1).totalFee * 100L);
            localAccount1.setUnconfirmedBalance(localAccount1.unconfirmedBalance - ((Block)localObject1).totalFee * 100L);
          }
          synchronized (Nxt.transactions)
          {
            for (int i = 0; i < ((Block)localObject1).numberOfTransactions; i++)
            {
              Nxt.Transaction localTransaction = (Nxt.Transaction)Nxt.transactions.remove(Long.valueOf(localObject1.transactions[i]));
              Nxt.unconfirmedTransactions.put(Long.valueOf(localObject1.transactions[i]), localTransaction);
              Nxt.Account localAccount2 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(localTransaction.senderPublicKey)));
              synchronized (localAccount2)
              {
                localAccount2.setBalance(localAccount2.balance + (localTransaction.amount + localTransaction.fee) * 100L);
              }
              ??? = (Nxt.Account)Nxt.accounts.get(Long.valueOf(localTransaction.recipient));
              synchronized (???)
              {
                ((Nxt.Account)???).setBalance(((Nxt.Account)???).balance - localTransaction.amount * 100L);
                ((Nxt.Account)???).setUnconfirmedBalance(((Nxt.Account)???).unconfirmedBalance - localTransaction.amount * 100L);
              }
              ??? = new JSONObject();
              ((JSONObject)???).put("index", Integer.valueOf(localTransaction.index));
              ((JSONObject)???).put("timestamp", Integer.valueOf(localTransaction.timestamp));
              ((JSONObject)???).put("deadline", Short.valueOf(localTransaction.deadline));
              ((JSONObject)???).put("recipient", Nxt.convert(localTransaction.recipient));
              ((JSONObject)???).put("amount", Integer.valueOf(localTransaction.amount));
              ((JSONObject)???).put("fee", Integer.valueOf(localTransaction.fee));
              ((JSONObject)???).put("sender", Nxt.convert(Nxt.Account.getId(localTransaction.senderPublicKey)));
              ((JSONObject)???).put("id", Nxt.convert(localTransaction.getId()));
              localJSONArray.add(???);
            }
          }
          ??? = new JSONArray();
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(((Block)localObject1).index));
          localJSONObject2.put("timestamp", Integer.valueOf(((Block)localObject1).timestamp));
          localJSONObject2.put("numberOfTransactions", Integer.valueOf(((Block)localObject1).numberOfTransactions));
          localJSONObject2.put("totalAmount", Integer.valueOf(((Block)localObject1).totalAmount));
          localJSONObject2.put("totalFee", Integer.valueOf(((Block)localObject1).totalFee));
          localJSONObject2.put("payloadLength", Integer.valueOf(((Block)localObject1).payloadLength));
          localJSONObject2.put("generator", Nxt.convert(Nxt.Account.getId(((Block)localObject1).generatorPublicKey)));
          localJSONObject2.put("height", Integer.valueOf(((Block)localObject1).height));
          localJSONObject2.put("version", Integer.valueOf(((Block)localObject1).version));
          localJSONObject2.put("block", Nxt.convert(((Block)localObject1).getId()));
          localJSONObject2.put("baseTarget", BigInteger.valueOf(((Block)localObject1).baseTarget).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
          ((JSONArray)???).add(localJSONObject2);
          localJSONObject1.put("addedOrphanedBlocks", ???);
          Nxt.lastBlock = ((Block)localObject1).previousBlock;
        }
        if (localJSONArray.size() > 0) {
          localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
        }
        Object localObject1 = Nxt.users.values().iterator();
        while (((Iterator)localObject1).hasNext())
        {
          ??? = (Nxt.User)((Iterator)localObject1).next();
          ((Nxt.User)???).send(localJSONObject1);
        }
      }
      catch (Exception localException)
      {
        Nxt.logMessage("19: " + localException.toString());
        return false;
      }
      return true;
    }
    
    static boolean pushBlock(ByteBuffer paramByteBuffer, boolean paramBoolean)
    {
      paramByteBuffer.flip();
      int i = paramByteBuffer.getInt();
      if (i != 1) {
        return false;
      }
      int j = paramByteBuffer.getInt();
      long l1 = paramByteBuffer.getLong();
      int k = paramByteBuffer.getInt();
      int m = paramByteBuffer.getInt();
      int n = paramByteBuffer.getInt();
      int i1 = paramByteBuffer.getInt();
      byte[] arrayOfByte1 = new byte[32];
      paramByteBuffer.get(arrayOfByte1);
      byte[] arrayOfByte2 = new byte[32];
      paramByteBuffer.get(arrayOfByte2);
      byte[] arrayOfByte3 = new byte[64];
      paramByteBuffer.get(arrayOfByte3);
      byte[] arrayOfByte4 = new byte[64];
      paramByteBuffer.get(arrayOfByte4);
      if (getLastBlock().previousBlock == l1) {
        return false;
      }
      int i2 = Nxt.getEpochTime(System.currentTimeMillis());
      if ((j > i2 + 15) || (j <= getLastBlock().timestamp)) {
        return false;
      }
      if ((i1 > 32640) || (224 + i1 != paramByteBuffer.capacity())) {
        return false;
      }
      Block localBlock = new Block(i, j, l1, k, m, n, i1, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4);
      synchronized (Nxt.blocks)
      {
        localBlock.index = (++Nxt.blockCounter);
      }
      try
      {
        if ((localBlock.previousBlock != Nxt.lastBlock) || (Nxt.blocks.get(Long.valueOf(localBlock.getId())) != null) || (!localBlock.verifyGenerationSignature()) || (!localBlock.verifyBlockSignature())) {
          return false;
        }
        ??? = new HashMap();
        HashSet localHashSet = new HashSet();
        localBlock.transactions = new long[localBlock.numberOfTransactions];
        for (int i3 = 0; i3 < localBlock.numberOfTransactions; i3++)
        {
          localObject1 = Nxt.Transaction.getTransaction(paramByteBuffer);
          synchronized (Nxt.transactions)
          {
            ((Nxt.Transaction)localObject1).index = (++Nxt.transactionCounter);
          }
          if (((HashMap)???).put(Long.valueOf(localBlock.transactions[i3] = ((Nxt.Transaction)localObject1).getId()), localObject1) != null) {
            return false;
          }
          switch (((Nxt.Transaction)localObject1).type)
          {
          case 1: 
            switch (((Nxt.Transaction)localObject1).subtype)
            {
            case 1: 
              if (!localHashSet.add(((Nxt.Transaction.MessagingAliasAssignmentAttachment)((Nxt.Transaction)localObject1).attachment).alias.toLowerCase())) {
                return false;
              }
              break;
            }
            break;
          }
        }
        Arrays.sort(localBlock.transactions);
        HashMap localHashMap = new HashMap();
        Object localObject1 = new HashMap();
        int i4 = 0;
        int i5 = 0;
        Object localObject3;
        Object localObject4;
        Object localObject5;
        Object localObject6;
        for (int i6 = 0; i6 < localBlock.numberOfTransactions; i6++)
        {
          localObject2 = (Nxt.Transaction)((HashMap)???).get(Long.valueOf(localBlock.transactions[i6]));
          if ((((Nxt.Transaction)localObject2).timestamp > i2 + 15) || (((Nxt.Transaction)localObject2).deadline < 1) || ((((Nxt.Transaction)localObject2).timestamp + ((Nxt.Transaction)localObject2).deadline * 60 < j) && (getLastBlock().height > 303)) || (((Nxt.Transaction)localObject2).fee <= 0) || (!((Nxt.Transaction)localObject2).validateAttachment()) || (Nxt.transactions.get(Long.valueOf(localBlock.transactions[i6])) != null) || ((((Nxt.Transaction)localObject2).referencedTransaction != 0L) && (Nxt.transactions.get(Long.valueOf(((Nxt.Transaction)localObject2).referencedTransaction)) == null) && (((HashMap)???).get(Long.valueOf(((Nxt.Transaction)localObject2).referencedTransaction)) == null)) || ((Nxt.unconfirmedTransactions.get(Long.valueOf(localBlock.transactions[i6])) == null) && (!((Nxt.Transaction)localObject2).verify()))) {
            break;
          }
          long l2 = Nxt.Account.getId(((Nxt.Transaction)localObject2).senderPublicKey);
          localObject3 = (Long)localHashMap.get(Long.valueOf(l2));
          if (localObject3 == null) {
            localObject3 = new Long(0L);
          }
          localHashMap.put(Long.valueOf(l2), Long.valueOf(((Long)localObject3).longValue() + (((Nxt.Transaction)localObject2).amount + ((Nxt.Transaction)localObject2).fee) * 100L));
          if (((Nxt.Transaction)localObject2).type == 0)
          {
            if (((Nxt.Transaction)localObject2).subtype != 0) {
              break;
            }
            i4 += ((Nxt.Transaction)localObject2).amount;
          }
          else if (((Nxt.Transaction)localObject2).type == 1)
          {
            if (((Nxt.Transaction)localObject2).subtype != 1) {
              break;
            }
          }
          else
          {
            if (((Nxt.Transaction)localObject2).type != 2) {
              break;
            }
            if (((Nxt.Transaction)localObject2).subtype == 1)
            {
              localObject4 = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)((Nxt.Transaction)localObject2).attachment;
              localObject5 = (HashMap)((HashMap)localObject1).get(Long.valueOf(l2));
              if (localObject5 == null)
              {
                localObject5 = new HashMap();
                ((HashMap)localObject1).put(Long.valueOf(l2), localObject5);
              }
              localObject6 = (Long)((HashMap)localObject5).get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject4).asset));
              if (localObject6 == null) {
                localObject6 = new Long(0L);
              }
              ((HashMap)localObject5).put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject4).asset), Long.valueOf(((Long)localObject6).longValue() + ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject4).quantity));
            }
            else if (((Nxt.Transaction)localObject2).subtype == 2)
            {
              localObject4 = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)((Nxt.Transaction)localObject2).attachment;
              localObject5 = (HashMap)((HashMap)localObject1).get(Long.valueOf(l2));
              if (localObject5 == null)
              {
                localObject5 = new HashMap();
                ((HashMap)localObject1).put(Long.valueOf(l2), localObject5);
              }
              localObject6 = (Long)((HashMap)localObject5).get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject4).asset));
              if (localObject6 == null) {
                localObject6 = new Long(0L);
              }
              ((HashMap)localObject5).put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject4).asset), Long.valueOf(((Long)localObject6).longValue() + ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject4).quantity));
            }
            else if (((Nxt.Transaction)localObject2).subtype == 3)
            {
              localObject4 = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)((Nxt.Transaction)localObject2).attachment;
              localHashMap.put(Long.valueOf(l2), Long.valueOf(((Long)localObject3).longValue() + ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject4).quantity * ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject4).price));
            }
            else
            {
              if ((((Nxt.Transaction)localObject2).subtype != 0) && (((Nxt.Transaction)localObject2).subtype != 4) && (((Nxt.Transaction)localObject2).subtype != 5)) {
                break;
              }
            }
          }
          i5 += ((Nxt.Transaction)localObject2).fee;
        }
        if ((i6 != localBlock.numberOfTransactions) || (i4 != localBlock.totalAmount) || (i5 != localBlock.totalFee)) {
          return false;
        }
        Object localObject2 = MessageDigest.getInstance("SHA-256");
        for (i6 = 0; i6 < localBlock.numberOfTransactions; i6++) {
          ((MessageDigest)localObject2).update(((Nxt.Transaction)((HashMap)???).get(Long.valueOf(localBlock.transactions[i6]))).getBytes());
        }
        if (!Arrays.equals(((MessageDigest)localObject2).digest(), localBlock.payloadHash)) {
          return false;
        }
        synchronized (Nxt.blocks)
        {
          localObject3 = localHashMap.entrySet().iterator();
          Map.Entry localEntry;
          while (((Iterator)localObject3).hasNext())
          {
            localEntry = (Map.Entry)((Iterator)localObject3).next();
            localObject4 = (Nxt.Account)Nxt.accounts.get(localEntry.getKey());
            if (((Nxt.Account)localObject4).balance < ((Long)localEntry.getValue()).longValue()) {
              return false;
            }
          }
          localObject3 = ((HashMap)localObject1).entrySet().iterator();
          while (((Iterator)localObject3).hasNext())
          {
            localEntry = (Map.Entry)((Iterator)localObject3).next();
            localObject4 = (Nxt.Account)Nxt.accounts.get(localEntry.getKey());
            localObject6 = ((HashMap)localEntry.getValue()).entrySet().iterator();
            while (((Iterator)localObject6).hasNext())
            {
              localObject5 = (Map.Entry)((Iterator)localObject6).next();
              long l4 = ((Long)((Map.Entry)localObject5).getKey()).longValue();
              long l5 = ((Long)((Map.Entry)localObject5).getValue()).longValue();
              if (((Integer)((Nxt.Account)localObject4).assetBalances.get(Long.valueOf(l4))).intValue() < l5) {
                return false;
              }
            }
          }
          if (localBlock.previousBlock != Nxt.lastBlock) {
            return false;
          }
          synchronized (Nxt.transactions)
          {
            localObject4 = ((HashMap)???).entrySet().iterator();
            while (((Iterator)localObject4).hasNext())
            {
              localObject3 = (Map.Entry)((Iterator)localObject4).next();
              localObject5 = (Nxt.Transaction)((Map.Entry)localObject3).getValue();
              ((Nxt.Transaction)localObject5).height = localBlock.height;
              Nxt.transactions.put((Long)((Map.Entry)localObject3).getKey(), localObject5);
            }
          }
          localBlock.analyze();
          ??? = new JSONArray();
          localObject3 = new JSONArray();
          localObject5 = ((HashMap)???).entrySet().iterator();
          Object localObject8;
          while (((Iterator)localObject5).hasNext())
          {
            localObject4 = (Map.Entry)((Iterator)localObject5).next();
            localObject6 = (Nxt.Transaction)((Map.Entry)localObject4).getValue();
            localJSONObject = new JSONObject();
            localJSONObject.put("index", Integer.valueOf(((Nxt.Transaction)localObject6).index));
            localJSONObject.put("blockTimestamp", Integer.valueOf(localBlock.timestamp));
            localJSONObject.put("transactionTimestamp", Integer.valueOf(((Nxt.Transaction)localObject6).timestamp));
            localJSONObject.put("sender", Nxt.convert(Nxt.Account.getId(((Nxt.Transaction)localObject6).senderPublicKey)));
            localJSONObject.put("recipient", Nxt.convert(((Nxt.Transaction)localObject6).recipient));
            localJSONObject.put("amount", Integer.valueOf(((Nxt.Transaction)localObject6).amount));
            localJSONObject.put("fee", Integer.valueOf(((Nxt.Transaction)localObject6).fee));
            localJSONObject.put("id", Nxt.convert(((Nxt.Transaction)localObject6).getId()));
            ((JSONArray)???).add(localJSONObject);
            localObject7 = (Nxt.Transaction)Nxt.unconfirmedTransactions.remove(((Map.Entry)localObject4).getKey());
            if (localObject7 != null)
            {
              localObject8 = new JSONObject();
              ((JSONObject)localObject8).put("index", Integer.valueOf(((Nxt.Transaction)localObject7).index));
              ((JSONArray)localObject3).add(localObject8);
              localObject9 = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(((Nxt.Transaction)localObject7).senderPublicKey)));
              synchronized (localObject9)
              {
                ((Nxt.Account)localObject9).setUnconfirmedBalance(((Nxt.Account)localObject9).unconfirmedBalance + (((Nxt.Transaction)localObject7).amount + ((Nxt.Transaction)localObject7).fee) * 100L);
              }
            }
          }
          long l3 = localBlock.getId();
          for (i6 = 0; i6 < localBlock.transactions.length; i6++) {
            ((Nxt.Transaction)Nxt.transactions.get(Long.valueOf(localBlock.transactions[i6]))).block = l3;
          }
          if (paramBoolean)
          {
            Nxt.Transaction.saveTransactions("transactions.nxt");
            saveBlocks("blocks.nxt", false);
          }
          if (localBlock.timestamp >= i2 - 15)
          {
            localObject6 = localBlock.getJSONObject(Nxt.transactions);
            ((JSONObject)localObject6).put("requestType", "processBlock");
            Nxt.Peer.sendToAllPeers((JSONObject)localObject6);
          }
          localObject6 = new JSONArray();
          JSONObject localJSONObject = new JSONObject();
          localJSONObject.put("index", Integer.valueOf(localBlock.index));
          localJSONObject.put("timestamp", Integer.valueOf(localBlock.timestamp));
          localJSONObject.put("numberOfTransactions", Integer.valueOf(localBlock.numberOfTransactions));
          localJSONObject.put("totalAmount", Integer.valueOf(localBlock.totalAmount));
          localJSONObject.put("totalFee", Integer.valueOf(localBlock.totalFee));
          localJSONObject.put("payloadLength", Integer.valueOf(localBlock.payloadLength));
          localJSONObject.put("generator", Nxt.convert(Nxt.Account.getId(localBlock.generatorPublicKey)));
          localJSONObject.put("height", Integer.valueOf(getLastBlock().height));
          localJSONObject.put("version", Integer.valueOf(localBlock.version));
          localJSONObject.put("block", Nxt.convert(localBlock.getId()));
          localJSONObject.put("baseTarget", BigInteger.valueOf(localBlock.baseTarget).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
          ((JSONArray)localObject6).add(localJSONObject);
          Object localObject7 = new JSONObject();
          ((JSONObject)localObject7).put("response", "processNewData");
          ((JSONObject)localObject7).put("addedConfirmedTransactions", ???);
          if (((JSONArray)localObject3).size() > 0) {
            ((JSONObject)localObject7).put("removedUnconfirmedTransactions", localObject3);
          }
          ((JSONObject)localObject7).put("addedRecentBlocks", localObject6);
          Object localObject9 = Nxt.users.values().iterator();
          while (((Iterator)localObject9).hasNext())
          {
            localObject8 = (Nxt.User)((Iterator)localObject9).next();
            ((Nxt.User)localObject8).send((JSONObject)localObject7);
          }
        }
        return true;
      }
      catch (Exception localException)
      {
        Nxt.logMessage("11: " + localException.toString());
      }
      return false;
    }
    
    static void saveBlocks(String paramString, boolean paramBoolean)
      throws Exception
    {
      synchronized (Nxt.blocks)
      {
        FileOutputStream localFileOutputStream = new FileOutputStream(paramString);
        ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);
        localObjectOutputStream.writeInt(Nxt.blockCounter);
        localObjectOutputStream.writeObject(Nxt.blocks);
        localObjectOutputStream.writeLong(Nxt.lastBlock);
        localObjectOutputStream.close();
        localFileOutputStream.close();
      }
    }
    
    boolean verifyBlockSignature()
      throws Exception
    {
      Nxt.Account localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(this.generatorPublicKey)));
      if (localAccount == null) {
        return false;
      }
      if (localAccount.publicKey == null) {
        localAccount.publicKey = this.generatorPublicKey;
      } else if (!Arrays.equals(this.generatorPublicKey, localAccount.publicKey)) {
        return false;
      }
      byte[] arrayOfByte1 = getBytes();
      byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
      System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
      return Nxt.Crypto.verify(this.blockSignature, arrayOfByte2, this.generatorPublicKey);
    }
    
    boolean verifyGenerationSignature()
    {
      try
      {
        Block localBlock;
        Nxt.Account localAccount;
        int i;
        BigInteger localBigInteger1;
        byte[] arrayOfByte;
        BigInteger localBigInteger2;
        if (getLastBlock().height <= 20000)
        {
          localBlock = (Block)Nxt.blocks.get(Long.valueOf(this.previousBlock));
          if (localBlock == null) {
            return false;
          }
          if (!Nxt.Crypto.verify(this.generationSignature, localBlock.generationSignature, this.generatorPublicKey)) {
            return false;
          }
          localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(this.generatorPublicKey)));
          if ((localAccount == null) || (localAccount.getEffectiveBalance() == 0)) {
            return false;
          }
          i = this.timestamp - localBlock.timestamp;
          localBigInteger1 = BigInteger.valueOf(getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
          arrayOfByte = MessageDigest.getInstance("SHA-256").digest(this.generationSignature);
          localBigInteger2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
          if (localBigInteger2.compareTo(localBigInteger1) >= 0) {
            return false;
          }
        }
        else
        {
          localBlock = (Block)Nxt.blocks.get(Long.valueOf(this.previousBlock));
          if (localBlock == null) {
            return false;
          }
          if (!Nxt.Crypto.verify(this.generationSignature, localBlock.generationSignature, this.generatorPublicKey)) {
            return false;
          }
          localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(this.generatorPublicKey)));
          if ((localAccount == null) || (localAccount.getEffectiveBalance() == 0)) {
            return false;
          }
          i = this.timestamp - localBlock.timestamp;
          localBigInteger1 = BigInteger.valueOf(getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
          arrayOfByte = MessageDigest.getInstance("SHA-256").digest(this.generationSignature);
          localBigInteger2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
          if (localBigInteger2.compareTo(localBigInteger1) >= 0) {
            return false;
          }
        }
        return true;
      }
      catch (Exception localException) {}
      return false;
    }
  }
  
  static class Crypto
  {
    static byte[] getPublicKey(String paramString)
    {
      try
      {
        byte[] arrayOfByte = new byte[32];
        Nxt.Curve25519.keygen(arrayOfByte, null, MessageDigest.getInstance("SHA-256").digest(paramString.getBytes("UTF-8")));
        return arrayOfByte;
      }
      catch (Exception localException) {}
      return null;
    }
    
    static byte[] sign(byte[] paramArrayOfByte, String paramString)
    {
      try
      {
        byte[] arrayOfByte1 = new byte[32];
        byte[] arrayOfByte2 = new byte[32];
        MessageDigest localMessageDigest = MessageDigest.getInstance("SHA-256");
        Nxt.Curve25519.keygen(arrayOfByte1, arrayOfByte2, localMessageDigest.digest(paramString.getBytes("UTF-8")));
        byte[] arrayOfByte3 = localMessageDigest.digest(paramArrayOfByte);
        localMessageDigest.update(arrayOfByte3);
        byte[] arrayOfByte4 = localMessageDigest.digest(arrayOfByte2);
        byte[] arrayOfByte5 = new byte[32];
        Nxt.Curve25519.keygen(arrayOfByte5, null, arrayOfByte4);
        localMessageDigest.update(arrayOfByte3);
        byte[] arrayOfByte6 = localMessageDigest.digest(arrayOfByte5);
        byte[] arrayOfByte7 = new byte[32];
        Nxt.Curve25519.sign(arrayOfByte7, arrayOfByte6, arrayOfByte4, arrayOfByte2);
        byte[] arrayOfByte8 = new byte[64];
        System.arraycopy(arrayOfByte7, 0, arrayOfByte8, 0, 32);
        System.arraycopy(arrayOfByte6, 0, arrayOfByte8, 32, 32);
        return arrayOfByte8;
      }
      catch (Exception localException) {}
      return null;
    }
    
    static boolean verify(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
    {
      try
      {
        byte[] arrayOfByte1 = new byte[32];
        byte[] arrayOfByte2 = new byte[32];
        System.arraycopy(paramArrayOfByte1, 0, arrayOfByte2, 0, 32);
        byte[] arrayOfByte3 = new byte[32];
        System.arraycopy(paramArrayOfByte1, 32, arrayOfByte3, 0, 32);
        Nxt.Curve25519.verify(arrayOfByte1, arrayOfByte2, arrayOfByte3, paramArrayOfByte3);
        MessageDigest localMessageDigest = MessageDigest.getInstance("SHA-256");
        byte[] arrayOfByte4 = localMessageDigest.digest(paramArrayOfByte2);
        localMessageDigest.update(arrayOfByte4);
        byte[] arrayOfByte5 = localMessageDigest.digest(arrayOfByte1);
        return Arrays.equals(arrayOfByte3, arrayOfByte5);
      }
      catch (Exception localException) {}
      return false;
    }
  }
  
  static class Curve25519
  {
    public static final int KEY_SIZE = 32;
    public static final byte[] ZERO = new byte[32];
    public static final byte[] PRIME = { -19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 127 };
    public static final byte[] ORDER = { -19, -45, -11, 92, 26, 99, 18, 88, -42, -100, -9, -94, -34, -7, -34, 20, 00000000000000016 };
    private static final int P25 = 33554431;
    private static final int P26 = 67108863;
    private static final byte[] ORDER_TIMES_8 = { 104, -97, -82, -25, -46, 24, -109, -64, -78, -26, -68, 23, -11, -50, -9, -90, 000000000000000-128 };
    private static final Nxt.Curve25519.long10 BASE_2Y = new Nxt.Curve25519.long10(39999547L, 18689728L, 59995525L, 1648697L, 57546132L, 24010086L, 19059592L, 5425144L, 63499247L, 16420658L);
    private static final Nxt.Curve25519.long10 BASE_R2Y = new Nxt.Curve25519.long10(5744L, 8160848L, 4790893L, 13779497L, 35730846L, 12541209L, 49101323L, 30047407L, 40071253L, 6226132L);
    
    public static final void clamp(byte[] paramArrayOfByte)
    {
      paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] & 0x7F));
      paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] | 0x40));
      int tmp22_21 = 0;
      paramArrayOfByte[tmp22_21] = ((byte)(paramArrayOfByte[tmp22_21] & 0xF8));
    }
    
    public static final void keygen(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
    {
      clamp(paramArrayOfByte3);
      core(paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, null);
    }
    
    public static final void curve(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
    {
      core(paramArrayOfByte1, null, paramArrayOfByte2, paramArrayOfByte3);
    }
    
    public static final boolean sign(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    {
      byte[] arrayOfByte1 = new byte[65];
      byte[] arrayOfByte2 = new byte[33];
      for (int j = 0; j < 32; j++) {
        paramArrayOfByte1[j] = 0;
      }
      j = mula_small(paramArrayOfByte1, paramArrayOfByte3, 0, paramArrayOfByte2, 32, -1);
      mula_small(paramArrayOfByte1, paramArrayOfByte1, 0, ORDER, 32, (15 - paramArrayOfByte1[31]) / 16);
      mula32(arrayOfByte1, paramArrayOfByte1, paramArrayOfByte4, 32, 1);
      divmod(arrayOfByte2, arrayOfByte1, 64, ORDER, 32);
      int i = 0;
      for (j = 0; j < 32; j++) {
        i |= (paramArrayOfByte1[j] = arrayOfByte1[j]);
      }
      return i != 0;
    }
    
    public static final void verify(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    {
      byte[] arrayOfByte = new byte[32];
      Nxt.Curve25519.long10[] arrayOflong101 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong102 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong103 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong104 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong105 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong106 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      int i = 0;
      int j = 0;
      int k = 0;
      int m = 0;
      set(arrayOflong101[0], 9);
      unpack(arrayOflong101[1], paramArrayOfByte4);
      x_to_y2(arrayOflong105[0], arrayOflong106[0], arrayOflong101[1]);
      sqrt(arrayOflong105[0], arrayOflong106[0]);
      int i1 = is_negative(arrayOflong105[0]);
      arrayOflong106[0]._0 += 39420360L;
      mul(arrayOflong106[1], BASE_2Y, arrayOflong105[0]);
      sub(arrayOflong105[i1], arrayOflong106[0], arrayOflong106[1]);
      add(arrayOflong105[(1 - i1)], arrayOflong106[0], arrayOflong106[1]);
      cpy(arrayOflong106[0], arrayOflong101[1]);
      arrayOflong106[0]._0 -= 9L;
      sqr(arrayOflong106[1], arrayOflong106[0]);
      recip(arrayOflong106[0], arrayOflong106[1], 0);
      mul(arrayOflong102[0], arrayOflong105[0], arrayOflong106[0]);
      sub(arrayOflong102[0], arrayOflong102[0], arrayOflong101[1]);
      arrayOflong102[0]._0 -= 486671L;
      mul(arrayOflong102[1], arrayOflong105[1], arrayOflong106[0]);
      sub(arrayOflong102[1], arrayOflong102[1], arrayOflong101[1]);
      arrayOflong102[1]._0 -= 486671L;
      mul_small(arrayOflong102[0], arrayOflong102[0], 1L);
      mul_small(arrayOflong102[1], arrayOflong102[1], 1L);
      for (int n = 0; n < 32; n++)
      {
        i = i >> 8 ^ paramArrayOfByte2[n] & 0xFF ^ (paramArrayOfByte2[n] & 0xFF) << 1;
        j = j >> 8 ^ paramArrayOfByte3[n] & 0xFF ^ (paramArrayOfByte3[n] & 0xFF) << 1;
        m = i ^ j ^ 0xFFFFFFFF;
        k = m & (k & 0x80) >> 7 ^ i;
        k ^= m & (k & 0x1) << 1;
        k ^= m & (k & 0x2) << 1;
        k ^= m & (k & 0x4) << 1;
        k ^= m & (k & 0x8) << 1;
        k ^= m & (k & 0x10) << 1;
        k ^= m & (k & 0x20) << 1;
        k ^= m & (k & 0x40) << 1;
        arrayOfByte[n] = ((byte)k);
      }
      k = (m & (k & 0x80) << 1 ^ i) >> 8;
      set(arrayOflong103[0], 1);
      cpy(arrayOflong103[1], arrayOflong101[k]);
      cpy(arrayOflong103[2], arrayOflong102[0]);
      set(arrayOflong104[0], 0);
      set(arrayOflong104[1], 1);
      set(arrayOflong104[2], 1);
      i = 0;
      j = 0;
      n = 32;
      while (n-- != 0)
      {
        i = i << 8 | paramArrayOfByte2[n] & 0xFF;
        j = j << 8 | paramArrayOfByte3[n] & 0xFF;
        k = k << 8 | arrayOfByte[n] & 0xFF;
        i1 = 8;
        while (i1-- != 0)
        {
          mont_prep(arrayOflong105[0], arrayOflong106[0], arrayOflong103[0], arrayOflong104[0]);
          mont_prep(arrayOflong105[1], arrayOflong106[1], arrayOflong103[1], arrayOflong104[1]);
          mont_prep(arrayOflong105[2], arrayOflong106[2], arrayOflong103[2], arrayOflong104[2]);
          i2 = ((i ^ i >> 1) >> i1 & 0x1) + ((j ^ j >> 1) >> i1 & 0x1);
          mont_dbl(arrayOflong103[2], arrayOflong104[2], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[0], arrayOflong104[0]);
          i2 = k >> i1 & 0x2 ^ (k >> i1 & 0x1) << 1;
          mont_add(arrayOflong105[1], arrayOflong106[1], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[1], arrayOflong104[1], arrayOflong101[(k >> i1 & 0x1)]);
          mont_add(arrayOflong105[2], arrayOflong106[2], arrayOflong105[0], arrayOflong106[0], arrayOflong103[2], arrayOflong104[2], arrayOflong102[(((i ^ j) >> i1 & 0x2) >> 1)]);
        }
      }
      int i2 = (i & 0x1) + (j & 0x1);
      recip(arrayOflong105[0], arrayOflong104[i2], 0);
      mul(arrayOflong105[1], arrayOflong103[i2], arrayOflong105[0]);
      pack(arrayOflong105[1], paramArrayOfByte1);
    }
    
    private static final void cpy32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
    {
      for (int i = 0; i < 32; i++) {
        paramArrayOfByte1[i] = paramArrayOfByte2[i];
      }
    }
    
    private static final int mula_small(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2, int paramInt3)
    {
      int i = 0;
      for (int j = 0; j < paramInt2; j++)
      {
        i += (paramArrayOfByte2[(j + paramInt1)] & 0xFF) + paramInt3 * (paramArrayOfByte3[j] & 0xFF);
        paramArrayOfByte1[(j + paramInt1)] = ((byte)i);
        i >>= 8;
      }
      return i;
    }
    
    private static final int mula32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt1, int paramInt2)
    {
      int i = 0;
      for (int j = 0; j < paramInt1; j++)
      {
        int k = paramInt2 * (paramArrayOfByte3[j] & 0xFF);
        i += mula_small(paramArrayOfByte1, paramArrayOfByte1, j, paramArrayOfByte2, 31, k) + (paramArrayOfByte1[(j + 31)] & 0xFF) + k * (paramArrayOfByte2[31] & 0xFF);
        paramArrayOfByte1[(j + 31)] = ((byte)i);
        i >>= 8;
      }
      paramArrayOfByte1[(j + 31)] = ((byte)(i + (paramArrayOfByte1[(j + 31)] & 0xFF)));
      return i >> 8;
    }
    
    private static final void divmod(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2)
    {
      int i = 0;
      int j = (paramArrayOfByte3[(paramInt2 - 1)] & 0xFF) << 8;
      if (paramInt2 > 1) {
        j |= paramArrayOfByte3[(paramInt2 - 2)] & 0xFF;
      }
      while (paramInt1-- >= paramInt2)
      {
        int k = i << 16 | (paramArrayOfByte2[paramInt1] & 0xFF) << 8;
        if (paramInt1 > 0) {
          k |= paramArrayOfByte2[(paramInt1 - 1)] & 0xFF;
        }
        k /= j;
        i += mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -k);
        paramArrayOfByte1[(paramInt1 - paramInt2 + 1)] = ((byte)(k + i & 0xFF));
        mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -i);
        i = paramArrayOfByte2[paramInt1] & 0xFF;
        paramArrayOfByte2[paramInt1] = 0;
      }
      paramArrayOfByte2[(paramInt2 - 1)] = ((byte)i);
    }
    
    private static final int numsize(byte[] paramArrayOfByte, int paramInt)
    {
      while ((paramInt-- != 0) && (paramArrayOfByte[paramInt] == 0)) {}
      return paramInt + 1;
    }
    
    private static final byte[] egcd32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    {
      int j = 32;
      for (int m = 0; m < 32; m++)
      {
        int tmp17_16 = 0;
        paramArrayOfByte2[m] = tmp17_16;
        paramArrayOfByte1[m] = tmp17_16;
      }
      paramArrayOfByte1[0] = 1;
      int i = numsize(paramArrayOfByte3, 32);
      if (i == 0) {
        return paramArrayOfByte2;
      }
      byte[] arrayOfByte = new byte[32];
      for (;;)
      {
        int k = j - i + 1;
        divmod(arrayOfByte, paramArrayOfByte4, j, paramArrayOfByte3, i);
        j = numsize(paramArrayOfByte4, j);
        if (j == 0) {
          return paramArrayOfByte1;
        }
        mula32(paramArrayOfByte2, paramArrayOfByte1, arrayOfByte, k, -1);
        k = i - j + 1;
        divmod(arrayOfByte, paramArrayOfByte3, i, paramArrayOfByte4, j);
        i = numsize(paramArrayOfByte3, i);
        if (i == 0) {
          return paramArrayOfByte2;
        }
        mula32(paramArrayOfByte1, paramArrayOfByte2, arrayOfByte, k, -1);
      }
    }
    
    private static final void unpack(Nxt.Curve25519.long10 paramlong10, byte[] paramArrayOfByte)
    {
      paramlong10._0 = (paramArrayOfByte[0] & 0xFF | (paramArrayOfByte[1] & 0xFF) << 8 | (paramArrayOfByte[2] & 0xFF) << 16 | (paramArrayOfByte[3] & 0xFF & 0x3) << 24);
      paramlong10._1 = ((paramArrayOfByte[3] & 0xFF & 0xFFFFFFFC) >> 2 | (paramArrayOfByte[4] & 0xFF) << 6 | (paramArrayOfByte[5] & 0xFF) << 14 | (paramArrayOfByte[6] & 0xFF & 0x7) << 22);
      paramlong10._2 = ((paramArrayOfByte[6] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[7] & 0xFF) << 5 | (paramArrayOfByte[8] & 0xFF) << 13 | (paramArrayOfByte[9] & 0xFF & 0x1F) << 21);
      paramlong10._3 = ((paramArrayOfByte[9] & 0xFF & 0xFFFFFFE0) >> 5 | (paramArrayOfByte[10] & 0xFF) << 3 | (paramArrayOfByte[11] & 0xFF) << 11 | (paramArrayOfByte[12] & 0xFF & 0x3F) << 19);
      paramlong10._4 = ((paramArrayOfByte[12] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[13] & 0xFF) << 2 | (paramArrayOfByte[14] & 0xFF) << 10 | (paramArrayOfByte[15] & 0xFF) << 18);
      paramlong10._5 = (paramArrayOfByte[16] & 0xFF | (paramArrayOfByte[17] & 0xFF) << 8 | (paramArrayOfByte[18] & 0xFF) << 16 | (paramArrayOfByte[19] & 0xFF & 0x1) << 24);
      paramlong10._6 = ((paramArrayOfByte[19] & 0xFF & 0xFFFFFFFE) >> 1 | (paramArrayOfByte[20] & 0xFF) << 7 | (paramArrayOfByte[21] & 0xFF) << 15 | (paramArrayOfByte[22] & 0xFF & 0x7) << 23);
      paramlong10._7 = ((paramArrayOfByte[22] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[23] & 0xFF) << 5 | (paramArrayOfByte[24] & 0xFF) << 13 | (paramArrayOfByte[25] & 0xFF & 0xF) << 21);
      paramlong10._8 = ((paramArrayOfByte[25] & 0xFF & 0xFFFFFFF0) >> 4 | (paramArrayOfByte[26] & 0xFF) << 4 | (paramArrayOfByte[27] & 0xFF) << 12 | (paramArrayOfByte[28] & 0xFF & 0x3F) << 20);
      paramlong10._9 = ((paramArrayOfByte[28] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[29] & 0xFF) << 2 | (paramArrayOfByte[30] & 0xFF) << 10 | (paramArrayOfByte[31] & 0xFF) << 18);
    }
    
    private static final boolean is_overflow(Nxt.Curve25519.long10 paramlong10)
    {
      return ((paramlong10._0 > 67108844L) && ((paramlong10._1 & paramlong10._3 & paramlong10._5 & paramlong10._7 & paramlong10._9) == 33554431L) && ((paramlong10._2 & paramlong10._4 & paramlong10._6 & paramlong10._8) == 67108863L)) || (paramlong10._9 > 33554431L);
    }
    
    private static final void pack(Nxt.Curve25519.long10 paramlong10, byte[] paramArrayOfByte)
    {
      int i = 0;
      int j = 0;
      i = (is_overflow(paramlong10) ? 1 : 0) - (paramlong10._9 < 0L ? 1 : 0);
      j = i * -33554432;
      i *= 19;
      long l = i + paramlong10._0 + (paramlong10._1 << 26);
      paramArrayOfByte[0] = ((byte)(int)l);
      paramArrayOfByte[1] = ((byte)(int)(l >> 8));
      paramArrayOfByte[2] = ((byte)(int)(l >> 16));
      paramArrayOfByte[3] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._2 << 19);
      paramArrayOfByte[4] = ((byte)(int)l);
      paramArrayOfByte[5] = ((byte)(int)(l >> 8));
      paramArrayOfByte[6] = ((byte)(int)(l >> 16));
      paramArrayOfByte[7] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._3 << 13);
      paramArrayOfByte[8] = ((byte)(int)l);
      paramArrayOfByte[9] = ((byte)(int)(l >> 8));
      paramArrayOfByte[10] = ((byte)(int)(l >> 16));
      paramArrayOfByte[11] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._4 << 6);
      paramArrayOfByte[12] = ((byte)(int)l);
      paramArrayOfByte[13] = ((byte)(int)(l >> 8));
      paramArrayOfByte[14] = ((byte)(int)(l >> 16));
      paramArrayOfByte[15] = ((byte)(int)(l >> 24));
      l = (l >> 32) + paramlong10._5 + (paramlong10._6 << 25);
      paramArrayOfByte[16] = ((byte)(int)l);
      paramArrayOfByte[17] = ((byte)(int)(l >> 8));
      paramArrayOfByte[18] = ((byte)(int)(l >> 16));
      paramArrayOfByte[19] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._7 << 19);
      paramArrayOfByte[20] = ((byte)(int)l);
      paramArrayOfByte[21] = ((byte)(int)(l >> 8));
      paramArrayOfByte[22] = ((byte)(int)(l >> 16));
      paramArrayOfByte[23] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._8 << 12);
      paramArrayOfByte[24] = ((byte)(int)l);
      paramArrayOfByte[25] = ((byte)(int)(l >> 8));
      paramArrayOfByte[26] = ((byte)(int)(l >> 16));
      paramArrayOfByte[27] = ((byte)(int)(l >> 24));
      l = (l >> 32) + (paramlong10._9 + j << 6);
      paramArrayOfByte[28] = ((byte)(int)l);
      paramArrayOfByte[29] = ((byte)(int)(l >> 8));
      paramArrayOfByte[30] = ((byte)(int)(l >> 16));
      paramArrayOfByte[31] = ((byte)(int)(l >> 24));
    }
    
    private static final void cpy(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102)
    {
      paramlong101._0 = paramlong102._0;
      paramlong101._1 = paramlong102._1;
      paramlong101._2 = paramlong102._2;
      paramlong101._3 = paramlong102._3;
      paramlong101._4 = paramlong102._4;
      paramlong101._5 = paramlong102._5;
      paramlong101._6 = paramlong102._6;
      paramlong101._7 = paramlong102._7;
      paramlong101._8 = paramlong102._8;
      paramlong101._9 = paramlong102._9;
    }
    
    private static final void set(Nxt.Curve25519.long10 paramlong10, int paramInt)
    {
      paramlong10._0 = paramInt;
      paramlong10._1 = 0L;
      paramlong10._2 = 0L;
      paramlong10._3 = 0L;
      paramlong10._4 = 0L;
      paramlong10._5 = 0L;
      paramlong10._6 = 0L;
      paramlong10._7 = 0L;
      paramlong10._8 = 0L;
      paramlong10._9 = 0L;
    }
    
    private static final void add(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103)
    {
      paramlong102._0 += paramlong103._0;
      paramlong102._1 += paramlong103._1;
      paramlong102._2 += paramlong103._2;
      paramlong102._3 += paramlong103._3;
      paramlong102._4 += paramlong103._4;
      paramlong102._5 += paramlong103._5;
      paramlong102._6 += paramlong103._6;
      paramlong102._7 += paramlong103._7;
      paramlong102._8 += paramlong103._8;
      paramlong102._9 += paramlong103._9;
    }
    
    private static final void sub(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103)
    {
      paramlong102._0 -= paramlong103._0;
      paramlong102._1 -= paramlong103._1;
      paramlong102._2 -= paramlong103._2;
      paramlong102._3 -= paramlong103._3;
      paramlong102._4 -= paramlong103._4;
      paramlong102._5 -= paramlong103._5;
      paramlong102._6 -= paramlong103._6;
      paramlong102._7 -= paramlong103._7;
      paramlong102._8 -= paramlong103._8;
      paramlong102._9 -= paramlong103._9;
    }
    
    private static final Nxt.Curve25519.long10 mul_small(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, long paramLong)
    {
      long l = paramlong102._8 * paramLong;
      paramlong101._8 = (l & 0x3FFFFFF);
      l = (l >> 26) + paramlong102._9 * paramLong;
      paramlong101._9 = (l & 0x1FFFFFF);
      l = 19L * (l >> 25) + paramlong102._0 * paramLong;
      paramlong101._0 = (l & 0x3FFFFFF);
      l = (l >> 26) + paramlong102._1 * paramLong;
      paramlong101._1 = (l & 0x1FFFFFF);
      l = (l >> 25) + paramlong102._2 * paramLong;
      paramlong101._2 = (l & 0x3FFFFFF);
      l = (l >> 26) + paramlong102._3 * paramLong;
      paramlong101._3 = (l & 0x1FFFFFF);
      l = (l >> 25) + paramlong102._4 * paramLong;
      paramlong101._4 = (l & 0x3FFFFFF);
      l = (l >> 26) + paramlong102._5 * paramLong;
      paramlong101._5 = (l & 0x1FFFFFF);
      l = (l >> 25) + paramlong102._6 * paramLong;
      paramlong101._6 = (l & 0x3FFFFFF);
      l = (l >> 26) + paramlong102._7 * paramLong;
      paramlong101._7 = (l & 0x1FFFFFF);
      l = (l >> 25) + paramlong101._8;
      paramlong101._8 = (l & 0x3FFFFFF);
      paramlong101._9 += (l >> 26);
      return paramlong101;
    }
    
    private static final Nxt.Curve25519.long10 mul(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103)
    {
      long l1 = paramlong102._0;
      long l2 = paramlong102._1;
      long l3 = paramlong102._2;
      long l4 = paramlong102._3;
      long l5 = paramlong102._4;
      long l6 = paramlong102._5;
      long l7 = paramlong102._6;
      long l8 = paramlong102._7;
      long l9 = paramlong102._8;
      long l10 = paramlong102._9;
      long l11 = paramlong103._0;
      long l12 = paramlong103._1;
      long l13 = paramlong103._2;
      long l14 = paramlong103._3;
      long l15 = paramlong103._4;
      long l16 = paramlong103._5;
      long l17 = paramlong103._6;
      long l18 = paramlong103._7;
      long l19 = paramlong103._8;
      long l20 = paramlong103._9;
      long l21 = l1 * l19 + l3 * l17 + l5 * l15 + l7 * l13 + l9 * l11 + 2L * (l2 * l18 + l4 * l16 + l6 * l14 + l8 * l12) + 38L * (l10 * l20);
      paramlong101._8 = (l21 & 0x3FFFFFF);
      l21 = (l21 >> 26) + l1 * l20 + l2 * l19 + l3 * l18 + l4 * l17 + l5 * l16 + l6 * l15 + l7 * l14 + l8 * l13 + l9 * l12 + l10 * l11;
      paramlong101._9 = (l21 & 0x1FFFFFF);
      l21 = l1 * l11 + 19L * ((l21 >> 25) + l3 * l19 + l5 * l17 + l7 * l15 + l9 * l13) + 38L * (l2 * l20 + l4 * l18 + l6 * l16 + l8 * l14 + l10 * l12);
      paramlong101._0 = (l21 & 0x3FFFFFF);
      l21 = (l21 >> 26) + l1 * l12 + l2 * l11 + 19L * (l3 * l20 + l4 * l19 + l5 * l18 + l6 * l17 + l7 * l16 + l8 * l15 + l9 * l14 + l10 * l13);
      paramlong101._1 = (l21 & 0x1FFFFFF);
      l21 = (l21 >> 25) + l1 * l13 + l3 * l11 + 19L * (l5 * l19 + l7 * l17 + l9 * l15) + 2L * (l2 * l12) + 38L * (l4 * l20 + l6 * l18 + l8 * l16 + l10 * l14);
      paramlong101._2 = (l21 & 0x3FFFFFF);
      l21 = (l21 >> 26) + l1 * l14 + l2 * l13 + l3 * l12 + l4 * l11 + 19L * (l5 * l20 + l6 * l19 + l7 * l18 + l8 * l17 + l9 * l16 + l10 * l15);
      paramlong101._3 = (l21 & 0x1FFFFFF);
      l21 = (l21 >> 25) + l1 * l15 + l3 * l13 + l5 * l11 + 19L * (l7 * l19 + l9 * l17) + 2L * (l2 * l14 + l4 * l12) + 38L * (l6 * l20 + l8 * l18 + l10 * l16);
      paramlong101._4 = (l21 & 0x3FFFFFF);
      l21 = (l21 >> 26) + l1 * l16 + l2 * l15 + l3 * l14 + l4 * l13 + l5 * l12 + l6 * l11 + 19L * (l7 * l20 + l8 * l19 + l9 * l18 + l10 * l17);
      paramlong101._5 = (l21 & 0x1FFFFFF);
      l21 = (l21 >> 25) + l1 * l17 + l3 * l15 + l5 * l13 + l7 * l11 + 19L * (l9 * l19) + 2L * (l2 * l16 + l4 * l14 + l6 * l12) + 38L * (l8 * l20 + l10 * l18);
      paramlong101._6 = (l21 & 0x3FFFFFF);
      l21 = (l21 >> 26) + l1 * l18 + l2 * l17 + l3 * l16 + l4 * l15 + l5 * l14 + l6 * l13 + l7 * l12 + l8 * l11 + 19L * (l9 * l20 + l10 * l19);
      paramlong101._7 = (l21 & 0x1FFFFFF);
      l21 = (l21 >> 25) + paramlong101._8;
      paramlong101._8 = (l21 & 0x3FFFFFF);
      paramlong101._9 += (l21 >> 26);
      return paramlong101;
    }
    
    private static final Nxt.Curve25519.long10 sqr(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102)
    {
      long l1 = paramlong102._0;
      long l2 = paramlong102._1;
      long l3 = paramlong102._2;
      long l4 = paramlong102._3;
      long l5 = paramlong102._4;
      long l6 = paramlong102._5;
      long l7 = paramlong102._6;
      long l8 = paramlong102._7;
      long l9 = paramlong102._8;
      long l10 = paramlong102._9;
      long l11 = l5 * l5 + 2L * (l1 * l9 + l3 * l7) + 38L * (l10 * l10) + 4L * (l2 * l8 + l4 * l6);
      paramlong101._8 = (l11 & 0x3FFFFFF);
      l11 = (l11 >> 26) + 2L * (l1 * l10 + l2 * l9 + l3 * l8 + l4 * l7 + l5 * l6);
      paramlong101._9 = (l11 & 0x1FFFFFF);
      l11 = 19L * (l11 >> 25) + l1 * l1 + 38L * (l3 * l9 + l5 * l7 + l6 * l6) + 76L * (l2 * l10 + l4 * l8);
      paramlong101._0 = (l11 & 0x3FFFFFF);
      l11 = (l11 >> 26) + 2L * (l1 * l2) + 38L * (l3 * l10 + l4 * l9 + l5 * l8 + l6 * l7);
      paramlong101._1 = (l11 & 0x1FFFFFF);
      l11 = (l11 >> 25) + 19L * (l7 * l7) + 2L * (l1 * l3 + l2 * l2) + 38L * (l5 * l9) + 76L * (l4 * l10 + l6 * l8);
      paramlong101._2 = (l11 & 0x3FFFFFF);
      l11 = (l11 >> 26) + 2L * (l1 * l4 + l2 * l3) + 38L * (l5 * l10 + l6 * l9 + l7 * l8);
      paramlong101._3 = (l11 & 0x1FFFFFF);
      l11 = (l11 >> 25) + l3 * l3 + 2L * (l1 * l5) + 38L * (l7 * l9 + l8 * l8) + 4L * (l2 * l4) + 76L * (l6 * l10);
      paramlong101._4 = (l11 & 0x3FFFFFF);
      l11 = (l11 >> 26) + 2L * (l1 * l6 + l2 * l5 + l3 * l4) + 38L * (l7 * l10 + l8 * l9);
      paramlong101._5 = (l11 & 0x1FFFFFF);
      l11 = (l11 >> 25) + 19L * (l9 * l9) + 2L * (l1 * l7 + l3 * l5 + l4 * l4) + 4L * (l2 * l6) + 76L * (l8 * l10);
      paramlong101._6 = (l11 & 0x3FFFFFF);
      l11 = (l11 >> 26) + 2L * (l1 * l8 + l2 * l7 + l3 * l6 + l4 * l5) + 38L * (l9 * l10);
      paramlong101._7 = (l11 & 0x1FFFFFF);
      l11 = (l11 >> 25) + paramlong101._8;
      paramlong101._8 = (l11 & 0x3FFFFFF);
      paramlong101._9 += (l11 >> 26);
      return paramlong101;
    }
    
    private static final void recip(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, int paramInt)
    {
      Nxt.Curve25519.long10 locallong101 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong102 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong103 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong104 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong105 = new Nxt.Curve25519.long10();
      sqr(locallong102, paramlong102);
      sqr(locallong103, locallong102);
      sqr(locallong101, locallong103);
      mul(locallong103, locallong101, paramlong102);
      mul(locallong101, locallong103, locallong102);
      sqr(locallong102, locallong101);
      mul(locallong104, locallong102, locallong103);
      sqr(locallong102, locallong104);
      sqr(locallong103, locallong102);
      sqr(locallong102, locallong103);
      sqr(locallong103, locallong102);
      sqr(locallong102, locallong103);
      mul(locallong103, locallong102, locallong104);
      sqr(locallong102, locallong103);
      sqr(locallong104, locallong102);
      for (int i = 1; i < 5; i++)
      {
        sqr(locallong102, locallong104);
        sqr(locallong104, locallong102);
      }
      mul(locallong102, locallong104, locallong103);
      sqr(locallong104, locallong102);
      sqr(locallong105, locallong104);
      for (i = 1; i < 10; i++)
      {
        sqr(locallong104, locallong105);
        sqr(locallong105, locallong104);
      }
      mul(locallong104, locallong105, locallong102);
      for (i = 0; i < 5; i++)
      {
        sqr(locallong102, locallong104);
        sqr(locallong104, locallong102);
      }
      mul(locallong102, locallong104, locallong103);
      sqr(locallong103, locallong102);
      sqr(locallong104, locallong103);
      for (i = 1; i < 25; i++)
      {
        sqr(locallong103, locallong104);
        sqr(locallong104, locallong103);
      }
      mul(locallong103, locallong104, locallong102);
      sqr(locallong104, locallong103);
      sqr(locallong105, locallong104);
      for (i = 1; i < 50; i++)
      {
        sqr(locallong104, locallong105);
        sqr(locallong105, locallong104);
      }
      mul(locallong104, locallong105, locallong103);
      for (i = 0; i < 25; i++)
      {
        sqr(locallong105, locallong104);
        sqr(locallong104, locallong105);
      }
      mul(locallong103, locallong104, locallong102);
      sqr(locallong102, locallong103);
      sqr(locallong103, locallong102);
      if (paramInt != 0)
      {
        mul(paramlong101, paramlong102, locallong103);
      }
      else
      {
        sqr(locallong102, locallong103);
        sqr(locallong103, locallong102);
        sqr(locallong102, locallong103);
        mul(paramlong101, locallong102, locallong101);
      }
    }
    
    private static final int is_negative(Nxt.Curve25519.long10 paramlong10)
    {
      return (int)(((is_overflow(paramlong10)) || (paramlong10._9 < 0L) ? 1 : 0) ^ paramlong10._0 & 1L);
    }
    
    private static final void sqrt(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102)
    {
      Nxt.Curve25519.long10 locallong101 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong102 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong103 = new Nxt.Curve25519.long10();
      add(locallong102, paramlong102, paramlong102);
      recip(locallong101, locallong102, 1);
      sqr(paramlong101, locallong101);
      mul(locallong103, locallong102, paramlong101);
      locallong103._0 -= 1L;
      mul(locallong102, locallong101, locallong103);
      mul(paramlong101, paramlong102, locallong102);
    }
    
    private static final void mont_prep(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103, Nxt.Curve25519.long10 paramlong104)
    {
      add(paramlong101, paramlong103, paramlong104);
      sub(paramlong102, paramlong103, paramlong104);
    }
    
    private static final void mont_add(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103, Nxt.Curve25519.long10 paramlong104, Nxt.Curve25519.long10 paramlong105, Nxt.Curve25519.long10 paramlong106, Nxt.Curve25519.long10 paramlong107)
    {
      mul(paramlong105, paramlong102, paramlong103);
      mul(paramlong106, paramlong101, paramlong104);
      add(paramlong101, paramlong105, paramlong106);
      sub(paramlong102, paramlong105, paramlong106);
      sqr(paramlong105, paramlong101);
      sqr(paramlong101, paramlong102);
      mul(paramlong106, paramlong101, paramlong107);
    }
    
    private static final void mont_dbl(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103, Nxt.Curve25519.long10 paramlong104, Nxt.Curve25519.long10 paramlong105, Nxt.Curve25519.long10 paramlong106)
    {
      sqr(paramlong101, paramlong103);
      sqr(paramlong102, paramlong104);
      mul(paramlong105, paramlong101, paramlong102);
      sub(paramlong102, paramlong101, paramlong102);
      mul_small(paramlong106, paramlong102, 121665L);
      add(paramlong101, paramlong101, paramlong106);
      mul(paramlong106, paramlong101, paramlong102);
    }
    
    private static final void x_to_y2(Nxt.Curve25519.long10 paramlong101, Nxt.Curve25519.long10 paramlong102, Nxt.Curve25519.long10 paramlong103)
    {
      sqr(paramlong101, paramlong103);
      mul_small(paramlong102, paramlong103, 486662L);
      add(paramlong101, paramlong101, paramlong102);
      paramlong101._0 += 1L;
      mul(paramlong102, paramlong101, paramlong103);
    }
    
    private static final void core(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    {
      Nxt.Curve25519.long10 locallong101 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong102 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong103 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong104 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10 locallong105 = new Nxt.Curve25519.long10();
      Nxt.Curve25519.long10[] arrayOflong101 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      Nxt.Curve25519.long10[] arrayOflong102 = { new Nxt.Curve25519.long10(), new Nxt.Curve25519.long10() };
      if (paramArrayOfByte4 != null) {
        unpack(locallong101, paramArrayOfByte4);
      } else {
        set(locallong101, 9);
      }
      set(arrayOflong101[0], 1);
      set(arrayOflong102[0], 0);
      cpy(arrayOflong101[1], locallong101);
      set(arrayOflong102[1], 1);
      int i = 32;
      Object localObject;
      while (i-- != 0)
      {
        if (i == 0) {
          i = 0;
        }
        int j = 8;
        while (j-- != 0)
        {
          int k = (paramArrayOfByte3[i] & 0xFF) >> j & 0x1;
          int m = (paramArrayOfByte3[i] & 0xFF ^ 0xFFFFFFFF) >> j & 0x1;
          localObject = arrayOflong101[m];
          Nxt.Curve25519.long10 locallong106 = arrayOflong102[m];
          Nxt.Curve25519.long10 locallong107 = arrayOflong101[k];
          Nxt.Curve25519.long10 locallong108 = arrayOflong102[k];
          mont_prep(locallong102, locallong103, (Nxt.Curve25519.long10)localObject, locallong106);
          mont_prep(locallong104, locallong105, locallong107, locallong108);
          mont_add(locallong102, locallong103, locallong104, locallong105, (Nxt.Curve25519.long10)localObject, locallong106, locallong101);
          mont_dbl(locallong102, locallong103, locallong104, locallong105, locallong107, locallong108);
        }
      }
      recip(locallong102, arrayOflong102[0], 0);
      mul(locallong101, arrayOflong101[0], locallong102);
      pack(locallong101, paramArrayOfByte1);
      if (paramArrayOfByte2 != null)
      {
        x_to_y2(locallong103, locallong102, locallong101);
        recip(locallong104, arrayOflong102[1], 0);
        mul(locallong103, arrayOflong101[1], locallong104);
        add(locallong103, locallong103, locallong101);
        locallong103._0 += 486671L;
        locallong101._0 -= 9L;
        sqr(locallong104, locallong101);
        mul(locallong101, locallong103, locallong104);
        sub(locallong101, locallong101, locallong102);
        locallong101._0 -= 39420360L;
        mul(locallong102, locallong101, BASE_R2Y);
        if (is_negative(locallong102) != 0) {
          cpy32(paramArrayOfByte2, paramArrayOfByte3);
        } else {
          mula_small(paramArrayOfByte2, ORDER_TIMES_8, 0, paramArrayOfByte3, 32, -1);
        }
        byte[] arrayOfByte1 = new byte[32];
        byte[] arrayOfByte2 = new byte[64];
        localObject = new byte[64];
        cpy32(arrayOfByte1, ORDER);
        cpy32(paramArrayOfByte2, egcd32(arrayOfByte2, (byte[])localObject, paramArrayOfByte2, arrayOfByte1));
        if ((paramArrayOfByte2[31] & 0x80) != 0) {
          mula_small(paramArrayOfByte2, paramArrayOfByte2, 0, ORDER, 32, 1);
        }
      }
    }
    
    private static final class long10
    {
      public long _0;
      public long _1;
      public long _2;
      public long _3;
      public long _4;
      public long _5;
      public long _6;
      public long _7;
      public long _8;
      public long _9;
      
      public long10() {}
      
      public long10(long paramLong1, long paramLong2, long paramLong3, long paramLong4, long paramLong5, long paramLong6, long paramLong7, long paramLong8, long paramLong9, long paramLong10)
      {
        this._0 = paramLong1;
        this._1 = paramLong2;
        this._2 = paramLong3;
        this._3 = paramLong4;
        this._4 = paramLong5;
        this._5 = paramLong6;
        this._6 = paramLong7;
        this._7 = paramLong8;
        this._8 = paramLong9;
        this._9 = paramLong10;
      }
    }
  }
  
  static class Peer
    implements Comparable<Peer>
  {
    static final int STATE_NONCONNECTED = 0;
    static final int STATE_CONNECTED = 1;
    static final int STATE_DISCONNECTED = 2;
    int index;
    String scheme;
    int port;
    String announcedAddress;
    String hallmark;
    long accountId;
    int weight;
    int date;
    long adjustedWeight;
    String application;
    String version;
    long blacklistingTime;
    int state;
    long downloadedVolume;
    long uploadedVolume;
    
    Peer(String paramString)
    {
      this.announcedAddress = paramString;
    }
    
    static Peer addPeer(String paramString1, String paramString2)
    {
      try
      {
        new URL("http://" + paramString1);
      }
      catch (Exception localException1)
      {
        return null;
      }
      try
      {
        new URL("http://" + paramString2);
      }
      catch (Exception localException2)
      {
        paramString2 = "";
      }
      if ((paramString1.equals("localhost")) || (paramString1.equals("127.0.0.1")) || (paramString1.equals("0:0:0:0:0:0:0:1"))) {
        return null;
      }
      synchronized (Nxt.peers)
      {
        if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0) && (Nxt.myAddress.equals(paramString2))) {
          return null;
        }
        Peer localPeer = (Peer)Nxt.peers.get(paramString2.length() > 0 ? paramString2 : paramString1);
        if (localPeer == null)
        {
          localPeer = new Peer(paramString2);
          localPeer.index = (++Nxt.peerCounter);
          Nxt.peers.put(paramString2.length() > 0 ? paramString2 : paramString1, localPeer);
        }
        return localPeer;
      }
    }
    
    boolean analyzeHallmark(String paramString1, String paramString2)
    {
      if (paramString2 == null) {
        return true;
      }
      try
      {
        byte[] arrayOfByte1 = Nxt.convert(paramString2);
        ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte[] arrayOfByte2 = new byte[32];
        localByteBuffer.get(arrayOfByte2);
        int i = localByteBuffer.getShort();
        byte[] arrayOfByte3 = new byte[i];
        localByteBuffer.get(arrayOfByte3);
        String str = new String(arrayOfByte3, "UTF-8");
        if ((str.length() > 100) || (!str.equals(paramString1))) {
          return false;
        }
        int j = localByteBuffer.getInt();
        if ((j <= 0) || (j > 1000000000)) {
          return false;
        }
        int k = localByteBuffer.getInt();
        localByteBuffer.get();
        byte[] arrayOfByte4 = new byte[64];
        localByteBuffer.get(arrayOfByte4);
        byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
        System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
        if (Nxt.Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2))
        {
          this.hallmark = paramString2;
          long l1 = Nxt.Account.getId(arrayOfByte2);
          Nxt.Account localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(l1));
          if (localAccount == null) {
            return false;
          }
          LinkedList localLinkedList = new LinkedList();
          int m = 0;
          synchronized (Nxt.peers)
          {
            this.accountId = l1;
            this.weight = j;
            this.date = k;
            Iterator localIterator1 = Nxt.peers.values().iterator();
            while (localIterator1.hasNext())
            {
              Peer localPeer1 = (Peer)localIterator1.next();
              if (localPeer1.accountId == l1)
              {
                localLinkedList.add(localPeer1);
                if (localPeer1.date > m) {
                  m = localPeer1.date;
                }
              }
            }
            long l2 = 0L;
            Iterator localIterator2 = localLinkedList.iterator();
            Peer localPeer2;
            while (localIterator2.hasNext())
            {
              localPeer2 = (Peer)localIterator2.next();
              if (localPeer2.date == m)
              {
                l2 += localPeer2.weight;
              }
              else
              {
                localPeer2.adjustedWeight = 0L;
                localPeer2.updateWeight();
              }
            }
            localIterator2 = localLinkedList.iterator();
            while (localIterator2.hasNext())
            {
              localPeer2 = (Peer)localIterator2.next();
              localPeer2.adjustedWeight = (1000000000L * localPeer2.weight / l2);
              localPeer2.updateWeight();
            }
          }
          return true;
        }
      }
      catch (Exception localException) {}
      return false;
    }
    
    void blacklist()
    {
      this.blacklistingTime = System.currentTimeMillis();
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray1 = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONArray1.add(localJSONObject2);
      localJSONObject1.put("removedKnownPeers", localJSONArray1);
      JSONArray localJSONArray2 = new JSONArray();
      JSONObject localJSONObject3 = new JSONObject();
      localJSONObject3.put("index", Integer.valueOf(this.index));
      localJSONObject3.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
      Iterator localIterator = Nxt.wellKnownPeers.iterator();
      Object localObject;
      while (localIterator.hasNext())
      {
        localObject = (String)localIterator.next();
        if (this.announcedAddress.equals(localObject))
        {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
          break;
        }
      }
      localJSONArray2.add(localJSONObject3);
      localJSONObject1.put("addedBlacklistedPeers", localJSONArray2);
      localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        localObject = (Nxt.User)localIterator.next();
        ((Nxt.User)localObject).send(localJSONObject1);
      }
    }
    
    public int compareTo(Peer paramPeer)
    {
      long l1 = getWeight();
      long l2 = paramPeer.getWeight();
      if (l1 > l2) {
        return -1;
      }
      if (l1 < l2) {
        return 1;
      }
      return this.index - paramPeer.index;
    }
    
    void connect()
    {
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("requestType", "getInfo");
      if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0)) {
        localJSONObject1.put("announcedAddress", Nxt.myAddress);
      }
      if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
        localJSONObject1.put("hallmark", Nxt.myHallmark);
      }
      localJSONObject1.put("application", "NRS");
      localJSONObject1.put("version", "0.4.7e");
      localJSONObject1.put("scheme", Nxt.myScheme);
      localJSONObject1.put("port", Integer.valueOf(Nxt.myPort));
      localJSONObject1.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
      JSONObject localJSONObject2 = send(localJSONObject1);
      if (localJSONObject2 != null)
      {
        this.application = ((String)localJSONObject2.get("application"));
        this.version = ((String)localJSONObject2.get("version"));
        if (analyzeHallmark(this.announcedAddress, (String)localJSONObject2.get("hallmark"))) {
          setState(1);
        } else {
          blacklist();
        }
      }
    }
    
    void deactivate()
    {
      if (this.state == 1) {
        disconnect();
      }
      setState(0);
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONArray.add(localJSONObject2);
      localJSONObject1.put("removedActivePeers", localJSONArray);
      Object localObject1;
      if (this.announcedAddress.length() > 0)
      {
        localObject1 = new JSONArray();
        localObject2 = new JSONObject();
        ((JSONObject)localObject2).put("index", Integer.valueOf(this.index));
        ((JSONObject)localObject2).put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
        Iterator localIterator = Nxt.wellKnownPeers.iterator();
        while (localIterator.hasNext())
        {
          String str = (String)localIterator.next();
          if (this.announcedAddress.equals(str))
          {
            ((JSONObject)localObject2).put("wellKnown", Boolean.valueOf(true));
            break;
          }
        }
        ((JSONArray)localObject1).add(localObject2);
        localJSONObject1.put("addedKnownPeers", localObject1);
      }
      Object localObject2 = Nxt.users.values().iterator();
      while (((Iterator)localObject2).hasNext())
      {
        localObject1 = (Nxt.User)((Iterator)localObject2).next();
        ((Nxt.User)localObject1).send(localJSONObject1);
      }
    }
    
    void disconnect()
    {
      setState(2);
    }
    
    static Peer getAnyPeer(int paramInt, boolean paramBoolean)
    {
      synchronized (Nxt.peers)
      {
        Collection localCollection = ((HashMap)Nxt.peers.clone()).values();
        Iterator localIterator = localCollection.iterator();
        Object localObject1;
        while (localIterator.hasNext())
        {
          localObject1 = (Peer)localIterator.next();
          if ((((Peer)localObject1).blacklistingTime > 0L) || (((Peer)localObject1).state != paramInt) || (((Peer)localObject1).announcedAddress.length() == 0) || ((paramBoolean) && (Nxt.enableHallmarkProtection) && (((Peer)localObject1).getWeight() < Nxt.pullThreshold))) {
            localIterator.remove();
          }
        }
        if (localCollection.size() > 0)
        {
          localObject1 = (Peer[])localCollection.toArray(new Peer[0]);
          long l1 = 0L;
          for (int i = 0; i < localObject1.length; i++)
          {
            long l3 = localObject1[i].getWeight();
            if (l3 == 0L) {
              l3 = 1L;
            }
            l1 += l3;
          }
          long l2 = ThreadLocalRandom.current().nextLong(l1);
          for (int j = 0; j < localObject1.length; j++)
          {
            Peer localPeer = localObject1[j];
            long l4 = localPeer.getWeight();
            if (l4 == 0L) {
              l4 = 1L;
            }
            if (l2 -= l4 < 0L) {
              return localPeer;
            }
          }
        }
        return null;
      }
    }
    
    static int getNumberOfConnectedPublicPeers()
    {
      int i = 0;
      synchronized (Nxt.peers)
      {
        Iterator localIterator = Nxt.peers.values().iterator();
        while (localIterator.hasNext())
        {
          Peer localPeer = (Peer)localIterator.next();
          if ((localPeer.state == 1) && (localPeer.announcedAddress.length() > 0)) {
            i++;
          }
        }
      }
      return i;
    }
    
    int getWeight()
    {
      if (this.accountId == 0L) {
        return 0;
      }
      Nxt.Account localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(this.accountId));
      if (localAccount == null) {
        return 0;
      }
      return (int)(this.adjustedWeight * (localAccount.balance / 100L) / 1000000000L);
    }
    
    void removeBlacklistedStatus()
    {
      setState(0);
      this.blacklistingTime = 0L;
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray1 = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONArray1.add(localJSONObject2);
      localJSONObject1.put("removedBlacklistedPeers", localJSONArray1);
      JSONArray localJSONArray2 = new JSONArray();
      JSONObject localJSONObject3 = new JSONObject();
      localJSONObject3.put("index", Integer.valueOf(this.index));
      localJSONObject3.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
      Iterator localIterator = Nxt.wellKnownPeers.iterator();
      Object localObject;
      while (localIterator.hasNext())
      {
        localObject = (String)localIterator.next();
        if (this.announcedAddress.equals(localObject))
        {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
          break;
        }
      }
      localJSONArray2.add(localJSONObject3);
      localJSONObject1.put("addedKnownPeers", localJSONArray2);
      localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        localObject = (Nxt.User)localIterator.next();
        ((Nxt.User)localObject).send(localJSONObject1);
      }
    }
    
    void removePeer()
    {
      Object localObject2 = Nxt.peers.entrySet().iterator();
      while (((Iterator)localObject2).hasNext())
      {
        localObject1 = (Map.Entry)((Iterator)localObject2).next();
        if (((Map.Entry)localObject1).getValue() == this)
        {
          Nxt.peers.remove(((Map.Entry)localObject1).getKey());
          break;
        }
      }
      Object localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("response", "processNewData");
      localObject2 = new JSONArray();
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("index", Integer.valueOf(this.index));
      ((JSONArray)localObject2).add(localJSONObject);
      ((JSONObject)localObject1).put("removedKnownPeers", localObject2);
      Iterator localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.User localUser = (Nxt.User)localIterator.next();
        localUser.send((JSONObject)localObject1);
      }
    }
    
    static void sendToAllPeers(JSONObject paramJSONObject)
    {
      Peer[] arrayOfPeer1;
      synchronized (Nxt.peers)
      {
        arrayOfPeer1 = (Peer[])Nxt.peers.values().toArray(new Peer[0]);
      }
      Arrays.sort(arrayOfPeer1);
      for (??? : arrayOfPeer1)
      {
        if ((Nxt.enableHallmarkProtection) && (???.getWeight() < Nxt.pushThreshold)) {
          break;
        }
        if ((???.blacklistingTime == 0L) && (???.state == 1) && (???.announcedAddress.length() > 0)) {
          ???.send(paramJSONObject);
        }
      }
    }
    
    JSONObject send(JSONObject paramJSONObject)
    {
      String str1 = null;
      int i = 0;
      HttpURLConnection localHttpURLConnection = null;
      JSONObject localJSONObject;
      try
      {
        if (Nxt.communicationLoggingMask != 0) {
          str1 = "\"" + this.announcedAddress + "\": " + paramJSONObject.toString();
        }
        paramJSONObject.put("protocol", Integer.valueOf(1));
        URL localURL = new URL("http://" + this.announcedAddress + (new URL("http://" + this.announcedAddress).getPort() < 0 ? ":7874" : "") + "/nxt");
        localHttpURLConnection = (HttpURLConnection)localURL.openConnection();
        localHttpURLConnection.setRequestMethod("POST");
        localHttpURLConnection.setDoOutput(true);
        localHttpURLConnection.setConnectTimeout(Nxt.connectTimeout);
        localHttpURLConnection.setReadTimeout(Nxt.readTimeout);
        byte[] arrayOfByte1 = paramJSONObject.toString().getBytes("UTF-8");
        OutputStream localOutputStream = localHttpURLConnection.getOutputStream();
        localOutputStream.write(arrayOfByte1);
        localOutputStream.close();
        updateUploadedVolume(arrayOfByte1.length);
        if (localHttpURLConnection.getResponseCode() == 200)
        {
          InputStream localInputStream = localHttpURLConnection.getInputStream();
          ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
          byte[] arrayOfByte2 = new byte[65536];
          int j;
          while ((j = localInputStream.read(arrayOfByte2)) > 0) {
            localByteArrayOutputStream.write(arrayOfByte2, 0, j);
          }
          localInputStream.close();
          String str2 = localByteArrayOutputStream.toString("UTF-8");
          if ((Nxt.communicationLoggingMask & 0x4) != 0)
          {
            str1 = str1 + " >>> " + str2;
            i = 1;
          }
          updateDownloadedVolume(str2.getBytes("UTF-8").length);
          localJSONObject = (JSONObject)JSONValue.parse(str2);
        }
        else
        {
          if ((Nxt.communicationLoggingMask & 0x2) != 0)
          {
            str1 = str1 + " >>> Peer responded with HTTP " + localHttpURLConnection.getResponseCode() + " code!";
            i = 1;
          }
          disconnect();
          localJSONObject = null;
        }
      }
      catch (Exception localException)
      {
        if ((Nxt.communicationLoggingMask & 0x1) != 0)
        {
          str1 = str1 + " >>> " + localException.toString();
          i = 1;
        }
        if (this.state == 0) {
          blacklist();
        } else {
          disconnect();
        }
        localJSONObject = null;
      }
      if (i != 0) {
        Nxt.logMessage(str1 + "\n");
      }
      if (localHttpURLConnection != null) {
        localHttpURLConnection.disconnect();
      }
      return localJSONObject;
    }
    
    void setState(int paramInt)
    {
      JSONObject localJSONObject1;
      JSONArray localJSONArray;
      JSONObject localJSONObject2;
      Iterator localIterator;
      Object localObject;
      if ((this.state == 0) && (paramInt != 0))
      {
        localJSONObject1 = new JSONObject();
        localJSONObject1.put("response", "processNewData");
        if (this.announcedAddress.length() > 0)
        {
          localJSONArray = new JSONArray();
          localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(this.index));
          localJSONArray.add(localJSONObject2);
          localJSONObject1.put("removedKnownPeers", localJSONArray);
        }
        localJSONArray = new JSONArray();
        localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(this.index));
        if (paramInt == 2) {
          localJSONObject2.put("disconnected", Boolean.valueOf(true));
        }
        localIterator = Nxt.peers.entrySet().iterator();
        while (localIterator.hasNext())
        {
          localObject = (Map.Entry)localIterator.next();
          if (((Map.Entry)localObject).getValue() == this)
          {
            localJSONObject2.put("address", ((String)((Map.Entry)localObject).getKey()).length() > 30 ? ((String)((Map.Entry)localObject).getKey()).substring(0, 30) + "..." : (String)((Map.Entry)localObject).getKey());
            break;
          }
        }
        localJSONObject2.put("announcedAddress", this.announcedAddress.length() > 30 ? this.announcedAddress.substring(0, 30) + "..." : this.announcedAddress);
        localJSONObject2.put("weight", Integer.valueOf(getWeight()));
        localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
        localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
        localJSONObject2.put("software", (this.application == null ? "?" : this.application) + " (" + (this.version == null ? "?" : this.version) + ")");
        localIterator = Nxt.wellKnownPeers.iterator();
        while (localIterator.hasNext())
        {
          localObject = (String)localIterator.next();
          if (this.announcedAddress.equals(localObject))
          {
            localJSONObject2.put("wellKnown", Boolean.valueOf(true));
            break;
          }
        }
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("addedActivePeers", localJSONArray);
        localIterator = Nxt.users.values().iterator();
        while (localIterator.hasNext())
        {
          localObject = (Nxt.User)localIterator.next();
          ((Nxt.User)localObject).send(localJSONObject1);
        }
      }
      else if ((this.state != 0) && (paramInt != 0))
      {
        localJSONObject1 = new JSONObject();
        localJSONObject1.put("response", "processNewData");
        localJSONArray = new JSONArray();
        localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(this.index));
        localJSONObject2.put(paramInt == 1 ? "connected" : "disconnected", Boolean.valueOf(true));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        localIterator = Nxt.users.values().iterator();
        while (localIterator.hasNext())
        {
          localObject = (Nxt.User)localIterator.next();
          ((Nxt.User)localObject).send(localJSONObject1);
        }
      }
      this.state = paramInt;
    }
    
    void updateDownloadedVolume(int paramInt)
    {
      this.downloadedVolume += paramInt;
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONObject2.put("downloaded", Long.valueOf(this.downloadedVolume));
      localJSONArray.add(localJSONObject2);
      localJSONObject1.put("changedActivePeers", localJSONArray);
      Iterator localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.User localUser = (Nxt.User)localIterator.next();
        localUser.send(localJSONObject1);
      }
    }
    
    void updateUploadedVolume(int paramInt)
    {
      this.uploadedVolume += paramInt;
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONObject2.put("uploaded", Long.valueOf(this.uploadedVolume));
      localJSONArray.add(localJSONObject2);
      localJSONObject1.put("changedActivePeers", localJSONArray);
      Iterator localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.User localUser = (Nxt.User)localIterator.next();
        localUser.send(localJSONObject1);
      }
    }
    
    void updateWeight()
    {
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      JSONArray localJSONArray = new JSONArray();
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(this.index));
      localJSONObject2.put("weight", Integer.valueOf(getWeight()));
      localJSONArray.add(localJSONObject2);
      localJSONObject1.put("changedActivePeers", localJSONArray);
      Iterator localIterator = Nxt.users.values().iterator();
      while (localIterator.hasNext())
      {
        Nxt.User localUser = (Nxt.User)localIterator.next();
        localUser.send(localJSONObject1);
      }
    }
  }
  
  static class Transaction
    implements Comparable<Transaction>, Serializable
  {
    static final long serialVersionUID = 0L;
    static final byte TYPE_PAYMENT = 0;
    static final byte TYPE_MESSAGING = 1;
    static final byte TYPE_COLORED_COINS = 2;
    static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
    static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    static final int ASSET_ISSUANCE_FEE = 1000;
    byte type;
    byte subtype;
    int timestamp;
    short deadline;
    byte[] senderPublicKey;
    long recipient;
    int amount;
    int fee;
    long referencedTransaction;
    byte[] signature;
    Nxt.Transaction.Attachment attachment;
    int index;
    long block;
    int height;
    
    Transaction(byte paramByte1, byte paramByte2, int paramInt1, short paramShort, byte[] paramArrayOfByte1, long paramLong1, int paramInt2, int paramInt3, long paramLong2, byte[] paramArrayOfByte2)
    {
      this.type = paramByte1;
      this.subtype = paramByte2;
      this.timestamp = paramInt1;
      this.deadline = paramShort;
      this.senderPublicKey = paramArrayOfByte1;
      this.recipient = paramLong1;
      this.amount = paramInt2;
      this.fee = paramInt3;
      this.referencedTransaction = paramLong2;
      this.signature = paramArrayOfByte2;
      this.height = 2147483647;
    }
    
    public int compareTo(Transaction paramTransaction)
    {
      if (this.height < paramTransaction.height) {
        return -1;
      }
      if (this.height > paramTransaction.height) {
        return 1;
      }
      if (this.fee * 1048576L / getBytes().length > paramTransaction.fee * 1048576L / paramTransaction.getBytes().length) {
        return -1;
      }
      if (this.fee * 1048576L / getBytes().length < paramTransaction.fee * 1048576L / paramTransaction.getBytes().length) {
        return 1;
      }
      if (this.timestamp < paramTransaction.timestamp) {
        return -1;
      }
      if (this.timestamp > paramTransaction.timestamp) {
        return 1;
      }
      if (this.index < paramTransaction.index) {
        return -1;
      }
      if (this.index > paramTransaction.index) {
        return 1;
      }
      return 0;
    }
    
    byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(128 + (this.attachment == null ? 0 : this.attachment.getBytes().length));
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.put(this.type);
      localByteBuffer.put(this.subtype);
      localByteBuffer.putInt(this.timestamp);
      localByteBuffer.putShort(this.deadline);
      localByteBuffer.put(this.senderPublicKey);
      localByteBuffer.putLong(this.recipient);
      localByteBuffer.putInt(this.amount);
      localByteBuffer.putInt(this.fee);
      localByteBuffer.putLong(this.referencedTransaction);
      localByteBuffer.put(this.signature);
      if (this.attachment != null) {
        localByteBuffer.put(this.attachment.getBytes());
      }
      return localByteBuffer.array();
    }
    
    long getId()
      throws Exception
    {
      byte[] arrayOfByte = MessageDigest.getInstance("SHA-256").digest(getBytes());
      BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      return localBigInteger.longValue();
    }
    
    JSONObject getJSONObject()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("type", Byte.valueOf(this.type));
      localJSONObject.put("subtype", Byte.valueOf(this.subtype));
      localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
      localJSONObject.put("deadline", Short.valueOf(this.deadline));
      localJSONObject.put("senderPublicKey", Nxt.convert(this.senderPublicKey));
      localJSONObject.put("recipient", Nxt.convert(this.recipient));
      localJSONObject.put("amount", Integer.valueOf(this.amount));
      localJSONObject.put("fee", Integer.valueOf(this.fee));
      localJSONObject.put("referencedTransaction", Nxt.convert(this.referencedTransaction));
      localJSONObject.put("signature", Nxt.convert(this.signature));
      if (this.attachment != null) {
        localJSONObject.put("attachment", this.attachment.getJSONObject());
      }
      return localJSONObject;
    }
    
    static Transaction getTransaction(ByteBuffer paramByteBuffer)
    {
      byte b1 = paramByteBuffer.get();
      byte b2 = paramByteBuffer.get();
      int i = paramByteBuffer.getInt();
      short s = paramByteBuffer.getShort();
      byte[] arrayOfByte1 = new byte[32];
      paramByteBuffer.get(arrayOfByte1);
      long l1 = paramByteBuffer.getLong();
      int j = paramByteBuffer.getInt();
      int k = paramByteBuffer.getInt();
      long l2 = paramByteBuffer.getLong();
      byte[] arrayOfByte2 = new byte[64];
      paramByteBuffer.get(arrayOfByte2);
      Transaction localTransaction = new Transaction(b1, b2, i, s, arrayOfByte1, l1, j, k, l2, arrayOfByte2);
      int m;
      byte[] arrayOfByte3;
      int n;
      byte[] arrayOfByte4;
      switch (b1)
      {
      case 1: 
        switch (b2)
        {
        case 1: 
          m = paramByteBuffer.get();
          arrayOfByte3 = new byte[m];
          paramByteBuffer.get(arrayOfByte3);
          n = paramByteBuffer.getShort();
          arrayOfByte4 = new byte[n];
          paramByteBuffer.get(arrayOfByte4);
          try
          {
            localTransaction.attachment = new Nxt.Transaction.MessagingAliasAssignmentAttachment(new String(arrayOfByte3, "UTF-8"), new String(arrayOfByte4, "UTF-8"));
          }
          catch (Exception localException1) {}
        }
        break;
      case 2: 
        long l3;
        long l4;
        switch (b2)
        {
        case 0: 
          m = paramByteBuffer.get();
          arrayOfByte3 = new byte[m];
          paramByteBuffer.get(arrayOfByte3);
          n = paramByteBuffer.getShort();
          arrayOfByte4 = new byte[n];
          paramByteBuffer.get(arrayOfByte4);
          int i1 = paramByteBuffer.getInt();
          try
          {
            localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment(new String(arrayOfByte3, "UTF-8"), new String(arrayOfByte4, "UTF-8"), i1);
          }
          catch (Exception localException2) {}
        case 1: 
          l3 = paramByteBuffer.getLong();
          n = paramByteBuffer.getInt();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAssetTransferAttachment(l3, n);
          break;
        case 2: 
          l3 = paramByteBuffer.getLong();
          n = paramByteBuffer.getInt();
          l4 = paramByteBuffer.getLong();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment(l3, n, l4);
          break;
        case 3: 
          l3 = paramByteBuffer.getLong();
          n = paramByteBuffer.getInt();
          l4 = paramByteBuffer.getLong();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment(l3, n, l4);
          break;
        case 4: 
          l3 = paramByteBuffer.getLong();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment(l3);
          break;
        case 5: 
          l3 = paramByteBuffer.getLong();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment(l3);
        }
        break;
      }
      return localTransaction;
    }
    
    static Transaction getTransaction(JSONObject paramJSONObject)
    {
      byte b1 = ((Long)paramJSONObject.get("type")).byteValue();
      byte b2 = ((Long)paramJSONObject.get("subtype")).byteValue();
      int i = ((Long)paramJSONObject.get("timestamp")).intValue();
      short s = ((Long)paramJSONObject.get("deadline")).shortValue();
      byte[] arrayOfByte1 = Nxt.convert((String)paramJSONObject.get("senderPublicKey"));
      long l1 = new BigInteger((String)paramJSONObject.get("recipient")).longValue();
      int j = ((Long)paramJSONObject.get("amount")).intValue();
      int k = ((Long)paramJSONObject.get("fee")).intValue();
      long l2 = new BigInteger((String)paramJSONObject.get("referencedTransaction")).longValue();
      byte[] arrayOfByte2 = Nxt.convert((String)paramJSONObject.get("signature"));
      Transaction localTransaction = new Transaction(b1, b2, i, s, arrayOfByte1, l1, j, k, l2, arrayOfByte2);
      JSONObject localJSONObject = (JSONObject)paramJSONObject.get("attachment");
      String str1;
      String str2;
      switch (b1)
      {
      case 1: 
        switch (b2)
        {
        case 1: 
          str1 = (String)localJSONObject.get("alias");
          str2 = (String)localJSONObject.get("uri");
          localTransaction.attachment = new Nxt.Transaction.MessagingAliasAssignmentAttachment(str1.trim(), str2.trim());
        }
        break;
      case 2: 
        int m;
        long l3;
        long l4;
        switch (b2)
        {
        case 0: 
          str1 = (String)localJSONObject.get("name");
          str2 = (String)localJSONObject.get("description");
          m = ((Long)localJSONObject.get("quantity")).intValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAssetIssuanceAttachment(str1.trim(), str2.trim(), m);
          break;
        case 1: 
          l3 = new BigInteger((String)localJSONObject.get("asset")).longValue();
          m = ((Long)localJSONObject.get("quantity")).intValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAssetTransferAttachment(l3, m);
          break;
        case 2: 
          l3 = new BigInteger((String)localJSONObject.get("asset")).longValue();
          m = ((Long)localJSONObject.get("quantity")).intValue();
          l4 = ((Long)localJSONObject.get("price")).longValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment(l3, m, l4);
          break;
        case 3: 
          l3 = new BigInteger((String)localJSONObject.get("asset")).longValue();
          m = ((Long)localJSONObject.get("quantity")).intValue();
          l4 = ((Long)localJSONObject.get("price")).longValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment(l3, m, l4);
          break;
        case 4: 
          l3 = new BigInteger((String)localJSONObject.get("order")).longValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsAskOrderCancellationAttachment(l3);
          break;
        case 5: 
          l3 = new BigInteger((String)localJSONObject.get("order")).longValue();
          localTransaction.attachment = new Nxt.Transaction.ColoredCoinsBidOrderCancellationAttachment(l3);
        }
        break;
      }
      return localTransaction;
    }
    
    static void loadTransactions(String paramString)
      throws Exception
    {
      FileInputStream localFileInputStream = new FileInputStream(paramString);
      ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);
      Nxt.transactionCounter = localObjectInputStream.readInt();
      Nxt.transactions = (HashMap)localObjectInputStream.readObject();
      localObjectInputStream.close();
      localFileInputStream.close();
    }
    
    static void processTransactions(JSONObject paramJSONObject, String paramString)
    {
      JSONArray localJSONArray1 = (JSONArray)paramJSONObject.get(paramString);
      JSONArray localJSONArray2 = new JSONArray();
      int i = 0;
      break label1032;
      for (;;)
      {
        JSONObject localJSONObject2 = (JSONObject)localJSONArray1.get(i);
        Transaction localTransaction = getTransaction(localJSONObject2);
        try
        {
          int j = Nxt.getEpochTime(System.currentTimeMillis());
          if ((localTransaction.timestamp <= j + 15) && (localTransaction.deadline >= 1) && (localTransaction.timestamp + localTransaction.deadline * 60 >= j) && (localTransaction.fee > 0) && (localTransaction.validateAttachment())) {
            synchronized (Nxt.transactions)
            {
              long l1 = localTransaction.getId();
              if ((Nxt.transactions.get(Long.valueOf(l1)) != null) || (Nxt.unconfirmedTransactions.get(Long.valueOf(l1)) != null) || (Nxt.doubleSpendingTransactions.get(Long.valueOf(l1)) != null) || (localTransaction.verify()))
              {
                long l2 = Nxt.Account.getId(localTransaction.senderPublicKey);
                Nxt.Account localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(l2));
                int k;
                if (localAccount == null)
                {
                  k = 1;
                }
                else
                {
                  int m = localTransaction.amount + localTransaction.fee;
                  synchronized (localAccount)
                  {
                    if (localAccount.unconfirmedBalance < m * 100L)
                    {
                      k = 1;
                    }
                    else
                    {
                      k = 0;
                      localAccount.setUnconfirmedBalance(localAccount.unconfirmedBalance - m * 100L);
                      if (localTransaction.type == 2) {
                        if (localTransaction.subtype == 1)
                        {
                          localObject1 = (Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localTransaction.attachment;
                          if ((localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).asset)) == null) || (((Integer)localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).asset))).intValue() < ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).quantity))
                          {
                            k = 1;
                            localAccount.setUnconfirmedBalance(localAccount.unconfirmedBalance + m * 100L);
                          }
                          else
                          {
                            localAccount.unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).asset), Integer.valueOf(((Integer)localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAssetTransferAttachment)localObject1).quantity));
                          }
                        }
                        else if (localTransaction.subtype == 2)
                        {
                          localObject1 = (Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localTransaction.attachment;
                          if ((localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).asset)) == null) || (((Integer)localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).asset))).intValue() < ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).quantity))
                          {
                            k = 1;
                            localAccount.setUnconfirmedBalance(localAccount.unconfirmedBalance + m * 100L);
                          }
                          else
                          {
                            localAccount.unconfirmedAssetBalances.put(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).asset), Integer.valueOf(((Integer)localAccount.unconfirmedAssetBalances.get(Long.valueOf(((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).asset))).intValue() - ((Nxt.Transaction.ColoredCoinsAskOrderPlacementAttachment)localObject1).quantity));
                          }
                        }
                        else if (localTransaction.subtype == 3)
                        {
                          localObject1 = (Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localTransaction.attachment;
                          if (localAccount.unconfirmedBalance < ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject1).quantity * ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject1).price)
                          {
                            k = 1;
                            localAccount.setUnconfirmedBalance(localAccount.unconfirmedBalance + m * 100L);
                          }
                          else
                          {
                            localAccount.setUnconfirmedBalance(localAccount.unconfirmedBalance - ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject1).quantity * ((Nxt.Transaction.ColoredCoinsBidOrderPlacementAttachment)localObject1).price);
                          }
                        }
                      }
                    }
                  }
                }
                localTransaction.index = (++Nxt.transactionCounter);
                if (k != 0)
                {
                  Nxt.doubleSpendingTransactions.put(Long.valueOf(localTransaction.getId()), localTransaction);
                }
                else
                {
                  Nxt.unconfirmedTransactions.put(Long.valueOf(localTransaction.getId()), localTransaction);
                  if (paramString.equals("transactions")) {
                    localJSONArray2.add(localJSONObject2);
                  }
                }
                JSONObject localJSONObject3 = new JSONObject();
                localJSONObject3.put("response", "processNewData");
                ??? = new JSONArray();
                Object localObject1 = new JSONObject();
                ((JSONObject)localObject1).put("index", Integer.valueOf(localTransaction.index));
                ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localTransaction.timestamp));
                ((JSONObject)localObject1).put("deadline", Short.valueOf(localTransaction.deadline));
                ((JSONObject)localObject1).put("recipient", Nxt.convert(localTransaction.recipient));
                ((JSONObject)localObject1).put("amount", Integer.valueOf(localTransaction.amount));
                ((JSONObject)localObject1).put("fee", Integer.valueOf(localTransaction.fee));
                ((JSONObject)localObject1).put("sender", Nxt.convert(l2));
                ((JSONObject)localObject1).put("id", Nxt.convert(localTransaction.getId()));
                ((JSONArray)???).add(localObject1);
                if (k != 0) {
                  localJSONObject3.put("addedDoubleSpendingTransactions", ???);
                } else {
                  localJSONObject3.put("addedUnconfirmedTransactions", ???);
                }
                Iterator localIterator = Nxt.users.values().iterator();
                while (localIterator.hasNext())
                {
                  Nxt.User localUser = (Nxt.User)localIterator.next();
                  localUser.send(localJSONObject3);
                }
              }
            }
          }
          if (i < localJSONArray1.size()) {}
        }
        catch (Exception localException)
        {
          Nxt.logMessage("15: " + localException.toString());
          i++;
        }
      }
      label1032:
      if (localJSONArray2.size() > 0)
      {
        JSONObject localJSONObject1 = new JSONObject();
        localJSONObject1.put("requestType", "processTransactions");
        localJSONObject1.put("transactions", localJSONArray2);
        Nxt.Peer.sendToAllPeers(localJSONObject1);
      }
    }
    
    static void saveTransactions(String paramString)
      throws Exception
    {
      synchronized (Nxt.transactions)
      {
        FileOutputStream localFileOutputStream = new FileOutputStream(paramString);
        ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);
        localObjectOutputStream.writeInt(Nxt.transactionCounter);
        localObjectOutputStream.writeObject(Nxt.transactions);
        localObjectOutputStream.close();
        localFileOutputStream.close();
      }
    }
    
    void sign(String paramString)
    {
      this.signature = Nxt.Crypto.sign(getBytes(), paramString);
      try
      {
        while (!verify())
        {
          this.timestamp += 1;
          this.signature = new byte[64];
          this.signature = Nxt.Crypto.sign(getBytes(), paramString);
        }
      }
      catch (Exception localException)
      {
        Nxt.logMessage("16: " + localException.toString());
      }
    }
    
    boolean validateAttachment()
    {
      if ((this.amount > 1000000000) || (this.fee > 1000000000)) {
        return false;
      }
      switch (this.type)
      {
      case 0: 
        switch (this.subtype)
        {
        case 0: 
          return (this.amount > 0) && (this.amount <= 1000000000);
        }
        return false;
      case 1: 
        switch (this.subtype)
        {
        case 1: 
          if (Nxt.Block.getLastBlock().height < 22000) {
            return false;
          }
          try
          {
            Nxt.Transaction.MessagingAliasAssignmentAttachment localMessagingAliasAssignmentAttachment = (Nxt.Transaction.MessagingAliasAssignmentAttachment)this.attachment;
            if ((this.recipient != 1739068987193023818L) || (this.amount != 0) || (localMessagingAliasAssignmentAttachment.alias.length() == 0) || (localMessagingAliasAssignmentAttachment.alias.length() > 100) || (localMessagingAliasAssignmentAttachment.uri.length() > 1000)) {
              return false;
            }
            String str = localMessagingAliasAssignmentAttachment.alias.toLowerCase();
            for (int i = 0; i < str.length(); i++) {
              if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
                return false;
              }
            }
            Nxt.Alias localAlias;
            synchronized (Nxt.aliases)
            {
              localAlias = (Nxt.Alias)Nxt.aliases.get(str);
            }
            return (localAlias == null) || (localAlias.account.id == Nxt.Account.getId(this.senderPublicKey));
          }
          catch (Exception localException)
          {
            return false;
          }
        }
        return false;
      }
      return false;
    }
    
    boolean verify()
      throws Exception
    {
      Nxt.Account localAccount = (Nxt.Account)Nxt.accounts.get(Long.valueOf(Nxt.Account.getId(this.senderPublicKey)));
      if (localAccount == null) {
        return false;
      }
      if (localAccount.publicKey == null) {
        localAccount.publicKey = this.senderPublicKey;
      } else if (!Arrays.equals(this.senderPublicKey, localAccount.publicKey)) {
        return false;
      }
      byte[] arrayOfByte = getBytes();
      for (int i = 64; i < 128; i++) {
        arrayOfByte[i] = 0;
      }
      return Nxt.Crypto.verify(this.signature, arrayOfByte, this.senderPublicKey);
    }
    
    static abstract interface Attachment
    {
      public abstract byte[] getBytes();
      
      public abstract JSONObject getJSONObject();
    }
    
    static class ColoredCoinsAskOrderCancellationAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long order;
      
      ColoredCoinsAskOrderCancellationAttachment(long paramLong)
      {
        this.order = paramLong;
      }
      
      public byte[] getBytes()
      {
        ByteBuffer localByteBuffer = ByteBuffer.allocate(8);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putLong(this.order);
        return localByteBuffer.array();
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("order", Nxt.convert(this.order));
        return localJSONObject;
      }
    }
    
    static class ColoredCoinsAskOrderPlacementAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      long price;
      
      ColoredCoinsAskOrderPlacementAttachment(long paramLong1, int paramInt, long paramLong2)
      {
        this.asset = paramLong1;
        this.quantity = paramInt;
        this.price = paramLong2;
      }
      
      public byte[] getBytes()
      {
        ByteBuffer localByteBuffer = ByteBuffer.allocate(20);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putLong(this.asset);
        localByteBuffer.putInt(this.quantity);
        localByteBuffer.putLong(this.price);
        return localByteBuffer.array();
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("asset", Nxt.convert(this.asset));
        localJSONObject.put("quantity", Integer.valueOf(this.quantity));
        localJSONObject.put("price", Long.valueOf(this.price));
        return localJSONObject;
      }
    }
    
    static class ColoredCoinsAssetIssuanceAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      String name;
      String description;
      int quantity;
      
      ColoredCoinsAssetIssuanceAttachment(String paramString1, String paramString2, int paramInt)
      {
        this.name = paramString1;
        this.description = (paramString2 == null ? "" : paramString2);
        this.quantity = paramInt;
      }
      
      public byte[] getBytes()
      {
        try
        {
          byte[] arrayOfByte1 = this.name.getBytes("UTF-8");
          byte[] arrayOfByte2 = this.description.getBytes("UTF-8");
          ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length + 4);
          localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
          localByteBuffer.put((byte)arrayOfByte1.length);
          localByteBuffer.put(arrayOfByte1);
          localByteBuffer.putShort((short)arrayOfByte2.length);
          localByteBuffer.put(arrayOfByte2);
          localByteBuffer.putInt(this.quantity);
          return localByteBuffer.array();
        }
        catch (Exception localException) {}
        return null;
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("name", this.name);
        localJSONObject.put("description", this.description);
        localJSONObject.put("quantity", Integer.valueOf(this.quantity));
        return localJSONObject;
      }
    }
    
    static class ColoredCoinsAssetTransferAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      
      ColoredCoinsAssetTransferAttachment(long paramLong, int paramInt)
      {
        this.asset = paramLong;
        this.quantity = paramInt;
      }
      
      public byte[] getBytes()
      {
        ByteBuffer localByteBuffer = ByteBuffer.allocate(12);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putLong(this.asset);
        localByteBuffer.putInt(this.quantity);
        return localByteBuffer.array();
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("asset", Nxt.convert(this.asset));
        localJSONObject.put("quantity", Integer.valueOf(this.quantity));
        return localJSONObject;
      }
    }
    
    static class ColoredCoinsBidOrderCancellationAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long order;
      
      ColoredCoinsBidOrderCancellationAttachment(long paramLong)
      {
        this.order = paramLong;
      }
      
      public byte[] getBytes()
      {
        ByteBuffer localByteBuffer = ByteBuffer.allocate(8);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putLong(this.order);
        return localByteBuffer.array();
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("order", Nxt.convert(this.order));
        return localJSONObject;
      }
    }
    
    static class ColoredCoinsBidOrderPlacementAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      long price;
      
      ColoredCoinsBidOrderPlacementAttachment(long paramLong1, int paramInt, long paramLong2)
      {
        this.asset = paramLong1;
        this.quantity = paramInt;
        this.price = paramLong2;
      }
      
      public byte[] getBytes()
      {
        ByteBuffer localByteBuffer = ByteBuffer.allocate(20);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putLong(this.asset);
        localByteBuffer.putInt(this.quantity);
        localByteBuffer.putLong(this.price);
        return localByteBuffer.array();
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("asset", Nxt.convert(this.asset));
        localJSONObject.put("quantity", Integer.valueOf(this.quantity));
        localJSONObject.put("price", Long.valueOf(this.price));
        return localJSONObject;
      }
    }
    
    static class MessagingAliasAssignmentAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      String alias;
      String uri;
      
      MessagingAliasAssignmentAttachment(String paramString1, String paramString2)
      {
        this.alias = paramString1;
        this.uri = paramString2;
      }
      
      public byte[] getBytes()
      {
        try
        {
          byte[] arrayOfByte1 = this.alias.getBytes("UTF-8");
          byte[] arrayOfByte2 = this.uri.getBytes("UTF-8");
          ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length);
          localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
          localByteBuffer.put((byte)arrayOfByte1.length);
          localByteBuffer.put(arrayOfByte1);
          localByteBuffer.putShort((short)arrayOfByte2.length);
          localByteBuffer.put(arrayOfByte2);
          return localByteBuffer.array();
        }
        catch (Exception localException) {}
        return null;
      }
      
      public JSONObject getJSONObject()
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("alias", this.alias);
        localJSONObject.put("uri", this.uri);
        return localJSONObject;
      }
    }
  }
  
  static class User
  {
    ConcurrentLinkedQueue<JSONObject> pendingResponses = new ConcurrentLinkedQueue();
    AsyncContext asyncContext;
    String secretPhrase;
    
    void deinitializeKeyPair()
    {
      this.secretPhrase = null;
    }
    
    BigInteger initializeKeyPair(String paramString)
      throws Exception
    {
      this.secretPhrase = paramString;
      byte[] arrayOfByte = MessageDigest.getInstance("SHA-256").digest(Nxt.Crypto.getPublicKey(paramString));
      BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      return localBigInteger;
    }
    
    void send(JSONObject paramJSONObject)
    {
      synchronized (this)
      {
        if (this.asyncContext == null)
        {
          this.pendingResponses.offer(paramJSONObject);
        }
        else
        {
          JSONArray localJSONArray = new JSONArray();
          Object localObject1;
          while ((localObject1 = (JSONObject)this.pendingResponses.poll()) != null) {
            localJSONArray.add(localObject1);
          }
          localJSONArray.add(paramJSONObject);
          JSONObject localJSONObject = new JSONObject();
          localJSONObject.put("responses", localJSONArray);
          try
          {
            this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
            ServletOutputStream localServletOutputStream = this.asyncContext.getResponse().getOutputStream();
            localServletOutputStream.write(localJSONObject.toString().getBytes("UTF-8"));
            localServletOutputStream.close();
            this.asyncContext.complete();
            this.asyncContext = null;
          }
          catch (Exception localException)
          {
            Nxt.logMessage("17: " + localException.toString());
          }
        }
      }
    }
  }
  
  static class UserAsyncListener
    implements AsyncListener
  {
    Nxt.User user;
    
    UserAsyncListener(Nxt.User paramUser)
    {
      this.user = paramUser;
    }
    
    public void onComplete(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onError(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      this.user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      ServletOutputStream localServletOutputStream = this.user.asyncContext.getResponse().getOutputStream();
      localServletOutputStream.write(new JSONObject().toString().getBytes("UTF-8"));
      localServletOutputStream.close();
      this.user.asyncContext.complete();
      this.user.asyncContext = null;
    }
    
    public void onStartAsync(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onTimeout(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      this.user.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      ServletOutputStream localServletOutputStream = this.user.asyncContext.getResponse().getOutputStream();
      localServletOutputStream.write(new JSONObject().toString().getBytes("UTF-8"));
      localServletOutputStream.close();
      this.user.asyncContext.complete();
      this.user.asyncContext = null;
    }
  }
}