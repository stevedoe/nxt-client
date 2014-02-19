package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Trade
{
  private static final ConcurrentMap<Long, List<Trade>> trades = new ConcurrentHashMap();
  private static final Collection<List<Trade>> allTrades = Collections.unmodifiableCollection(trades.values());
  private final Long blockId;
  private final Long askOrderId;
  private final Long bidOrderId;
  private final int quantity;
  private final long price;
  
  public static Collection<List<Trade>> getAllTrades()
  {
    return allTrades;
  }
  
  static void addTrade(Long paramLong1, Long paramLong2, Long paramLong3, Long paramLong4, int paramInt, long paramLong)
  {
    Object localObject = (List)trades.get(paramLong1);
    if (localObject == null)
    {
      localObject = new CopyOnWriteArrayList();
      
      trades.put(paramLong1, localObject);
    }
    ((List)localObject).add(new Trade(paramLong2, paramLong3, paramLong4, paramInt, paramLong));
  }
  
  static void clear()
  {
    trades.clear();
  }
  
  private Trade(Long paramLong1, Long paramLong2, Long paramLong3, int paramInt, long paramLong)
  {
    this.blockId = paramLong1;
    this.askOrderId = paramLong2;
    this.bidOrderId = paramLong3;
    this.quantity = paramInt;
    this.price = paramLong;
  }
  
  public Long getBlockId()
  {
    return this.blockId;
  }
  
  public Long getAskOrderId()
  {
    return this.askOrderId;
  }
  
  public Long getBidOrderId()
  {
    return this.bidOrderId;
  }
  
  public int getQuantity()
  {
    return this.quantity;
  }
  
  public long getPrice()
  {
    return this.price;
  }
  
  public static List<Trade> getTrades(Long paramLong)
  {
    return Collections.unmodifiableList((List)trades.get(paramLong));
  }
}