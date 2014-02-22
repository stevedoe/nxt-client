package nxt.user;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import nxt.Account;
import nxt.Account.Event;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.BlockchainProcessor.Event;
import nxt.Generator;
import nxt.Generator.Event;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.TransactionProcessor.Event;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.peer.Peers;
import nxt.peer.Peers.Event;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Logger;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class Users
{
  private static final ConcurrentMap<String, User> users = new ConcurrentHashMap();
  private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());
  private static final AtomicInteger peerCounter = new AtomicInteger();
  private static final ConcurrentMap<String, Integer> peerIndexMap = new ConcurrentHashMap();
  private static final AtomicInteger blockCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> blockIndexMap = new ConcurrentHashMap();
  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> transactionIndexMap = new ConcurrentHashMap();
  static final Set<String> allowedUserHosts;
  
  static
  {
    String str1 = Nxt.getStringProperty("nxt.allowedUserHosts");
    Object localObject1;
    if (!str1.equals("*"))
    {
      HashSet localHashSet = new HashSet();
      for (localObject1 : str1.split(";"))
      {
        localObject1 = ((String)localObject1).trim();
        if (((String)localObject1).length() > 0) {
          localHashSet.add(localObject1);
        }
      }
      allowedUserHosts = Collections.unmodifiableSet(localHashSet);
    }
    else
    {
      allowedUserHosts = null;
    }
    boolean bool1 = Nxt.getBooleanProperty("nxt.enableUIServer").booleanValue();
    if (bool1) {
      try
      {
        int i = Nxt.getIntProperty("nxt.uiServerPort");
        String str2 = Nxt.getStringProperty("nxt.uiServerHost");
        Server localServer = new Server();
        

        boolean bool2 = Nxt.getBooleanProperty("nxt.uiSSL").booleanValue();
        if (bool2)
        {
          Logger.logMessage("Using SSL (https) for the user interface server");
          localObject2 = new HttpConfiguration();
          ((HttpConfiguration)localObject2).setSecureScheme("https");
          ((HttpConfiguration)localObject2).setSecurePort(i);
          ((HttpConfiguration)localObject2).addCustomizer(new SecureRequestCustomizer());
          localObject3 = new SslContextFactory();
          ((SslContextFactory)localObject3).setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
          ((SslContextFactory)localObject3).setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
          ((SslContextFactory)localObject3).setExcludeCipherSuites(new String[] { "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" });
          

          localObject1 = new ServerConnector(localServer, new ConnectionFactory[] { new SslConnectionFactory((SslContextFactory)localObject3, "http/1.1"), new HttpConnectionFactory((HttpConfiguration)localObject2) });
        }
        else
        {
          localObject1 = new ServerConnector(localServer);
        }
        ((ServerConnector)localObject1).setPort(i);
        ((ServerConnector)localObject1).setHost(str2);
        ((ServerConnector)localObject1).setIdleTimeout(Nxt.getIntProperty("nxt.uiServerIdleTimeout"));
        localServer.addConnector((Connector)localObject1);
        

        Object localObject2 = new HandlerList();
        
        Object localObject3 = new ResourceHandler();
        ((ResourceHandler)localObject3).setDirectoriesListed(false);
        ((ResourceHandler)localObject3).setWelcomeFiles(new String[] { "index.html" });
        ((ResourceHandler)localObject3).setResourceBase(Nxt.getStringProperty("nxt.uiResourceBase"));
        
        ((HandlerList)localObject2).addHandler((Handler)localObject3);
        
        String str3 = Nxt.getStringProperty("nxt.javadocResourceBase");
        if (str3 != null)
        {
          localObject4 = new ContextHandler("/doc");
          localObject5 = new ResourceHandler();
          ((ResourceHandler)localObject5).setDirectoriesListed(false);
          ((ResourceHandler)localObject5).setWelcomeFiles(new String[] { "index.html" });
          ((ResourceHandler)localObject5).setResourceBase(str3);
          ((ContextHandler)localObject4).setHandler((Handler)localObject5);
          ((HandlerList)localObject2).addHandler((Handler)localObject4);
        }
        Object localObject4 = new ServletHandler();
        Object localObject5 = ((ServletHandler)localObject4).addServletWithMapping(UserServlet.class, "/nxt");
        ((ServletHolder)localObject5).setAsyncSupported(true);
        if (Nxt.getBooleanProperty("nxt.uiServerCORS").booleanValue())
        {
          FilterHolder localFilterHolder = ((ServletHandler)localObject4).addFilterWithMapping(CrossOriginFilter.class, "/*", 0);
          localFilterHolder.setInitParameter("allowedHeaders", "*");
          localFilterHolder.setAsyncSupported(true);
        }
        ((HandlerList)localObject2).addHandler((Handler)localObject4);
        
        ((HandlerList)localObject2).addHandler(new DefaultHandler());
        
        localServer.setHandler((Handler)localObject2);
        localServer.setStopAtShutdown(true);
        localServer.start();
        Logger.logMessage("Started user interface server at " + str2 + ":" + i);
      }
      catch (Exception localException)
      {
        Logger.logDebugMessage("Failed to start user interface server", localException);
        throw new RuntimeException(localException.toString(), localException);
      }
    } else {
      Logger.logMessage("User interface server not enabled");
    }
    Account.addListener(new Listener()
    {
      public void notify(Account paramAnonymousAccount)
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("response", "setBalance");
        localJSONObject.put("balance", Long.valueOf(paramAnonymousAccount.getUnconfirmedBalance()));
        byte[] arrayOfByte = paramAnonymousAccount.getPublicKey();
        for (User localUser : Users.users.values()) {
          if ((localUser.getSecretPhrase() != null) && (Arrays.equals(localUser.getPublicKey(), arrayOfByte))) {
            localUser.send(localJSONObject);
          }
        }
      }
    }, Account.Event.UNCONFIRMED_BALANCE);
    






    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedBlacklistedPeers", localJSONArray2);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.BLACKLIST);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedActivePeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedKnownPeers", localJSONArray2);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.DEACTIVATE);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedBlacklistedPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedKnownPeers", localJSONArray2);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.UNBLACKLIST);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.REMOVE);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("downloaded", Long.valueOf(paramAnonymousPeer.getDownloadedVolume()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.DOWNLOADED_VOLUME);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("uploaded", Long.valueOf(paramAnonymousPeer.getUploadedVolume()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.UPLOADED_VOLUME);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("weight", Integer.valueOf(paramAnonymousPeer.getWeight()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.WEIGHT);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        if (paramAnonymousPeer.getState() == Peer.State.DISCONNECTED) {
          localJSONObject3.put("disconnected", Boolean.valueOf(true));
        }
        localJSONObject3.put("address", Convert.truncate(paramAnonymousPeer.getPeerAddress(), "-", 25, true));
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("weight", Integer.valueOf(paramAnonymousPeer.getWeight()));
        localJSONObject3.put("downloaded", Long.valueOf(paramAnonymousPeer.getDownloadedVolume()));
        localJSONObject3.put("uploaded", Long.valueOf(paramAnonymousPeer.getUploadedVolume()));
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedActivePeers", localJSONArray2);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.ADDED_ACTIVE_PEER);
    


    Peers.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousPeer)));
        localJSONObject2.put(paramAnonymousPeer.getState() == Peer.State.CONNECTED ? "connected" : "disconnected", Boolean.valueOf(true));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, Peers.Event.CHANGED_ACTIVE_PEER);
    






    Nxt.getTransactionProcessor().addListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(Users.getIndex(localTransaction)));
          localJSONArray.add(localJSONObject2);
        }
        localJSONObject1.put("removedUnconfirmedTransactions", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    


    Nxt.getTransactionProcessor().addListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(Users.getIndex(localTransaction)));
          localJSONObject2.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
          localJSONObject2.put("deadline", Short.valueOf(localTransaction.getDeadline()));
          localJSONObject2.put("recipient", Convert.toUnsignedLong(localTransaction.getRecipientId()));
          localJSONObject2.put("amount", Integer.valueOf(localTransaction.getAmount()));
          localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
          localJSONObject2.put("sender", Convert.toUnsignedLong(localTransaction.getSenderId()));
          localJSONObject2.put("id", localTransaction.getStringId());
          localJSONArray.add(localJSONObject2);
        }
        localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    


    Nxt.getTransactionProcessor().addListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(Users.getIndex(localTransaction)));
          localJSONObject2.put("blockTimestamp", Integer.valueOf(localTransaction.getBlock().getTimestamp()));
          localJSONObject2.put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
          localJSONObject2.put("sender", Convert.toUnsignedLong(localTransaction.getSenderId()));
          localJSONObject2.put("recipient", Convert.toUnsignedLong(localTransaction.getRecipientId()));
          localJSONObject2.put("amount", Integer.valueOf(localTransaction.getAmount()));
          localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
          localJSONObject2.put("id", localTransaction.getStringId());
          localJSONArray.add(localJSONObject2);
        }
        localJSONObject1.put("addedConfirmedTransactions", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    


    Nxt.getTransactionProcessor().addListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(Users.getIndex(localTransaction)));
          localJSONObject2.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
          localJSONObject2.put("deadline", Short.valueOf(localTransaction.getDeadline()));
          localJSONObject2.put("recipient", Convert.toUnsignedLong(localTransaction.getRecipientId()));
          localJSONObject2.put("amount", Integer.valueOf(localTransaction.getAmount()));
          localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
          localJSONObject2.put("sender", Convert.toUnsignedLong(localTransaction.getSenderId()));
          localJSONObject2.put("id", localTransaction.getStringId());
          localJSONArray.add(localJSONObject2);
        }
        localJSONObject1.put("addedDoubleSpendingTransactions", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, TransactionProcessor.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
    


    Nxt.getBlockchainProcessor().addListener(new Listener()
    {
      public void notify(Block paramAnonymousBlock)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousBlock)));
        localJSONObject2.put("timestamp", Integer.valueOf(paramAnonymousBlock.getTimestamp()));
        localJSONObject2.put("numberOfTransactions", Integer.valueOf(paramAnonymousBlock.getTransactionIds().size()));
        localJSONObject2.put("totalAmount", Integer.valueOf(paramAnonymousBlock.getTotalAmount()));
        localJSONObject2.put("totalFee", Integer.valueOf(paramAnonymousBlock.getTotalFee()));
        localJSONObject2.put("payloadLength", Integer.valueOf(paramAnonymousBlock.getPayloadLength()));
        localJSONObject2.put("generator", Convert.toUnsignedLong(paramAnonymousBlock.getGeneratorId()));
        localJSONObject2.put("height", Integer.valueOf(paramAnonymousBlock.getHeight()));
        localJSONObject2.put("version", Integer.valueOf(paramAnonymousBlock.getVersion()));
        localJSONObject2.put("block", paramAnonymousBlock.getStringId());
        localJSONObject2.put("baseTarget", BigInteger.valueOf(paramAnonymousBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("addedOrphanedBlocks", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, BlockchainProcessor.Event.BLOCK_POPPED);
    


    Nxt.getBlockchainProcessor().addListener(new Listener()
    {
      public void notify(Block paramAnonymousBlock)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(Users.getIndex(paramAnonymousBlock)));
        localJSONObject2.put("timestamp", Integer.valueOf(paramAnonymousBlock.getTimestamp()));
        localJSONObject2.put("numberOfTransactions", Integer.valueOf(paramAnonymousBlock.getTransactionIds().size()));
        localJSONObject2.put("totalAmount", Integer.valueOf(paramAnonymousBlock.getTotalAmount()));
        localJSONObject2.put("totalFee", Integer.valueOf(paramAnonymousBlock.getTotalFee()));
        localJSONObject2.put("payloadLength", Integer.valueOf(paramAnonymousBlock.getPayloadLength()));
        localJSONObject2.put("generator", Convert.toUnsignedLong(paramAnonymousBlock.getGeneratorId()));
        localJSONObject2.put("height", Integer.valueOf(paramAnonymousBlock.getHeight()));
        localJSONObject2.put("version", Integer.valueOf(paramAnonymousBlock.getVersion()));
        localJSONObject2.put("block", paramAnonymousBlock.getStringId());
        localJSONObject2.put("baseTarget", BigInteger.valueOf(paramAnonymousBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("addedRecentBlocks", localJSONArray);
        Users.sendNewDataToAll(localJSONObject1);
      }
    }, BlockchainProcessor.Event.BLOCK_PUSHED);
    





    Generator.addListener(new Listener()
    {
      public void notify(Generator paramAnonymousGenerator)
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("response", "setBlockGenerationDeadline");
        localJSONObject.put("deadline", Long.valueOf(paramAnonymousGenerator.getDeadline()));
        for (User localUser : Users.users.values()) {
          if (Arrays.equals(paramAnonymousGenerator.getPublicKey(), localUser.getPublicKey())) {
            localUser.send(localJSONObject);
          }
        }
      }
    }, Generator.Event.GENERATION_DEADLINE);
  }
  
  static Collection<User> getAllUsers()
  {
    return allUsers;
  }
  
  static User getUser(String paramString)
  {
    Object localObject = (User)users.get(paramString);
    if (localObject == null)
    {
      localObject = new User(paramString);
      User localUser = (User)users.putIfAbsent(paramString, localObject);
      if (localUser != null)
      {
        localObject = localUser;
        ((User)localObject).setInactive(false);
      }
    }
    else
    {
      ((User)localObject).setInactive(false);
    }
    return localObject;
  }
  
  static User remove(User paramUser)
  {
    return (User)users.remove(paramUser.getUserId());
  }
  
  private static void sendNewDataToAll(JSONObject paramJSONObject)
  {
    paramJSONObject.put("response", "processNewData");
    sendToAll(paramJSONObject);
  }
  
  private static void sendToAll(JSONStreamAware paramJSONStreamAware)
  {
    for (User localUser : users.values()) {
      localUser.send(paramJSONStreamAware);
    }
  }
  
  static int getIndex(Peer paramPeer)
  {
    Integer localInteger = (Integer)peerIndexMap.get(paramPeer.getPeerAddress());
    if (localInteger == null)
    {
      localInteger = Integer.valueOf(peerCounter.incrementAndGet());
      peerIndexMap.put(paramPeer.getPeerAddress(), localInteger);
    }
    return localInteger.intValue();
  }
  
  static int getIndex(Block paramBlock)
  {
    Integer localInteger = (Integer)blockIndexMap.get(paramBlock.getId());
    if (localInteger == null)
    {
      localInteger = Integer.valueOf(blockCounter.incrementAndGet());
      blockIndexMap.put(paramBlock.getId(), localInteger);
    }
    return localInteger.intValue();
  }
  
  static int getIndex(Transaction paramTransaction)
  {
    Integer localInteger = (Integer)transactionIndexMap.get(paramTransaction.getId());
    if (localInteger == null)
    {
      localInteger = Integer.valueOf(transactionCounter.incrementAndGet());
      transactionIndexMap.put(paramTransaction.getId(), localInteger);
    }
    return localInteger.intValue();
  }
  
  public static void init() {}
}