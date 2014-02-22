package nxt.util;

public abstract interface Observable<T, E extends Enum<E>>
{
  public abstract boolean addListener(Listener<T> paramListener, E paramE);
  
  public abstract boolean removeListener(Listener<T> paramListener, E paramE);
}