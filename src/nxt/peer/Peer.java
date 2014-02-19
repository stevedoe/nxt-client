package nxt.peer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import nxt.Account;
import nxt.Account.Event;
import nxt.Blockchain.BlockOutOfOrderException;
import nxt.Nxt;
import nxt.NxtException;
import nxt.ThreadPools;
import nxt.Transaction.NotYetEnabledException;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

public final class Peer
  implements Comparable<Peer>
{
  public static enum State
  {
    NON_CONNECTED,  CONNECTED,  DISCONNECTED;
    
    private State() {}
  }
  
  public static enum Event
  {
    BLACKLIST,  UNBLACKLIST,  DEACTIVATE,  REMOVE,  DOWNLOADED_VOLUME,  UPLOADED_VOLUME,  WEIGHT,  ADDED_ACTIVE_PEER,  CHANGED_ACTIVE_PEER;
    
    private Event() {}
  }
  
  private static final Listeners<Peer, Event> listeners = new Listeners();
  private static final ConcurrentMap<String, Peer> peers = new ConcurrentHashMap();
  private static final Collection<Peer> allPeers = Collections.unmodifiableCollection(peers.values());
  
  static
  {
    Account.addListener(new Listener()
    {
      public void notify(Account paramAnonymousAccount)
      {
        for (Peer localPeer : Peer.peers.values()) {
          if ((paramAnonymousAccount.getId().equals(localPeer.accountId)) && (localPeer.adjustedWeight > 0L)) {
            Peer.listeners.notify(localPeer, Peer.Event.WEIGHT);
          }
        }
      }
    }, Account.Event.BALANCE);
  }
  
  public static final Runnable peerConnectingThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          if (Peer.access$400() < Nxt.maxNumberOfConnectedPublicPeers)
          {
            Peer localPeer = Peer.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0 ? Peer.State.NON_CONNECTED : Peer.State.DISCONNECTED, false);
            if (localPeer != null) {
              localPeer.connect();
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
  public static final Runnable peerUnBlacklistingThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          l = System.currentTimeMillis();
          for (Peer localPeer : Peer.peers.values()) {
            if ((localPeer.blacklistingTime > 0L) && (localPeer.blacklistingTime + Nxt.blacklistingPeriod <= l)) {
              localPeer.removeBlacklistedStatus();
            }
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
  public static final Runnable getMorePeersThread = new Runnable()
  {
    private final JSONStreamAware getPeersRequest;
    
    public void run()
    {
      try
      {
        try
        {
          Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
          if (localPeer == null) {
            return;
          }
          JSONObject localJSONObject = localPeer.send(this.getPeersRequest);
          if (localJSONObject == null) {
            return;
          }
          JSONArray localJSONArray = (JSONArray)localJSONObject.get("peers");
          for (Object localObject : localJSONArray) {
            Peer.addPeer((String)localObject);
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
  private final String peerAddress;
  private String announcedAddress;
  private int port;
  private boolean shareAddress;
  private String hallmark;
  private String platform;
  private String application;
  private String version;
  private int weight;
  private int date;
  private Long accountId;
  private long adjustedWeight;
  private volatile long blacklistingTime;
  private volatile State state;
  private volatile long downloadedVolume;
  private volatile long uploadedVolume;
  
  public static boolean addListener(Listener<Peer> paramListener, Event paramEvent)
  {
    return listeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean removeListener(Listener<Peer> paramListener, Event paramEvent)
  {
    return listeners.removeListener(paramListener, paramEvent);
  }
  
  public static Collection<Peer> getAllPeers()
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
  
  public static Peer addPeer(String paramString1, String paramString2)
  {
    Object localObject = parseHostAndPort(paramString1);
    if (localObject == null) {
      return null;
    }
    String str = parseHostAndPort(paramString2);
    if ((Nxt.myAddress != null) && (Nxt.myAddress.length() > 0) && (Nxt.myAddress.equalsIgnoreCase(str))) {
      return null;
    }
    if (str != null) {
      localObject = str;
    }
    Peer localPeer = (Peer)peers.get(localObject);
    if (localPeer == null)
    {
      localPeer = new Peer((String)localObject, str);
      peers.put(localObject, localPeer);
    }
    return localPeer;
  }
  
  public static void sendToSomePeers(JSONObject paramJSONObject)
  {
    JSONStreamAware localJSONStreamAware = JSON.prepareRequest(paramJSONObject);
    
    int i = 0;
    ArrayList localArrayList = new ArrayList();
    for (Peer localPeer : peers.values()) {
      if ((!Nxt.enableHallmarkProtection) || (localPeer.getWeight() >= Nxt.pushThreshold))
      {
        Object localObject;
        if ((!localPeer.isBlacklisted()) && (localPeer.state == State.CONNECTED) && (localPeer.announcedAddress != null))
        {
          localObject = ThreadPools.sendInParallel(localPeer, localJSONStreamAware);
          localArrayList.add(localObject);
        }
        if (localArrayList.size() >= Nxt.sendToPeersLimit - i)
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
        if (i >= Nxt.sendToPeersLimit) {
          return;
        }
      }
    }
  }
  
  public static Peer getAnyPeer(State paramState, boolean paramBoolean)
  {
    ArrayList localArrayList = new ArrayList();
    for (Peer localPeer1 : peers.values()) {
      if ((!localPeer1.isBlacklisted()) && (localPeer1.state == paramState) && (localPeer1.announcedAddress != null) && ((!paramBoolean) || (!Nxt.enableHallmarkProtection) || (localPeer1.getWeight() >= Nxt.pullThreshold))) {
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
  
  private static String parseHostAndPort(String paramString)
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
      if ((localPeer.state == State.CONNECTED) && (localPeer.announcedAddress != null)) {
        i++;
      }
    }
    return i;
  }
  
  private Peer(String paramString1, String paramString2)
  {
    this.peerAddress = paramString1;
    this.announcedAddress = paramString2;
    try
    {
      this.port = new URL("http://" + paramString2).getPort();
    }
    catch (MalformedURLException localMalformedURLException) {}
    this.state = State.NON_CONNECTED;
  }
  
  public String getPeerAddress()
  {
    return this.peerAddress;
  }
  
  public State getState()
  {
    return this.state;
  }
  
  public long getDownloadedVolume()
  {
    return this.downloadedVolume;
  }
  
  public long getUploadedVolume()
  {
    return this.uploadedVolume;
  }
  
  public String getVersion()
  {
    return this.version;
  }
  
  void setVersion(String paramString)
  {
    this.version = paramString;
  }
  
  public String getApplication()
  {
    return this.application;
  }
  
  void setApplication(String paramString)
  {
    this.application = paramString;
  }
  
  public String getPlatform()
  {
    return this.platform;
  }
  
  void setPlatform(String paramString)
  {
    this.platform = paramString;
  }
  
  public String getHallmark()
  {
    return this.hallmark;
  }
  
  public boolean shareAddress()
  {
    return this.shareAddress;
  }
  
  void setShareAddress(boolean paramBoolean)
  {
    this.shareAddress = paramBoolean;
  }
  
  public String getAnnouncedAddress()
  {
    return this.announcedAddress;
  }
  
  void setAnnouncedAddress(String paramString)
  {
    String str = parseHostAndPort(paramString);
    if (str != null)
    {
      this.announcedAddress = str;
      try
      {
        this.port = new URL("http://" + str).getPort();
      }
      catch (MalformedURLException localMalformedURLException) {}
    }
  }
  
  public boolean isWellKnown()
  {
    return (this.announcedAddress != null) && (Nxt.wellKnownPeers.contains(this.announcedAddress));
  }
  
  public boolean isBlacklisted()
  {
    return this.blacklistingTime > 0L;
  }
  
  public int compareTo(Peer paramPeer)
  {
    if (this.weight > paramPeer.weight) {
      return -1;
    }
    if (this.weight < paramPeer.weight) {
      return 1;
    }
    return 0;
  }
  
  public void blacklist(NxtException paramNxtException)
  {
    if (((paramNxtException instanceof Transaction.NotYetEnabledException)) || ((paramNxtException instanceof Blockchain.BlockOutOfOrderException))) {
      return;
    }
    if (!isBlacklisted()) {
      Logger.logDebugMessage("Blacklisting " + this.peerAddress + " because of: " + paramNxtException.getMessage());
    }
    blacklist();
  }
  
  public void blacklist()
  {
    this.blacklistingTime = System.currentTimeMillis();
    deactivate();
    listeners.notify(this, Event.BLACKLIST);
  }
  
  public void deactivate()
  {
    if (this.state == State.CONNECTED) {
      setState(State.DISCONNECTED);
    }
    setState(State.NON_CONNECTED);
    listeners.notify(this, Event.DEACTIVATE);
  }
  
  public int getWeight()
  {
    if (this.accountId == null) {
      return 0;
    }
    Account localAccount = Account.getAccount(this.accountId);
    if (localAccount == null) {
      return 0;
    }
    return (int)(this.adjustedWeight * (localAccount.getBalance() / 100L) / 1000000000L);
  }
  
  public String getSoftware()
  {
    StringBuilder localStringBuilder = new StringBuilder();
    localStringBuilder.append(Convert.truncate(this.application, "?", 10, false));
    localStringBuilder.append(" (");
    localStringBuilder.append(Convert.truncate(this.version, "?", 10, false));
    localStringBuilder.append(")").append(" @ ");
    localStringBuilder.append(Convert.truncate(this.platform, "?", 10, false));
    return localStringBuilder.toString();
  }
  
  public void removeBlacklistedStatus()
  {
    setState(State.NON_CONNECTED);
    this.blacklistingTime = 0L;
    listeners.notify(this, Event.UNBLACKLIST);
  }
  
  public void removePeer()
  {
    peers.values().remove(this);
    listeners.notify(this, Event.REMOVE);
  }
  
  public JSONObject send(JSONStreamAware paramJSONStreamAware)
  {
    String str = null;
    int i = 0;
    HttpURLConnection localHttpURLConnection = null;
    JSONObject localJSONObject;
    try
    {
      if (Nxt.communicationLoggingMask != 0)
      {
        localObject1 = new StringWriter();
        paramJSONStreamAware.writeJSONString((Writer)localObject1);
        str = "\"" + this.announcedAddress + "\": " + ((StringWriter)localObject1).toString();
      }
      Object localObject1 = new URL("http://" + this.announcedAddress + (this.port <= 0 ? ":7874" : "") + "/nxt");
      
      localHttpURLConnection = (HttpURLConnection)((URL)localObject1).openConnection();
      localHttpURLConnection.setRequestMethod("POST");
      localHttpURLConnection.setDoOutput(true);
      localHttpURLConnection.setConnectTimeout(Nxt.connectTimeout);
      localHttpURLConnection.setReadTimeout(Nxt.readTimeout);
      
      CountingOutputStream localCountingOutputStream = new CountingOutputStream(localHttpURLConnection.getOutputStream());
      Object localObject2 = new BufferedWriter(new OutputStreamWriter(localCountingOutputStream, "UTF-8"));Object localObject3 = null;
      try
      {
        paramJSONStreamAware.writeJSONString((Writer)localObject2);
      }
      catch (Throwable localThrowable2)
      {
        localObject3 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localObject2 != null) {
          if (localObject3 != null) {
            try
            {
              ((Writer)localObject2).close();
            }
            catch (Throwable localThrowable5)
            {
              ((Throwable)localObject3).addSuppressed(localThrowable5);
            }
          } else {
            ((Writer)localObject2).close();
          }
        }
      }
      updateUploadedVolume(localCountingOutputStream.getCount());
      if (localHttpURLConnection.getResponseCode() == 200)
      {
        if ((Nxt.communicationLoggingMask & 0x4) != 0)
        {
          localObject2 = new ByteArrayOutputStream();
          localObject3 = new byte[65536];
          
          Object localObject6 = localHttpURLConnection.getInputStream();Object localObject7 = null;
          try
          {
            int j;
            while ((j = ((InputStream)localObject6).read((byte[])localObject3)) > 0) {
              ((ByteArrayOutputStream)localObject2).write((byte[])localObject3, 0, j);
            }
          }
          catch (Throwable localThrowable7)
          {
            localObject7 = localThrowable7;throw localThrowable7;
          }
          finally
          {
            if (localObject6 != null) {
              if (localObject7 != null) {
                try
                {
                  ((InputStream)localObject6).close();
                }
                catch (Throwable localThrowable8)
                {
                  localObject7.addSuppressed(localThrowable8);
                }
              } else {
                ((InputStream)localObject6).close();
              }
            }
          }
          localObject6 = ((ByteArrayOutputStream)localObject2).toString("UTF-8");
          str = str + " >>> " + (String)localObject6;
          i = 1;
          updateDownloadedVolume(((String)localObject6).getBytes("UTF-8").length);
          localJSONObject = (JSONObject)JSONValue.parse((String)localObject6);
        }
        else
        {
          localObject2 = new CountingInputStream(localHttpURLConnection.getInputStream());
          localObject3 = new BufferedReader(new InputStreamReader((InputStream)localObject2, "UTF-8"));Object localObject4 = null;
          try
          {
            localJSONObject = (JSONObject)JSONValue.parse((Reader)localObject3);
          }
          catch (Throwable localThrowable4)
          {
            localObject4 = localThrowable4;throw localThrowable4;
          }
          finally
          {
            if (localObject3 != null) {
              if (localObject4 != null) {
                try
                {
                  ((Reader)localObject3).close();
                }
                catch (Throwable localThrowable9)
                {
                  localObject4.addSuppressed(localThrowable9);
                }
              } else {
                ((Reader)localObject3).close();
              }
            }
          }
          updateDownloadedVolume(((CountingInputStream)localObject2).getCount());
        }
      }
      else
      {
        if ((Nxt.communicationLoggingMask & 0x2) != 0)
        {
          str = str + " >>> Peer responded with HTTP " + localHttpURLConnection.getResponseCode() + " code!";
          i = 1;
        }
        setState(State.DISCONNECTED);
        localJSONObject = null;
      }
    }
    catch (RuntimeException|IOException localRuntimeException)
    {
      if ((!(localRuntimeException instanceof UnknownHostException)) && (!(localRuntimeException instanceof SocketTimeoutException)) && (!(localRuntimeException instanceof SocketException))) {
        Logger.logDebugMessage("Error sending JSON request", localRuntimeException);
      }
      if ((Nxt.communicationLoggingMask & 0x1) != 0)
      {
        str = str + " >>> " + localRuntimeException.toString();
        i = 1;
      }
      if (this.state == State.NON_CONNECTED) {
        blacklist();
      } else {
        setState(State.DISCONNECTED);
      }
      localJSONObject = null;
    }
    if (i != 0) {
      Logger.logMessage(str + "\n");
    }
    if (localHttpURLConnection != null) {
      localHttpURLConnection.disconnect();
    }
    return localJSONObject;
  }
  
  void setState(State paramState)
  {
    State localState = this.state;
    this.state = paramState;
    if ((localState == State.NON_CONNECTED) && (paramState != State.NON_CONNECTED)) {
      listeners.notify(this, Event.ADDED_ACTIVE_PEER);
    } else if ((localState != State.NON_CONNECTED) && (paramState != State.NON_CONNECTED)) {
      listeners.notify(this, Event.CHANGED_ACTIVE_PEER);
    }
  }
  
  void updateDownloadedVolume(long paramLong)
  {
    this.downloadedVolume += paramLong;
    listeners.notify(this, Event.DOWNLOADED_VOLUME);
  }
  
  void updateUploadedVolume(long paramLong)
  {
    this.uploadedVolume += paramLong;
    listeners.notify(this, Event.UPLOADED_VOLUME);
  }
  
  private void connect()
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
    localJSONObject1.put("version", "0.7.6");
    localJSONObject1.put("platform", Nxt.myPlatform);
    localJSONObject1.put("scheme", Nxt.myScheme);
    localJSONObject1.put("port", Integer.valueOf(Nxt.myPort));
    localJSONObject1.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
    JSONObject localJSONObject2 = send(JSON.prepareRequest(localJSONObject1));
    if (localJSONObject2 != null)
    {
      this.application = ((String)localJSONObject2.get("application"));
      this.version = ((String)localJSONObject2.get("version"));
      this.platform = ((String)localJSONObject2.get("platform"));
      this.shareAddress = Boolean.TRUE.equals(localJSONObject2.get("shareAddress"));
      if (analyzeHallmark(this.announcedAddress, (String)localJSONObject2.get("hallmark"))) {
        setState(State.CONNECTED);
      } else {
        blacklist();
      }
    }
  }
  
  boolean analyzeHallmark(String paramString1, String paramString2)
  {
    if ((paramString2 == null) || (paramString2.equals(this.hallmark))) {
      return true;
    }
    try
    {
      Hallmark localHallmark = Hallmark.parseHallmark(paramString2);
      if ((!localHallmark.isValid()) || (!localHallmark.getHost().equals(paramString1))) {
        return false;
      }
      this.hallmark = paramString2;
      Long localLong = Account.getId(localHallmark.getPublicKey());
      ArrayList localArrayList = new ArrayList();
      int i = 0;
      this.accountId = localLong;
      this.weight = localHallmark.getWeight();
      this.date = localHallmark.getDate();
      for (Peer localPeer1 : peers.values()) {
        if (localLong.equals(localPeer1.accountId))
        {
          localArrayList.add(localPeer1);
          if (localPeer1.date > i) {
            i = localPeer1.date;
          }
        }
      }
      long l = 0L;
      for (Iterator localIterator2 = localArrayList.iterator(); localIterator2.hasNext();)
      {
        localPeer2 = (Peer)localIterator2.next();
        if (localPeer2.date == i) {
          l += localPeer2.weight;
        } else {
          localPeer2.weight = 0;
        }
      }
      Peer localPeer2;
      for (localIterator2 = localArrayList.iterator(); localIterator2.hasNext();)
      {
        localPeer2 = (Peer)localIterator2.next();
        localPeer2.adjustedWeight = (1000000000L * localPeer2.weight / l);
        listeners.notify(localPeer2, Event.WEIGHT);
      }
      return true;
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logDebugMessage("Failed to analyze hallmark for peer " + this.announcedAddress + ", " + localRuntimeException.toString());
    }
    return false;
  }
}