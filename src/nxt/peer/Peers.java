package nxt.peer;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import nxt.Account;
import nxt.Account.Event;
import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.DoSFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class Peers
{
  static final int LOGGING_MASK_EXCEPTIONS = 1;
  static final int LOGGING_MASK_NON200_RESPONSES = 2;
  static final int LOGGING_MASK_200_RESPONSES = 4;
  static final int communicationLoggingMask;
  static final Set<String> wellKnownPeers;
  static final int connectTimeout;
  static final int readTimeout;
  static final int blacklistingPeriod;
  private static final int DEFAULT_PEER_PORT = 7874;
  private static final String myPlatform;
  private static final String myAddress;
  private static final int myPeerServerPort;
  private static final String myHallmark;
  private static final boolean shareMyAddress;
  private static final int maxNumberOfConnectedPublicPeers;
  private static final boolean enableHallmarkProtection;
  private static final int pushThreshold;
  private static final int pullThreshold;
  private static final int sendToPeersLimit;
  static final JSONStreamAware myPeerInfoRequest;
  static final JSONStreamAware myPeerInfoResponse;
  
  public static enum Event
  {
    BLACKLIST,  UNBLACKLIST,  DEACTIVATE,  REMOVE,  DOWNLOADED_VOLUME,  UPLOADED_VOLUME,  WEIGHT,  ADDED_ACTIVE_PEER,  CHANGED_ACTIVE_PEER,  NEW_PEER;
    
    private Event() {}
  }
  
  private static final Listeners<Peer, Event> listeners = new Listeners();
  private static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap();
  static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());
  private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);
  private static final Runnable peerUnBlacklistingThread;
  private static final Runnable peerConnectingThread;
  private static final Runnable getMorePeersThread;
  
  private static class Init
  {
    private static void init() {}
    
    static
    {
      if (Peers.shareMyAddress)
      {
        Server localServer = new Server();
        ServerConnector localServerConnector = new ServerConnector(localServer);
        localServerConnector.setPort(Peers.myPeerServerPort);
        final String str = Nxt.getStringProperty("nxt.peerServerHost");
        localServerConnector.setHost(str);
        localServerConnector.setIdleTimeout(Nxt.getIntProperty("nxt.peerServerIdleTimeout"));
        localServer.addConnector(localServerConnector);
        
        ServletHandler localServletHandler = new ServletHandler();
        localServletHandler.addServletWithMapping(PeerServlet.class, "/*");
        FilterHolder localFilterHolder = localServletHandler.addFilterWithMapping(DoSFilter.class, "/*", 0);
        localFilterHolder.setInitParameter("maxRequestsPerSec", Nxt.getStringProperty("nxt.peerServerDoSFilter.maxRequestsPerSec"));
        localFilterHolder.setInitParameter("delayMs", Nxt.getStringProperty("nxt.peerServerDoSFilter.delayMs"));
        localFilterHolder.setInitParameter("trackSessions", "false");
        localFilterHolder.setAsyncSupported(true);
        
        localServer.setHandler(localServletHandler);
        localServer.setStopAtShutdown(true);
        ThreadPool.runBeforeStart(new Runnable()
        {
          public void run()
          {
            try
            {
              this.val$peerServer.start();
              Logger.logMessage("Started peer networking server at " + str + ":" + Peers.myPeerServerPort);
            }
            catch (Exception localException)
            {
              Logger.logDebugMessage("Failed to start peer networking server", localException);
              throw new RuntimeException(localException.toString(), localException);
            }
          }
        });
      }
      else
      {
        Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
      }
    }
  }
  
  static
  {
    myPlatform = Nxt.getStringProperty("nxt.myPlatform");
    myAddress = Nxt.getStringProperty("nxt.myAddress");
    myPeerServerPort = Nxt.getIntProperty("nxt.peerServerPort");
    shareMyAddress = Nxt.getBooleanProperty("nxt.shareMyAddress").booleanValue();
    myHallmark = Nxt.getStringProperty("nxt.myHallmark");
    if ((myHallmark != null) && (myHallmark.length() > 0)) {
      try
      {
        Hallmark localHallmark = Hallmark.parseHallmark(myHallmark);
        if ((!localHallmark.isValid()) || (myAddress == null)) {
          throw new RuntimeException();
        }
        localObject1 = new URI("http://" + myAddress.trim());
        localObject2 = ((URI)localObject1).getHost();
        if (!localHallmark.getHost().equals(localObject2)) {
          throw new RuntimeException();
        }
      }
      catch (RuntimeException|URISyntaxException localRuntimeException)
      {
        Logger.logMessage("Your hallmark is invalid: " + myHallmark + " for your address: " + myAddress);
        throw new RuntimeException(localRuntimeException.toString(), localRuntimeException);
      }
    }
    JSONObject localJSONObject = new JSONObject();
    if ((myAddress != null) && (myAddress.length() > 0)) {
      if (myAddress.indexOf(':') > 0) {
        localJSONObject.put("announcedAddress", myAddress);
      } else {
        localJSONObject.put("announcedAddress", myAddress + (myPeerServerPort != 7874 ? ":" + myPeerServerPort : ""));
      }
    }
    if ((myHallmark != null) && (myHallmark.length() > 0)) {
      localJSONObject.put("hallmark", myHallmark);
    }
    localJSONObject.put("application", "NRS");
    localJSONObject.put("version", "0.8.1e");
    localJSONObject.put("platform", myPlatform);
    localJSONObject.put("shareAddress", Boolean.valueOf(shareMyAddress));
    myPeerInfoResponse = JSON.prepare(localJSONObject);
    localJSONObject.put("requestType", "getInfo");
    myPeerInfoRequest = JSON.prepareRequest(localJSONObject);
    
    Object localObject1 = Nxt.getStringProperty("nxt.wellKnownPeers");
    Object localObject2 = new HashSet();
    Object localObject3;
    if ((localObject1 != null) && (((String)localObject1).length() > 0))
    {
      for (localObject3 : ((String)localObject1).split(";"))
      {
        localObject3 = ((String)localObject3).trim();
        if (((String)localObject3).length() > 0) {
          ((Set)localObject2).add(localObject3);
        }
      }
    }
    else
    {
      Logger.logMessage("No wellKnownPeers defined, using random nxtcrypto.org and nxtbase.com nodes");
      for (int i = 1; i <= 12; i++) {
        if (ThreadLocalRandom.current().nextInt(4) == 1) {
          ((Set)localObject2).add("vps" + i + ".nxtcrypto.org");
        }
      }
      for (i = 1; i <= 99; i++) {
        if (ThreadLocalRandom.current().nextInt(10) == 1) {
          ((Set)localObject2).add("node" + i + ".nxtbase.com");
        }
      }
    }
    wellKnownPeers = Collections.unmodifiableSet((Set)localObject2);
    
    maxNumberOfConnectedPublicPeers = Nxt.getIntProperty("nxt.maxNumberOfConnectedPublicPeers");
    connectTimeout = Nxt.getIntProperty("nxt.connectTimeout");
    readTimeout = Nxt.getIntProperty("nxt.readTimeout");
    enableHallmarkProtection = Nxt.getBooleanProperty("nxt.enableHallmarkProtection").booleanValue();
    pushThreshold = Nxt.getIntProperty("nxt.pushThreshold");
    pullThreshold = Nxt.getIntProperty("nxt.pullThreshold");
    
    blacklistingPeriod = Nxt.getIntProperty("nxt.blacklistingPeriod");
    communicationLoggingMask = Nxt.getIntProperty("nxt.communicationLoggingMask");
    sendToPeersLimit = Nxt.getIntProperty("nxt.sendToPeersLimit");
    
    StringBuilder localStringBuilder = new StringBuilder();
    for (String str : wellKnownPeers)
    {
      localObject3 = addPeer(str);
      if (localObject3 != null) {
        localStringBuilder.append(((Peer)localObject3).getPeerAddress()).append("; ");
      }
    }
    Logger.logDebugMessage("Well known peers: " + localStringBuilder.toString());
    















































    peerUnBlacklistingThread = new Runnable()
    {
      public void run()
      {
        try
        {
          try
          {
            l = System.currentTimeMillis();
            for (PeerImpl localPeerImpl : Peers.peers.values()) {
              localPeerImpl.updateBlacklistedStatus(l);
            }
          }
          catch (Exception localException)
          {
            long l;
            Logger.logDebugMessage("Error un-blacklisting peer", localException);
          }
        }
        catch (Throwable localThrowable)
        {
          Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
          localThrowable.printStackTrace();
          System.exit(1);
        }
      }
    };
    peerConnectingThread = new Runnable()
    {
      public void run()
      {
        try
        {
          try
          {
            if (Peers.access$300() < Peers.maxNumberOfConnectedPublicPeers)
            {
              PeerImpl localPeerImpl = (PeerImpl)Peers.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
              if (localPeerImpl != null) {
                localPeerImpl.connect();
              }
            }
          }
          catch (Exception localException)
          {
            Logger.logDebugMessage("Error connecting to peer", localException);
          }
        }
        catch (Throwable localThrowable)
        {
          Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
          localThrowable.printStackTrace();
          System.exit(1);
        }
      }
    };
    getMorePeersThread = new Runnable()
    {
      private final JSONStreamAware getPeersRequest;
      
      public void run()
      {
        try
        {
          try
          {
            Peer localPeer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
            if (localPeer == null) {
              return;
            }
            JSONObject localJSONObject = localPeer.send(this.getPeersRequest);
            if (localJSONObject == null) {
              return;
            }
            JSONArray localJSONArray = (JSONArray)localJSONObject.get("peers");
            for (Object localObject : localJSONArray) {
              Peers.addPeer((String)localObject);
            }
          }
          catch (Exception localException)
          {
            Logger.logDebugMessage("Error requesting peers from a peer", localException);
          }
        }
        catch (Throwable localThrowable)
        {
          Logger.logMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + localThrowable.toString());
          localThrowable.printStackTrace();
          System.exit(1);
        }
      }
    };
    Account.addListener(new Listener()
    {
      public void notify(Account paramAnonymousAccount)
      {
        for (PeerImpl localPeerImpl : Peers.peers.values()) {
          if ((localPeerImpl.getHallmark() != null) && (localPeerImpl.getHallmark().getAccountId().equals(paramAnonymousAccount.getId()))) {
            Peers.listeners.notify(localPeerImpl, Peers.Event.WEIGHT);
          }
        }
      }
    }, Account.Event.BALANCE);
    






    ThreadPool.scheduleThread(peerConnectingThread, 5);
    ThreadPool.scheduleThread(peerUnBlacklistingThread, 1);
    ThreadPool.scheduleThread(getMorePeersThread, 5);
  }
  
  public static void shutdown()
  {
    ThreadPool.shutdownExecutor(sendToPeersService);
  }
  
  public static boolean addListener(Listener<Peer> paramListener, Event paramEvent)
  {
    return listeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean removeListener(Listener<Peer> paramListener, Event paramEvent)
  {
    return listeners.removeListener(paramListener, paramEvent);
  }
  
  static void notifyListeners(Peer paramPeer, Event paramEvent)
  {
    listeners.notify(paramPeer, paramEvent);
  }
  
  public static Collection<? extends Peer> getAllPeers()
  {
    return allPeers;
  }
  
  public static Peer getPeer(String paramString)
  {
    return (Peer)peers.get(paramString);
  }
  
  public static Peer addPeer(String paramString)
  {
    return addPeer(paramString, paramString);
  }
  
  static PeerImpl addPeer(String paramString1, String paramString2)
  {
    Object localObject = normalizeHostAndPort(paramString1);
    if (localObject == null) {
      return null;
    }
    String str = normalizeHostAndPort(paramString2);
    if ((myAddress != null) && (myAddress.length() > 0) && (myAddress.equalsIgnoreCase(str))) {
      return null;
    }
    if (str != null) {
      localObject = str;
    }
    PeerImpl localPeerImpl = (PeerImpl)peers.get(localObject);
    if (localPeerImpl == null)
    {
      localPeerImpl = new PeerImpl((String)localObject, str);
      peers.put(localObject, localPeerImpl);
      listeners.notify(localPeerImpl, Event.NEW_PEER);
    }
    return localPeerImpl;
  }
  
  static PeerImpl removePeer(PeerImpl paramPeerImpl)
  {
    return (PeerImpl)peers.remove(paramPeerImpl.getPeerAddress());
  }
  
  public static void sendToSomePeers(JSONObject paramJSONObject)
  {
    final JSONStreamAware localJSONStreamAware = JSON.prepareRequest(paramJSONObject);
    
    int i = 0;
    ArrayList localArrayList = new ArrayList();
    for (Peer localPeer : peers.values()) {
      if ((!enableHallmarkProtection) || (localPeer.getWeight() >= pushThreshold))
      {
        Object localObject;
        if ((!localPeer.isBlacklisted()) && (localPeer.getState() == Peer.State.CONNECTED) && (localPeer.getAnnouncedAddress() != null))
        {
          localObject = sendToPeersService.submit(new Callable()
          {
            public JSONObject call()
            {
              return this.val$peer.send(localJSONStreamAware);
            }
          });
          localArrayList.add(localObject);
        }
        if (localArrayList.size() >= sendToPeersLimit - i)
        {
          for (localObject = localArrayList.iterator(); ((Iterator)localObject).hasNext();)
          {
            Future localFuture = (Future)((Iterator)localObject).next();
            try
            {
              JSONObject localJSONObject = (JSONObject)localFuture.get();
              if ((localJSONObject != null) && (localJSONObject.get("error") == null)) {
                i++;
              }
            }
            catch (InterruptedException localInterruptedException)
            {
              Thread.currentThread().interrupt();
            }
            catch (ExecutionException localExecutionException)
            {
              Logger.logDebugMessage("Error in sendToSomePeers", localExecutionException);
            }
          }
          localArrayList.clear();
        }
        if (i >= sendToPeersLimit) {
          return;
        }
      }
    }
  }
  
  public static Peer getAnyPeer(Peer.State paramState, boolean paramBoolean)
  {
    ArrayList localArrayList = new ArrayList();
    for (Peer localPeer1 : peers.values()) {
      if ((!localPeer1.isBlacklisted()) && (localPeer1.getState() == paramState) && ((!paramBoolean) || (!enableHallmarkProtection) || (localPeer1.getWeight() >= pullThreshold))) {
        localArrayList.add(localPeer1);
      }
    }
    long l2;
    if (localArrayList.size() > 0)
    {
      long l1 = 0L;
      for (Peer localPeer2 : localArrayList)
      {
        long l3 = localPeer2.getWeight();
        if (l3 == 0L) {
          l3 = 1L;
        }
        l1 += l3;
      }
      l2 = ThreadLocalRandom.current().nextLong(l1);
      for (Peer localPeer3 : localArrayList)
      {
        long l4 = localPeer3.getWeight();
        if (l4 == 0L) {
          l4 = 1L;
        }
        if (l2 -= l4 < 0L) {
          return localPeer3;
        }
      }
    }
    return null;
  }
  
  static String normalizeHostAndPort(String paramString)
  {
    try
    {
      if (paramString == null) {
        return null;
      }
      URI localURI = new URI("http://" + paramString.trim());
      String str = localURI.getHost();
      if ((str == null) || (str.equals("")) || (str.equals("localhost")) || (str.equals("127.0.0.1")) || (str.equals("0:0:0:0:0:0:0:1"))) {
        return null;
      }
      InetAddress localInetAddress = InetAddress.getByName(str);
      if ((localInetAddress.isAnyLocalAddress()) || (localInetAddress.isLoopbackAddress()) || (localInetAddress.isLinkLocalAddress())) {
        return null;
      }
      int i = localURI.getPort();
      return str + ':' + i;
    }
    catch (URISyntaxException|UnknownHostException localURISyntaxException) {}
    return null;
  }
  
  private static int getNumberOfConnectedPublicPeers()
  {
    int i = 0;
    for (Peer localPeer : peers.values()) {
      if ((localPeer.getState() == Peer.State.CONNECTED) && (localPeer.getAnnouncedAddress() != null)) {
        i++;
      }
    }
    return i;
  }
  
  public static void init() {}
}