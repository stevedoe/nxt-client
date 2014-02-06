package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Alias
{
  private static final ConcurrentMap<String, Alias> aliases = new ConcurrentHashMap();
  private static final ConcurrentMap<Long, Alias> aliasIdToAliasMappings = new ConcurrentHashMap();
  private static final Collection<Alias> allAliases = Collections.unmodifiableCollection(aliases.values());
  private final Account account;
  private final Long id;
  private final String aliasName;
  private volatile String aliasURI;
  private volatile int timestamp;
  
  public static Collection<Alias> getAllAliases()
  {
    return allAliases;
  }
  
  public static Alias getAlias(String paramString)
  {
    return (Alias)aliases.get(paramString);
  }
  
  public static Alias getAlias(Long paramLong)
  {
    return (Alias)aliasIdToAliasMappings.get(paramLong);
  }
  
  static void addOrUpdateAlias(Account paramAccount, Long paramLong, String paramString1, String paramString2, int paramInt)
  {
    String str = paramString1.toLowerCase();
    Alias localAlias1 = new Alias(paramAccount, paramLong, paramString1, paramString2, paramInt);
    Alias localAlias2 = (Alias)aliases.putIfAbsent(str, localAlias1);
    if (localAlias2 == null)
    {
      aliasIdToAliasMappings.putIfAbsent(paramLong, localAlias1);
    }
    else
    {
      localAlias2.aliasURI = paramString2;
      localAlias2.timestamp = paramInt;
    }
  }
  
  static void clear()
  {
    aliases.clear();
    aliasIdToAliasMappings.clear();
  }
  
  private Alias(Account paramAccount, Long paramLong, String paramString1, String paramString2, int paramInt)
  {
    this.account = paramAccount;
    this.id = paramLong;
    this.aliasName = paramString1;
    this.aliasURI = paramString2;
    this.timestamp = paramInt;
  }
  
  public Long getId()
  {
    return this.id;
  }
  
  public String getAliasName()
  {
    return this.aliasName;
  }
  
  public String getURI()
  {
    return this.aliasURI;
  }
  
  public int getTimestamp()
  {
    return this.timestamp;
  }
  
  public Account getAccount()
  {
    return this.account;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof Alias)) && (getId().equals(((Alias)paramObject).getId()));
  }
  
  public int hashCode()
  {
    return getId().hashCode();
  }
}