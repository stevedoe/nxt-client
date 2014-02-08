package nxt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.SortedMap;
import java.util.TreeMap;
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
import nxt.util.DbIterator;
import nxt.util.DbIterator.ResultSetReader;
import nxt.util.DbUtils;
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
  private static final AtomicInteger transactionCounter = new AtomicInteger();
  private static final ConcurrentMap<Long, Transaction> doubleSpendingTransactions = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Transaction> unconfirmedTransactions = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Transaction> nonBroadcastedTransactions = new ConcurrentHashMap();
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
                  for (Object localObject2 : localJSONArray1)
                  {
                    localObject4 = Convert.parseUnsignedLong((String)localObject2);
                    if (Block.hasBlock((Long)localObject4))
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
                      if (!Block.hasBlock((Long)localObject6)) {
                        break;
                      }
                      localObject1 = localObject6;
                    }
                  } while (i == j);
                  Block localBlock1 = Block.findBlock((Long)localObject1);
                  if (((Block)Blockchain.lastBlock.get()).getHeight() - localBlock1.getHeight() < 720)
                  {
                    Object localObject3 = localObject1;
                    localObject4 = new LinkedList();
                    localObject5 = new HashMap();
                    Object localObject7;
                    for (;;)
                    {
                      localObject6 = new JSONObject();
                      ((JSONObject)localObject6).put("requestType", "getNextBlocks");
                      ((JSONObject)localObject6).put("blockId", Convert.convert((Long)localObject3));
                      localJSONObject1 = localPeer.send(JSON.prepareRequest((JSONObject)localObject6));
                      if (localJSONObject1 == null) {
                        break;
                      }
                      JSONArray localJSONArray2 = (JSONArray)localJSONObject1.get("nextBlocks");
                      if ((localJSONArray2 == null) || (localJSONArray2.size() == 0)) {
                        break;
                      }
                      synchronized (Blockchain.class)
                      {
                        for (localObject7 = localJSONArray2.iterator(); ((Iterator)localObject7).hasNext();)
                        {
                          Object localObject8 = ((Iterator)localObject7).next();
                          JSONObject localJSONObject2 = (JSONObject)localObject8;
                          Block localBlock2;
                          try
                          {
                            localBlock2 = Block.getBlock(localJSONObject2);
                          }
                          catch (NxtException.ValidationException localValidationException1)
                          {
                            localPeer.blacklist(localValidationException1);
                            return;
                          }
                          localObject3 = localBlock2.getId();
                          JSONArray localJSONArray3;
                          if (((Block)Blockchain.lastBlock.get()).getId().equals(localBlock2.getPreviousBlockId()))
                          {
                            localJSONArray3 = (JSONArray)localJSONObject2.get("transactions");
                            try
                            {
                              Transaction[] arrayOfTransaction = new Transaction[localJSONArray3.size()];
                              for (int n = 0; n < arrayOfTransaction.length; n++) {
                                arrayOfTransaction[n] = Transaction.getTransaction((JSONObject)localJSONArray3.get(n));
                              }
                              try
                              {
                                Blockchain.pushBlock(localBlock2, arrayOfTransaction);
                              }
                              catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException2)
                              {
                                Logger.logDebugMessage("Failed to accept block " + localBlock2.getStringId() + " at height " + ((Block)Blockchain.lastBlock.get()).getHeight() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
                                

                                localPeer.blacklist(localBlockNotAcceptedException2);
                                return;
                              }
                            }
                            catch (NxtException.ValidationException localValidationException2)
                            {
                              localPeer.blacklist(localValidationException2);
                              return;
                            }
                          }
                          else if ((!Block.hasBlock(localBlock2.getId())) && (localBlock2.transactionIds.length <= 255))
                          {
                            ((LinkedList)localObject4).add(localBlock2);
                            
                            localJSONArray3 = (JSONArray)localJSONObject2.get("transactions");
                            try
                            {
                              for (int m = 0; m < localBlock2.transactionIds.length; m++)
                              {
                                Transaction localTransaction = Transaction.getTransaction((JSONObject)localJSONArray3.get(m));
                                localBlock2.transactionIds[m] = localTransaction.getId();
                                localBlock2.blockTransactions[m] = localTransaction;
                                ((HashMap)localObject5).put(localBlock2.transactionIds[m], localTransaction);
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
                    if ((!((LinkedList)localObject4).isEmpty()) && (((Block)Blockchain.lastBlock.get()).getHeight() - localBlock1.getHeight() < 720)) {
                      synchronized (Blockchain.class)
                      {
                        localBigInteger1 = ((Block)Blockchain.lastBlock.get()).getCumulativeDifficulty();
                        for (;;)
                        {
                          int k;
                          try
                          {
                            while ((!((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) && (Blockchain.access$500())) {}
                            if (((Block)Blockchain.lastBlock.get()).getId().equals(localObject1)) {
                              for (??? = ((LinkedList)localObject4).iterator(); ((Iterator)???).hasNext();)
                              {
                                localObject7 = (Block)((Iterator)???).next();
                                if (((Block)Blockchain.lastBlock.get()).getId().equals(((Block)localObject7).getPreviousBlockId())) {
                                  try
                                  {
                                    Blockchain.pushBlock((Block)localObject7, ((Block)localObject7).blockTransactions);
                                  }
                                  catch (Blockchain.BlockNotAcceptedException localBlockNotAcceptedException1)
                                  {
                                    Logger.logDebugMessage("Failed to push future block " + ((Block)localObject7).getStringId() + " received from " + localPeer.getPeerAddress() + ", blacklisting");
                                    
                                    localPeer.blacklist(localBlockNotAcceptedException1);
                                    break;
                                  }
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
                          if (localBlock1.getNextBlockId() != null) {
                            Block.deleteBlock(localBlock1.getNextBlockId());
                          }
                          Logger.logMessage("Re-scanning blockchain...");
                          Blockchain.access$600();
                          Logger.logMessage("...Done");
                        }
                      }
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
            if ((Blockchain.unconfirmedTransactions.get(localTransaction.getId()) == null) && (!Transaction.hasTransaction(localTransaction.getId()))) {
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
  
  public static DbIterator<Block> getAllBlocks()
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  public static DbIterator<Block> getAllBlocks(Account paramAccount, int paramInt)
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_public_key = ? ORDER BY db_id ASC");
      localPreparedStatement.setInt(1, paramInt);
      localPreparedStatement.setBytes(2, paramAccount.getPublicKey());
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public Block get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return Block.getBlock(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  /* Error */
  public static int getBlockCount()
  {
    // Byte code:
    //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_0
    //   4: aconst_null
    //   5: astore_1
    //   6: aload_0
    //   7: ldc 29
    //   9: invokeinterface 12 2 0
    //   14: astore_2
    //   15: aconst_null
    //   16: astore_3
    //   17: aload_2
    //   18: invokeinterface 30 1 0
    //   23: astore 4
    //   25: aload 4
    //   27: invokeinterface 31 1 0
    //   32: pop
    //   33: aload 4
    //   35: iconst_1
    //   36: invokeinterface 32 2 0
    //   41: istore 5
    //   43: aload_2
    //   44: ifnull +33 -> 77
    //   47: aload_3
    //   48: ifnull +23 -> 71
    //   51: aload_2
    //   52: invokeinterface 33 1 0
    //   57: goto +20 -> 77
    //   60: astore 6
    //   62: aload_3
    //   63: aload 6
    //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   68: goto +9 -> 77
    //   71: aload_2
    //   72: invokeinterface 33 1 0
    //   77: aload_0
    //   78: ifnull +33 -> 111
    //   81: aload_1
    //   82: ifnull +23 -> 105
    //   85: aload_0
    //   86: invokeinterface 36 1 0
    //   91: goto +20 -> 111
    //   94: astore 6
    //   96: aload_1
    //   97: aload 6
    //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   102: goto +9 -> 111
    //   105: aload_0
    //   106: invokeinterface 36 1 0
    //   111: iload 5
    //   113: ireturn
    //   114: astore 4
    //   116: aload 4
    //   118: astore_3
    //   119: aload 4
    //   121: athrow
    //   122: astore 7
    //   124: aload_2
    //   125: ifnull +33 -> 158
    //   128: aload_3
    //   129: ifnull +23 -> 152
    //   132: aload_2
    //   133: invokeinterface 33 1 0
    //   138: goto +20 -> 158
    //   141: astore 8
    //   143: aload_3
    //   144: aload 8
    //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   149: goto +9 -> 158
    //   152: aload_2
    //   153: invokeinterface 33 1 0
    //   158: aload 7
    //   160: athrow
    //   161: astore_2
    //   162: aload_2
    //   163: astore_1
    //   164: aload_2
    //   165: athrow
    //   166: astore 9
    //   168: aload_0
    //   169: ifnull +33 -> 202
    //   172: aload_1
    //   173: ifnull +23 -> 196
    //   176: aload_0
    //   177: invokeinterface 36 1 0
    //   182: goto +20 -> 202
    //   185: astore 10
    //   187: aload_1
    //   188: aload 10
    //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   193: goto +9 -> 202
    //   196: aload_0
    //   197: invokeinterface 36 1 0
    //   202: aload 9
    //   204: athrow
    //   205: astore_0
    //   206: new 20	java/lang/RuntimeException
    //   209: dup
    //   210: aload_0
    //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
    //   214: aload_0
    //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   218: athrow
    // Line number table:
    //   Java source line #557	-> byte code offset #0
    //   Java source line #558	-> byte code offset #17
    //   Java source line #559	-> byte code offset #25
    //   Java source line #560	-> byte code offset #33
    //   Java source line #561	-> byte code offset #43
    //   Java source line #557	-> byte code offset #114
    //   Java source line #561	-> byte code offset #122
    //   Java source line #557	-> byte code offset #161
    //   Java source line #561	-> byte code offset #166
    //   Java source line #562	-> byte code offset #206
    // Local variable table:
    //   start	length	slot	name	signature
    //   3	194	0	localConnection	Connection
    //   205	10	0	localSQLException	SQLException
    //   5	183	1	localObject1	Object
    //   14	139	2	localPreparedStatement	PreparedStatement
    //   161	4	2	localThrowable1	Throwable
    //   16	128	3	localObject2	Object
    //   23	11	4	localResultSet	ResultSet
    //   114	6	4	localThrowable2	Throwable
    //   60	4	6	localThrowable3	Throwable
    //   94	4	6	localThrowable4	Throwable
    //   122	37	7	localObject3	Object
    //   141	4	8	localThrowable5	Throwable
    //   166	37	9	localObject4	Object
    //   185	4	10	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   51	57	60	java/lang/Throwable
    //   85	91	94	java/lang/Throwable
    //   17	43	114	java/lang/Throwable
    //   17	43	122	finally
    //   114	124	122	finally
    //   132	138	141	java/lang/Throwable
    //   6	77	161	java/lang/Throwable
    //   114	161	161	java/lang/Throwable
    //   6	77	166	finally
    //   114	168	166	finally
    //   176	182	185	java/lang/Throwable
    //   0	111	205	java/sql/SQLException
    //   114	205	205	java/sql/SQLException
  }
  
  public static DbIterator<Transaction> getAllTransactions()
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  public static DbIterator<Transaction> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt)
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement;
      if (paramByte1 >= 0)
      {
        if (paramByte2 >= 0)
        {
          localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? AND subtype = ? ORDER BY timestamp ASC");
          localPreparedStatement.setInt(1, paramInt);
          localPreparedStatement.setLong(2, paramAccount.getId().longValue());
          localPreparedStatement.setLong(3, paramAccount.getId().longValue());
          localPreparedStatement.setByte(4, paramByte1);
          localPreparedStatement.setByte(5, paramByte2);
        }
        else
        {
          localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) AND type = ? ORDER BY timestamp ASC");
          localPreparedStatement.setInt(1, paramInt);
          localPreparedStatement.setLong(2, paramAccount.getId().longValue());
          localPreparedStatement.setLong(3, paramAccount.getId().longValue());
          localPreparedStatement.setByte(4, paramByte1);
        }
      }
      else
      {
        localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction WHERE timestamp >= ? AND (recipient_id = ? OR sender_account_id = ?) ORDER BY timestamp ASC");
        localPreparedStatement.setInt(1, paramInt);
        localPreparedStatement.setLong(2, paramAccount.getId().longValue());
        localPreparedStatement.setLong(3, paramAccount.getId().longValue());
      }
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public Transaction get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return Transaction.getTransaction(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  /* Error */
  public static int getTransactionCount()
  {
    // Byte code:
    //   0: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_0
    //   4: aconst_null
    //   5: astore_1
    //   6: aload_0
    //   7: ldc 49
    //   9: invokeinterface 12 2 0
    //   14: astore_2
    //   15: aconst_null
    //   16: astore_3
    //   17: aload_2
    //   18: invokeinterface 30 1 0
    //   23: astore 4
    //   25: aload 4
    //   27: invokeinterface 31 1 0
    //   32: pop
    //   33: aload 4
    //   35: iconst_1
    //   36: invokeinterface 32 2 0
    //   41: istore 5
    //   43: aload_2
    //   44: ifnull +33 -> 77
    //   47: aload_3
    //   48: ifnull +23 -> 71
    //   51: aload_2
    //   52: invokeinterface 33 1 0
    //   57: goto +20 -> 77
    //   60: astore 6
    //   62: aload_3
    //   63: aload 6
    //   65: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   68: goto +9 -> 77
    //   71: aload_2
    //   72: invokeinterface 33 1 0
    //   77: aload_0
    //   78: ifnull +33 -> 111
    //   81: aload_1
    //   82: ifnull +23 -> 105
    //   85: aload_0
    //   86: invokeinterface 36 1 0
    //   91: goto +20 -> 111
    //   94: astore 6
    //   96: aload_1
    //   97: aload 6
    //   99: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   102: goto +9 -> 111
    //   105: aload_0
    //   106: invokeinterface 36 1 0
    //   111: iload 5
    //   113: ireturn
    //   114: astore 4
    //   116: aload 4
    //   118: astore_3
    //   119: aload 4
    //   121: athrow
    //   122: astore 7
    //   124: aload_2
    //   125: ifnull +33 -> 158
    //   128: aload_3
    //   129: ifnull +23 -> 152
    //   132: aload_2
    //   133: invokeinterface 33 1 0
    //   138: goto +20 -> 158
    //   141: astore 8
    //   143: aload_3
    //   144: aload 8
    //   146: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   149: goto +9 -> 158
    //   152: aload_2
    //   153: invokeinterface 33 1 0
    //   158: aload 7
    //   160: athrow
    //   161: astore_2
    //   162: aload_2
    //   163: astore_1
    //   164: aload_2
    //   165: athrow
    //   166: astore 9
    //   168: aload_0
    //   169: ifnull +33 -> 202
    //   172: aload_1
    //   173: ifnull +23 -> 196
    //   176: aload_0
    //   177: invokeinterface 36 1 0
    //   182: goto +20 -> 202
    //   185: astore 10
    //   187: aload_1
    //   188: aload 10
    //   190: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   193: goto +9 -> 202
    //   196: aload_0
    //   197: invokeinterface 36 1 0
    //   202: aload 9
    //   204: athrow
    //   205: astore_0
    //   206: new 20	java/lang/RuntimeException
    //   209: dup
    //   210: aload_0
    //   211: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
    //   214: aload_0
    //   215: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   218: athrow
    // Line number table:
    //   Java source line #622	-> byte code offset #0
    //   Java source line #623	-> byte code offset #17
    //   Java source line #624	-> byte code offset #25
    //   Java source line #625	-> byte code offset #33
    //   Java source line #626	-> byte code offset #43
    //   Java source line #622	-> byte code offset #114
    //   Java source line #626	-> byte code offset #122
    //   Java source line #622	-> byte code offset #161
    //   Java source line #626	-> byte code offset #166
    //   Java source line #627	-> byte code offset #206
    // Local variable table:
    //   start	length	slot	name	signature
    //   3	194	0	localConnection	Connection
    //   205	10	0	localSQLException	SQLException
    //   5	183	1	localObject1	Object
    //   14	139	2	localPreparedStatement	PreparedStatement
    //   161	4	2	localThrowable1	Throwable
    //   16	128	3	localObject2	Object
    //   23	11	4	localResultSet	ResultSet
    //   114	6	4	localThrowable2	Throwable
    //   60	4	6	localThrowable3	Throwable
    //   94	4	6	localThrowable4	Throwable
    //   122	37	7	localObject3	Object
    //   141	4	8	localThrowable5	Throwable
    //   166	37	9	localObject4	Object
    //   185	4	10	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   51	57	60	java/lang/Throwable
    //   85	91	94	java/lang/Throwable
    //   17	43	114	java/lang/Throwable
    //   17	43	122	finally
    //   114	124	122	finally
    //   132	138	141	java/lang/Throwable
    //   6	77	161	java/lang/Throwable
    //   114	161	161	java/lang/Throwable
    //   6	77	166	finally
    //   114	168	166	finally
    //   176	182	185	java/lang/Throwable
    //   0	111	205	java/sql/SQLException
    //   114	205	205	java/sql/SQLException
  }
  
  /* Error */
  public static java.util.List<Long> getBlockIdsAfter(Long paramLong, int paramInt)
  {
    // Byte code:
    //   0: iload_1
    //   1: sipush 1440
    //   4: if_icmple +13 -> 17
    //   7: new 50	java/lang/IllegalArgumentException
    //   10: dup
    //   11: ldc 51
    //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   16: athrow
    //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   20: astore_2
    //   21: aconst_null
    //   22: astore_3
    //   23: aload_2
    //   24: ldc 53
    //   26: invokeinterface 12 2 0
    //   31: astore 4
    //   33: aconst_null
    //   34: astore 5
    //   36: aload_2
    //   37: ldc 54
    //   39: invokeinterface 12 2 0
    //   44: astore 6
    //   46: aconst_null
    //   47: astore 7
    //   49: aload 4
    //   51: iconst_1
    //   52: aload_0
    //   53: invokevirtual 42	java/lang/Long:longValue	()J
    //   56: invokeinterface 43 4 0
    //   61: aload 4
    //   63: invokeinterface 30 1 0
    //   68: astore 8
    //   70: aload 8
    //   72: invokeinterface 31 1 0
    //   77: ifne +130 -> 207
    //   80: aload 8
    //   82: invokeinterface 55 1 0
    //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
    //   90: astore 9
    //   92: aload 6
    //   94: ifnull +37 -> 131
    //   97: aload 7
    //   99: ifnull +25 -> 124
    //   102: aload 6
    //   104: invokeinterface 33 1 0
    //   109: goto +22 -> 131
    //   112: astore 10
    //   114: aload 7
    //   116: aload 10
    //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   121: goto +10 -> 131
    //   124: aload 6
    //   126: invokeinterface 33 1 0
    //   131: aload 4
    //   133: ifnull +37 -> 170
    //   136: aload 5
    //   138: ifnull +25 -> 163
    //   141: aload 4
    //   143: invokeinterface 33 1 0
    //   148: goto +22 -> 170
    //   151: astore 10
    //   153: aload 5
    //   155: aload 10
    //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   160: goto +10 -> 170
    //   163: aload 4
    //   165: invokeinterface 33 1 0
    //   170: aload_2
    //   171: ifnull +33 -> 204
    //   174: aload_3
    //   175: ifnull +23 -> 198
    //   178: aload_2
    //   179: invokeinterface 36 1 0
    //   184: goto +20 -> 204
    //   187: astore 10
    //   189: aload_3
    //   190: aload 10
    //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   195: goto +9 -> 204
    //   198: aload_2
    //   199: invokeinterface 36 1 0
    //   204: aload 9
    //   206: areturn
    //   207: new 57	java/util/ArrayList
    //   210: dup
    //   211: invokespecial 58	java/util/ArrayList:<init>	()V
    //   214: astore 9
    //   216: aload 8
    //   218: ldc 59
    //   220: invokeinterface 60 2 0
    //   225: istore 10
    //   227: aload 6
    //   229: iconst_1
    //   230: iload 10
    //   232: invokeinterface 24 3 0
    //   237: aload 6
    //   239: iconst_2
    //   240: iload_1
    //   241: invokeinterface 24 3 0
    //   246: aload 6
    //   248: invokeinterface 30 1 0
    //   253: astore 8
    //   255: aload 8
    //   257: invokeinterface 31 1 0
    //   262: ifeq +26 -> 288
    //   265: aload 9
    //   267: aload 8
    //   269: ldc 61
    //   271: invokeinterface 62 2 0
    //   276: invokestatic 63	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   279: invokeinterface 64 2 0
    //   284: pop
    //   285: goto -30 -> 255
    //   288: aload 8
    //   290: invokeinterface 55 1 0
    //   295: aload 9
    //   297: astore 11
    //   299: aload 6
    //   301: ifnull +37 -> 338
    //   304: aload 7
    //   306: ifnull +25 -> 331
    //   309: aload 6
    //   311: invokeinterface 33 1 0
    //   316: goto +22 -> 338
    //   319: astore 12
    //   321: aload 7
    //   323: aload 12
    //   325: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   328: goto +10 -> 338
    //   331: aload 6
    //   333: invokeinterface 33 1 0
    //   338: aload 4
    //   340: ifnull +37 -> 377
    //   343: aload 5
    //   345: ifnull +25 -> 370
    //   348: aload 4
    //   350: invokeinterface 33 1 0
    //   355: goto +22 -> 377
    //   358: astore 12
    //   360: aload 5
    //   362: aload 12
    //   364: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   367: goto +10 -> 377
    //   370: aload 4
    //   372: invokeinterface 33 1 0
    //   377: aload_2
    //   378: ifnull +33 -> 411
    //   381: aload_3
    //   382: ifnull +23 -> 405
    //   385: aload_2
    //   386: invokeinterface 36 1 0
    //   391: goto +20 -> 411
    //   394: astore 12
    //   396: aload_3
    //   397: aload 12
    //   399: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   402: goto +9 -> 411
    //   405: aload_2
    //   406: invokeinterface 36 1 0
    //   411: aload 11
    //   413: areturn
    //   414: astore 8
    //   416: aload 8
    //   418: astore 7
    //   420: aload 8
    //   422: athrow
    //   423: astore 13
    //   425: aload 6
    //   427: ifnull +37 -> 464
    //   430: aload 7
    //   432: ifnull +25 -> 457
    //   435: aload 6
    //   437: invokeinterface 33 1 0
    //   442: goto +22 -> 464
    //   445: astore 14
    //   447: aload 7
    //   449: aload 14
    //   451: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   454: goto +10 -> 464
    //   457: aload 6
    //   459: invokeinterface 33 1 0
    //   464: aload 13
    //   466: athrow
    //   467: astore 6
    //   469: aload 6
    //   471: astore 5
    //   473: aload 6
    //   475: athrow
    //   476: astore 15
    //   478: aload 4
    //   480: ifnull +37 -> 517
    //   483: aload 5
    //   485: ifnull +25 -> 510
    //   488: aload 4
    //   490: invokeinterface 33 1 0
    //   495: goto +22 -> 517
    //   498: astore 16
    //   500: aload 5
    //   502: aload 16
    //   504: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   507: goto +10 -> 517
    //   510: aload 4
    //   512: invokeinterface 33 1 0
    //   517: aload 15
    //   519: athrow
    //   520: astore 4
    //   522: aload 4
    //   524: astore_3
    //   525: aload 4
    //   527: athrow
    //   528: astore 17
    //   530: aload_2
    //   531: ifnull +33 -> 564
    //   534: aload_3
    //   535: ifnull +23 -> 558
    //   538: aload_2
    //   539: invokeinterface 36 1 0
    //   544: goto +20 -> 564
    //   547: astore 18
    //   549: aload_3
    //   550: aload 18
    //   552: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   555: goto +9 -> 564
    //   558: aload_2
    //   559: invokeinterface 36 1 0
    //   564: aload 17
    //   566: athrow
    //   567: astore_2
    //   568: new 20	java/lang/RuntimeException
    //   571: dup
    //   572: aload_2
    //   573: invokevirtual 21	java/sql/SQLException:toString	()Ljava/lang/String;
    //   576: aload_2
    //   577: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   580: athrow
    // Line number table:
    //   Java source line #632	-> byte code offset #0
    //   Java source line #633	-> byte code offset #7
    //   Java source line #635	-> byte code offset #17
    //   Java source line #636	-> byte code offset #23
    //   Java source line #635	-> byte code offset #33
    //   Java source line #637	-> byte code offset #36
    //   Java source line #635	-> byte code offset #46
    //   Java source line #638	-> byte code offset #49
    //   Java source line #639	-> byte code offset #61
    //   Java source line #640	-> byte code offset #70
    //   Java source line #641	-> byte code offset #80
    //   Java source line #642	-> byte code offset #87
    //   Java source line #654	-> byte code offset #92
    //   Java source line #644	-> byte code offset #207
    //   Java source line #645	-> byte code offset #216
    //   Java source line #646	-> byte code offset #227
    //   Java source line #647	-> byte code offset #237
    //   Java source line #648	-> byte code offset #246
    //   Java source line #649	-> byte code offset #255
    //   Java source line #650	-> byte code offset #265
    //   Java source line #652	-> byte code offset #288
    //   Java source line #653	-> byte code offset #295
    //   Java source line #654	-> byte code offset #299
    //   Java source line #635	-> byte code offset #414
    //   Java source line #654	-> byte code offset #423
    //   Java source line #635	-> byte code offset #467
    //   Java source line #654	-> byte code offset #476
    //   Java source line #635	-> byte code offset #520
    //   Java source line #654	-> byte code offset #528
    //   Java source line #655	-> byte code offset #568
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	581	0	paramLong	Long
    //   0	581	1	paramInt	int
    //   20	539	2	localConnection	Connection
    //   567	10	2	localSQLException	SQLException
    //   22	528	3	localObject1	Object
    //   31	480	4	localPreparedStatement1	PreparedStatement
    //   520	6	4	localThrowable1	Throwable
    //   34	467	5	localObject2	Object
    //   44	414	6	localPreparedStatement2	PreparedStatement
    //   467	7	6	localThrowable2	Throwable
    //   47	401	7	localObject3	Object
    //   68	221	8	localResultSet	ResultSet
    //   414	7	8	localThrowable3	Throwable
    //   90	206	9	localObject4	Object
    //   112	5	10	localThrowable4	Throwable
    //   151	5	10	localThrowable5	Throwable
    //   187	4	10	localThrowable6	Throwable
    //   225	6	10	i	int
    //   297	115	11	localObject5	Object
    //   319	5	12	localThrowable7	Throwable
    //   358	5	12	localThrowable8	Throwable
    //   394	4	12	localThrowable9	Throwable
    //   423	42	13	localObject6	Object
    //   445	5	14	localThrowable10	Throwable
    //   476	42	15	localObject7	Object
    //   498	5	16	localThrowable11	Throwable
    //   528	37	17	localObject8	Object
    //   547	4	18	localThrowable12	Throwable
    // Exception table:
    //   from	to	target	type
    //   102	109	112	java/lang/Throwable
    //   141	148	151	java/lang/Throwable
    //   178	184	187	java/lang/Throwable
    //   309	316	319	java/lang/Throwable
    //   348	355	358	java/lang/Throwable
    //   385	391	394	java/lang/Throwable
    //   49	92	414	java/lang/Throwable
    //   207	299	414	java/lang/Throwable
    //   49	92	423	finally
    //   207	299	423	finally
    //   414	425	423	finally
    //   435	442	445	java/lang/Throwable
    //   36	131	467	java/lang/Throwable
    //   207	338	467	java/lang/Throwable
    //   414	467	467	java/lang/Throwable
    //   36	131	476	finally
    //   207	338	476	finally
    //   414	478	476	finally
    //   488	495	498	java/lang/Throwable
    //   23	170	520	java/lang/Throwable
    //   207	377	520	java/lang/Throwable
    //   414	520	520	java/lang/Throwable
    //   23	170	528	finally
    //   207	377	528	finally
    //   414	530	528	finally
    //   538	544	547	java/lang/Throwable
    //   17	204	567	java/sql/SQLException
    //   207	411	567	java/sql/SQLException
    //   414	567	567	java/sql/SQLException
  }
  
  /* Error */
  public static java.util.List<Block> getBlocksAfter(Long paramLong, int paramInt)
  {
    // Byte code:
    //   0: iload_1
    //   1: sipush 1440
    //   4: if_icmple +13 -> 17
    //   7: new 50	java/lang/IllegalArgumentException
    //   10: dup
    //   11: ldc 51
    //   13: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   16: athrow
    //   17: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   20: astore_2
    //   21: aconst_null
    //   22: astore_3
    //   23: aload_2
    //   24: ldc 53
    //   26: invokeinterface 12 2 0
    //   31: astore 4
    //   33: aconst_null
    //   34: astore 5
    //   36: aload_2
    //   37: ldc 65
    //   39: invokeinterface 12 2 0
    //   44: astore 6
    //   46: aconst_null
    //   47: astore 7
    //   49: aload 4
    //   51: iconst_1
    //   52: aload_0
    //   53: invokevirtual 42	java/lang/Long:longValue	()J
    //   56: invokeinterface 43 4 0
    //   61: aload 4
    //   63: invokeinterface 30 1 0
    //   68: astore 8
    //   70: aload 8
    //   72: invokeinterface 31 1 0
    //   77: ifne +130 -> 207
    //   80: aload 8
    //   82: invokeinterface 55 1 0
    //   87: invokestatic 56	java/util/Collections:emptyList	()Ljava/util/List;
    //   90: astore 9
    //   92: aload 6
    //   94: ifnull +37 -> 131
    //   97: aload 7
    //   99: ifnull +25 -> 124
    //   102: aload 6
    //   104: invokeinterface 33 1 0
    //   109: goto +22 -> 131
    //   112: astore 10
    //   114: aload 7
    //   116: aload 10
    //   118: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   121: goto +10 -> 131
    //   124: aload 6
    //   126: invokeinterface 33 1 0
    //   131: aload 4
    //   133: ifnull +37 -> 170
    //   136: aload 5
    //   138: ifnull +25 -> 163
    //   141: aload 4
    //   143: invokeinterface 33 1 0
    //   148: goto +22 -> 170
    //   151: astore 10
    //   153: aload 5
    //   155: aload 10
    //   157: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   160: goto +10 -> 170
    //   163: aload 4
    //   165: invokeinterface 33 1 0
    //   170: aload_2
    //   171: ifnull +33 -> 204
    //   174: aload_3
    //   175: ifnull +23 -> 198
    //   178: aload_2
    //   179: invokeinterface 36 1 0
    //   184: goto +20 -> 204
    //   187: astore 10
    //   189: aload_3
    //   190: aload 10
    //   192: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   195: goto +9 -> 204
    //   198: aload_2
    //   199: invokeinterface 36 1 0
    //   204: aload 9
    //   206: areturn
    //   207: new 57	java/util/ArrayList
    //   210: dup
    //   211: invokespecial 58	java/util/ArrayList:<init>	()V
    //   214: astore 9
    //   216: aload 8
    //   218: ldc 59
    //   220: invokeinterface 60 2 0
    //   225: istore 10
    //   227: aload 6
    //   229: iconst_1
    //   230: iload 10
    //   232: invokeinterface 24 3 0
    //   237: aload 6
    //   239: iconst_2
    //   240: iload_1
    //   241: invokeinterface 24 3 0
    //   246: aload 6
    //   248: invokeinterface 30 1 0
    //   253: astore 8
    //   255: aload 8
    //   257: invokeinterface 31 1 0
    //   262: ifeq +20 -> 282
    //   265: aload 9
    //   267: aload_2
    //   268: aload 8
    //   270: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
    //   273: invokeinterface 64 2 0
    //   278: pop
    //   279: goto -24 -> 255
    //   282: aload 8
    //   284: invokeinterface 55 1 0
    //   289: aload 9
    //   291: astore 11
    //   293: aload 6
    //   295: ifnull +37 -> 332
    //   298: aload 7
    //   300: ifnull +25 -> 325
    //   303: aload 6
    //   305: invokeinterface 33 1 0
    //   310: goto +22 -> 332
    //   313: astore 12
    //   315: aload 7
    //   317: aload 12
    //   319: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   322: goto +10 -> 332
    //   325: aload 6
    //   327: invokeinterface 33 1 0
    //   332: aload 4
    //   334: ifnull +37 -> 371
    //   337: aload 5
    //   339: ifnull +25 -> 364
    //   342: aload 4
    //   344: invokeinterface 33 1 0
    //   349: goto +22 -> 371
    //   352: astore 12
    //   354: aload 5
    //   356: aload 12
    //   358: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   361: goto +10 -> 371
    //   364: aload 4
    //   366: invokeinterface 33 1 0
    //   371: aload_2
    //   372: ifnull +33 -> 405
    //   375: aload_3
    //   376: ifnull +23 -> 399
    //   379: aload_2
    //   380: invokeinterface 36 1 0
    //   385: goto +20 -> 405
    //   388: astore 12
    //   390: aload_3
    //   391: aload 12
    //   393: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   396: goto +9 -> 405
    //   399: aload_2
    //   400: invokeinterface 36 1 0
    //   405: aload 11
    //   407: areturn
    //   408: astore 8
    //   410: aload 8
    //   412: astore 7
    //   414: aload 8
    //   416: athrow
    //   417: astore 13
    //   419: aload 6
    //   421: ifnull +37 -> 458
    //   424: aload 7
    //   426: ifnull +25 -> 451
    //   429: aload 6
    //   431: invokeinterface 33 1 0
    //   436: goto +22 -> 458
    //   439: astore 14
    //   441: aload 7
    //   443: aload 14
    //   445: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   448: goto +10 -> 458
    //   451: aload 6
    //   453: invokeinterface 33 1 0
    //   458: aload 13
    //   460: athrow
    //   461: astore 6
    //   463: aload 6
    //   465: astore 5
    //   467: aload 6
    //   469: athrow
    //   470: astore 15
    //   472: aload 4
    //   474: ifnull +37 -> 511
    //   477: aload 5
    //   479: ifnull +25 -> 504
    //   482: aload 4
    //   484: invokeinterface 33 1 0
    //   489: goto +22 -> 511
    //   492: astore 16
    //   494: aload 5
    //   496: aload 16
    //   498: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   501: goto +10 -> 511
    //   504: aload 4
    //   506: invokeinterface 33 1 0
    //   511: aload 15
    //   513: athrow
    //   514: astore 4
    //   516: aload 4
    //   518: astore_3
    //   519: aload 4
    //   521: athrow
    //   522: astore 17
    //   524: aload_2
    //   525: ifnull +33 -> 558
    //   528: aload_3
    //   529: ifnull +23 -> 552
    //   532: aload_2
    //   533: invokeinterface 36 1 0
    //   538: goto +20 -> 558
    //   541: astore 18
    //   543: aload_3
    //   544: aload 18
    //   546: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   549: goto +9 -> 558
    //   552: aload_2
    //   553: invokeinterface 36 1 0
    //   558: aload 17
    //   560: athrow
    //   561: astore_2
    //   562: new 20	java/lang/RuntimeException
    //   565: dup
    //   566: aload_2
    //   567: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
    //   570: aload_2
    //   571: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   574: athrow
    // Line number table:
    //   Java source line #660	-> byte code offset #0
    //   Java source line #661	-> byte code offset #7
    //   Java source line #663	-> byte code offset #17
    //   Java source line #664	-> byte code offset #23
    //   Java source line #663	-> byte code offset #33
    //   Java source line #665	-> byte code offset #36
    //   Java source line #663	-> byte code offset #46
    //   Java source line #666	-> byte code offset #49
    //   Java source line #667	-> byte code offset #61
    //   Java source line #668	-> byte code offset #70
    //   Java source line #669	-> byte code offset #80
    //   Java source line #670	-> byte code offset #87
    //   Java source line #682	-> byte code offset #92
    //   Java source line #672	-> byte code offset #207
    //   Java source line #673	-> byte code offset #216
    //   Java source line #674	-> byte code offset #227
    //   Java source line #675	-> byte code offset #237
    //   Java source line #676	-> byte code offset #246
    //   Java source line #677	-> byte code offset #255
    //   Java source line #678	-> byte code offset #265
    //   Java source line #680	-> byte code offset #282
    //   Java source line #681	-> byte code offset #289
    //   Java source line #682	-> byte code offset #293
    //   Java source line #663	-> byte code offset #408
    //   Java source line #682	-> byte code offset #417
    //   Java source line #663	-> byte code offset #461
    //   Java source line #682	-> byte code offset #470
    //   Java source line #663	-> byte code offset #514
    //   Java source line #682	-> byte code offset #522
    //   Java source line #683	-> byte code offset #562
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	575	0	paramLong	Long
    //   0	575	1	paramInt	int
    //   20	533	2	localConnection	Connection
    //   561	10	2	localValidationException	NxtException.ValidationException
    //   22	522	3	localObject1	Object
    //   31	474	4	localPreparedStatement1	PreparedStatement
    //   514	6	4	localThrowable1	Throwable
    //   34	461	5	localObject2	Object
    //   44	408	6	localPreparedStatement2	PreparedStatement
    //   461	7	6	localThrowable2	Throwable
    //   47	395	7	localObject3	Object
    //   68	215	8	localResultSet	ResultSet
    //   408	7	8	localThrowable3	Throwable
    //   90	200	9	localObject4	Object
    //   112	5	10	localThrowable4	Throwable
    //   151	5	10	localThrowable5	Throwable
    //   187	4	10	localThrowable6	Throwable
    //   225	6	10	i	int
    //   291	115	11	localObject5	Object
    //   313	5	12	localThrowable7	Throwable
    //   352	5	12	localThrowable8	Throwable
    //   388	4	12	localThrowable9	Throwable
    //   417	42	13	localObject6	Object
    //   439	5	14	localThrowable10	Throwable
    //   470	42	15	localObject7	Object
    //   492	5	16	localThrowable11	Throwable
    //   522	37	17	localObject8	Object
    //   541	4	18	localThrowable12	Throwable
    // Exception table:
    //   from	to	target	type
    //   102	109	112	java/lang/Throwable
    //   141	148	151	java/lang/Throwable
    //   178	184	187	java/lang/Throwable
    //   303	310	313	java/lang/Throwable
    //   342	349	352	java/lang/Throwable
    //   379	385	388	java/lang/Throwable
    //   49	92	408	java/lang/Throwable
    //   207	293	408	java/lang/Throwable
    //   49	92	417	finally
    //   207	293	417	finally
    //   408	419	417	finally
    //   429	436	439	java/lang/Throwable
    //   36	131	461	java/lang/Throwable
    //   207	332	461	java/lang/Throwable
    //   408	461	461	java/lang/Throwable
    //   36	131	470	finally
    //   207	332	470	finally
    //   408	472	470	finally
    //   482	489	492	java/lang/Throwable
    //   23	170	514	java/lang/Throwable
    //   207	371	514	java/lang/Throwable
    //   408	514	514	java/lang/Throwable
    //   23	170	522	finally
    //   207	371	522	finally
    //   408	524	522	finally
    //   532	538	541	java/lang/Throwable
    //   17	204	561	nxt/NxtException$ValidationException
    //   17	204	561	java/sql/SQLException
    //   207	405	561	nxt/NxtException$ValidationException
    //   207	405	561	java/sql/SQLException
    //   408	561	561	nxt/NxtException$ValidationException
    //   408	561	561	java/sql/SQLException
  }
  
  public static long getBlockIdAtHeight(int paramInt)
  {
    Block localBlock = (Block)lastBlock.get();
    if (paramInt > localBlock.getHeight()) {
      throw new IllegalArgumentException("Invalid height " + paramInt + ", current blockchain is at " + localBlock.getHeight());
    }
    if (paramInt == localBlock.getHeight()) {
      return localBlock.getId().longValue();
    }
    return Block.findBlockIdAtHeight(paramInt);
  }
  
  /* Error */
  public static java.util.List<Block> getBlocksFromHeight(int paramInt)
  {
    // Byte code:
    //   0: iload_0
    //   1: iflt +23 -> 24
    //   4: getstatic 6	nxt/Blockchain:lastBlock	Ljava/util/concurrent/atomic/AtomicReference;
    //   7: invokevirtual 69	java/util/concurrent/atomic/AtomicReference:get	()Ljava/lang/Object;
    //   10: checkcast 70	nxt/Block
    //   13: invokevirtual 71	nxt/Block:getHeight	()I
    //   16: iload_0
    //   17: isub
    //   18: sipush 1440
    //   21: if_icmple +13 -> 34
    //   24: new 50	java/lang/IllegalArgumentException
    //   27: dup
    //   28: ldc 81
    //   30: invokespecial 52	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   33: athrow
    //   34: invokestatic 10	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   37: astore_1
    //   38: aconst_null
    //   39: astore_2
    //   40: aload_1
    //   41: ldc 82
    //   43: invokeinterface 12 2 0
    //   48: astore_3
    //   49: aconst_null
    //   50: astore 4
    //   52: aload_3
    //   53: iconst_1
    //   54: iload_0
    //   55: invokeinterface 24 3 0
    //   60: aload_3
    //   61: invokeinterface 30 1 0
    //   66: astore 5
    //   68: new 57	java/util/ArrayList
    //   71: dup
    //   72: invokespecial 58	java/util/ArrayList:<init>	()V
    //   75: astore 6
    //   77: aload 5
    //   79: invokeinterface 31 1 0
    //   84: ifeq +20 -> 104
    //   87: aload 6
    //   89: aload_1
    //   90: aload 5
    //   92: invokestatic 66	nxt/Block:getBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/Block;
    //   95: invokeinterface 64 2 0
    //   100: pop
    //   101: goto -24 -> 77
    //   104: aload 6
    //   106: astore 7
    //   108: aload_3
    //   109: ifnull +35 -> 144
    //   112: aload 4
    //   114: ifnull +24 -> 138
    //   117: aload_3
    //   118: invokeinterface 33 1 0
    //   123: goto +21 -> 144
    //   126: astore 8
    //   128: aload 4
    //   130: aload 8
    //   132: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   135: goto +9 -> 144
    //   138: aload_3
    //   139: invokeinterface 33 1 0
    //   144: aload_1
    //   145: ifnull +33 -> 178
    //   148: aload_2
    //   149: ifnull +23 -> 172
    //   152: aload_1
    //   153: invokeinterface 36 1 0
    //   158: goto +20 -> 178
    //   161: astore 8
    //   163: aload_2
    //   164: aload 8
    //   166: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   169: goto +9 -> 178
    //   172: aload_1
    //   173: invokeinterface 36 1 0
    //   178: aload 7
    //   180: areturn
    //   181: astore 5
    //   183: aload 5
    //   185: astore 4
    //   187: aload 5
    //   189: athrow
    //   190: astore 9
    //   192: aload_3
    //   193: ifnull +35 -> 228
    //   196: aload 4
    //   198: ifnull +24 -> 222
    //   201: aload_3
    //   202: invokeinterface 33 1 0
    //   207: goto +21 -> 228
    //   210: astore 10
    //   212: aload 4
    //   214: aload 10
    //   216: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   219: goto +9 -> 228
    //   222: aload_3
    //   223: invokeinterface 33 1 0
    //   228: aload 9
    //   230: athrow
    //   231: astore_3
    //   232: aload_3
    //   233: astore_2
    //   234: aload_3
    //   235: athrow
    //   236: astore 11
    //   238: aload_1
    //   239: ifnull +33 -> 272
    //   242: aload_2
    //   243: ifnull +23 -> 266
    //   246: aload_1
    //   247: invokeinterface 36 1 0
    //   252: goto +20 -> 272
    //   255: astore 12
    //   257: aload_2
    //   258: aload 12
    //   260: invokevirtual 35	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   263: goto +9 -> 272
    //   266: aload_1
    //   267: invokeinterface 36 1 0
    //   272: aload 11
    //   274: athrow
    //   275: astore_1
    //   276: new 20	java/lang/RuntimeException
    //   279: dup
    //   280: aload_1
    //   281: invokevirtual 68	java/lang/Exception:toString	()Ljava/lang/String;
    //   284: aload_1
    //   285: invokespecial 22	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   288: athrow
    // Line number table:
    //   Java source line #699	-> byte code offset #0
    //   Java source line #700	-> byte code offset #24
    //   Java source line #702	-> byte code offset #34
    //   Java source line #703	-> byte code offset #40
    //   Java source line #702	-> byte code offset #49
    //   Java source line #704	-> byte code offset #52
    //   Java source line #705	-> byte code offset #60
    //   Java source line #706	-> byte code offset #68
    //   Java source line #707	-> byte code offset #77
    //   Java source line #708	-> byte code offset #87
    //   Java source line #710	-> byte code offset #104
    //   Java source line #711	-> byte code offset #108
    //   Java source line #702	-> byte code offset #181
    //   Java source line #711	-> byte code offset #190
    //   Java source line #702	-> byte code offset #231
    //   Java source line #711	-> byte code offset #236
    //   Java source line #712	-> byte code offset #276
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	289	0	paramInt	int
    //   37	230	1	localConnection	Connection
    //   275	10	1	localSQLException	SQLException
    //   39	219	2	localObject1	Object
    //   48	175	3	localPreparedStatement	PreparedStatement
    //   231	4	3	localThrowable1	Throwable
    //   50	163	4	localObject2	Object
    //   66	25	5	localResultSet	ResultSet
    //   181	7	5	localThrowable2	Throwable
    //   75	30	6	localArrayList1	java.util.ArrayList
    //   126	5	8	localThrowable3	Throwable
    //   161	4	8	localThrowable4	Throwable
    //   190	39	9	localObject3	Object
    //   210	5	10	localThrowable5	Throwable
    //   236	37	11	localObject4	Object
    //   255	4	12	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   117	123	126	java/lang/Throwable
    //   152	158	161	java/lang/Throwable
    //   52	108	181	java/lang/Throwable
    //   52	108	190	finally
    //   181	192	190	finally
    //   201	207	210	java/lang/Throwable
    //   40	144	231	java/lang/Throwable
    //   181	231	231	java/lang/Throwable
    //   40	144	236	finally
    //   181	238	236	finally
    //   246	252	255	java/lang/Throwable
    //   34	178	275	java/sql/SQLException
    //   34	178	275	nxt/NxtException$ValidationException
    //   181	275	275	java/sql/SQLException
    //   181	275	275	nxt/NxtException$ValidationException
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
    return Block.findBlock(paramLong);
  }
  
  public static Transaction getTransaction(Long paramLong)
  {
    return Transaction.findTransaction(paramLong);
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
    throws NxtException
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
    try
    {
      pushBlock(localBlock, arrayOfTransaction);
      return true;
    }
    catch (BlockNotAcceptedException localBlockNotAcceptedException)
    {
      Logger.logDebugMessage("Block " + localBlock.getStringId() + " not accepted: " + localBlockNotAcceptedException.getMessage());
      throw localBlockNotAcceptedException;
    }
  }
  
  static void addBlock(Block paramBlock)
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        try
        {
          Block.saveBlock(localConnection, paramBlock);
          lastBlock.set(paramBlock);
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
  
  static void init()
  {
    if (!Block.hasBlock(Genesis.GENESIS_BLOCK_ID))
    {
      Logger.logMessage("Genesis block not in database, starting from scratch");
      
      TreeMap localTreeMap = new TreeMap();
      try
      {
        for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++)
        {
          localObject1 = Transaction.newTransaction(0, (short)0, Genesis.CREATOR_PUBLIC_KEY, Genesis.GENESIS_RECIPIENTS[i], Genesis.GENESIS_AMOUNTS[i], 0, null, Genesis.GENESIS_SIGNATURES[i]);
          
          ((Transaction)localObject1).setIndex(transactionCounter.incrementAndGet());
          localTreeMap.put(((Transaction)localObject1).getId(), localObject1);
        }
        Block localBlock = new Block(-1, 0, null, localTreeMap.size(), 1000000000, 0, localTreeMap.size() * 128, null, Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE);
        
        localBlock.setIndex(blockCounter.incrementAndGet());
        
        Object localObject1 = (Transaction[])localTreeMap.values().toArray(new Transaction[localTreeMap.size()]);
        MessageDigest localMessageDigest = Crypto.sha256();
        for (int j = 0; j < localObject1.length; j++)
        {
          Object localObject2 = localObject1[j];
          localBlock.transactionIds[j] = localObject2.getId();
          localBlock.blockTransactions[j] = localObject2;
          localMessageDigest.update(localObject2.getBytes());
        }
        localBlock.setPayloadHash(localMessageDigest.digest());
        for (Transaction localTransaction : localBlock.blockTransactions) {
          localTransaction.setBlock(localBlock);
        }
        addBlock(localBlock);
      }
      catch (NxtException.ValidationException localValidationException)
      {
        Logger.logMessage(localValidationException.getMessage());
        System.exit(1);
      }
    }
    Logger.logMessage("Scanning blockchain...");
    scan();
    Logger.logMessage("...Done");
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
            if (((!Transaction.hasTransaction((Long)localObject3)) && (!unconfirmedTransactions.containsKey(localObject3)) && (!doubleSpendingTransactions.containsKey(localObject3)) && (!localTransaction.verify())) || 
            



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
    PriorityQueue localPriorityQueue = new PriorityQueue(getTransactionCount(), new Comparator()
    {
      public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
      {
        long l1 = paramAnonymousTransaction1.getId().longValue();
        long l2 = paramAnonymousTransaction2.getId().longValue();
        return paramAnonymousTransaction1.getTimestamp() > paramAnonymousTransaction2.getTimestamp() ? 1 : paramAnonymousTransaction1.getTimestamp() < paramAnonymousTransaction2.getTimestamp() ? -1 : l1 > l2 ? 1 : l1 < l2 ? -1 : 0;
      }
    });
    Object localObject1 = getAllTransactions();Object localObject2 = null;
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
  
  private static void pushBlock(Block paramBlock, Transaction[] paramArrayOfTransaction)
    throws Blockchain.BlockNotAcceptedException
  {
    int i = Convert.getEpochTime();
    JSONArray localJSONArray1;
    JSONArray localJSONArray2;
    synchronized (Blockchain.class)
    {
      try
      {
        Block localBlock = (Block)lastBlock.get();
        if (!localBlock.getId().equals(paramBlock.getPreviousBlockId())) {
          throw new BlockOutOfOrderException("Previous block id doesn't match", null);
        }
        if (paramBlock.getVersion() != (localBlock.getHeight() < 30000 ? 1 : 2)) {
          throw new BlockNotAcceptedException("Invalid version " + paramBlock.getVersion(), null);
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
              throw new BlockNotAcceptedException("Checksum failed", null);
            }
            Logger.logMessage("Checksum passed at block 30000");
          }
        }
        if ((paramBlock.getVersion() != 1) && (!Arrays.equals(Crypto.sha256().digest(localBlock.getBytes()), paramBlock.getPreviousBlockHash()))) {
          throw new BlockNotAcceptedException("Previos block hash doesn't match", null);
        }
        if ((paramBlock.getTimestamp() > i + 15) || (paramBlock.getTimestamp() <= localBlock.getTimestamp())) {
          throw new BlockNotAcceptedException("Invalid timestamp: " + paramBlock.getTimestamp() + " current time is " + i + ", previous block timestamp is " + localBlock.getTimestamp(), null);
        }
        if ((paramBlock.getId().equals(Long.valueOf(0L))) || (Block.hasBlock(paramBlock.getId()))) {
          throw new BlockNotAcceptedException("Duplicate block or invalid id", null);
        }
        if ((!paramBlock.verifyGenerationSignature()) || (!paramBlock.verifyBlockSignature())) {
          throw new BlockNotAcceptedException("Signature verification failed", null);
        }
        paramBlock.setIndex(blockCounter.incrementAndGet());
        
        localObject1 = new HashMap();
        for (int j = 0; j < paramBlock.transactionIds.length; j++)
        {
          localObject2 = paramArrayOfTransaction[j];
          ((Transaction)localObject2).setIndex(transactionCounter.incrementAndGet());
          if (((Map)localObject1).put(paramBlock.transactionIds[j] =  = ((Transaction)localObject2).getId(), localObject2) != null) {
            throw new BlockNotAcceptedException("Block contains duplicate transactions: " + ((Transaction)localObject2).getStringId(), null);
          }
        }
        Arrays.sort(paramBlock.transactionIds);
        
        HashMap localHashMap1 = new HashMap();
        Object localObject2 = new HashMap();
        HashMap localHashMap2 = new HashMap();
        int k = 0;int m = 0;
        MessageDigest localMessageDigest = Crypto.sha256();
        Object localObject5;
        for (int n = 0; n < paramBlock.transactionIds.length; n++)
        {
          localObject4 = paramBlock.transactionIds[n];
          localObject5 = (Transaction)((Map)localObject1).get(localObject4);
          if ((((Transaction)localObject5).getTimestamp() > i + 15) || (((Transaction)localObject5).getTimestamp() > paramBlock.getTimestamp() + 15) || ((((Transaction)localObject5).getExpiration() < paramBlock.getTimestamp()) && (localBlock.getHeight() != 303))) {
            throw new BlockNotAcceptedException("Invalid transaction timestamp " + ((Transaction)localObject5).getTimestamp() + " for transaction " + ((Transaction)localObject5).getStringId() + ", current time is " + i + ", block timestamp is " + paramBlock.getTimestamp(), null);
          }
          if (Transaction.hasTransaction((Long)localObject4)) {
            throw new BlockNotAcceptedException("Transaction " + ((Transaction)localObject5).getStringId() + " is already in the blockchain", null);
          }
          if ((((Transaction)localObject5).getReferencedTransactionId() != null) && (!Transaction.hasTransaction(((Transaction)localObject5).getReferencedTransactionId())) && (((Map)localObject1).get(((Transaction)localObject5).getReferencedTransactionId()) == null)) {
            throw new BlockNotAcceptedException("Missing referenced transaction " + Convert.convert(((Transaction)localObject5).getReferencedTransactionId()) + " for transaction " + ((Transaction)localObject5).getStringId(), null);
          }
          if ((unconfirmedTransactions.get(localObject4) == null) && (!((Transaction)localObject5).verify())) {
            throw new BlockNotAcceptedException("Signature verification failed for transaction " + ((Transaction)localObject5).getStringId(), null);
          }
          if (((Transaction)localObject5).getId().equals(Long.valueOf(0L))) {
            throw new BlockNotAcceptedException("Invalid transaction id", null);
          }
          if (((Transaction)localObject5).isDuplicate(localHashMap1)) {
            throw new BlockNotAcceptedException("Transaction is a duplicate: " + ((Transaction)localObject5).getStringId(), null);
          }
          paramBlock.blockTransactions[n] = localObject5;
          
          k += ((Transaction)localObject5).getAmount();
          
          ((Transaction)localObject5).updateTotals((Map)localObject2, localHashMap2);
          
          m += ((Transaction)localObject5).getFee();
          
          localMessageDigest.update(((Transaction)localObject5).getBytes());
        }
        if ((k != paramBlock.getTotalAmount()) || (m != paramBlock.getTotalFee())) {
          throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", null);
        }
        if (!Arrays.equals(localMessageDigest.digest(), paramBlock.getPayloadHash())) {
          throw new BlockNotAcceptedException("Payload hash doesn't match", null);
        }
        for (Object localObject3 = ((Map)localObject2).entrySet().iterator(); ((Iterator)localObject3).hasNext();)
        {
          localObject4 = (Map.Entry)((Iterator)localObject3).next();
          localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
          if (((Account)localObject5).getBalance() < ((Long)((Map.Entry)localObject4).getValue()).longValue()) {
            throw new BlockNotAcceptedException("Not enough funds in sender account: " + Convert.convert(((Account)localObject5).getId()), null);
          }
        }
        for (localObject3 = localHashMap2.entrySet().iterator(); ((Iterator)localObject3).hasNext();)
        {
          localObject4 = (Map.Entry)((Iterator)localObject3).next();
          localObject5 = Account.getAccount((Long)((Map.Entry)localObject4).getKey());
          for (localIterator = ((Map)((Map.Entry)localObject4).getValue()).entrySet().iterator(); localIterator.hasNext();)
          {
            localObject6 = (Map.Entry)localIterator.next();
            localObject7 = (Long)((Map.Entry)localObject6).getKey();
            localObject8 = (Long)((Map.Entry)localObject6).getValue();
            if (((Account)localObject5).getAssetBalance((Long)localObject7).intValue() < ((Long)localObject8).longValue()) {
              throw new BlockNotAcceptedException("Asset balance not sufficient in sender account " + Convert.convert(((Account)localObject5).getId()), null);
            }
          }
        }
        Iterator localIterator;
        paramBlock.setHeight(localBlock.getHeight() + 1);
        
        localObject3 = null;
        for (localObject6 : paramBlock.blockTransactions)
        {
          ((Transaction)localObject6).setBlock(paramBlock);
          if ((transactionHashes.putIfAbsent(((Transaction)localObject6).getHash(), localObject6) != null) && (paramBlock.getHeight() != 58294))
          {
            localObject3 = localObject6;
            break;
          }
        }
        if (localObject3 != null)
        {
          for (localObject6 : paramBlock.blockTransactions) {
            if (!((Transaction)localObject6).equals(localObject3))
            {
              localObject7 = (Transaction)transactionHashes.get(((Transaction)localObject6).getHash());
              if ((localObject7 != null) && (((Transaction)localObject7).equals(localObject6))) {
                transactionHashes.remove(((Transaction)localObject6).getHash());
              }
            }
          }
          throw new BlockNotAcceptedException("Duplicate hash of transaction " + ((Transaction)localObject3).getStringId(), null);
        }
        paramBlock.calculateBaseTarget();
        
        addBlock(paramBlock);
        
        paramBlock.apply();
        
        localJSONArray1 = new JSONArray();
        localJSONArray2 = new JSONArray();
        for (localObject4 = ((Map)localObject1).entrySet().iterator(); ((Iterator)localObject4).hasNext();)
        {
          Map.Entry localEntry = (Map.Entry)((Iterator)localObject4).next();
          
          Transaction localTransaction = (Transaction)localEntry.getValue();
          
          localObject6 = new JSONObject();
          ((JSONObject)localObject6).put("index", Integer.valueOf(localTransaction.getIndex()));
          ((JSONObject)localObject6).put("blockTimestamp", Integer.valueOf(paramBlock.getTimestamp()));
          ((JSONObject)localObject6).put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
          ((JSONObject)localObject6).put("sender", Convert.convert(localTransaction.getSenderAccountId()));
          ((JSONObject)localObject6).put("recipient", Convert.convert(localTransaction.getRecipientId()));
          ((JSONObject)localObject6).put("amount", Integer.valueOf(localTransaction.getAmount()));
          ((JSONObject)localObject6).put("fee", Integer.valueOf(localTransaction.getFee()));
          ((JSONObject)localObject6).put("id", localTransaction.getStringId());
          localJSONArray1.add(localObject6);
          
          localObject7 = (Transaction)unconfirmedTransactions.remove(localEntry.getKey());
          if (localObject7 != null)
          {
            localObject8 = new JSONObject();
            ((JSONObject)localObject8).put("index", Integer.valueOf(((Transaction)localObject7).getIndex()));
            localJSONArray2.add(localObject8);
            
            Account localAccount = Account.getAccount(((Transaction)localObject7).getSenderAccountId());
            localAccount.addToUnconfirmedBalance((((Transaction)localObject7).getAmount() + ((Transaction)localObject7).getFee()) * 100L);
          }
        }
      }
      catch (RuntimeException localRuntimeException)
      {
        Object localObject4;
        Object localObject6;
        Object localObject7;
        Object localObject8;
        Logger.logMessage("Error pushing block", localRuntimeException);
        throw new BlockNotAcceptedException(localRuntimeException.toString(), null);
      }
    }
    if (paramBlock.getTimestamp() >= i - 15)
    {
      ??? = paramBlock.getJSONObject();
      ((JSONObject)???).put("requestType", "processBlock");
      
      Peer.sendToSomePeers((JSONObject)???);
    }
    ??? = new JSONArray();
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("index", Integer.valueOf(paramBlock.getIndex()));
    localJSONObject.put("timestamp", Integer.valueOf(paramBlock.getTimestamp()));
    localJSONObject.put("numberOfTransactions", Integer.valueOf(paramBlock.transactionIds.length));
    localJSONObject.put("totalAmount", Integer.valueOf(paramBlock.getTotalAmount()));
    localJSONObject.put("totalFee", Integer.valueOf(paramBlock.getTotalFee()));
    localJSONObject.put("payloadLength", Integer.valueOf(paramBlock.getPayloadLength()));
    localJSONObject.put("generator", Convert.convert(paramBlock.getGeneratorAccountId()));
    localJSONObject.put("height", Integer.valueOf(paramBlock.getHeight()));
    localJSONObject.put("version", Integer.valueOf(paramBlock.getVersion()));
    localJSONObject.put("block", paramBlock.getStringId());
    localJSONObject.put("baseTarget", BigInteger.valueOf(paramBlock.getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
    ((JSONArray)???).add(localJSONObject);
    
    Object localObject1 = new JSONObject();
    ((JSONObject)localObject1).put("response", "processNewData");
    ((JSONObject)localObject1).put("addedConfirmedTransactions", localJSONArray1);
    if (localJSONArray2.size() > 0) {
      ((JSONObject)localObject1).put("removedUnconfirmedTransactions", localJSONArray2);
    }
    ((JSONObject)localObject1).put("addedRecentBlocks", ???);
    
    User.sendToAll((JSONStreamAware)localObject1);
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
        
        Logger.logDebugMessage("Will pop block " + localBlock.getStringId() + " at height " + localBlock.getHeight());
        if (localBlock.getId().equals(Genesis.GENESIS_BLOCK_ID)) {
          return false;
        }
        localObject1 = Block.findBlock(localBlock.getPreviousBlockId());
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
        for (Transaction localTransaction1 : localBlock.blockTransactions)
        {
          Transaction localTransaction2 = (Transaction)transactionHashes.get(localTransaction1.getHash());
          if ((localTransaction2 != null) && (localTransaction2.equals(localTransaction1))) {
            transactionHashes.remove(localTransaction1.getHash());
          }
          unconfirmedTransactions.put(localTransaction1.getId(), localTransaction1);
          
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
        Block.deleteBlock(localBlock.getId());
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
    Account.clear();
    Alias.clear();
    Asset.clear();
    Order.clear();
    unconfirmedTransactions.clear();
    doubleSpendingTransactions.clear();
    nonBroadcastedTransactions.clear();
    transactionHashes.clear();
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
            Block localBlock = Block.getBlock(localConnection, localResultSet);
            if (!localBlock.getId().equals(localLong)) {
              throw new NxtException.ValidationException("Database blocks in the wrong order!");
            }
            lastBlock.set(localBlock);
            localBlock.apply();
            localLong = localBlock.getNextBlockId();
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
  }
  
  private static void generateBlock(String paramString)
  {
    TreeSet localTreeSet = new TreeSet();
    for (Object localObject1 = unconfirmedTransactions.values().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Transaction)((Iterator)localObject1).next();
      if ((((Transaction)localObject2).getReferencedTransactionId() == null) || (Transaction.hasTransaction(((Transaction)localObject2).getReferencedTransactionId()))) {
        localTreeSet.add(localObject2);
      }
    }
    localObject1 = new HashMap();
    Object localObject2 = new HashMap();
    HashMap localHashMap = new HashMap();
    
    int i = 0;
    int j = 0;
    int k = 0;
    
    int m = Convert.getEpochTime();
    Object localObject3;
    while (k <= 32640)
    {
      int n = ((Map)localObject1).size();
      for (localObject3 = localTreeSet.iterator(); ((Iterator)localObject3).hasNext();)
      {
        localObject4 = (Transaction)((Iterator)localObject3).next();
        
        int i1 = ((Transaction)localObject4).getSize();
        if ((((Map)localObject1).get(((Transaction)localObject4).getId()) == null) && (k + i1 <= 32640))
        {
          localObject5 = ((Transaction)localObject4).getSenderAccountId();
          localObject6 = (Long)localHashMap.get(localObject5);
          if (localObject6 == null) {
            localObject6 = Long.valueOf(0L);
          }
          long l = (((Transaction)localObject4).getAmount() + ((Transaction)localObject4).getFee()) * 100L;
          if (((Long)localObject6).longValue() + l <= Account.getAccount((Long)localObject5).getBalance()) {
            if ((((Transaction)localObject4).getTimestamp() <= m + 15) && (((Transaction)localObject4).getExpiration() >= m) && 
            


              (!((Transaction)localObject4).isDuplicate((Map)localObject2)))
            {
              localHashMap.put(localObject5, Long.valueOf(((Long)localObject6).longValue() + l));
              
              ((Map)localObject1).put(((Transaction)localObject4).getId(), localObject4);
              k += i1;
              i += ((Transaction)localObject4).getAmount();
              j += ((Transaction)localObject4).getFee();
            }
          }
        }
      }
      if (((Map)localObject1).size() == n) {
        break;
      }
    }
    byte[] arrayOfByte1 = Crypto.getPublicKey(paramString);
    

    Object localObject4 = (Block)lastBlock.get();
    try
    {
      if (((Block)localObject4).getHeight() < 30000)
      {
        localObject3 = new Block(1, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64]);
      }
      else
      {
        byte[] arrayOfByte2 = Crypto.sha256().digest(((Block)localObject4).getBytes());
        localObject3 = new Block(2, m, ((Block)localObject4).getId(), ((Map)localObject1).size(), i, j, k, null, arrayOfByte1, null, new byte[64], arrayOfByte2);
      }
    }
    catch (NxtException.ValidationException localValidationException)
    {
      Logger.logMessage("Error generating block", localValidationException);
      return;
    }
    int i2 = 0;
    for (Object localObject5 = ((Map)localObject1).keySet().iterator(); ((Iterator)localObject5).hasNext();)
    {
      localObject6 = (Long)((Iterator)localObject5).next();
      ((Block)localObject3).transactionIds[(i2++)] = localObject6;
    }
    Arrays.sort(((Block)localObject3).transactionIds);
    localObject5 = Crypto.sha256();
    for (i2 = 0; i2 < ((Block)localObject3).transactionIds.length; i2++)
    {
      localObject6 = (Transaction)((Map)localObject1).get(localObject3.transactionIds[i2]);
      ((MessageDigest)localObject5).update(((Transaction)localObject6).getBytes());
      ((Block)localObject3).blockTransactions[i2] = localObject6;
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
  
  static void purgeExpiredHashes(int paramInt)
  {
    Iterator localIterator = transactionHashes.entrySet().iterator();
    while (localIterator.hasNext()) {
      if (((Transaction)((Map.Entry)localIterator.next()).getValue()).getExpiration() < paramInt) {
        localIterator.remove();
      }
    }
  }
  
  public static class BlockNotAcceptedException
    extends NxtException
  {
    private BlockNotAcceptedException(String paramString)
    {
      super();
    }
  }
  
  public static class BlockOutOfOrderException
    extends Blockchain.BlockNotAcceptedException
  {
    private BlockOutOfOrderException(String paramString)
    {
      super(null);
    }
  }
}