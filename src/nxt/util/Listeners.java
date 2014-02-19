package nxt.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Listeners<T, E extends Enum<E>>
{
  private final ConcurrentHashMap<Enum<E>, List<Listener<T>>> listenersMap = new ConcurrentHashMap();
  
  public boolean addListener(Listener<T> paramListener, Enum<E> paramEnum)
  {
    synchronized (paramEnum)
    {
      Object localObject1 = (List)this.listenersMap.get(paramEnum);
      if (localObject1 == null)
      {
        localObject1 = new CopyOnWriteArrayList();
        this.listenersMap.put(paramEnum, localObject1);
      }
      return ((List)localObject1).add(paramListener);
    }
  }
  
  public boolean removeListener(Listener<T> paramListener, Enum<E> paramEnum)
  {
    synchronized (paramEnum)
    {
      List localList = (List)this.listenersMap.get(paramEnum);
      if (localList != null) {
        return localList.remove(paramListener);
      }
    }
    return false;
  }
  
  public void notify(T paramT, Enum<E> paramEnum)
  {
    List localList = (List)this.listenersMap.get(paramEnum);
    if (localList != null) {
      for (Listener localListener : localList) {
        localListener.notify(paramT);
      }
    }
  }
}