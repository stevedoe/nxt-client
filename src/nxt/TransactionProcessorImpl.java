package nxt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class TransactionProcessorImpl
  implements TransactionProcessor
{
  private static final TransactionProcessorImpl instance = new TransactionProcessorImpl();
  
  static TransactionProcessorImpl getInstance()
  {
    return instance;
  }
  
  private final ConcurrentMap<Long, TransactionImpl> doubleSpendingTransactions = new ConcurrentHashMap();
  private final ConcurrentMap<Long, TransactionImpl> unconfirmedTransactions = new ConcurrentHashMap();
  private final Collection<TransactionImpl> allUnconfirmedTransactions = Collections.unmodifiableCollection(this.unconfirmedTransactions.values());
  private final ConcurrentMap<Long, TransactionImpl> nonBroadcastedTransactions = new ConcurrentHashMap();
  
  private static class TransactionHashInfo
  {
    private final Long transactionId;
    private final int expiration;
    
    private TransactionHashInfo(Transaction paramTransaction)
    {
      this.transactionId = paramTransaction.getId();
      this.expiration = paramTransaction.getExpiration();
    }
  }
  
  private final ConcurrentMap<String, TransactionHashInfo> transactionHashes = new ConcurrentHashMap();
  private final Listeners<List<Transaction>, TransactionProcessor.Event> transactionListeners = new Listeners();
  private final Runnable removeUnconfirmedTransactionsThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          int i = Convert.getEpochTime();
          ArrayList localArrayList = new ArrayList();
          
          Iterator localIterator = TransactionProcessorImpl.this.unconfirmedTransactions.values().iterator();
          while (localIterator.hasNext())
          {
            Transaction localTransaction = (Transaction)localIterator.next();
            if (localTransaction.getExpiration() < i)
            {
              localIterator.remove();
              Account localAccount = Account.getAccount(localTransaction.getSenderId());
              localAccount.addToUnconfirmedBalance((localTransaction.getAmount() + localTransaction.getFee()) * 100L);
              localArrayList.add(localTransaction);
            }
          }
          if (localArrayList.size() > 0) {
            TransactionProcessorImpl.this.transactionListeners.notify(localArrayList, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
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
  private final Runnable rebroadcastTransactionsThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          JSONArray localJSONArray = new JSONArray();
          for (Object localObject = TransactionProcessorImpl.this.nonBroadcastedTransactions.values().iterator(); ((Iterator)localObject).hasNext();)
          {
            Transaction localTransaction = (Transaction)((Iterator)localObject).next();
            if ((TransactionProcessorImpl.this.unconfirmedTransactions.get(localTransaction.getId()) == null) && (!TransactionDb.hasTransaction(localTransaction.getId()))) {
              localJSONArray.add(localTransaction.getJSONObject());
            } else {
              TransactionProcessorImpl.this.nonBroadcastedTransactions.remove(localTransaction.getId());
            }
          }
          if (localJSONArray.size() > 0)
          {
            localObject = new JSONObject();
            ((JSONObject)localObject).put("requestType", "processTransactions");
            ((JSONObject)localObject).put("transactions", localJSONArray);
            Peers.sendToSomePeers((JSONObject)localObject);
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
  private final Runnable processTransactionsThread = new Runnable()
  {
    private final JSONStreamAware getUnconfirmedTransactionsRequest;
    
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
          JSONObject localJSONObject = localPeer.send(this.getUnconfirmedTransactionsRequest);
          if (localJSONObject == null) {
            return;
          }
          JSONArray localJSONArray = (JSONArray)localJSONObject.get("unconfirmedTransactions");
          if (localJSONArray == null) {
            return;
          }
          TransactionProcessorImpl.this.processJSONTransactions(localJSONArray, false);
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
  
  private TransactionProcessorImpl()
  {
    ThreadPool.scheduleThread(this.processTransactionsThread, 5);
    ThreadPool.scheduleThread(this.removeUnconfirmedTransactionsThread, 1);
    ThreadPool.scheduleThread(this.rebroadcastTransactionsThread, 60);
  }
  
  public boolean addListener(Listener<List<Transaction>> paramListener, TransactionProcessor.Event paramEvent)
  {
    return this.transactionListeners.addListener(paramListener, paramEvent);
  }
  
  public boolean removeListener(Listener<List<Transaction>> paramListener, TransactionProcessor.Event paramEvent)
  {
    return this.transactionListeners.removeListener(paramListener, paramEvent);
  }
  
  public Collection<TransactionImpl> getAllUnconfirmedTransactions()
  {
    return this.allUnconfirmedTransactions;
  }
  
  public Transaction getUnconfirmedTransaction(Long paramLong)
  {
    return (Transaction)this.unconfirmedTransactions.get(paramLong);
  }
  
  public void broadcast(Transaction paramTransaction)
  {
    processTransactions(Arrays.asList(new TransactionImpl[] { (TransactionImpl)paramTransaction }), true);
    this.nonBroadcastedTransactions.put(paramTransaction.getId(), (TransactionImpl)paramTransaction);
    Logger.logDebugMessage("Accepted new transaction " + paramTransaction.getStringId());
  }
  
  public void processPeerTransactions(JSONObject paramJSONObject)
  {
    JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
    processJSONTransactions(localJSONArray, true);
  }
  
  public Transaction parseTransaction(byte[] paramArrayOfByte)
    throws NxtException.ValidationException
  {
    try
    {
      ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      
      byte b1 = localByteBuffer.get();
      byte b2 = localByteBuffer.get();
      int i = localByteBuffer.getInt();
      short s = localByteBuffer.getShort();
      byte[] arrayOfByte1 = new byte[32];
      localByteBuffer.get(arrayOfByte1);
      Long localLong1 = Long.valueOf(localByteBuffer.getLong());
      int j = localByteBuffer.getInt();
      int k = localByteBuffer.getInt();
      Long localLong2 = Convert.zeroToNull(localByteBuffer.getLong());
      byte[] arrayOfByte2 = new byte[64];
      localByteBuffer.get(arrayOfByte2);
      
      TransactionType localTransactionType = TransactionType.findTransactionType(b1, b2);
      TransactionImpl localTransactionImpl = new TransactionImpl(localTransactionType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
      

      localTransactionType.loadAttachment(localTransactionImpl, localByteBuffer);
      
      return localTransactionImpl;
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString());
    }
  }
  
  public Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2)
    throws NxtException.ValidationException
  {
    return new TransactionImpl(TransactionType.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
  }
  
  public Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, Attachment paramAttachment)
    throws NxtException.ValidationException
  {
    TransactionImpl localTransactionImpl = new TransactionImpl(paramAttachment.getTransactionType(), paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
    
    localTransactionImpl.setAttachment(paramAttachment);
    return localTransactionImpl;
  }
  
  TransactionImpl newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
    throws NxtException.ValidationException
  {
    return new TransactionImpl(TransactionType.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte1, paramLong1, paramInt2, paramInt3, paramLong2, paramArrayOfByte2);
  }
  
  TransactionImpl parseTransaction(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    try
    {
      byte b1 = ((Long)paramJSONObject.get("type")).byteValue();
      byte b2 = ((Long)paramJSONObject.get("subtype")).byteValue();
      int i = ((Long)paramJSONObject.get("timestamp")).intValue();
      short s = ((Long)paramJSONObject.get("deadline")).shortValue();
      byte[] arrayOfByte1 = Convert.parseHexString((String)paramJSONObject.get("senderPublicKey"));
      Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("recipient"));
      if (localLong1 == null) {
        localLong1 = Long.valueOf(0L);
      }
      int j = ((Long)paramJSONObject.get("amount")).intValue();
      int k = ((Long)paramJSONObject.get("fee")).intValue();
      Long localLong2 = Convert.parseUnsignedLong((String)paramJSONObject.get("referencedTransaction"));
      byte[] arrayOfByte2 = Convert.parseHexString((String)paramJSONObject.get("signature"));
      
      TransactionType localTransactionType = TransactionType.findTransactionType(b1, b2);
      TransactionImpl localTransactionImpl = new TransactionImpl(localTransactionType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
      

      JSONObject localJSONObject = (JSONObject)paramJSONObject.get("attachment");
      
      localTransactionType.loadAttachment(localTransactionImpl, localJSONObject);
      
      return localTransactionImpl;
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString());
    }
  }
  
  void clear()
  {
    this.unconfirmedTransactions.clear();
    this.doubleSpendingTransactions.clear();
    this.nonBroadcastedTransactions.clear();
    this.transactionHashes.clear();
  }
  
  void apply(BlockImpl paramBlockImpl)
  {
    paramBlockImpl.apply();
    for (TransactionImpl localTransactionImpl : paramBlockImpl.getTransactions())
    {
      localTransactionImpl.apply();
      this.transactionHashes.put(localTransactionImpl.getHash(), new TransactionHashInfo(localTransactionImpl, null));
    }
    purgeExpiredHashes(paramBlockImpl.getTimestamp());
  }
  
  void undo(BlockImpl paramBlockImpl)
    throws TransactionType.UndoNotSupportedException
  {
    ArrayList localArrayList = new ArrayList();
    for (TransactionImpl localTransactionImpl : paramBlockImpl.getTransactions())
    {
      TransactionHashInfo localTransactionHashInfo = (TransactionHashInfo)this.transactionHashes.get(localTransactionImpl.getHash());
      if ((localTransactionHashInfo != null) && (localTransactionHashInfo.transactionId.equals(localTransactionImpl.getId()))) {
        this.transactionHashes.remove(localTransactionImpl.getHash());
      }
      this.unconfirmedTransactions.put(localTransactionImpl.getId(), localTransactionImpl);
      localTransactionImpl.undo();
      localArrayList.add(localTransactionImpl);
    }
    if (localArrayList.size() > 0) {
      this.transactionListeners.notify(localArrayList, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    }
  }
  
  TransactionImpl checkTransactionHashes(BlockImpl paramBlockImpl)
  {
    Object localObject = null;
    for (Iterator localIterator = paramBlockImpl.getTransactions().iterator(); localIterator.hasNext();)
    {
      localTransactionImpl = (TransactionImpl)localIterator.next();
      if ((this.transactionHashes.putIfAbsent(localTransactionImpl.getHash(), new TransactionHashInfo(localTransactionImpl, null)) != null) && (paramBlockImpl.getHeight() != 58294))
      {
        localObject = localTransactionImpl;
        break;
      }
    }
    TransactionImpl localTransactionImpl;
    if (localObject != null) {
      for (localIterator = paramBlockImpl.getTransactions().iterator(); localIterator.hasNext();)
      {
        localTransactionImpl = (TransactionImpl)localIterator.next();
        if (!localTransactionImpl.equals(localObject))
        {
          TransactionHashInfo localTransactionHashInfo = (TransactionHashInfo)this.transactionHashes.get(localTransactionImpl.getHash());
          if ((localTransactionHashInfo != null) && (localTransactionHashInfo.transactionId.equals(localTransactionImpl.getId()))) {
            this.transactionHashes.remove(localTransactionImpl.getHash());
          }
        }
      }
    }
    return localObject;
  }
  
  void updateUnconfirmedTransactions(BlockImpl paramBlockImpl)
  {
    ArrayList localArrayList1 = new ArrayList();
    ArrayList localArrayList2 = new ArrayList();
    for (Transaction localTransaction1 : paramBlockImpl.getTransactions())
    {
      localArrayList1.add(localTransaction1);
      Transaction localTransaction2 = (Transaction)this.unconfirmedTransactions.remove(localTransaction1.getId());
      if (localTransaction2 != null)
      {
        localArrayList2.add(localTransaction2);
        Account localAccount = Account.getAccount(localTransaction2.getSenderId());
        localAccount.addToUnconfirmedBalance((localTransaction2.getAmount() + localTransaction2.getFee()) * 100L);
      }
    }
    if (localArrayList2.size() > 0) {
      this.transactionListeners.notify(localArrayList2, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }
    if (localArrayList1.size() > 0) {
      this.transactionListeners.notify(localArrayList1, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }
  }
  
  private void purgeExpiredHashes(int paramInt)
  {
    Iterator localIterator = this.transactionHashes.entrySet().iterator();
    while (localIterator.hasNext()) {
      if (((TransactionHashInfo)((Map.Entry)localIterator.next()).getValue()).expiration < paramInt) {
        localIterator.remove();
      }
    }
  }
  
  private void processJSONTransactions(JSONArray paramJSONArray, boolean paramBoolean)
  {
    ArrayList localArrayList = new ArrayList();
    for (Object localObject : paramJSONArray) {
      try
      {
        localArrayList.add(parseTransaction((JSONObject)localObject));
      }
      catch (NxtException.ValidationException localValidationException)
      {
        if (!(localValidationException instanceof TransactionType.NotYetEnabledException)) {
          Logger.logDebugMessage("Dropping invalid transaction", localValidationException);
        }
      }
    }
    processTransactions(localArrayList, paramBoolean);
  }
  
  private void processTransactions(List<TransactionImpl> paramList, boolean paramBoolean)
  {
    JSONArray localJSONArray = new JSONArray();
    ArrayList localArrayList1 = new ArrayList();
    ArrayList localArrayList2 = new ArrayList();
    for (Object localObject1 = paramList.iterator(); ((Iterator)localObject1).hasNext();)
    {
      TransactionImpl localTransactionImpl = (TransactionImpl)((Iterator)localObject1).next();
      try
      {
        int i = Convert.getEpochTime();
        if ((localTransactionImpl.getTimestamp() > i + 15) || (localTransactionImpl.getExpiration() < i) || (localTransactionImpl.getDeadline() <= 1440))
        {
          boolean bool;
          synchronized (BlockchainImpl.getInstance())
          {
            Long localLong = localTransactionImpl.getId();
            if (((!TransactionDb.hasTransaction(localLong)) && (!this.unconfirmedTransactions.containsKey(localLong)) && (!this.doubleSpendingTransactions.containsKey(localLong)) && (!localTransactionImpl.verify())) || 
            



              (this.transactionHashes.containsKey(localTransactionImpl.getHash()))) {
              continue;
            }
            bool = localTransactionImpl.isDoubleSpending();
            if (bool)
            {
              this.doubleSpendingTransactions.put(localLong, localTransactionImpl);
            }
            else
            {
              if (paramBoolean) {
                if (this.nonBroadcastedTransactions.containsKey(localLong)) {
                  Logger.logDebugMessage("Received back transaction " + localTransactionImpl.getStringId() + " that we generated, will not forward to peers");
                } else {
                  localJSONArray.add(localTransactionImpl.getJSONObject());
                }
              }
              this.unconfirmedTransactions.put(localLong, localTransactionImpl);
            }
          }
          if (bool) {
            localArrayList2.add(localTransactionImpl);
          } else {
            localArrayList1.add(localTransactionImpl);
          }
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
      Peers.sendToSomePeers((JSONObject)localObject1);
    }
    if (localArrayList1.size() > 0) {
      this.transactionListeners.notify(localArrayList1, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
    }
    if (localArrayList2.size() > 0) {
      this.transactionListeners.notify(localArrayList2, TransactionProcessor.Event.ADDED_DOUBLESPENDING_TRANSACTIONS);
    }
  }
}