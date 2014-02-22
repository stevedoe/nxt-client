package nxt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import nxt.crypto.Crypto;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.JSON;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class BlockchainProcessorImpl
  implements BlockchainProcessor
{
  private static final byte[] CHECKSUM_TRANSPARENT_FORGING = { 27, -54, -59, -98, 49, -42, 48, -68, -112, 49, 41, 94, -41, 78, -84, 27, -87, -22, -28, 36, -34, -90, 112, -50, -9, 5, 89, -35, 80, -121, -128, 112 };
  private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();
  
  static BlockchainProcessorImpl getInstance()
  {
    return instance;
  }
  
  private final BlockchainImpl blockchain = BlockchainImpl.getInstance();
  private final TransactionProcessorImpl transactionProcessor = TransactionProcessorImpl.getInstance();
  private final Listeners<Block, BlockchainProcessor.Event> blockListeners = new Listeners();
  private volatile Peer lastBlockchainFeeder;
  private final Runnable getMoreBlocksThread = new Runnable()
  {
    private final JSONStreamAware getCumulativeDifficultyRequest;
    private boolean peerHasMore;
    
    public void run()
    {
      try
      {
        try
        {
          this.peerHasMore = true;
          Peer localPeer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
          if (localPeer == null) {
            return;
          }
          BlockchainProcessorImpl.this.lastBlockchainFeeder = localPeer;
          JSONObject localJSONObject1 = localPeer.send(this.getCumulativeDifficultyRequest);
          if (localJSONObject1 == null) {
            return;
          }
          BigInteger localBigInteger1 = BlockchainProcessorImpl.this.blockchain.getLastBlock().getCumulativeDifficulty();
          String str = (String)localJSONObject1.get("cumulativeDifficulty");
          if (str == null) {
            return;
          }
          BigInteger localBigInteger2 = new BigInteger(str);
          if (localBigInteger2.compareTo(localBigInteger1) <= 0) {
            return;
          }
          Long localLong1 = Genesis.GENESIS_BLOCK_ID;
          if (!BlockchainProcessorImpl.this.blockchain.getLastBlock().getId().equals(Genesis.GENESIS_BLOCK_ID)) {
            localLong1 = getCommonMilestoneBlockId(localPeer);
          }
          if ((localLong1 == null) || (!this.peerHasMore)) {
            return;
          }
          localLong1 = getCommonBlockId(localPeer, localLong1);
          if ((localLong1 == null) || (!this.peerHasMore)) {
            return;
          }
          BlockImpl localBlockImpl1 = BlockDb.findBlock(localLong1);
          if (BlockchainProcessorImpl.this.blockchain.getLastBlock().getHeight() - localBlockImpl1.getHeight() >= 720) {
            return;
          }
          Long localLong2 = localLong1;
          ArrayList localArrayList = new ArrayList();
          for (;;)
          {
            JSONArray localJSONArray = getNextBlocks(localPeer, localLong2);
            if ((localJSONArray == null) || (localJSONArray.size() == 0)) {
              break;
            }
            synchronized (BlockchainProcessorImpl.this.blockchain)
            {
              for (Object localObject1 : localJSONArray)
              {
                JSONObject localJSONObject2 = (JSONObject)localObject1;
                BlockImpl localBlockImpl2;
                try
                {
                  localBlockImpl2 = BlockchainProcessorImpl.this.parseBlock(localJSONObject2);
                }
                catch (NxtException.ValidationException localValidationException)
                {
                  localPeer.blacklist(localValidationException);
                  return;
                }
                localLong2 = localBlockImpl2.getId();
                if (BlockchainProcessorImpl.this.blockchain.getLastBlock().getId().equals(localBlockImpl2.getPreviousBlockId())) {
                  try
                  {
                    BlockchainProcessorImpl.this.pushBlock(localBlockImpl2);
                  }
                  catch (BlockchainProcessor.BlockNotAcceptedException localBlockNotAcceptedException)
                  {
                    localPeer.blacklist(localBlockNotAcceptedException);
                    return;
                  }
                } else if (!BlockDb.hasBlock(localBlockImpl2.getId())) {
                  localArrayList.add(localBlockImpl2);
                }
              }
            }
          }
          if ((!localArrayList.isEmpty()) && (BlockchainProcessorImpl.this.blockchain.getLastBlock().getHeight() - localBlockImpl1.getHeight() < 720)) {
            processFork(localPeer, localArrayList, localBlockImpl1);
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
    
    private Long getCommonMilestoneBlockId(Peer paramAnonymousPeer)
    {
      Object localObject1 = null;
      JSONArray localJSONArray;
      for (;;)
      {
        JSONObject localJSONObject1 = new JSONObject();
        localJSONObject1.put("requestType", "getMilestoneBlockIds");
        if (localObject1 == null) {
          localJSONObject1.put("lastBlockId", BlockchainProcessorImpl.this.blockchain.getLastBlock().getStringId());
        } else {
          localJSONObject1.put("lastMilestoneBlockId", localObject1);
        }
        JSONObject localJSONObject2 = paramAnonymousPeer.send(JSON.prepareRequest(localJSONObject1));
        if (localJSONObject2 == null) {
          return null;
        }
        localJSONArray = (JSONArray)localJSONObject2.get("milestoneBlockIds");
        if (localJSONArray == null) {
          return null;
        }
        if (localJSONArray.isEmpty()) {
          return Genesis.GENESIS_BLOCK_ID;
        }
        if (localJSONArray.size() > 20)
        {
          Logger.logDebugMessage("Obsolete or rogue peer " + paramAnonymousPeer.getPeerAddress() + " sends too many milestoneBlockIds, blacklisting");
          paramAnonymousPeer.blacklist();
          return null;
        }
        if (Boolean.TRUE.equals(localJSONObject2.get("last"))) {
          this.peerHasMore = false;
        }
        for (Object localObject2 : localJSONArray)
        {
          Long localLong = Convert.parseUnsignedLong((String)localObject2);
          if (BlockDb.hasBlock(localLong))
          {
            if ((localObject1 == null) && (localJSONArray.size() > 1)) {
              this.peerHasMore = false;
            }
            return localLong;
          }
          localObject1 = (String)localObject2;
        }
      }
    }
    
    private Long getCommonBlockId(Peer paramAnonymousPeer, Long paramAnonymousLong)
    {
      for (;;)
      {
        JSONObject localJSONObject1 = new JSONObject();
        localJSONObject1.put("requestType", "getNextBlockIds");
        localJSONObject1.put("blockId", Convert.toUnsignedLong(paramAnonymousLong));
        JSONObject localJSONObject2 = paramAnonymousPeer.send(JSON.prepareRequest(localJSONObject1));
        if (localJSONObject2 == null) {
          return null;
        }
        JSONArray localJSONArray = (JSONArray)localJSONObject2.get("nextBlockIds");
        if ((localJSONArray == null) || (localJSONArray.size() == 0)) {
          return null;
        }
        if (localJSONArray.size() > 1440)
        {
          Logger.logDebugMessage("Obsolete or rogue peer " + paramAnonymousPeer.getPeerAddress() + " sends too many nextBlockIds, blacklisting");
          paramAnonymousPeer.blacklist();
          return null;
        }
        for (Object localObject : localJSONArray)
        {
          Long localLong = Convert.parseUnsignedLong((String)localObject);
          if (!BlockDb.hasBlock(localLong)) {
            return paramAnonymousLong;
          }
          paramAnonymousLong = localLong;
        }
      }
    }
    
    private JSONArray getNextBlocks(Peer paramAnonymousPeer, Long paramAnonymousLong)
    {
      JSONObject localJSONObject1 = new JSONObject();
      localJSONObject1.put("requestType", "getNextBlocks");
      localJSONObject1.put("blockId", Convert.toUnsignedLong(paramAnonymousLong));
      JSONObject localJSONObject2 = paramAnonymousPeer.send(JSON.prepareRequest(localJSONObject1));
      if (localJSONObject2 == null) {
        return null;
      }
      JSONArray localJSONArray = (JSONArray)localJSONObject2.get("nextBlocks");
      if (localJSONArray == null) {
        return null;
      }
      if (localJSONArray.size() > 1440)
      {
        Logger.logDebugMessage("Obsolete or rogue peer " + paramAnonymousPeer.getPeerAddress() + " sends too many nextBlocks, blacklisting");
        paramAnonymousPeer.blacklist();
        return null;
      }
      return localJSONArray;
    }
    
    private void processFork(Peer paramAnonymousPeer, List<BlockImpl> paramAnonymousList, Block paramAnonymousBlock)
    {
      synchronized (BlockchainProcessorImpl.this.blockchain)
      {
        BigInteger localBigInteger = BlockchainProcessorImpl.this.blockchain.getLastBlock().getCumulativeDifficulty();
        int i;
        try
        {
          while ((!BlockchainProcessorImpl.this.blockchain.getLastBlock().getId().equals(paramAnonymousBlock.getId())) && (BlockchainProcessorImpl.this.popLastBlock())) {}
          if (BlockchainProcessorImpl.this.blockchain.getLastBlock().getId().equals(paramAnonymousBlock.getId())) {
            for (BlockImpl localBlockImpl : paramAnonymousList) {
              if (BlockchainProcessorImpl.this.blockchain.getLastBlock().getId().equals(localBlockImpl.getPreviousBlockId())) {
                try
                {
                  BlockchainProcessorImpl.this.pushBlock(localBlockImpl);
                }
                catch (BlockchainProcessor.BlockNotAcceptedException localBlockNotAcceptedException)
                {
                  paramAnonymousPeer.blacklist(localBlockNotAcceptedException);
                  break;
                }
              }
            }
          }
          i = BlockchainProcessorImpl.this.blockchain.getLastBlock().getCumulativeDifficulty().compareTo(localBigInteger) < 0 ? 1 : 0;
          if (i != 0)
          {
            Logger.logDebugMessage("Rescan caused by peer " + paramAnonymousPeer.getPeerAddress() + ", blacklisting");
            paramAnonymousPeer.blacklist();
          }
        }
        catch (TransactionType.UndoNotSupportedException localUndoNotSupportedException)
        {
          Logger.logDebugMessage(localUndoNotSupportedException.getMessage());
          Logger.logDebugMessage("Popping off last block not possible, will do a rescan");
          i = 1;
        }
        if (i != 0)
        {
          if (paramAnonymousBlock.getNextBlockId() != null)
          {
            Logger.logDebugMessage("Last block is " + BlockchainProcessorImpl.this.blockchain.getLastBlock().getStringId() + " at " + BlockchainProcessorImpl.this.blockchain.getLastBlock().getHeight());
            Logger.logDebugMessage("Deleting blocks after height " + paramAnonymousBlock.getHeight());
            BlockDb.deleteBlock(paramAnonymousBlock.getNextBlockId());
          }
          Logger.logMessage("Will do a re-scan");
          BlockchainProcessorImpl.this.scan();
          Logger.logDebugMessage("Last block is " + BlockchainProcessorImpl.this.blockchain.getLastBlock().getStringId() + " at " + BlockchainProcessorImpl.this.blockchain.getLastBlock().getHeight());
        }
      }
    }
  };
  
  private BlockchainProcessorImpl()
  {
    this.blockListeners.addListener(new Listener()
    {
      public void notify(Block paramAnonymousBlock)
      {
        if (paramAnonymousBlock.getHeight() % 5000 == 0) {
          Logger.logDebugMessage("processed block " + paramAnonymousBlock.getHeight());
        }
      }
    }, BlockchainProcessor.Event.BLOCK_SCANNED);
    



    addGenesisBlock();
    scan();
    ThreadPool.scheduleThread(this.getMoreBlocksThread, 1);
  }
  
  public boolean addListener(Listener<Block> paramListener, BlockchainProcessor.Event paramEvent)
  {
    return this.blockListeners.addListener(paramListener, paramEvent);
  }
  
  public boolean removeListener(Listener<Block> paramListener, BlockchainProcessor.Event paramEvent)
  {
    return this.blockListeners.removeListener(paramListener, paramEvent);
  }
  
  public Peer getLastBlockchainFeeder()
  {
    return this.lastBlockchainFeeder;
  }
  
  public void processPeerBlock(JSONObject paramJSONObject)
    throws NxtException
  {
    BlockImpl localBlockImpl = parseBlock(paramJSONObject);
    pushBlock(localBlockImpl);
  }
  
  public void fullReset()
  {
    synchronized (this.blockchain)
    {
      Logger.logMessage("Deleting blockchain...");
      
      BlockDb.deleteAll();
      addGenesisBlock();
      scan();
    }
  }
  
  private void addBlock(BlockImpl paramBlockImpl)
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        try
        {
          BlockDb.saveBlock(localConnection, paramBlockImpl);
          this.blockchain.setLastBlock(paramBlockImpl);
          localConnection.commit();
        }
        catch (SQLException localSQLException2)
        {
          localConnection.rollback();
          throw localSQLException2;
        }
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localConnection != null) {
          if (localObject1 != null) {
            try
            {
              localConnection.close();
            }
            catch (Throwable localThrowable3)
            {
              localObject1.addSuppressed(localThrowable3);
            }
          } else {
            localConnection.close();
          }
        }
      }
    }
    catch (SQLException localSQLException1)
    {
      throw new RuntimeException(localSQLException1.toString(), localSQLException1);
    }
  }
  
  private void addGenesisBlock()
  {
    if (BlockDb.hasBlock(Genesis.GENESIS_BLOCK_ID))
    {
      Logger.logMessage("Genesis block already in database");
      return;
    }
    Logger.logMessage("Genesis block not in database, starting from scratch");
    try
    {
      TreeMap localTreeMap = new TreeMap();
      for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
      {
        localObject = this.transactionProcessor.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
        
        localTreeMap.put(((TransactionImpl)localObject).getId(), localObject);
      }
      MessageDigest localMessageDigest = Crypto.sha256();
      for (Object localObject = localTreeMap.values().iterator(); ((Iterator)localObject).hasNext();)
      {
        Transaction localTransaction = (Transaction)((Iterator)localObject).next();
        localMessageDigest.update(localTransaction.getBytes());
      }
      localObject = new BlockImpl(-1, 0, null, 1000000000, 0, localTreeMap.size() * 128, localMessageDigest.digest(), Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE, null, new ArrayList(localTreeMap.values()));
      

      ((BlockImpl)localObject).setPrevious(null);
      
      addBlock((BlockImpl)localObject);
    }
    catch (NxtException.ValidationException localValidationException)
    {
      Logger.logMessage(localValidationException.getMessage());
      throw new RuntimeException(localValidationException.toString(), localValidationException);
    }
  }
  
  private byte[] calculateTransactionsChecksum()
  {
    PriorityQueue localPriorityQueue = new PriorityQueue(this.blockchain.getTransactionCount(), new Comparator()
    {
      public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
      {
        long l1 = paramAnonymousTransaction1.getId().longValue();
        long l2 = paramAnonymousTransaction2.getId().longValue();
        return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
      }
    });
    Object localObject1 = this.blockchain.getAllTransactions();Object localObject2 = null;
    try
    {
      while (((DbIterator)localObject1).hasNext()) {
        localPriorityQueue.add(((DbIterator)localObject1).next());
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
            ((DbIterator)localObject1).close();
          }
          catch (Throwable localThrowable3)
          {
            localObject2.addSuppressed(localThrowable3);
          }
        } else {
          ((DbIterator)localObject1).close();
        }
      }
    }
    localObject1 = Crypto.sha256();
    while (!localPriorityQueue.isEmpty()) {
      ((MessageDigest)localObject1).update(((Transaction)localPriorityQueue.poll()).getBytes());
    }
    return ((MessageDigest)localObject1).digest();
  }
  
  private void pushBlock(BlockImpl paramBlockImpl)
    throws BlockchainProcessor.BlockNotAcceptedException
  {
    int i = Convert.getEpochTime();
    synchronized (this.blockchain)
    {
      try
      {
        BlockImpl localBlockImpl = this.blockchain.getLastBlock();
        if (!localBlockImpl.getId().equals(paramBlockImpl.getPreviousBlockId())) {
          throw new BlockchainProcessor.BlockOutOfOrderException("Previous block id doesn't match");
        }
        if (paramBlockImpl.getVersion() != (localBlockImpl.getHeight() < 30000 ? 1 : 2)) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Invalid version " + paramBlockImpl.getVersion());
        }
        if (localBlockImpl.getHeight() == 30000)
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
              throw new BlockchainProcessor.BlockNotAcceptedException("Checksum failed");
            }
            Logger.logMessage("Checksum passed at block 30000");
          }
        }
        if ((paramBlockImpl.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlockImpl.getBytes()), paramBlockImpl.getPreviousBlockHash()))) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Previous block hash doesn't match");
        }
        if ((paramBlockImpl.getTimestamp() > i + 15) || (paramBlockImpl.getTimestamp() <= localBlockImpl.getTimestamp())) {
          throw new BlockchainProcessor.BlockOutOfOrderException("Invalid timestamp: " + paramBlockImpl.getTimestamp() + " current time is " + i + ", previous block timestamp is " + localBlockImpl.getTimestamp());
        }
        if ((paramBlockImpl.getId().equals(Long.valueOf(0L))) || (BlockDb.hasBlock(paramBlockImpl.getId()))) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate block or invalid id");
        }
        if ((!paramBlockImpl.verifyGenerationSignature()) || (!paramBlockImpl.verifyBlockSignature())) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Signature verification failed");
        }
        Object localObject1 = new HashMap();
        HashMap localHashMap1 = new HashMap();
        HashMap localHashMap2 = new HashMap();
        int j = 0;int k = 0;
        MessageDigest localMessageDigest = Crypto.sha256();
        for (Object localObject2 = paramBlockImpl.getTransactions().iterator(); ((Iterator)localObject2).hasNext();)
        {
          localObject3 = (TransactionImpl)((Iterator)localObject2).next();
          if ((((TransactionImpl)localObject3).getTimestamp() > i + 15) || (((TransactionImpl)localObject3).getTimestamp() > paramBlockImpl.getTimestamp() + 15) || ((((TransactionImpl)localObject3).getExpiration() < paramBlockImpl.getTimestamp()) && (localBlockImpl.getHeight() != 303))) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid transaction timestamp " + ((TransactionImpl)localObject3).getTimestamp() + " for transaction " + ((TransactionImpl)localObject3).getStringId() + ", current time is " + i + ", block timestamp is " + paramBlockImpl.getTimestamp());
          }
          if (TransactionDb.hasTransaction(((TransactionImpl)localObject3).getId())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Transaction " + ((TransactionImpl)localObject3).getStringId() + " is already in the blockchain");
          }
          if ((((TransactionImpl)localObject3).getReferencedTransactionId() != null) && (!TransactionDb.hasTransaction(((TransactionImpl)localObject3).getReferencedTransactionId())) && (Collections.binarySearch(paramBlockImpl.getTransactionIds(), ((TransactionImpl)localObject3).getReferencedTransactionId()) < 0)) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Missing referenced transaction " + Convert.toUnsignedLong(((TransactionImpl)localObject3).getReferencedTransactionId()) + " for transaction " + ((TransactionImpl)localObject3).getStringId());
          }
          if ((this.transactionProcessor.getUnconfirmedTransaction(((TransactionImpl)localObject3).getId()) != null) && (!((TransactionImpl)localObject3).verify())) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Signature verification failed for transaction " + ((TransactionImpl)localObject3).getStringId());
          }
          if (((TransactionImpl)localObject3).getId().equals(Long.valueOf(0L))) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Invalid transaction id");
          }
          if (((TransactionImpl)localObject3).isDuplicate((Map)localObject1)) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Transaction is a duplicate: " + ((TransactionImpl)localObject3).getStringId());
          }
          try
          {
            ((TransactionImpl)localObject3).validateAttachment();
          }
          catch (NxtException.ValidationException localValidationException)
          {
            throw new BlockchainProcessor.BlockNotAcceptedException(localValidationException.getMessage());
          }
          j += ((TransactionImpl)localObject3).getAmount();
          
          ((TransactionImpl)localObject3).updateTotals(localHashMap1, localHashMap2);
          
          k += ((TransactionImpl)localObject3).getFee();
          
          localMessageDigest.update(((TransactionImpl)localObject3).getBytes());
        }
        Object localObject3;
        if ((j != paramBlockImpl.getTotalAmount()) || (k != paramBlockImpl.getTotalFee())) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Total amount or fee don't match transaction totals");
        }
        if (!Arrays.equals(localMessageDigest.digest(), paramBlockImpl.getPayloadHash())) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Payload hash doesn't match");
        }
        for (localObject2 = localHashMap1.entrySet().iterator(); ((Iterator)localObject2).hasNext();)
        {
          localObject3 = (Map.Entry)((Iterator)localObject2).next();
          localAccount = Account.getAccount((Long)((Map.Entry)localObject3).getKey());
          if (localAccount.getBalance() < ((Long)((Map.Entry)localObject3).getValue()).longValue()) {
            throw new BlockchainProcessor.BlockNotAcceptedException("Not enough funds in sender account: " + Convert.toUnsignedLong(localAccount.getId()));
          }
        }
        Account localAccount;
        for (localObject2 = localHashMap2.entrySet().iterator(); ((Iterator)localObject2).hasNext();)
        {
          localObject3 = (Map.Entry)((Iterator)localObject2).next();
          localAccount = Account.getAccount((Long)((Map.Entry)localObject3).getKey());
          for (Map.Entry localEntry : ((Map)((Map.Entry)localObject3).getValue()).entrySet())
          {
            Long localLong1 = (Long)localEntry.getKey();
            Long localLong2 = (Long)localEntry.getValue();
            if (localAccount.getAssetBalance(localLong1) < localLong2.longValue()) {
              throw new BlockchainProcessor.BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.toUnsignedLong(localAccount.getId()));
            }
          }
        }
        paramBlockImpl.setPrevious(localBlockImpl);
        
        localObject2 = this.transactionProcessor.checkTransactionHashes(paramBlockImpl);
        if (localObject2 != null) {
          throw new BlockchainProcessor.BlockNotAcceptedException("Duplicate hash of transaction " + ((Transaction)localObject2).getStringId());
        }
        addBlock(paramBlockImpl);
        
        this.transactionProcessor.apply(paramBlockImpl);
        
        this.transactionProcessor.updateUnconfirmedTransactions(paramBlockImpl);
      }
      catch (RuntimeException localRuntimeException)
      {
        Logger.logMessage("Error pushing block", localRuntimeException);
        throw new BlockchainProcessor.BlockNotAcceptedException(localRuntimeException.toString());
      }
    }
    if (paramBlockImpl.getTimestamp() >= Convert.getEpochTime() - 15)
    {
      ??? = paramBlockImpl.getJSONObject();
      ((JSONObject)???).put("requestType", "processBlock");
      Peers.sendToSomePeers((JSONObject)???);
    }
    this.blockListeners.notify(paramBlockImpl, BlockchainProcessor.Event.BLOCK_PUSHED);
  }
  
  private boolean popLastBlock()
    throws TransactionType.UndoNotSupportedException
  {
    try
    {
      BlockImpl localBlockImpl1;
      synchronized (this.blockchain)
      {
        localBlockImpl1 = this.blockchain.getLastBlock();
        Logger.logDebugMessage("Will pop block " + localBlockImpl1.getStringId() + " at height " + localBlockImpl1.getHeight());
        if (localBlockImpl1.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
          return false;
        }
        BlockImpl localBlockImpl2 = BlockDb.findBlock(localBlockImpl1.getPreviousBlockId());
        if (localBlockImpl2 == null)
        {
          Logger.logMessage("Previous block is null");
          throw new IllegalStateException();
        }
        this.blockchain.setLastBlock(localBlockImpl1, localBlockImpl2);
        Account localAccount = Account.getAccount(localBlockImpl1.getGeneratorId());
        localAccount.undo(localBlockImpl1.getHeight());
        localAccount.addToBalanceAndUnconfirmedBalance(-localBlockImpl1.getTotalFee() * 100L);
        this.transactionProcessor.undo(localBlockImpl1);
        BlockDb.deleteBlock(localBlockImpl1.getId());
      }
      this.blockListeners.notify(localBlockImpl1, BlockchainProcessor.Event.BLOCK_POPPED);
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logMessage("Error popping last block", localRuntimeException);
      return false;
    }
    return true;
  }
  
  void generateBlock(String paramString)
  {
    TreeSet localTreeSet = new TreeSet();
    for (Object localObject1 = this.transactionProcessor.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (TransactionImpl)((Iterator)localObject1).next();
      if ((((TransactionImpl)localObject2).getReferencedTransactionId() == null) || (TransactionDb.hasTransaction(((TransactionImpl)localObject2).getReferencedTransactionId()))) {
        localTreeSet.add(localObject2);
      }
    }
    localObject1 = new TreeMap();
    Object localObject2 = new HashMap();
    HashMap localHashMap = new HashMap();
    
    int i = 0;
    int j = 0;
    int k = 0;
    
    int m = Convert.getEpochTime();
    Object localObject7;
    while (k <= 32640)
    {
      int n = ((SortedMap)localObject1).size();
      for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (TransactionImpl)((Iterator)localObject3).next();
        
        int i1 = ((TransactionImpl)localObject4).getSize();
        if ((((SortedMap)localObject1).get(((TransactionImpl)localObject4).getId()) == null) && (k + i1 <= 32640))
        {
          localObject6 = ((TransactionImpl)localObject4).getSenderId();
          localObject7 = (Long)localHashMap.get(localObject6);
          if (localObject7 == null) {
            localObject7 = Long.valueOf(0L);
          }
          long l = (((TransactionImpl)localObject4).getAmount() + ((TransactionImpl)localObject4).getFee()) * 100L;
          if (((Long)localObject7).longValue() + l <= Account.getAccount((Long)localObject6).getBalance()) {
            if ((((TransactionImpl)localObject4).getTimestamp() <= m + 15) && (((TransactionImpl)localObject4).getExpiration() >= m) && 
            


              (!((TransactionImpl)localObject4).isDuplicate((Map)localObject2)))
            {
              try
              {
                ((TransactionImpl)localObject4).validateAttachment();
              }
              catch (NxtException.ValidationException localValidationException1) {}
              continue;
              

              localHashMap.put(localObject6, Long.valueOf(((Long)localObject7).longValue() + l));
              
              ((SortedMap)localObject1).put(((TransactionImpl)localObject4).getId(), localObject4);
              k += i1;
              i += ((TransactionImpl)localObject4).getAmount();
              j += ((TransactionImpl)localObject4).getFee();
            }
          }
        }
      }
      if (((SortedMap)localObject1).size() == n) {
        break;
      }
    }
    byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
    
    Object localObject3 = Crypto.sha256();
    for (Object localObject4 = ((SortedMap)localObject1).values().iterator(); ((Iterator)localObject4).hasNext();)
    {
      localObject5 = (Transaction)((Iterator)localObject4).next();
      ((MessageDigest)localObject3).update(((Transaction)localObject5).getBytes());
    }
    Object localObject5;
    localObject4 = ((MessageDigest)localObject3).digest();
    

    Object localObject6 = this.blockchain.getLastBlock();
    if (((BlockImpl)localObject6).getHeight() < 30000)
    {
      localObject5 = Crypto.sign(((BlockImpl)localObject6).getGenerationSignature(), paramString);
    }
    else
    {
      ((MessageDigest)localObject3).update(((BlockImpl)localObject6).getGenerationSignature());
      localObject5 = ((MessageDigest)localObject3).digest(arrayOfByte1);
    }
    int i2 = ((BlockImpl)localObject6).getHeight() < 30000 ? 1 : 2;
    byte[] arrayOfByte2 = i2 == 1 ? null : Crypto.sha256().digest(((BlockImpl)localObject6).getBytes());
    try
    {
      localObject7 = new BlockImpl(i2, m, ((BlockImpl)localObject6).getId(), i, j, k, (byte[])localObject4, arrayOfByte1, (byte[])localObject5, null, arrayOfByte2, new ArrayList(((SortedMap)localObject1).values()));
    }
    catch (NxtException.ValidationException localValidationException2)
    {
      Logger.logMessage("Error generating block", localValidationException2);
      return;
    }
    ((BlockImpl)localObject7).sign(paramString);
    
    ((BlockImpl)localObject7).setPrevious((BlockImpl)localObject6);
    try
    {
      if ((((BlockImpl)localObject7).verifyBlockSignature()) && (((BlockImpl)localObject7).verifyGenerationSignature()))
      {
        pushBlock((BlockImpl)localObject7);
        this.blockListeners.notify(localObject7, BlockchainProcessor.Event.BLOCK_GENERATED);
        Logger.logDebugMessage("Account " + Convert.toUnsignedLong(((BlockImpl)localObject7).getGeneratorId()) + " generated block " + ((BlockImpl)localObject7).getStringId());
      }
      else
      {
        Logger.logDebugMessage("Account " + Convert.toUnsignedLong(((BlockImpl)localObject7).getGeneratorId()) + " generated an incorrect block.");
      }
    }
    catch (BlockchainProcessor.BlockNotAcceptedException localBlockNotAcceptedException)
    {
      Logger.logDebugMessage("Generate block failed: " + localBlockNotAcceptedException.getMessage());
    }
  }
  
  private BlockImpl parseBlock(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    try
    {
      int i = ((Long)paramJSONObject.get("version")).intValue();
      int j = ((Long)paramJSONObject.get("timestamp")).intValue();
      Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("previousBlock"));
      int k = ((Long)paramJSONObject.get("totalAmount")).intValue();
      int m = ((Long)paramJSONObject.get("totalFee")).intValue();
      int n = ((Long)paramJSONObject.get("payloadLength")).intValue();
      byte[] arrayOfByte1 = Convert.parseHexString((String)paramJSONObject.get("payloadHash"));
      byte[] arrayOfByte2 = Convert.parseHexString((String)paramJSONObject.get("generatorPublicKey"));
      byte[] arrayOfByte3 = Convert.parseHexString((String)paramJSONObject.get("generationSignature"));
      byte[] arrayOfByte4 = Convert.parseHexString((String)paramJSONObject.get("blockSignature"));
      byte[] arrayOfByte5 = i == 1 ? null : Convert.parseHexString((String)paramJSONObject.get("previousBlockHash"));
      
      TreeMap localTreeMap = new TreeMap();
      JSONArray localJSONArray = (JSONArray)paramJSONObject.get("transactions");
      for (Object localObject : localJSONArray)
      {
        TransactionImpl localTransactionImpl = this.transactionProcessor.parseTransaction((JSONObject)localObject);
        if (localTreeMap.put(localTransactionImpl.getId(), localTransactionImpl) != null) {
          throw new NxtException.ValidationException("Block contains duplicate transactions: " + localTransactionImpl.getStringId());
        }
      }
      return new BlockImpl(i, j, localLong, k, m, n, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4, arrayOfByte5, new ArrayList(localTreeMap.values()));
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString(), localRuntimeException);
    }
  }
  
  private void scan()
  {
    synchronized (this.blockchain)
    {
      Logger.logDebugMessage("Scanning blockchain...");
      Account.clear();
      Alias.clear();
      Asset.clear();
      Order.clear();
      Poll.clear();
      Trade.clear();
      Vote.clear();
      this.transactionProcessor.clear();
      try
      {
        Connection localConnection = Db.getConnection();Object localObject1 = null;
        try
        {
          PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");Object localObject2 = null;
          try
          {
            Long localLong = Genesis.GENESIS_BLOCK_ID;
            
            ResultSet localResultSet = localPreparedStatement.executeQuery();
            while (localResultSet.next())
            {
              BlockImpl localBlockImpl = BlockDb.findBlock(localConnection, localResultSet);
              if (!localBlockImpl.getId().equals(localLong)) {
                throw new NxtException.ValidationException("Database blocks in the wrong order!");
              }
              this.blockchain.setLastBlock(localBlockImpl);
              this.transactionProcessor.apply(localBlockImpl);
              this.blockListeners.notify(localBlockImpl, BlockchainProcessor.Event.BLOCK_SCANNED);
              localLong = localBlockImpl.getNextBlockId();
            }
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
          if (localConnection != null) {
            if (localObject1 != null) {
              try
              {
                localConnection.close();
              }
              catch (Throwable localThrowable6)
              {
                localObject1.addSuppressed(localThrowable6);
              }
            } else {
              localConnection.close();
            }
          }
        }
      }
      catch (NxtException.ValidationException|SQLException localValidationException)
      {
        throw new RuntimeException(localValidationException.toString(), localValidationException);
      }
      Logger.logDebugMessage("...done");
    }
  }
}