package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Order
{
  private static final SortedSet emptySortedSet = Collections.unmodifiableSortedSet(new TreeSet());
  private final Long id;
  private final Account account;
  private final Long assetId;
  private final long price;
  private final long height;
  private volatile int quantity;
  
  static void clear()
  {
    Ask.askOrders.clear();
    Ask.sortedAskOrders.clear();
    Bid.bidOrders.clear();
    Bid.sortedBidOrders.clear();
  }
  
  private static void matchOrders(Long paramLong)
  {
    SortedSet localSortedSet1 = (SortedSet)Ask.sortedAskOrders.get(paramLong);
    SortedSet localSortedSet2 = (SortedSet)Bid.sortedBidOrders.get(paramLong);
    if ((localSortedSet1 == null) || (localSortedSet2 == null)) {
      return;
    }
    while ((!localSortedSet1.isEmpty()) && (!localSortedSet2.isEmpty()))
    {
      Ask localAsk = (Ask)localSortedSet1.first();
      Bid localBid = (Bid)localSortedSet2.first();
      if (localAsk.getPrice() > localBid.getPrice()) {
        break;
      }
      int i = localAsk.quantity < localBid.quantity ? localAsk.quantity : localBid.quantity;
      long l = (localAsk.getHeight() < localBid.getHeight()) || ((localAsk.getHeight() == localBid.getHeight()) && (localAsk.getId().longValue() < localBid.getId().longValue())) ? localAsk.getPrice() : localBid.getPrice();
      if (localAsk.quantity -= i == 0) {
        Ask.removeOrder(localAsk.getId());
      }
      localAsk.getAccount().addToBalanceAndUnconfirmedBalance(i * l);
      if (localBid.quantity -= i == 0) {
        Bid.removeOrder(localBid.getId());
      }
      localBid.getAccount().addToAssetAndUnconfirmedAssetBalance(paramLong, i);
    }
  }
  
  private Order(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
  {
    this.id = paramLong1;
    this.account = paramAccount;
    this.assetId = paramLong2;
    this.quantity = paramInt;
    this.price = paramLong;
    this.height = Blockchain.getLastBlock().getHeight();
  }
  
  public Long getId()
  {
    return this.id;
  }
  
  public Account getAccount()
  {
    return this.account;
  }
  
  public Long getAssetId()
  {
    return this.assetId;
  }
  
  public long getPrice()
  {
    return this.price;
  }
  
  public final int getQuantity()
  {
    return this.quantity;
  }
  
  public long getHeight()
  {
    return this.height;
  }
  
  private int compareTo(Order paramOrder)
  {
    if (this.height < paramOrder.height) {
      return -1;
    }
    if (this.height > paramOrder.height) {
      return 1;
    }
    if (this.id.longValue() < paramOrder.id.longValue()) {
      return -1;
    }
    if (this.id.longValue() > paramOrder.id.longValue()) {
      return 1;
    }
    return 0;
  }
  
  public static final class Ask
    extends Order
    implements Comparable<Ask>
  {
    private static final ConcurrentMap<Long, Ask> askOrders = new ConcurrentHashMap();
    private static final ConcurrentMap<Long, SortedSet<Ask>> sortedAskOrders = new ConcurrentHashMap();
    private static final Collection<Ask> allAskOrders = Collections.unmodifiableCollection(askOrders.values());
    
    public static Collection<Ask> getAllAskOrders()
    {
      return allAskOrders;
    }
    
    public static Ask getAskOrder(Long paramLong)
    {
      return (Ask)askOrders.get(paramLong);
    }
    
    public static SortedSet<Ask> getSortedOrders(Long paramLong)
    {
      SortedSet localSortedSet = (SortedSet)sortedAskOrders.get(paramLong);
      return localSortedSet == null ? Order.emptySortedSet : Collections.unmodifiableSortedSet(localSortedSet);
    }
    
    static void addOrder(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
    {
      paramAccount.addToAssetAndUnconfirmedAssetBalance(paramLong2, -paramInt);
      Ask localAsk = new Ask(paramLong1, paramAccount, paramLong2, paramInt, paramLong);
      askOrders.put(localAsk.getId(), localAsk);
      Object localObject = (SortedSet)sortedAskOrders.get(paramLong2);
      if (localObject == null)
      {
        localObject = new TreeSet();
        sortedAskOrders.put(paramLong2, localObject);
      }
      ((SortedSet)localObject).add(localAsk);
      Order.matchOrders(paramLong2);
    }
    
    static Ask removeOrder(Long paramLong)
    {
      Ask localAsk = (Ask)askOrders.remove(paramLong);
      if (localAsk != null) {
        ((SortedSet)sortedAskOrders.get(localAsk.getAssetId())).remove(localAsk);
      }
      return localAsk;
    }
    
    private Ask(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
    {
      super(paramAccount, paramLong2, paramInt, paramLong, null);
    }
    
    public int compareTo(Ask paramAsk)
    {
      if (getPrice() < paramAsk.getPrice()) {
        return -1;
      }
      if (getPrice() > paramAsk.getPrice()) {
        return 1;
      }
      return super.compareTo(paramAsk);
    }
    
    public boolean equals(Object paramObject)
    {
      return ((paramObject instanceof Ask)) && (getId().equals(((Ask)paramObject).getId()));
    }
    
    public int hashCode()
    {
      return getId().hashCode();
    }
  }
  
  public static final class Bid
    extends Order
    implements Comparable<Bid>
  {
    private static final ConcurrentMap<Long, Bid> bidOrders = new ConcurrentHashMap();
    private static final ConcurrentMap<Long, SortedSet<Bid>> sortedBidOrders = new ConcurrentHashMap();
    private static final Collection<Bid> allBidOrders = Collections.unmodifiableCollection(bidOrders.values());
    
    public static Collection<Bid> getAllBidOrders()
    {
      return allBidOrders;
    }
    
    public static Bid getBidOrder(Long paramLong)
    {
      return (Bid)bidOrders.get(paramLong);
    }
    
    public static SortedSet<Bid> getSortedOrders(Long paramLong)
    {
      SortedSet localSortedSet = (SortedSet)sortedBidOrders.get(paramLong);
      return localSortedSet == null ? Order.emptySortedSet : Collections.unmodifiableSortedSet(localSortedSet);
    }
    
    static void addOrder(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
    {
      paramAccount.addToBalanceAndUnconfirmedBalance(-paramInt * paramLong);
      Bid localBid = new Bid(paramLong1, paramAccount, paramLong2, paramInt, paramLong);
      bidOrders.put(localBid.getId(), localBid);
      Object localObject = (SortedSet)sortedBidOrders.get(paramLong2);
      if (localObject == null)
      {
        localObject = new TreeSet();
        sortedBidOrders.put(paramLong2, localObject);
      }
      ((SortedSet)localObject).add(localBid);
      Order.matchOrders(paramLong2);
    }
    
    static Bid removeOrder(Long paramLong)
    {
      Bid localBid = (Bid)bidOrders.remove(paramLong);
      if (localBid != null) {
        ((SortedSet)sortedBidOrders.get(localBid.getAssetId())).remove(localBid);
      }
      return localBid;
    }
    
    private Bid(Long paramLong1, Account paramAccount, Long paramLong2, int paramInt, long paramLong)
    {
      super(paramAccount, paramLong2, paramInt, paramLong, null);
    }
    
    public int compareTo(Bid paramBid)
    {
      if (getPrice() > paramBid.getPrice()) {
        return -1;
      }
      if (getPrice() < paramBid.getPrice()) {
        return 1;
      }
      return super.compareTo(paramBid);
    }
    
    public boolean equals(Object paramObject)
    {
      return ((paramObject instanceof Bid)) && (getId().equals(((Bid)paramObject).getId()));
    }
    
    public int hashCode()
    {
      return getId().hashCode();
    }
  }
}