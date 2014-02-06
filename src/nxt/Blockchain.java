package nxt;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.user.User;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class Blockchain
{
  private static final byte[] CHECKSUM_TRANSPARENT_FORGING = { 27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112 };
  private static volatile Peer lastBlockchainFeeder;
  private static final AtomicInteger blockCounter = new AtomicInteger();
  private static final AtomicReference<Block> lastBlock = new AtomicReference();
  private static final ConcurrentMap<Long, Block> blocks = new ConcurrentHashMap();
  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap();
  private static final Collection<Block> allBlocks = Collections.unmodifiableCollection(blocks.values());
  private static final Collection<Transaction> allTransactions = Collections.unmodifiableCollection(transactions.values());
  private static final Collection<Transaction> allUnconfirmedTransactions = Collections.unmodifiableCollection(unconfirmedTransactions.values());
  static final ConcurrentMap<String, Transaction> transactionHashes = new ConcurrentHashMap();
  static final Runnable processTransactionsThread = new Runnable()
  {
    private final JSONStreamAware getUnconfirmedTransactionsRequest;
    
    public void run()
    {
      try
      {
        try
        {
          Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
          if (localPeer != null)
          {
            JSONObject localJSONObject = localPeer.send(this.getUnconfirmedTransactionsRequest);
            if (localJSONObject != null) {
              try
              {
                Blockchain.processUnconfirmedTransactions(localJSONObject);
              }
              catch (NxtException.ValidationException localValidationException)
              {
                localPeer.blacklist(localValidationException);
              }
            }
          }
        }
        catch (Exception localException)
        {
          Logger.logDebugMessage("Error processing unconfirmed transactions from peer", localException);
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
  static final Runnable removeUnconfirmedTransactionsThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          int i = Convert.getEpochTime();
          JSONArray localJSONArray = new JSONArray();
          
          Iterator localIterator = Blockchain.unconfirmedTransactions.values().iterator();
          Object localObject;
          while (localIterator.hasNext())
          {
            localObject = (Transaction)localIterator.next();
            if (((Transaction)localObject).getExpiration() < i)
            {
              localIterator.remove();
              
              Account localAccount = Account.getAccount(((Transaction)localObject).getSenderAccountId());
              localAccount.addToUnconfirmedBalance((((Transaction)localObject).getAmount() + ((Transaction)localObject).getFee()) * 100L);
              
              JSONObject localJSONObject = new JSONObject();
              localJSONObject.put("index", Integer.valueOf(((Transaction)localObject).getIndex()));
              localJSONArray.add(localJSONObject);
            }
          }
          if (localJSONArray.size() > 0)
          {
            localObject = new JSONObject();
            ((JSONObject)localObject).put("response", "processNewData");
            
            ((JSONObject)localObject).put("removedUnconfirmedTransactions", localJSONArray);
            

            User.sendToAll((JSONStreamAware)localObject);
          }
        }
        catch (Exception localException)
        {
          Logger.logDebugMessage("Error removing unconfirmed transactions", localException);
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
  static final Runnable getMoreBlocksThread = new Runnable()
  {
    private final JSONStreamAware getCumulativeDifficultyRequest;
    private final JSONStreamAware getMilestoneBlockIdsRequest;
    
    public void run()
    {
      try
      {
        try
        {
          Peer localPeer = Peer.getAnyPeer(Peer.State.CONNECTED, true);
          if (localPeer != null)
          {
            Blockchain.access$202(localPeer);
            
            JSONObject localJSONObject1 = localPeer.send(this.getCumulativeDifficultyRequest);
            if (localJSONObject1 != null)
            {
              BigInteger localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
              String str = (String)localJSONObject1.get("cumulativeDifficulty");
              if (str == null) {
                return;
              }
              BigInteger localBigInteger2 = new BigInteger(str);
              if (localBigInteger2.compareTo(localBigInteger1) > 0)
              {
                localJSONObject1 = localPeer.send(this.getMilestoneBlockIdsRequest);
                if (localJSONObject1 != null)
                {
                  Object localObject1 = Genesis.GENESIS_BLOCK_ID;
                  
                  JSONArray localJSONArray1 = (JSONArray)localJSONObject1.get("milestoneBlockIds");
                  if (localJSONArray1 == null) {
                    return;
                  }
                  for (Object localObject3 : localJSONArray1)
                  {
                    localObject4 = Convert.parseUnsignedLong((String)localObject3);
                    localObject5 = (Block)Blockchain.blocks.get(localObject4);
                    if (localObject5 != null)
                    {
                      localObject1 = localObject4;
                      
                      break;
                    }
                  }
                  Object localObject4;
                  Object localObject5;
                  int j;
                  int i;
                  Object localObject6;
                  do
                  {
                    localObject4 = new JSONObject();
                    ((JSONObject)localObject4).put("requestType", "getNextBlockIds");
                    ((JSONObject)localObject4).put("blockId", Convert.convert((Long)localObject1));
                    localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject4));
                    if (localJSONObject1 == null) {
                      return;
                    }
                    localObject5 = (JSONArray)localJSONObject1.get("nextBlockIds");
                    if ((localObject5 == null) || ((j = ((JSONArray)localObject5).size()) == 0)) {
                      return;
                    }
                    for (i = 0; i < j; i++)
                    {
                      localObject6 = Convert.parseUnsignedLong((String)((JSONArray)localObject5).get(i));
                      if (Blockchain.blocks.get(localObject6) == null) {
                        break;
                      }
                      localObject1 = localObject6;
                    }
                  } while (i == j);
                  if (((Block)Blockchain.lastBlock.get()).getHeight() - ((Block)Blockchain.blocks.get(localObject1)).getHeight() < 720)
                  {
                    Object localObject2 = localObject1;
                    LinkedList localLinkedList = new LinkedList();
                    localObject4 = new HashMap();
                    Object localObject7;
                    for (;;)
                    {
                      localObject5 = new JSONObject();
                      ((JSONObject)localObject5).put("requestType", "getNextBlocks");
                      ((JSONObject)localObject5).put("blockId", Convert.convert((Long)localObject2));
                      localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject5));
                      if (localJSONObject1 == null) {
                        break;
                      }
                      localObject6 = (JSONArray)localJSONObject1.get("nextBlocks");
                      if ((localObject6 == null) || (((JSONArray)localObject6).size() == 0)) {
                        break;
                      }
                      synchronized (Blockchain.class)
                      {
                        for (localObject7 = ((JSONArray)localObject6).iterator(); ((Iterator)localObject7).hasNext();)
                        {
                          Object localObject8 = ((Iterator)localObject7).next();
                          JSONObject localJSONObject2 = (JSONObject)localObject8;
                          Block localBlock;
                          try
                          {
                            localBlock = Block.getBlock(localJSONObject2);
                          }
                          catch (NxtException.ValidationException localValidationException1)
                          {
                            localPeer.blacklist(localValidationException1);
                            return;
                          }
                          localObject2 = localBlock.getId();
                          JSONArray localJSONArray2;
                          if (((Block)Blockchain.lastBlock.get()).getId().equals(localBlock.getPreviousBlockId()))
                          {
                            localJSONArray2 = (JSONArray)localJSONObject2.get("transactions");
                            try
                            {
                              Transaction[] arrayOfTransaction = new Transaction[localJSONArray2.size()];
                              for (int n = 0; n < arrayOfTransaction.length; n++) {
                                arrayOfTransaction[n] = Transaction.getTransaction((JSONObject)localJSONArray2.get(n));
                              }
                              if (!Blockchain.pushBlock(localBlock, arrayOfTransaction, false))
                              {
                                Logger.logDebugMessage("Failed to accept block received from " + localPeer.getPeerAddress() + ", blacklisting");
                                localPeer.blacklist();
                                return;
                              }
                            }
                            catch (NxtException.ValidationException localValidationException2)
                            {
                              localPeer.blacklist(localValidationException2);
                              return;
                            }
                          }
                          else if ((Blockchain.blocks.get(localBlock.getId()) == null) && (localBlock.transactionIds.length <= 255))
                          {
                            localLinkedList.add(localBlock);
                            
                            localJSONArray2 = (JSONArray)localJSONObject2.get("transactions");
                            try
                            {
                              for (int m = 0; m < localBlock.transactionIds.length; m++)
                              {
                                Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray2.get(m));
                                localBlock.transactionIds[m] = localTransaction.getId();
                                localBlock.blockTransactions[m] = localTransaction;
                                ((HashMap)localObject4).put(localBlock.transactionIds[m], localTransaction);
                              }
                            }
                            catch (NxtException.ValidationException localValidationException3)
                            {
                              localPeer.blacklist(localValidationException3);
                              return;
                            }
                          }
                        }
                      }
                    }
                    if ((!localLinkedList.isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - ((Block)Blockchain.blocks.get(localObject1)).getHeight() < 720)) {
                      synchronized (Blockchain.class)
                      {
                        Blockchain.saveBlocks("blocks.nxt.bak");
                        Blockchain.saveTransactions("transactions.nxt.bak");
                        
                        localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
                        for (;;)
                        {
                          int k;
                          try
                          {
                            while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$800())) {}
                            if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
                              for (??? = localLinkedList.iterator(); ((Iterator)???).hasNext();)
                              {
                                localObject7 = (Block)((Iterator)???).next();
                                if ((((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) && 
                                  (!Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions, false))) {
                                  break;
                                }
                              }
                            }
                            k = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty().compareTo(localBigInteger1) < 0 ? 1 : 0;
                            if (k != 0)
                            {
                              Logger.logDebugMessage("Rescan caused by peer " + localPeer.getPeerAddress() + ", blacklisting");
                              localPeer.blacklist();
                            }
                          }
                          catch (Transaction.UndoNotSupportedException localUndoNotSupportedException)
                          {
                            Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
                            Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
                            k = 1;
                          }
                        }
                        if (k != 0)
                        {
                          Blockchain.loadBlocks("blocks.nxt.bak");
                          Blockchain.loadTransactions("transactions.nxt.bak");
                          Account.clear();
                          Alias.clear();
                          Asset.clear();
                          Order.clear();
                          Blockchain.unconfirmedTransactions.clear();
                          Blockchain.doubleSpendingTransactions.clear();
                          Blockchain.nonBroadcastedTransactions.clear();
                          Blockchain.transactionHashes.clear();
                          Logger.logMessage("Re-scanning blockchain...");
                          Blockchain.access$1300();
                          Logger.logMessage("...Done");
                        }
                      }
                    }
                    synchronized (Blockchain.class)
                    {
                      Blockchain.saveBlocks("blocks.nxt");
                      Blockchain.saveTransactions("transactions.nxt");
                    }
                  }
                }
              }
            }
          }
        }
        catch (Exception localException)
        {
          Logger.logDebugMessage("Error in milestone blocks processing thread", localException);
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
  static final Runnable generateBlockThread = new Runnable()
  {
    private final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
    private final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
    
    public void run()
    {
      try
      {
        try
        {
          HashMap localHashMap = new HashMap();
          for (localIterator = User.getAllUsers().iterator(); localIterator.hasNext();)
          {
            localObject1 = (User)localIterator.next();
            if (((User)localObject1).getSecretPhrase() != null)
            {
              localAccount = Account.getAccount(((User)localObject1).getPublicKey());
              if ((localAccount != null) && (localAccount.getEffectiveBalance() > 0)) {
                localHashMap.put(localAccount, localObject1);
              }
            }
          }
          for (localIterator = localHashMap.entrySet().iterator(); localIterator.hasNext();)
          {
            localObject1 = (Map.Entry)localIterator.next();
            
            localAccount = (Account)((Map.Entry)localObject1).getKey();
            User localUser = (User)((Map.Entry)localObject1).getValue();
            Block localBlock = (Block)Blockchain.lastBlock.get();
            if (this.lastBlocks.get(localAccount) != localBlock)
            {
              long l = localAccount.getEffectiveBalance();
              if (l > 0L)
              {
                MessageDigest localMessageDigest = Crypto.sha256();
                byte[] arrayOfByte;
                if (localBlock.getHeight() < 30000)
                {
                  localObject2 = Crypto.sign(localBlock.getGenerationSignature(), localUser.getSecretPhrase());
                  arrayOfByte = localMessageDigest.digest((byte[])localObject2);
                }
                else
                {
                  localMessageDigest.update(localBlock.getGenerationSignature());
                  arrayOfByte = localMessageDigest.digest(localUser.getPublicKey());
                }
                Object localObject2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
                
                this.lastBlocks.put(localAccount, localBlock);
                this.hits.put(localAccount, localObject2);
                
                JSONObject localJSONObject = new JSONObject();
                localJSONObject.put("response", "setBlockGenerationDeadline");
                localJSONObject.put("deadline", Long.valueOf(((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - localBlock.getTimestamp())));
                
                localUser.send(localJSONObject);
              }
            }
            else
            {
              int i = Convert.getEpochTime() - localBlock.getTimestamp();
              if (i > 0)
              {
                BigInteger localBigInteger = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
                if (((BigInteger)this.hits.get(localAccount)).compareTo(localBigInteger) < 0) {
                  Blockchain.generateBlock(localUser.getSecretPhrase());
                }
              }
            }
          }
        }
        catch (Exception localException)
        {
          Iterator localIterator;
          Object localObject1;
          Account localAccount;
          Logger.logDebugMessage("Error in block generation thread", localException);
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
  static final Runnable rebroadcastTransactionsThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          JSONArray localJSONArray = new JSONArray();
          for (Object localObject = Blockchain.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
          {
            Transaction localTransaction = (Transaction)((Iterator)localObject).next();
            if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (Blockchain.transactions.get(localTransaction.getId()) == null)) {
              localJSONArray.add(localTransaction.getJSONObject());
            } else {
              Blockchain.nonBroadcastedTransactions.remove(localTransaction.getId());
            }
          }
          if (localJSONArray.size() > 0)
          {
            localObject = new JSONObject();
            ((JSONObject)localObject).put("requestType", "processTransactions");
            ((JSONObject)localObject).put("transactions", localJSONArray);
            
            Peer.sendToSomePeers((JSONObject)localObject);
          }
        }
        catch (Exception localException)
        {
          Logger.logDebugMessage("Error in transaction re-broadcasting thread", localException);
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
  
  public static Collection<Block> getAllBlocks()
  {
    return allBlocks;
  }
  
  public static Collection<Transaction> getAllTransactions()
  {
    return allTransactions;
  }
  
  public static Collection<Transaction> getAllUnconfirmedTransactions()
  {
    return allUnconfirmedTransactions;
  }
  
  public static Block getLastBlock()
  {
    return (Block)lastBlock.get();
  }
  
  public static Block getBlock(Long paramLong)
  {
    return (Block)blocks.get(paramLong);
  }
  
  public static Transaction getTransaction(Long paramLong)
  {
    return (Transaction)transactions.get(paramLong);
  }
  
  public static Transaction getUnconfirmedTransaction(Long paramLong)
  {
    return (Transaction)unconfirmedTransactions.get(paramLong);
  }
  
  public static void broadcast(Transaction paramTransaction)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("requestType", "processTransactions");
    JSONArray localJSONArray = new JSONArray();
    localJSONArray.add(paramTransaction.getJSONObject());
    localJSONObject.put("transactions", localJSONArray);
    
    Peer.sendToSomePeers(localJSONObject);
    
    nonBroadcastedTransactions.put(paramTransaction.getId(), paramTransaction);
  }
  
  public static Peer getLastBlockchainFeeder()
  {
    return lastBlockchainFeeder;
  }
  
  public static void processTransactions(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
    processTransactions(localJSONArray, false);
  }
  
  public static boolean pushBlock(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    Block localBlock = Block.getBlock(paramJSONObject);
    if (!((Block)lastBlock.get()).getId().equals(localBlock.getPreviousBlockId())) {
      return false;
    }
    JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
    Transaction[] arrayOfTransaction = new Transaction[localJSONArray.size()];
    for (int i = 0; i < arrayOfTransaction.length; i++) {
      arrayOfTransaction[i] = Transaction.getTransaction((JSONObject)localJSONArray.get(i));
    }
    return pushBlock(localBlock, arrayOfTransaction, true);
  }
  
  static void addBlock(Block paramBlock)
  {
    if (paramBlock.getPreviousBlockId() == null)
    {
      blocks.put(paramBlock.getId(), paramBlock);
      lastBlock.set(paramBlock);
    }
    else
    {
      if (!lastBlock.compareAndSet(blocks.get(paramBlock.getPreviousBlockId()), paramBlock)) {
        throw new IllegalStateException("Last block not equal to this.previousBlock");
      }
      if (blocks.putIfAbsent(paramBlock.getId(), paramBlock) != null) {
        throw new IllegalStateException("duplicate block id: " + paramBlock.getId());
      }
    }
  }
  
  static void init()
  {
    Object localObject1;
    try
    {
      Logger.logMessage("Loading transactions...");
      loadTransactions("transactions.nxt");
      Logger.logMessage("...Done");
    }
    catch (FileNotFoundException localFileNotFoundException1)
    {
      Logger.logMessage("transactions.nxt not found, starting from scratch");
      transactions.clear();
      Transaction localTransaction;
      try
      {
        for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
        {
          localTransaction = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
          

          transactions.put(localTransaction.getId(), localTransaction);
        }
      }
      catch (NxtException.ValidationException localValidationException1)
      {
        Logger.logMessage(localValidationException1.getMessage());
        System.exit(1);
      }
      for (localObject1 = transactions.values().iterator(); ((Iterator)localObject1).hasNext();)
      {
        localTransaction = (Transaction)((Iterator)localObject1).next();
        localTransaction.setIndex(transactionCounter.incrementAndGet());
        localTransaction.setBlockId(Genesis.GENESIS_BLOCK_ID);
      }
      saveTransactions("transactions.nxt");
    }
    try
    {
      Logger.logMessage("Loading blocks...");
      loadBlocks("blocks.nxt");
      Logger.logMessage("...Done");
    }
    catch (FileNotFoundException localFileNotFoundException2)
    {
      Logger.logMessage("blocks.nxt not found, starting from scratch");
      blocks.clear();
      try
      {
        localObject1 = new Block(-1, 0, null, transactions.size(), 1000000000, 0, transactions.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
        
        ((Block)localObject1).setIndex(blockCounter.incrementAndGet());
        
        int j = 0;
        for (Object localObject2 = transactions.keySet().iterator(); ((Iterator)localObject2).hasNext();)
        {
          localObject3 = (Long)((Iterator)localObject2).next();
          
          ((Block)localObject1).transactionIds[(j++)] = localObject3;
        }
        Object localObject3;
        Arrays.sort(((Block)localObject1).transactionIds);
        localObject2 = Crypto.sha256();
        for (j = 0; j < ((Block)localObject1).transactionIds.length; j++)
        {
          localObject3 = (Transaction)transactions.get(localObject1.transactionIds[j]);
          ((MessageDigest)localObject2).update(((Transaction)localObject3).getBytes());
          ((Block)localObject1).blockTransactions[j] = localObject3;
        }
        ((Block)localObject1).setPayloadHash(((MessageDigest)localObject2).digest());
        
        blocks.put(Genesis.GENESIS_BLOCK_ID, localObject1);
        lastBlock.set(localObject1);
      }
      catch (NxtException.ValidationException localValidationException2)
      {
        Logger.logMessage(localValidationException2.getMessage());
        System.exit(1);
      }
      saveBlocks("blocks.nxt");
    }
    Logger.logMessage("Scanning blockchain...");
    scan();
    Logger.logMessage("...Done");
  }
  
  static void shutdown()
  {
    try
    {
      saveBlocks("blocks.nxt");
      Logger.logMessage("Saved blocks.nxt");
    }
    catch (RuntimeException localRuntimeException1)
    {
      Logger.logMessage("Error saving blocks", localRuntimeException1);
    }
    try
    {
      saveTransactions("transactions.nxt");
      Logger.logMessage("Saved transactions.nxt");
    }
    catch (RuntimeException localRuntimeException2)
    {
      Logger.logMessage("Error saving transactions", localRuntimeException2);
    }
  }
  
  private static void processUnconfirmedTransactions(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    JSONArray localJSONArray = (JSONArray)paramJSONObject.get("unconfirmedTransactions");
    processTransactions(localJSONArray, true);
  }
  
  private static void processTransactions(JSONArray paramJSONArray, boolean paramBoolean)
    throws NxtException.ValidationException
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject1 = paramJSONArray.iterator(); ((Iterator)localObject1).hasNext();)
    {
      Object localObject2 = ((Iterator)localObject1).next();
      try
      {
        Transaction localTransaction = Transaction.getTransaction((JSONObject)localObject2);
        
        int i = Convert.getEpochTime();
        if ((localTransaction.getTimestamp() > i + 15) || (localTransaction.getExpiration() < i) || (localTransaction.getDeadline() <= 1440))
        {
          boolean bool;
          synchronized (Blockchain.class)
          {
            localObject3 = localTransaction.getId();
            if (((!transactions.containsKey(localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
            



              (transactionHashes.containsKey(localTransaction.getHash()))) {
              continue;
            }
            bool = localTransaction.isDoubleSpending();
            
            localTransaction.setIndex(transactionCounter.incrementAndGet());
            if (bool)
            {
              doubleSpendingTransactions.put(localObject3, localTransaction);
            }
            else
            {
              unconfirmedTransactions.put(localObject3, localTransaction);
              if (!paramBoolean) {
                localJSONArray.add(localObject2);
              }
            }
          }
          ??? = new JSONObject();
          ((JSONObject)???).put("response", "processNewData");
          
          Object localObject3 = new JSONArray();
          JSONObject localJSONObject = new JSONObject();
          localJSONObject.put("index", Integer.valueOf(localTransaction.getIndex()));
          localJSONObject.put("timestamp", Integer.valueOf(localTransaction.getTimestamp()));
          localJSONObject.put("deadline", Short.valueOf(localTransaction.getDeadline()));
          localJSONObject.put("recipient", Convert.convert(localTransaction.getRecipientId()));
          localJSONObject.put("amount", Integer.valueOf(localTransaction.getAmount()));
          localJSONObject.put("fee", Integer.valueOf(localTransaction.getFee()));
          localJSONObject.put("sender", Convert.convert(localTransaction.getSenderAccountId()));
          localJSONObject.put("id", localTransaction.getStringId());
          ((JSONArray)localObject3).add(localJSONObject);
          if (bool) {
            ((JSONObject)???).put("addedDoubleSpendingTransactions", localObject3);
          } else {
            ((JSONObject)???).put("addedUnconfirmedTransactions", localObject3);
          }
          User.sendToAll((JSONStreamAware)???);
        }
      }
      catch (RuntimeException localRuntimeException)
      {
        Logger.logMessage("Error processing transaction", localRuntimeException);
      }
    }
    if (localJSONArray.size() > 0)
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("requestType", "processTransactions");
      ((JSONObject)localObject1).put("transactions", localJSONArray);
      
      Peer.sendToSomePeers((JSONObject)localObject1);
    }
  }
  
  private static synchronized byte[] calculateTransactionsChecksum()
  {
    PriorityQueue localPriorityQueue = new PriorityQueue(transactions.size(), new Comparator()
    {
      public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
      {
        long l1 = paramAnonymousTransaction1.getId().longValue();
        long l2 = paramAnonymousTransaction2.getId().longValue();
        return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
      }
    });
    localPriorityQueue.addAll(transactions.values());
    MessageDigest localMessageDigest = Crypto.sha256();
    while (!localPriorityQueue.isEmpty()) {
      localMessageDigest.update(((Transaction)localPriorityQueue.poll()).getBytes());
    }
    return localMessageDigest.digest();
  }
  
  private static boolean pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction, boolean paramBoolean)
  {
    int i = Convert.getEpochTime();
    JSONArray localJSONArray1;
    JSONArray localJSONArray2;
    synchronized (Blockchain.class)
    {
      try
      {
        Block localBlock = (Block)lastBlock.get();
        if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
          return false;
        }
        if (localBlock.getHeight() == 30000)
        {
          localObject1 = calculateTransactionsChecksum();
          if (CHECKSUM_TRANSPARENT_FORGING == null)
          {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString((byte[])localObject1));
          }
          else
          {
            if (!Arrays.equals((byte[])localObject1, CHECKSUM_TRANSPARENT_FORGING))
            {
              Logger.logMessage("Checksum failed at block 30000");
              return false;
            }
            Logger.logMessage("Checksum passed at block 30000");
          }
        }
        if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
          return false;
        }
        if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
          return false;
        }
        if ((!localBlock.getId().equals(paramBlock.getPreviousBlockId())) || (paramBlock.getId().equals(Long.valueOf(0L))) || (blocks.containsKey(paramBlock.getId())) || (!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
          return false;
        }
        paramBlock.setIndex(blockCounter.incrementAndGet());
        
        localObject1 = new HashMap();
        HashMap localHashMap1 = new HashMap();
        for (int j = 0; j < paramBlock.transactionIds.length; j++)
        {
          localObject2 = paramArrayOfTransaction[j];
          ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
          if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
            return false;
          }
          if (((Transaction)localObject2).isDuplicate(localHashMap1)) {
            return false;
          }
        }
        Arrays.sort(paramBlock.transactionIds);
        
        HashMap localHashMap2 = new HashMap();
        Object localObject2 = new HashMap();
        int k = 0;int m = 0;
        MessageDigest localMessageDigest = Crypto.sha256();
        Object localObject6;
        Object localObject7;
        for (localObject6 : paramBlock.transactionIds)
        {
          localObject7 = (Transaction)((Map)localObject1).get(localObject6);
          if ((((Transaction)localObject7).getTimestamp() > i + 15) || ((((Transaction)localObject7).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() > 303)) || (transactions.get(localObject6) != null) || ((((Transaction)localObject7).getReferencedTransactionId() != null) && (transactions.get(((Transaction)localObject7).getReferencedTransactionId()) == null) && (((Map)localObject1).get(((Transaction)localObject7).getReferencedTransactionId()) == null)) || ((unconfirmedTransactions.get(localObject6) == null) && (!((Transaction)localObject7).verify())) || (((Transaction)localObject7).getId().equals(Long.valueOf(0L)))) {
            return false;
          }
          k += ((Transaction)localObject7).getAmount();
          
          ((Transaction)localObject7).updateTotals(localHashMap2, (Map)localObject2);
          
          m += ((Transaction)localObject7).getFee();
          
          localMessageDigest.update(((Transaction)localObject7).getBytes());
        }
        if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
          return false;
        }
        if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
          return false;
        }
        for (??? = localHashMap2.entrySet().iterator(); ((Iterator)???).hasNext();)
        {
          localObject4 = (Map.Entry)((Iterator)???).next();
          localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
          if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
            return false;
          }
        }
        Object localObject5;
        for (??? = ((Map)localObject2).entrySet().iterator(); ((Iterator)???).hasNext();)
        {
          localObject4 = (Map.Entry)((Iterator)???).next();
          localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
          for (localObject6 = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); ((Iterator)localObject6).hasNext();)
          {
            localObject7 = (Map.Entry)((Iterator)localObject6).next();
            long l1 = ((Long)((Map.Entry)localObject7).getKey()).longValue();
            long l2 = ((Long)((Map.Entry)localObject7).getValue()).longValue();
            if (((Account)localObject5).getAssetBalance(Long.valueOf(l1)).intValue() < l2) {
              return false;
            }
          }
        }
        paramBlock.setHeight(localBlock.getHeight() + 1);
        
        ??? = null;
        for (Object localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
        {
          localObject5 = (Map.Entry)((Iterator)localObject4).next();
          localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
          ((Transaction)localObject6).setHeight(paramBlock.getHeight());
          ((Transaction)localObject6).setBlockId(paramBlock.getId());
          if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
          {
            ??? = localObject6;
            break;
          }
          if (transactions.putIfAbsent(((Map.Entry)localObject5).getKey(), localObject6) != null)
          {
            Logger.logMessage("duplicate transaction id " + ((Map.Entry)localObject5).getKey());
            ??? = localObject6;
            break;
          }
        }
        if (??? != null)
        {
          for (localObject4 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject4).hasNext();)
          {
            localObject5 = (Long)((Iterator)localObject4).next();
            if (!((Long)localObject5).equals(((Transaction)???).getId()))
            {
              localObject6 = (Transaction)transactions.remove(localObject5);
              if (localObject6 != null)
              {
                localObject7 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
                if ((localObject7 != null) && (((Transaction)localObject7).getId().equals(localObject5))) {
                  transactionHashes.remove(((Transaction)localObject6).getHash());
                }
              }
            }
          }
          return false;
        }
        paramBlock.apply();
        
        purgeExpiredHashes();
        
        localJSONArray1 = new JSONArray();
        localJSONArray2 = new JSONArray();
        for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
        {
          localObject5 = (Map.Entry)((Iterator)localObject4).next();
          
          localObject6 = (Transaction)((Map.Entry)localObject5).getValue();
          
          localObject7 = new JSONObject();
          ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
          ((JSONObject)localObject7).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
          ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
          ((JSONObject)localObject7).put("sender", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
          ((JSONObject)localObject7).put("recipient", Convert.convert(((Transaction)localObject6).getRecipientId()));
          ((JSONObject)localObject7).put("amount", Integer.valueOf(((Transaction)localObject6).getAmount()));
          ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
          ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
          localJSONArray1.add(localObject7);
          
          Transaction localTransaction = (Transaction)unconfirmedTransactions.remove(((Map.Entry)localObject5).getKey());
          if (localTransaction != null)
          {
            JSONObject localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
            localJSONArray2.add(localJSONObject2);
            
            Account localAccount = Account.getAccount(localTransaction.getSenderAccountId());
            localAccount.addToUnconfirmedBalance((localTransaction.getAmount() + localTransaction.getFee()) * 100L);
          }
        }
        if (paramBoolean)
        {
          saveTransactions("transactions.nxt");
          saveBlocks("blocks.nxt");
        }
      }
      catch (RuntimeException localRuntimeException)
      {
        Logger.logMessage("Error pushing block", localRuntimeException);
        return false;
      }
    }
    if (paramBlock.getTimestamp() >= i - 15)
    {
      ??? = paramBlock.getJSONObject();
      ((JSONObject)???).put("requestType", "processBlock");
      
      Peer.sendToSomePeers((JSONObject)???);
    }
    ??? = new JSONArray();
    JSONObject localJSONObject1 = new JSONObject();
    localJSONObject1.put("index", Integer.valueOf(paramBlock.getIndex()));
    localJSONObject1.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
    localJSONObject1.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
    localJSONObject1.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
    localJSONObject1.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
    localJSONObject1.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
    localJSONObject1.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
    localJSONObject1.put("height", Integer.valueOf(paramBlock.getHeight()));
    localJSONObject1.put("version", Integer.valueOf(paramBlock.getVersion()));
    localJSONObject1.put("block", paramBlock.getStringId());
    localJSONObject1.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
    ((JSONArray)???).add(localJSONObject1);
    
    Object localObject1 = new JSONObject();
    ((JSONObject)localObject1).put("response", "processNewData");
    ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
    if (localJSONArray2.size() > 0) {
      ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
    }
    ((JSONObject)localObject1).put("addedRecentBlocks", ???);
    
    User.sendToAll((JSONStreamAware)localObject1);
    
    return true;
  }
  
  private static boolean popLastBlock()
    throws Transaction.UndoNotSupportedException
  {
    try
    {
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("response", "processNewData");
      
      JSONArray localJSONArray = new JSONArray();
      Block localBlock;
      synchronized (Blockchain.class)
      {
        localBlock = (Block)lastBlock.get();
        if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
          return false;
        }
        localObject1 = (Block)blocks.get(localBlock.getPreviousBlockId());
        if (localObject1 == null)
        {
          Logger.logMessage("Previous block is null");
          throw new IllegalStateException();
        }
        if (!lastBlock.compareAndSet(localBlock, localObject1))
        {
          Logger.logMessage("This block is no longer last block");
          throw new IllegalStateException();
        }
        Account localAccount = Account.getAccount(localBlock.getGeneratorAccountId());
        localAccount.addToBalanceAndUnconfirmedBalance(-localBlock.getTotalFee() * 100L);
        for (Long localLong : localBlock.transactionIds)
        {
          Transaction localTransaction1 = (Transaction)transactions.remove(localLong);
          Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
          if ((localTransaction2 != null) && (localTransaction2.getId().equals(localLong))) {
            transactionHashes.remove(localTransaction1.getHash());
          }
          unconfirmedTransactions.put(localLong, localTransaction1);
          
          localTransaction1.undo();
          
          JSONObject localJSONObject2 = new JSONObject();
          localJSONObject2.put("index", Integer.valueOf(localTransaction1.getIndex()));
          localJSONObject2.put("timestamp", Integer.valueOf(localTransaction1.getTimestamp()));
          localJSONObject2.put("deadline", Short.valueOf(localTransaction1.getDeadline()));
          localJSONObject2.put("recipient", Convert.convert(localTransaction1.getRecipientId()));
          localJSONObject2.put("amount", Integer.valueOf(localTransaction1.getAmount()));
          localJSONObject2.put("fee", Integer.valueOf(localTransaction1.getFee()));
          localJSONObject2.put("sender", Convert.convert(localTransaction1.getSenderAccountId()));
          localJSONObject2.put("id", localTransaction1.getStringId());
          localJSONArray.add(localJSONObject2);
        }
      }
      ??? = new JSONArray();
      Object localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("index", Integer.valueOf(localBlock.getIndex()));
      ((JSONObject)localObject1).put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
      ((JSONObject)localObject1).put("numberOfTransactions", Integer.valueOf(localBlock.transactionIds.length));
      ((JSONObject)localObject1).put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
      ((JSONObject)localObject1).put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
      ((JSONObject)localObject1).put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
      ((JSONObject)localObject1).put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
      ((JSONObject)localObject1).put("height", Integer.valueOf(localBlock.getHeight()));
      ((JSONObject)localObject1).put("version", Integer.valueOf(localBlock.getVersion()));
      ((JSONObject)localObject1).put("block", localBlock.getStringId());
      ((JSONObject)localObject1).put("baseTarget", BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
      ((JSONArray)???).add(localObject1);
      localJSONObject1.put("addedOrphanedBlocks", ???);
      if (localJSONArray.size() > 0) {
        localJSONObject1.put("addedUnconfirmedTransactions", localJSONArray);
      }
      User.sendToAll(localJSONObject1);
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logMessage("Error popping last block", localRuntimeException);
      return false;
    }
    return true;
  }
  
  private static synchronized void scan()
  {
    HashMap localHashMap = new HashMap(blocks);
    blocks.clear();
    Long localLong = Genesis.GENESIS_BLOCK_ID;
    Block localBlock;
    while ((localBlock = (Block)localHashMap.get(localLong)) != null)
    {
      localBlock.apply();
      localLong = localBlock.getNextBlockId();
    }
  }
  
  private static void generateBlock(String paramString)
  {
    TreeSet localTreeSet = new TreeSet();
    for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Transaction)((Iterator)localObject1).next();
      if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (transactions.get(((Transaction)localObject2).getReferencedTransactionId()) != null)) {
        localTreeSet.add(localObject2);
      }
    }
    localObject1 = new HashMap();
    Object localObject2 = new HashMap();
    HashMap localHashMap = new HashMap();
    
    int i = 0;
    int j = 0;
    int k = 0;
    Object localObject3;
    while (k <= 32640)
    {
      int m = ((Map)localObject1).size();
      for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Transaction)((Iterator)localObject3).next();
        
        int n = ((Transaction)localObject4).getSize();
        if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + n <= 32640))
        {
          localObject5 = ((Transaction)localObject4).getSenderAccountId();
          localObject6 = (Long)localHashMap.get(localObject5);
          if (localObject6 == null) {
            localObject6 = Long.valueOf(0L);
          }
          long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
          if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
            if (!((Transaction)localObject4).isDuplicate((Map)localObject2))
            {
              localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
              
              ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
              k += n;
              i += ((Transaction)localObject4).getAmount();
              j += ((Transaction)localObject4).getFee();
            }
          }
        }
      }
      if (((Map)localObject1).size() == m) {
        break;
      }
    }
    byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
    

    Object localObject4 = (Block)lastBlock.get();
    try
    {
      if (((Block)localObject4).getHeight() < 30000)
      {
        localObject3 = new Block(1, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
      }
      else
      {
        byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
        localObject3 = new Block(2, Convert.getEpochTime(), ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
      }
    }
    catch (NxtException.ValidationException localValidationException)
    {
      Logger.logMessage("Error generating block", localValidationException);
      return;
    }
    int i1 = 0;
    for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
    {
      localObject6 = (Long)((Iterator)localObject5).next();
      ((Block)localObject3).transactionIds[(i1++)] = localObject6;
    }
    Arrays.sort(((Block)localObject3).transactionIds);
    localObject5 = Crypto.sha256();
    for (i1 = 0; i1 < ((Block)localObject3).transactionIds.length; i1++)
    {
      localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i1]);
      ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
      ((Block)localObject3).blockTransactions[i1] = localObject6;
    }
    ((Block)localObject3).setPayloadHash(((MessageDigest)localObject5).digest());
    if (((Block)localObject4).getHeight() < 30000)
    {
      ((Block)localObject3).setGenerationSignature(Crypto.sign(((Block)localObject4).getGenerationSignature(), paramString));
    }
    else
    {
      ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
      ((Block)localObject3).setGenerationSignature(((MessageDigest)localObject5).digest(arrayOfByte1));
    }
    Object localObject6 = ((Block)localObject3).getBytes();
    byte[] arrayOfByte3 = new byte[localObject6.length - 64];
    System.arraycopy(localObject6, 0, arrayOfByte3, 0, arrayOfByte3.length);
    ((Block)localObject3).setBlockSignature(Crypto.sign(arrayOfByte3, paramString));
    if ((((Block)localObject3).verifyBlockSignature()) && (((Block)localObject3).verifyGenerationSignature()))
    {
      JSONObject localJSONObject = ((Block)localObject3).getJSONObject();
      localJSONObject.put("requestType", "processBlock");
      Peer.sendToSomePeers(localJSONObject);
    }
    else
    {
      Logger.logMessage("Generated an incorrect block. Waiting for the next one...");
    }
  }
  
  private static void purgeExpiredHashes()
  {
    int i = Convert.getEpochTime();
    Iterator localIterator = transactionHashes.entrySet().iterator();
    while (localIterator.hasNext()) {
      if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < i) {
        localIterator.remove();
      }
    }
  }
  
  private static void loadTransactions(String paramString)
    throws FileNotFoundException
  {
    try
    {
      FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
      try
      {
        ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
        try
        {
          transactionCounter.set(localObjectInputStream.readInt());
          transactions.clear();
          transactions.putAll((HashMap)localObjectInputStream.readObject());
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localFileInputStream != null) {
          if (localObject1 != null) {
            try
            {
              localFileInputStream.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localFileInputStream.close();
          }
        }
      }
    }
    catch (FileNotFoundException localFileNotFoundException)
    {
      throw localFileNotFoundException;
    }
    catch (IOException|ClassNotFoundException localIOException)
    {
      Logger.logMessage("Error loading transactions from " + paramString, localIOException);
      System.exit(1);
    }
  }
  
  private static void saveTransactions(String paramString)
  {
    try
    {
      FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
      try
      {
        ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
        try
        {
          localObjectOutputStream.writeInt(transactionCounter.get());
          localObjectOutputStream.writeObject(new HashMap(transactions));
          localObjectOutputStream.close();
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localFileOutputStream != null) {
          if (localObject1 != null) {
            try
            {
              localFileOutputStream.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localFileOutputStream.close();
          }
        }
      }
    }
    catch (IOException localIOException)
    {
      Logger.logMessage("Error saving transactions to " + paramString, localIOException);
      throw new RuntimeException(localIOException);
    }
  }
  
  private static void loadBlocks(String paramString)
    throws FileNotFoundException
  {
    try
    {
      FileInputStream localFileInputStream = new FileInputStream(paramString);Object localObject1 = null;
      try
      {
        ObjectInputStream localObjectInputStream = new ObjectInputStream(localFileInputStream);Object localObject2 = null;
        try
        {
          blockCounter.set(localObjectInputStream.readInt());
          blocks.clear();
          blocks.putAll((HashMap)localObjectInputStream.readObject());
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localFileInputStream != null) {
          if (localObject1 != null) {
            try
            {
              localFileInputStream.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localFileInputStream.close();
          }
        }
      }
    }
    catch (FileNotFoundException localFileNotFoundException)
    {
      throw localFileNotFoundException;
    }
    catch (IOException|ClassNotFoundException localIOException)
    {
      Logger.logMessage("Error loading blocks from " + paramString, localIOException);
      System.exit(1);
    }
  }
  
  private static void saveBlocks(String paramString)
  {
    try
    {
      FileOutputStream localFileOutputStream = new FileOutputStream(paramString);Object localObject1 = null;
      try
      {
        ObjectOutputStream localObjectOutputStream = new ObjectOutputStream(localFileOutputStream);Object localObject2 = null;
        try
        {
          localObjectOutputStream.writeInt(blockCounter.get());
          localObjectOutputStream.writeObject(new HashMap(blocks));
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localFileOutputStream != null) {
          if (localObject1 != null) {
            try
            {
              localFileOutputStream.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localFileOutputStream.close();
          }
        }
      }
    }
    catch (IOException localIOException)
    {
      Logger.logMessage("Error saving blocks to " + paramString, localIOException);
      throw new RuntimeException(localIOException);
    }
  }
}