package nxt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;

public final class Account
{
  private static final int maxTrackedBalanceConfirmations = 2881;
  
  public static enum Event
  {
    BALANCE,  UNCONFIRMED_BALANCE;
    
    private Event() {}
  }
  
  private static final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap();
  private static final Collection<Account> allAccounts = Collections.unmodifiableCollection(accounts.values());
  private static final Listeners<Account, Event> listeners = new Listeners();
  private final Long id;
  private final int height;
  private byte[] publicKey;
  private int keyHeight;
  private long balance;
  private long unconfirmedBalance;
  
  public static boolean addListener(Listener<Account> paramListener, Event paramEvent)
  {
    return listeners.addListener(paramListener, paramEvent);
  }
  
  public static boolean removeListener(Listener<Account> paramListener, Event paramEvent)
  {
    return listeners.removeListener(paramListener, paramEvent);
  }
  
  public static Collection<Account> getAllAccounts()
  {
    return allAccounts;
  }
  
  public static Account getAccount(Long paramLong)
  {
    return (Account)accounts.get(paramLong);
  }
  
  public static Account getAccount(byte[] paramArrayOfByte)
  {
    return (Account)accounts.get(getId(paramArrayOfByte));
  }
  
  public static Long getId(byte[] paramArrayOfByte)
  {
    byte[] arrayOfByte = Crypto.sha256().digest(paramArrayOfByte);
    BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
    
    return Long.valueOf(localBigInteger.longValue());
  }
  
  static Account addOrGetAccount(Long paramLong)
  {
    Account localAccount1 = new Account(paramLong);
    Account localAccount2 = (Account)accounts.putIfAbsent(paramLong, localAccount1);
    return localAccount2 != null ? localAccount2 : localAccount1;
  }
  
  static void clear()
  {
    accounts.clear();
  }
  
  private final List<GuaranteedBalance> guaranteedBalances = new ArrayList();
  private final Map<Long, Integer> assetBalances = new HashMap();
  private final Map<Long, Integer> unconfirmedAssetBalances = new HashMap();
  
  private Account(Long paramLong)
  {
    this.id = paramLong;
    this.height = Nxt.getBlockchain().getLastBlock().getHeight();
  }
  
  public Long getId()
  {
    return this.id;
  }
  
  public synchronized byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  public synchronized long getBalance()
  {
    return this.balance;
  }
  
  public synchronized long getUnconfirmedBalance()
  {
    return this.unconfirmedBalance;
  }
  
  public int getEffectiveBalance()
  {
    Block localBlock = Nxt.getBlockchain().getLastBlock();
    if ((localBlock.getHeight() < 51000) && (this.height < 47000))
    {
      if (this.height == 0) {
        return (int)(getBalance() / 100L);
      }
      if (localBlock.getHeight() - this.height < 1440) {
        return 0;
      }
      int i = 0;
      for (Transaction localTransaction : localBlock.getTransactions()) {
        if (localTransaction.getRecipientId().equals(this.id)) {
          i += localTransaction.getAmount();
        }
      }
      return (int)(getBalance() / 100L) - i;
    }
    return (int)(getGuaranteedBalance(1440) / 100L);
  }
  
  public synchronized long getGuaranteedBalance(int paramInt)
  {
    if ((paramInt > 2881) || (paramInt >= Nxt.getBlockchain().getLastBlock().getHeight()) || (paramInt < 0)) {
      throw new IllegalArgumentException("Number of required confirmations must be between 0 and 2881");
    }
    if (this.guaranteedBalances.isEmpty()) {
      return 0L;
    }
    int i = Collections.binarySearch(this.guaranteedBalances, new GuaranteedBalance(Nxt.getBlockchain().getLastBlock().getHeight() - paramInt, 0L, null));
    if (i == -1) {
      return 0L;
    }
    if (i < -1) {
      i = -i - 2;
    }
    if (i > this.guaranteedBalances.size() - 1) {
      i = this.guaranteedBalances.size() - 1;
    }
    GuaranteedBalance localGuaranteedBalance;
    while (((localGuaranteedBalance = (GuaranteedBalance)this.guaranteedBalances.get(i)).ignore) && (i > 0)) {
      i--;
    }
    return localGuaranteedBalance.ignore ? 0L : localGuaranteedBalance.balance;
  }
  
  public synchronized Integer getUnconfirmedAssetBalance(Long paramLong)
  {
    return (Integer)this.unconfirmedAssetBalances.get(paramLong);
  }
  
  public Map<Long, Integer> getAssetBalances()
  {
    return Collections.unmodifiableMap(this.assetBalances);
  }
  
  synchronized boolean setOrVerify(byte[] paramArrayOfByte, int paramInt)
  {
    if (this.publicKey == null)
    {
      this.publicKey = paramArrayOfByte;
      this.keyHeight = -1;
      return true;
    }
    if (Arrays.equals(this.publicKey, paramArrayOfByte)) {
      return true;
    }
    if (this.keyHeight == -1)
    {
      Logger.logMessage("DUPLICATE KEY!!!");
      Logger.logMessage("Account key for " + Convert.toUnsignedLong(this.id) + " was already set to a different one at the same height " + ", current height is " + paramInt + ", rejecting new key");
      
      return false;
    }
    if (this.keyHeight >= paramInt)
    {
      Logger.logMessage("DUPLICATE KEY!!!");
      Logger.logMessage("Changing key for account " + Convert.toUnsignedLong(this.id) + " at height " + paramInt + ", was previously set to a different one at height " + this.keyHeight);
      
      this.publicKey = paramArrayOfByte;
      this.keyHeight = paramInt;
      return true;
    }
    Logger.logMessage("DUPLICATE KEY!!!");
    Logger.logMessage("Invalid key for account " + Convert.toUnsignedLong(this.id) + " at height " + paramInt + ", was already set to a different one at height " + this.keyHeight);
    
    return false;
  }
  
  synchronized void apply(int paramInt)
  {
    if (this.publicKey == null) {
      throw new IllegalStateException("Public key has not been set for account " + Convert.toUnsignedLong(this.id) + " at height " + paramInt + ", key height is " + this.keyHeight);
    }
    if ((this.keyHeight == -1) || (this.keyHeight > paramInt)) {
      this.keyHeight = paramInt;
    }
  }
  
  synchronized void undo(int paramInt)
  {
    if (this.keyHeight >= paramInt)
    {
      Logger.logDebugMessage("Unsetting key for account " + Convert.toUnsignedLong(this.id) + " at height " + paramInt + ", was previously set at height " + this.keyHeight);
      
      this.publicKey = null;
      this.keyHeight = -1;
    }
    if (this.height == paramInt)
    {
      Logger.logDebugMessage("Removing account " + Convert.toUnsignedLong(this.id) + " which was created in the popped off block");
      accounts.remove(getId());
    }
  }
  
  synchronized int getAssetBalance(Long paramLong)
  {
    return Convert.nullToZero((Integer)this.assetBalances.get(paramLong));
  }
  
  synchronized void addToAssetBalance(Long paramLong, int paramInt)
  {
    Integer localInteger = (Integer)this.assetBalances.get(paramLong);
    if (localInteger == null) {
      this.assetBalances.put(paramLong, Integer.valueOf(paramInt));
    } else {
      this.assetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
    }
  }
  
  synchronized void addToUnconfirmedAssetBalance(Long paramLong, int paramInt)
  {
    Integer localInteger = (Integer)this.unconfirmedAssetBalances.get(paramLong);
    if (localInteger == null) {
      this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(paramInt));
    } else {
      this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
    }
  }
  
  synchronized void addToAssetAndUnconfirmedAssetBalance(Long paramLong, int paramInt)
  {
    Integer localInteger = (Integer)this.assetBalances.get(paramLong);
    if (localInteger == null)
    {
      this.assetBalances.put(paramLong, Integer.valueOf(paramInt));
      this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(paramInt));
    }
    else
    {
      this.assetBalances.put(paramLong, Integer.valueOf(localInteger.intValue() + paramInt));
      this.unconfirmedAssetBalances.put(paramLong, Integer.valueOf(((Integer)this.unconfirmedAssetBalances.get(paramLong)).intValue() + paramInt));
    }
  }
  
  void addToBalance(long paramLong)
  {
    synchronized (this)
    {
      this.balance += paramLong;
      addToGuaranteedBalance(paramLong);
    }
    listeners.notify(this, Event.BALANCE);
  }
  
  void addToUnconfirmedBalance(long paramLong)
  {
    synchronized (this)
    {
      this.unconfirmedBalance += paramLong;
    }
    listeners.notify(this, Event.UNCONFIRMED_BALANCE);
  }
  
  void addToBalanceAndUnconfirmedBalance(long paramLong)
  {
    synchronized (this)
    {
      this.balance += paramLong;
      this.unconfirmedBalance += paramLong;
      addToGuaranteedBalance(paramLong);
    }
    listeners.notify(this, Event.BALANCE);
    listeners.notify(this, Event.UNCONFIRMED_BALANCE);
  }
  
  private synchronized void addToGuaranteedBalance(long paramLong)
  {
    int i = Nxt.getBlockchain().getLastBlock().getHeight();
    GuaranteedBalance localGuaranteedBalance1 = null;
    if ((this.guaranteedBalances.size() > 0) && ((localGuaranteedBalance1 = (GuaranteedBalance)this.guaranteedBalances.get(this.guaranteedBalances.size() - 1)).height > i))
    {
      if (paramLong > 0L) {
        for (GuaranteedBalance localGuaranteedBalance2 : this.guaranteedBalances) {
          localGuaranteedBalance2.balance += paramLong;
        }
      }
      localGuaranteedBalance1.ignore = true;
      return;
    }
    int j = 0;
    for (int k = 0; k < this.guaranteedBalances.size(); k++)
    {
      GuaranteedBalance localGuaranteedBalance3 = (GuaranteedBalance)this.guaranteedBalances.get(k);
      if ((localGuaranteedBalance3.height < i - 2881) && (k < this.guaranteedBalances.size() - 1) && (((GuaranteedBalance)this.guaranteedBalances.get(k + 1)).height >= i - 2881))
      {
        j = k;
        if ((i >= 64000) && (i < 67000)) {
          localGuaranteedBalance3.balance += paramLong;
        } else if ((i >= 67000) && (paramLong < 0L)) {
          localGuaranteedBalance3.balance += paramLong;
        }
      }
      else if (paramLong < 0L)
      {
        localGuaranteedBalance3.balance += paramLong;
      }
    }
    if (j > 0)
    {
      Iterator localIterator2 = this.guaranteedBalances.iterator();
      while ((localIterator2.hasNext()) && (j > 0))
      {
        localIterator2.next();
        localIterator2.remove();
        j--;
      }
    }
    if ((this.guaranteedBalances.size() == 0) || (localGuaranteedBalance1.height < i))
    {
      this.guaranteedBalances.add(new GuaranteedBalance(i, this.balance, null));
    }
    else if (localGuaranteedBalance1.height == i)
    {
      localGuaranteedBalance1.balance = this.balance;
      localGuaranteedBalance1.ignore = false;
    }
    else
    {
      throw new IllegalStateException("last guaranteed balance height exceeds blockchain height");
    }
  }
  
  private static class GuaranteedBalance
    implements Comparable<GuaranteedBalance>
  {
    final int height;
    long balance;
    boolean ignore;
    
    private GuaranteedBalance(int paramInt, long paramLong)
    {
      this.height = paramInt;
      this.balance = paramLong;
      this.ignore = false;
    }
    
    public int compareTo(GuaranteedBalance paramGuaranteedBalance)
    {
      if (this.height < paramGuaranteedBalance.height) {
        return -1;
      }
      if (this.height > paramGuaranteedBalance.height) {
        return 1;
      }
      return 0;
    }
    
    public String toString()
    {
      return "height: " + this.height + ", guaranteed: " + this.balance;
    }
  }
}