package nxt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

public final class Generator
{
  public static enum Event
  {
    GENERATION_DEADLINE;
    
    private Event() {}
  }
  
  private static final Listeners<Generator, Event> listeners = new Listeners();
  private static final ConcurrentMap<Account, Block> lastBlocks = new ConcurrentHashMap();
  private static final ConcurrentMap<Account, BigInteger> hits = new ConcurrentHashMap();
  private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap();
  private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
  static final Runnable generateBlockThread = new Runnable()
  {
    public void run()
    {
      try
      {
        try
        {
          for (Generator localGenerator : Generator.generators.values()) {
            localGenerator.forge();
          }
        }
        catch (Exception localException)
        {
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
  private final Account account;
  private final String secretPhrase;
  private final byte[] publicKey;
  private volatile long deadline;
  
  public static boolean addListener(Listener<Generator> paramListener, Event paramEvent)
  {
    return listeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean removeListener(Listener<Generator> paramListener, Event paramEvent)
  {
    return listeners.removeListener(paramListener, paramEvent);
  }
  
  public static Generator startForging(String paramString)
  {
    byte[] arrayOfByte = Crypto.getPublicKey(paramString);
    return startForging(paramString, arrayOfByte);
  }
  
  public static Generator startForging(String paramString, byte[] paramArrayOfByte)
  {
    Account localAccount = Account.getAccount(paramArrayOfByte);
    if (localAccount == null) {
      return null;
    }
    Generator localGenerator1 = new Generator(paramString, paramArrayOfByte, localAccount);
    Generator localGenerator2 = (Generator)generators.putIfAbsent(paramString, localGenerator1);
    return localGenerator2 != null ? localGenerator2 : localGenerator1;
  }
  
  public static Generator stopForging(String paramString)
  {
    Generator localGenerator = (Generator)generators.remove(paramString);
    if (localGenerator != null)
    {
      lastBlocks.remove(localGenerator.account);
      hits.remove(localGenerator.account);
    }
    return localGenerator;
  }
  
  public static Collection<Generator> getAllGenerators()
  {
    return allGenerators;
  }
  
  private Generator(String paramString, byte[] paramArrayOfByte, Account paramAccount)
  {
    this.secretPhrase = paramString;
    this.publicKey = paramArrayOfByte;
    this.account = paramAccount;
    forge();
  }
  
  public byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  public long getDeadline()
  {
    return this.deadline;
  }
  
  private void forge()
  {
    long l1 = this.account.getEffectiveBalance();
    if (l1 <= 0L) {
      return;
    }
    Block localBlock = Blockchain.getLastBlock();
    Object localObject1;
    if (!localBlock.equals(lastBlocks.get(this.account)))
    {
      MessageDigest localMessageDigest = Crypto.sha256();
      if (localBlock.getHeight() < 30000)
      {
        localObject2 = Crypto.sign(localBlock.getGenerationSignature(), this.secretPhrase);
        localObject1 = localMessageDigest.digest((byte[])localObject2);
      }
      else
      {
        localMessageDigest.update(localBlock.getGenerationSignature());
        localObject1 = localMessageDigest.digest(this.publicKey);
      }
      Object localObject2 = new BigInteger(1, new byte[] { localObject1[7], localObject1[6], localObject1[5], localObject1[4], localObject1[3], localObject1[2], localObject1[1], localObject1[0] });
      
      lastBlocks.put(this.account, localBlock);
      hits.put(this.account, localObject2);
      
      long l2 = ((BigInteger)localObject2).divide(BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(this.account.getEffectiveBalance()))).longValue();
      long l3 = Convert.getEpochTime() - localBlock.getTimestamp();
      
      this.deadline = (l2 - l3);
      
      listeners.notify(this, Event.GENERATION_DEADLINE);
    }
    int i = Convert.getEpochTime() - localBlock.getTimestamp();
    if (i > 0)
    {
      localObject1 = BigInteger.valueOf(localBlock.getBaseTarget()).multiply(BigInteger.valueOf(l1)).multiply(BigInteger.valueOf(i));
      if (((BigInteger)hits.get(this.account)).compareTo((BigInteger)localObject1) < 0) {
        Blockchain.generateBlock(this.secretPhrase);
      }
    }
  }
}