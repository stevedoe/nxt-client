package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.util.Convert;

public final class Asset
{
  private static final ConcurrentMap<Long, Asset> assets = new ConcurrentHashMap();
  private static final ConcurrentMap<String, Asset> assetNameToAssetMappings = new ConcurrentHashMap();
  private static final Collection<Asset> allAssets = Collections.unmodifiableCollection(assets.values());
  private final Long assetId;
  private final Long accountId;
  private final String name;
  private final String description;
  private final int quantity;
  
  public static Collection<Asset> getAllAssets()
  {
    return allAssets;
  }
  
  public static Asset getAsset(Long paramLong)
  {
    return (Asset)assets.get(paramLong);
  }
  
  public static Asset getAsset(String paramString)
  {
    return (Asset)assetNameToAssetMappings.get(paramString);
  }
  
  static void addAsset(Long paramLong1, Long paramLong2, String paramString1, String paramString2, int paramInt)
  {
    Asset localAsset = new Asset(paramLong1, paramLong2, paramString1, paramString2, paramInt);
    if (assets.putIfAbsent(paramLong1, localAsset) != null) {
      throw new IllegalStateException("Asset with id " + Convert.toUnsignedLong(paramLong1) + " already exists");
    }
    if (assetNameToAssetMappings.putIfAbsent(paramString1.toLowerCase(), localAsset) != null) {
      throw new IllegalStateException("Asset with name " + paramString1.toLowerCase() + " already exists");
    }
  }
  
  static void removeAsset(Long paramLong)
  {
    Asset localAsset = (Asset)assets.remove(paramLong);
    assetNameToAssetMappings.remove(localAsset.getName());
  }
  
  static void clear()
  {
    assets.clear();
    assetNameToAssetMappings.clear();
  }
  
  private Asset(Long paramLong1, Long paramLong2, String paramString1, String paramString2, int paramInt)
  {
    this.assetId = paramLong1;
    this.accountId = paramLong2;
    this.name = paramString1;
    this.description = paramString2;
    this.quantity = paramInt;
  }
  
  public Long getId()
  {
    return this.assetId;
  }
  
  public Long getAccountId()
  {
    return this.accountId;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getDescription()
  {
    return this.description;
  }
  
  public int getQuantity()
  {
    return this.quantity;
  }
}