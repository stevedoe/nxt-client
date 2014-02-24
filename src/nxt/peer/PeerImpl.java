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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import nxt.Account;
import nxt.BlockchainProcessor.BlockOutOfOrderException;
import nxt.NxtException;
import nxt.TransactionType.NotYetEnabledException;
import nxt.util.Convert;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

final class PeerImpl
  implements Peer
{
  private final String peerAddress;
  private volatile String announcedAddress;
  private volatile int port;
  private volatile boolean shareAddress;
  private volatile Hallmark hallmark;
  private volatile String platform;
  private volatile String application;
  private volatile String version;
  private volatile long adjustedWeight;
  private volatile long blacklistingTime;
  private volatile Peer.State state;
  private volatile long downloadedVolume;
  private volatile long uploadedVolume;
  
  PeerImpl(String paramString1, String paramString2)
  {
    this.peerAddress = paramString1;
    this.announcedAddress = paramString2;
    try
    {
      this.port = new URL("http://" + paramString2).getPort();
    }
    catch (MalformedURLException localMalformedURLException) {}
    this.state = Peer.State.NON_CONNECTED;
  }
  
  public String getPeerAddress()
  {
    return this.peerAddress;
  }
  
  public Peer.State getState()
  {
    return this.state;
  }
  
  void setState(Peer.State paramState)
  {
    if (this.state == paramState) {
      return;
    }
    if (this.state == Peer.State.NON_CONNECTED)
    {
      this.state = paramState;
      Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER);
    }
    else if (paramState != Peer.State.NON_CONNECTED)
    {
      this.state = paramState;
      Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER);
    }
  }
  
  public long getDownloadedVolume()
  {
    return this.downloadedVolume;
  }
  
  void updateDownloadedVolume(long paramLong)
  {
    synchronized (this)
    {
      this.downloadedVolume += paramLong;
    }
    Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME);
  }
  
  public long getUploadedVolume()
  {
    return this.uploadedVolume;
  }
  
  void updateUploadedVolume(long paramLong)
  {
    synchronized (this)
    {
      this.uploadedVolume += paramLong;
    }
    Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME);
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
    String str = Peers.normalizeHostAndPort(paramString);
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
    return (this.announcedAddress != null) && (Peers.wellKnownPeers.contains(this.announcedAddress));
  }
  
  public Hallmark getHallmark()
  {
    return this.hallmark;
  }
  
  public int getWeight()
  {
    if (this.hallmark == null) {
      return 0;
    }
    Account localAccount = Account.getAccount(this.hallmark.getAccountId());
    if (localAccount == null) {
      return 0;
    }
    return (int)(this.adjustedWeight * (localAccount.getBalance() / 100L) / 1000000000L);
  }
  
  public boolean isBlacklisted()
  {
    return this.blacklistingTime > 0L;
  }
  
  public void blacklist(NxtException paramNxtException)
  {
    if (((paramNxtException instanceof TransactionType.NotYetEnabledException)) || ((paramNxtException instanceof BlockchainProcessor.BlockOutOfOrderException))) {
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
    setState(Peer.State.NON_CONNECTED);
    
    Peers.notifyListeners(this, Peers.Event.BLACKLIST);
  }
  
  public void unBlacklist()
  {
    setState(Peer.State.NON_CONNECTED);
    this.blacklistingTime = 0L;
    Peers.notifyListeners(this, Peers.Event.UNBLACKLIST);
  }
  
  void updateBlacklistedStatus(long paramLong)
  {
    if ((this.blacklistingTime > 0L) && (this.blacklistingTime + Peers.blacklistingPeriod <= paramLong)) {
      unBlacklist();
    }
  }
  
  public void deactivate()
  {
    setState(Peer.State.NON_CONNECTED);
    Peers.notifyListeners(this, Peers.Event.DEACTIVATE);
  }
  
  public void remove()
  {
    Peers.removePeer(this);
    Peers.notifyListeners(this, Peers.Event.REMOVE);
  }
  
  public JSONObject send(JSONStreamAware paramJSONStreamAware)
  {
    String str1 = null;
    int i = 0;
    HttpURLConnection localHttpURLConnection = null;
    JSONObject localJSONObject;
    try
    {
      String str2 = this.announcedAddress != null ? this.announcedAddress : this.peerAddress;
      if (Peers.communicationLoggingMask != 0)
      {
        localObject1 = new StringWriter();
        paramJSONStreamAware.writeJSONString((Writer)localObject1);
        str1 = "\"" + str2 + "\": " + ((StringWriter)localObject1).toString();
      }
      Object localObject1 = new URL("http://" + str2 + (this.port <= 0 ? ":7874" : "") + "/nxt");
      
      localHttpURLConnection = (HttpURLConnection)((URL)localObject1).openConnection();
      localHttpURLConnection.setRequestMethod("POST");
      localHttpURLConnection.setDoOutput(true);
      localHttpURLConnection.setConnectTimeout(Peers.connectTimeout);
      localHttpURLConnection.setReadTimeout(Peers.readTimeout);
      
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
        if ((Peers.communicationLoggingMask & 0x4) != 0)
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
          str1 = str1 + " >>> " + (String)localObject6;
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
        if ((Peers.communicationLoggingMask & 0x2) != 0)
        {
          str1 = str1 + " >>> Peer responded with HTTP " + localHttpURLConnection.getResponseCode() + " code!";
          i = 1;
        }
        if (this.state == Peer.State.CONNECTED) {
          setState(Peer.State.DISCONNECTED);
        } else {
          setState(Peer.State.NON_CONNECTED);
        }
        localJSONObject = null;
      }
    }
    catch (RuntimeException|IOException localRuntimeException)
    {
      if ((!(localRuntimeException instanceof UnknownHostException)) && (!(localRuntimeException instanceof SocketTimeoutException)) && (!(localRuntimeException instanceof SocketException))) {
        Logger.logDebugMessage("Error sending JSON request", localRuntimeException);
      }
      if ((Peers.communicationLoggingMask & 0x1) != 0)
      {
        str1 = str1 + " >>> " + localRuntimeException.toString();
        i = 1;
      }
      if (this.state == Peer.State.NON_CONNECTED) {
        blacklist();
      } else if (this.state == Peer.State.CONNECTED) {
        setState(Peer.State.DISCONNECTED);
      }
      localJSONObject = null;
    }
    if (i != 0) {
      Logger.logMessage(str1 + "\n");
    }
    if (localHttpURLConnection != null) {
      localHttpURLConnection.disconnect();
    }
    return localJSONObject;
  }
  
  public int compareTo(Peer paramPeer)
  {
    if (getWeight() > paramPeer.getWeight()) {
      return -1;
    }
    if (getWeight() < paramPeer.getWeight()) {
      return 1;
    }
    return 0;
  }
  
  void connect()
  {
    JSONObject localJSONObject = send(Peers.myPeerInfoRequest);
    if (localJSONObject != null)
    {
      this.application = ((String)localJSONObject.get("application"));
      this.version = ((String)localJSONObject.get("version"));
      this.platform = ((String)localJSONObject.get("platform"));
      this.shareAddress = Boolean.TRUE.equals(localJSONObject.get("shareAddress"));
      if (this.announcedAddress == null)
      {
        setAnnouncedAddress(this.peerAddress);
        Logger.logDebugMessage("Connected to peer without announced address, setting to " + this.peerAddress);
      }
      if (analyzeHallmark(this.announcedAddress, (String)localJSONObject.get("hallmark"))) {
        setState(Peer.State.CONNECTED);
      } else {
        blacklist();
      }
    }
    else
    {
      setState(Peer.State.NON_CONNECTED);
    }
  }
  
  boolean analyzeHallmark(String paramString1, String paramString2)
  {
    if ((paramString2 == null) && (this.hallmark == null)) {
      return true;
    }
    if ((this.hallmark != null) && (this.hallmark.getHallmarkString().equals(paramString2))) {
      return true;
    }
    if (paramString2 == null)
    {
      this.hallmark = null;
      return true;
    }
    try
    {
      URI localURI = new URI("http://" + paramString1.trim());
      String str = localURI.getHost();
      
      Hallmark localHallmark = Hallmark.parseHallmark(paramString2);
      if ((!localHallmark.isValid()) || ((!localHallmark.getHost().equals(str)) && (!InetAddress.getByName(str).equals(InetAddress.getByName(localHallmark.getHost()))))) {
        return false;
      }
      this.hallmark = localHallmark;
      Long localLong = Account.getId(localHallmark.getPublicKey());
      ArrayList localArrayList = new ArrayList();
      int i = 0;
      long l = 0L;
      for (Iterator localIterator = Peers.allPeers.iterator(); localIterator.hasNext();)
      {
        localPeerImpl = (PeerImpl)localIterator.next();
        if (localPeerImpl.hallmark != null) {
          if (localLong.equals(localPeerImpl.hallmark.getAccountId()))
          {
            localArrayList.add(localPeerImpl);
            if (localPeerImpl.hallmark.getDate() > i)
            {
              i = localPeerImpl.hallmark.getDate();
              l = localPeerImpl.getHallmarkWeight(i);
            }
            else
            {
              l += localPeerImpl.getHallmarkWeight(i);
            }
          }
        }
      }
      PeerImpl localPeerImpl;
      for (localIterator = localArrayList.iterator(); localIterator.hasNext();)
      {
        localPeerImpl = (PeerImpl)localIterator.next();
        localPeerImpl.adjustedWeight = (1000000000L * localPeerImpl.getHallmarkWeight(i) / l);
        Peers.notifyListeners(localPeerImpl, Peers.Event.WEIGHT);
      }
      return true;
    }
    catch (URISyntaxException|UnknownHostException|RuntimeException localURISyntaxException)
    {
      Logger.logDebugMessage("Failed to analyze hallmark for peer " + paramString1 + ", " + localURISyntaxException.toString());
    }
    return false;
  }
  
  private int getHallmarkWeight(int paramInt)
  {
    if ((this.hallmark == null) || (!this.hallmark.isValid()) || (this.hallmark.getDate() != paramInt)) {
      return 0;
    }
    return this.hallmark.getWeight();
  }
}