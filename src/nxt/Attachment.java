package nxt;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public abstract interface Attachment
{
  public abstract int getSize();
  
  public abstract byte[] getBytes();
  
  public abstract JSONStreamAware getJSON();
  
  public abstract Transaction.Type getTransactionType();
  
  public static class MessagingArbitraryMessage
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final byte[] message;
    
    public MessagingArbitraryMessage(byte[] paramArrayOfByte)
    {
      this.message = paramArrayOfByte;
    }
    
    public int getSize()
    {
      return 4 + this.message.length;
    }
    
    public byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putInt(this.message.length);
      localByteBuffer.put(this.message);
      
      return localByteBuffer.array();
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("message", Convert.convert(this.message));
      
      return localJSONObject;
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
    }
    
    public byte[] getMessage()
    {
      return this.message;
    }
  }
  
  public static class MessagingAliasAssignment
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final String aliasName;
    private final String aliasURI;
    
    public MessagingAliasAssignment(String paramString1, String paramString2)
    {
      this.aliasName = paramString1.trim().intern();
      this.aliasURI = paramString2.trim().intern();
    }
    
    public int getSize()
    {
      try
      {
        return 1 + this.aliasName.getBytes("UTF-8").length + 2 + this.aliasURI.getBytes("UTF-8").length;
      }
      catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
      {
        Logger.logMessage("Error in getBytes", localRuntimeException);
      }
      return 0;
    }
    
    public byte[] getBytes()
    {
      try
      {
        byte[] arrayOfByte1 = this.aliasName.getBytes("UTF-8");
        byte[] arrayOfByte2 = this.aliasURI.getBytes("UTF-8");
        
        ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.put((byte)arrayOfByte1.length);
        localByteBuffer.put(arrayOfByte1);
        localByteBuffer.putShort((short)arrayOfByte2.length);
        localByteBuffer.put(arrayOfByte2);
        
        return localByteBuffer.array();
      }
      catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
      {
        Logger.logMessage("Error in getBytes", localRuntimeException);
      }
      return null;
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("alias", this.aliasName);
      localJSONObject.put("uri", this.aliasURI);
      
      return localJSONObject;
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
    }
    
    public String getAliasName()
    {
      return this.aliasName;
    }
    
    public String getAliasURI()
    {
      return this.aliasURI;
    }
  }
  
  public static class ColoredCoinsAssetIssuance
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final String name;
    private final String description;
    private final int quantity;
    
    public ColoredCoinsAssetIssuance(String paramString1, String paramString2, int paramInt)
    {
      this.name = paramString1;
      this.description = (paramString2 == null ? "" : paramString2);
      this.quantity = paramInt;
    }
    
    public int getSize()
    {
      try
      {
        return 1 + this.name.getBytes("UTF-8").length + 2 + this.description.getBytes("UTF-8").length + 4;
      }
      catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
      {
        Logger.logMessage("Error in getBytes", localRuntimeException);
      }
      return 0;
    }
    
    public byte[] getBytes()
    {
      try
      {
        byte[] arrayOfByte1 = this.name.getBytes("UTF-8");
        byte[] arrayOfByte2 = this.description.getBytes("UTF-8");
        
        ByteBuffer localByteBuffer = ByteBuffer.allocate(1 + arrayOfByte1.length + 2 + arrayOfByte2.length + 4);
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.put((byte)arrayOfByte1.length);
        localByteBuffer.put(arrayOfByte1);
        localByteBuffer.putShort((short)arrayOfByte2.length);
        localByteBuffer.put(arrayOfByte2);
        localByteBuffer.putInt(this.quantity);
        
        return localByteBuffer.array();
      }
      catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
      {
        Logger.logMessage("Error in getBytes", localRuntimeException);
      }
      return null;
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("name", this.name);
      localJSONObject.put("description", this.description);
      localJSONObject.put("quantity", Integer.valueOf(this.quantity));
      
      return localJSONObject;
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
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
  
  public static class ColoredCoinsAssetTransfer
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final Long assetId;
    private final int quantity;
    
    public ColoredCoinsAssetTransfer(Long paramLong, int paramInt)
    {
      this.assetId = paramLong;
      this.quantity = paramInt;
    }
    
    public int getSize()
    {
      return 12;
    }
    
    public byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putLong(Convert.nullToZero(this.assetId));
      localByteBuffer.putInt(this.quantity);
      
      return localByteBuffer.array();
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("asset", Convert.convert(this.assetId));
      localJSONObject.put("quantity", Integer.valueOf(this.quantity));
      
      return localJSONObject;
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
    }
    
    public Long getAssetId()
    {
      return this.assetId;
    }
    
    public int getQuantity()
    {
      return this.quantity;
    }
  }
  
  public static abstract class ColoredCoinsOrderPlacement
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final Long assetId;
    private final int quantity;
    private final long price;
    
    private ColoredCoinsOrderPlacement(Long paramLong, int paramInt, long paramLong1)
    {
      this.assetId = paramLong;
      this.quantity = paramInt;
      this.price = paramLong1;
    }
    
    public int getSize()
    {
      return 20;
    }
    
    public byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putLong(Convert.nullToZero(this.assetId));
      localByteBuffer.putInt(this.quantity);
      localByteBuffer.putLong(this.price);
      
      return localByteBuffer.array();
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("asset", Convert.convert(this.assetId));
      localJSONObject.put("quantity", Integer.valueOf(this.quantity));
      localJSONObject.put("price", Long.valueOf(this.price));
      
      return localJSONObject;
    }
    
    public Long getAssetId()
    {
      return this.assetId;
    }
    
    public int getQuantity()
    {
      return this.quantity;
    }
    
    public long getPrice()
    {
      return this.price;
    }
  }
  
  public static class ColoredCoinsAskOrderPlacement
    extends Attachment.ColoredCoinsOrderPlacement
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsAskOrderPlacement(Long paramLong, int paramInt, long paramLong1)
    {
      super(paramInt, paramLong1, null);
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
    }
  }
  
  public static class ColoredCoinsBidOrderPlacement
    extends Attachment.ColoredCoinsOrderPlacement
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsBidOrderPlacement(Long paramLong, int paramInt, long paramLong1)
    {
      super(paramInt, paramLong1, null);
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
    }
  }
  
  public static abstract class ColoredCoinsOrderCancellation
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final Long orderId;
    
    private ColoredCoinsOrderCancellation(Long paramLong)
    {
      this.orderId = paramLong;
    }
    
    public int getSize()
    {
      return 8;
    }
    
    public byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putLong(Convert.nullToZero(this.orderId));
      
      return localByteBuffer.array();
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("order", Convert.convert(this.orderId));
      
      return localJSONObject;
    }
    
    public Long getOrderId()
    {
      return this.orderId;
    }
  }
  
  public static class ColoredCoinsAskOrderCancellation
    extends Attachment.ColoredCoinsOrderCancellation
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsAskOrderCancellation(Long paramLong)
    {
      super(null);
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
    }
  }
  
  public static class ColoredCoinsBidOrderCancellation
    extends Attachment.ColoredCoinsOrderCancellation
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsBidOrderCancellation(Long paramLong)
    {
      super(null);
    }
    
    public Transaction.Type getTransactionType()
    {
      return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
    }
  }
}