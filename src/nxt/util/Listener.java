package nxt.util;

public abstract interface Listener<T>
{
  public abstract void notify(T paramT);
}