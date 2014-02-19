package nxt.user;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.Account;
import nxt.Account.Event;
import nxt.Block;
import nxt.Blockchain;
import nxt.Blockchain.Event;
import nxt.Generator;
import nxt.Generator.Event;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.peer.Peer.Event;
import nxt.peer.Peer.State;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class User
{
  private static final ConcurrentMap<String, User> users = new ConcurrentHashMap();
  private static final Collection<User> allUsers = Collections.unmodifiableCollection(users.values());
  private static final AtomicInteger peerCounter = new AtomicInteger();
  private static final ConcurrentMap<String, Integer> peerIndexMap = new ConcurrentHashMap();
  private static final AtomicInteger blockCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> blockIndexMap = new ConcurrentHashMap();
  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Integer> transactionIndexMap = new ConcurrentHashMap();
  private volatile String secretPhrase;
  private volatile byte[] publicKey;
  private volatile boolean isInactive;
  
  static
  {
    Account.addListener(new Listener()
    {
      public void notify(Account paramAnonymousAccount)
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("response", "setBalance");
        localJSONObject.put("balance", Long.valueOf(paramAnonymousAccount.getUnconfirmedBalance()));
        byte[] arrayOfByte = paramAnonymousAccount.getPublicKey();
        for (User localUser : User.users.values()) {
          if ((localUser.getSecretPhrase() != null) && (Arrays.equals(localUser.getPublicKey(), arrayOfByte))) {
            localUser.send(localJSONObject);
          }
        }
      }
    }, Account.Event.UNCONFIRMED_BALANCE);
    






    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedBlacklistedPeers", localJSONArray2);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.BLACKLIST);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedActivePeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedKnownPeers", localJSONArray2);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.DEACTIVATE);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedBlacklistedPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject3.put("address", paramAnonymousPeer.getPeerAddress());
        localJSONObject3.put("announcedAddress", Convert.truncate(paramAnonymousPeer.getAnnouncedAddress(), "-", 25, true));
        if (paramAnonymousPeer.isWellKnown()) {
          localJSONObject3.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONObject3.put("software", paramAnonymousPeer.getSoftware());
        localJSONArray2.add(localJSONObject3);
        localJSONObject1.put("addedKnownPeers", localJSONArray2);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.UNBLACKLIST);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.REMOVE);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("downloaded", Long.valueOf(paramAnonymousPeer.getDownloadedVolume()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.DOWNLOADED_VOLUME);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("uploaded", Long.valueOf(paramAnonymousPeer.getUploadedVolume()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.UPLOADED_VOLUME);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject2.put("weight", Integer.valueOf(paramAnonymousPeer.getWeight()));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.WEIGHT);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray1 = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONArray1.add(localJSONObject2);
        localJSONObject1.put("removedKnownPeers", localJSONArray1);
        JSONArray localJSONArray2 = new JSONArray();
        JSONObject localJSONObject3 = new JSONObject();
        localJSONObject3.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.ADDED_ACTIVE_PEER);
    


    Peer.addListener(new Listener()
    {
      public void notify(Peer paramAnonymousPeer)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousPeer)));
        localJSONObject2.put(paramAnonymousPeer.getState() == Peer.State.CONNECTED ? "connected" : "disconnected", Boolean.valueOf(true));
        localJSONArray.add(localJSONObject2);
        localJSONObject1.put("changedActivePeers", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Peer.Event.CHANGED_ACTIVE_PEER);
    






    Blockchain.addTransactionListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(User.getIndex(localTransaction)));
          localJSONArray.add(localJSONObject2);
        }
        localJSONObject1.put("removedUnconfirmedTransactions", localJSONArray);
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    


    Blockchain.addTransactionListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(User.getIndex(localTransaction)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    


    Blockchain.addTransactionListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(User.getIndex(localTransaction)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.ADDED_CONFIRMED_TRANSACTIONS);
    


    Blockchain.addTransactionListener(new Listener()
    {
      public void notify(List<Transaction> paramAnonymousList)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        for (Transaction localTransaction : paramAnonymousList)
        {
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(User.getIndex(localTransaction)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
    


    Blockchain.addBlockListener(new Listener()
    {
      public void notify(Block paramAnonymousBlock)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousBlock)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.BLOCK_POPPED);
    


    Blockchain.addBlockListener(new Listener()
    {
      public void notify(Block paramAnonymousBlock)
      {
        JSONObject localJSONObject1 = new JSONObject();
        JSONArray localJSONArray = new JSONArray();
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("index", Integer.valueOf(User.getIndex(paramAnonymousBlock)));
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
        User.sendNewDataToAll(localJSONObject1);
      }
    }, Blockchain.Event.BLOCK_PUSHED);
    





    Generator.addListener(new Listener()
    {
      public void notify(Generator paramAnonymousGenerator)
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("response", "setBlockGenerationDeadline");
        localJSONObject.put("deadline", Long.valueOf(paramAnonymousGenerator.getDeadline()));
        for (User localUser : User.allUsers) {
          if (Arrays.equals(paramAnonymousGenerator.getPublicKey(), localUser.getPublicKey())) {
            localUser.send(localJSONObject);
          }
        }
      }
    }, Generator.Event.GENERATION_DEADLINE);
  }
  
  public static Collection<User> getAllUsers()
  {
    return allUsers;
  }
  
  public static User getUser(String paramString)
  {
    Object localObject = (User)users.get(paramString);
    if (localObject == null)
    {
      localObject = new User();
      User localUser = (User)users.putIfAbsent(paramString, localObject);
      if (localUser != null)
      {
        localObject = localUser;
        ((User)localObject).isInactive = false;
      }
    }
    else
    {
      ((User)localObject).isInactive = false;
    }
    return localObject;
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
  
  private final ConcurrentLinkedQueue<JSONStreamAware> pendingResponses = new ConcurrentLinkedQueue();
  private AsyncContext asyncContext;
  
  public byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  String getSecretPhrase()
  {
    return this.secretPhrase;
  }
  
  boolean isInactive()
  {
    return this.isInactive;
  }
  
  void enqueue(JSONStreamAware paramJSONStreamAware)
  {
    this.pendingResponses.offer(paramJSONStreamAware);
  }
  
  void lockAccount()
  {
    Generator.stopForging(this.secretPhrase);
    this.secretPhrase = null;
  }
  
  Long unlockAccount(String paramString)
  {
    this.publicKey = Crypto.getPublicKey(paramString);
    this.secretPhrase = paramString;
    Generator.startForging(paramString, this.publicKey);
    return Account.getId(this.publicKey);
  }
  
  public synchronized void processPendingResponses(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    JSONArray localJSONArray = new JSONArray();
    JSONStreamAware localJSONStreamAware;
    while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
      localJSONArray.add(localJSONStreamAware);
    }
    Object localObject1;
    Object localObject2;
    if (localJSONArray.size() > 0)
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("responses", localJSONArray);
      Object localObject3;
      if (this.asyncContext != null)
      {
        this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        localObject2 = this.asyncContext.getResponse().getWriter();localObject3 = null;
        try
        {
          ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
        }
        catch (Throwable localThrowable4)
        {
          localObject3 = localThrowable4;throw localThrowable4;
        }
        finally
        {
          if (localObject2 != null) {
            if (localObject3 != null) {
              try
              {
                ((Writer)localObject2).close();
              }
              catch (Throwable localThrowable7)
              {
                localObject3.addSuppressed(localThrowable7);
              }
            } else {
              ((Writer)localObject2).close();
            }
          }
        }
        this.asyncContext.complete();
        this.asyncContext = paramHttpServletRequest.startAsync();
        this.asyncContext.addListener(new UserAsyncListener(null));
        this.asyncContext.setTimeout(5000L);
      }
      else
      {
        paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
        localObject2 = paramHttpServletResponse.getWriter();localObject3 = null;
        try
        {
          ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
        }
        catch (Throwable localThrowable6)
        {
          localObject3 = localThrowable6;throw localThrowable6;
        }
        finally
        {
          if (localObject2 != null) {
            if (localObject3 != null) {
              try
              {
                ((Writer)localObject2).close();
              }
              catch (Throwable localThrowable8)
              {
                localObject3.addSuppressed(localThrowable8);
              }
            } else {
              ((Writer)localObject2).close();
            }
          }
        }
      }
    }
    else
    {
      if (this.asyncContext != null)
      {
        this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        localObject1 = this.asyncContext.getResponse().getWriter();localObject2 = null;
        try
        {
          JSON.emptyJSON.writeJSONString((Writer)localObject1);
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
                ((Writer)localObject1).close();
              }
              catch (Throwable localThrowable9)
              {
                ((Throwable)localObject2).addSuppressed(localThrowable9);
              }
            } else {
              ((Writer)localObject1).close();
            }
          }
        }
        this.asyncContext.complete();
      }
      this.asyncContext = paramHttpServletRequest.startAsync();
      this.asyncContext.addListener(new UserAsyncListener(null));
      this.asyncContext.setTimeout(5000L);
    }
  }
  
  private synchronized void send(JSONStreamAware paramJSONStreamAware)
  {
    if (this.asyncContext == null)
    {
      if (this.isInactive) {
        return;
      }
      if (this.pendingResponses.size() > 1000)
      {
        this.pendingResponses.clear();
        
        this.isInactive = true;
        if (this.secretPhrase == null) {
          users.values().remove(this);
        }
        return;
      }
      this.pendingResponses.offer(paramJSONStreamAware);
    }
    else
    {
      JSONArray localJSONArray = new JSONArray();
      JSONStreamAware localJSONStreamAware;
      while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
        localJSONArray.add(localJSONStreamAware);
      }
      localJSONArray.add(paramJSONStreamAware);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("responses", localJSONArray);
      
      this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      try
      {
        PrintWriter localPrintWriter = this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          localJSONObject.writeJSONString(localPrintWriter);
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
      }
      catch (IOException localIOException)
      {
        Logger.logMessage("Error sending response to user", localIOException);
      }
      this.asyncContext.complete();
      this.asyncContext = null;
    }
  }
  
  private final class UserAsyncListener
    implements AsyncListener
  {
    private UserAsyncListener() {}
    
    public void onComplete(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onError(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      synchronized (User.this)
      {
        User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        
        PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          JSON.emptyJSON.writeJSONString(localPrintWriter);
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
        User.this.asyncContext.complete();
        User.this.asyncContext = null;
      }
    }
    
    public void onStartAsync(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onTimeout(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      synchronized (User.this)
      {
        User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        
        PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          JSON.emptyJSON.writeJSONString(localPrintWriter);
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
        User.this.asyncContext.complete();
        User.this.asyncContext = null;
      }
    }
  }
}