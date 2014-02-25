package nxt;

import [B;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public abstract interface Attachment
{
  public abstract int getSize();
  
  public abstract byte[] getBytes();
  
  public abstract JSONStreamAware getJSON();
  
  public abstract TransactionType getTransactionType();
  
  public static final class MessagingArbitraryMessage
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
      localJSONObject.put("message", Convert.toHexString(this.message));
      
      return localJSONObject;
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.Messaging.ARBITRARY_MESSAGE;
    }
    
    public byte[] getMessage()
    {
      return this.message;
    }
  }
  
  public static final class MessagingAliasAssignment
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
    
    public TransactionType getTransactionType()
    {
      return TransactionType.Messaging.ALIAS_ASSIGNMENT;
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
  
  public static final class MessagingPollCreation
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final String pollName;
    private final String pollDescription;
    private final String[] pollOptions;
    private final byte minNumberOfOptions;
    private final byte maxNumberOfOptions;
    private final boolean optionsAreBinary;
    
    public MessagingPollCreation(String paramString1, String paramString2, String[] paramArrayOfString, byte paramByte1, byte paramByte2, boolean paramBoolean)
    {
      this.pollName = paramString1;
      this.pollDescription = paramString2;
      this.pollOptions = paramArrayOfString;
      this.minNumberOfOptions = paramByte1;
      this.maxNumberOfOptions = paramByte2;
      this.optionsAreBinary = paramBoolean;
    }
    
    public int getSize()
    {
      try
      {
        int i = 2 + this.pollName.getBytes("UTF-8").length + 2 + this.pollDescription.getBytes("UTF-8").length + 1;
        for (String str : this.pollOptions) {
          i += 2 + str.getBytes("UTF-8").length;
        }
        i += 3;
        return i;
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
        byte[] arrayOfByte1 = this.pollName.getBytes("UTF-8");
        byte[] arrayOfByte2 = this.pollDescription.getBytes("UTF-8");
        byte[][] arrayOfByte3 = new byte[this.pollOptions.length][];
        for (int i = 0; i < this.pollOptions.length; i++) {
          arrayOfByte3[i] = this.pollOptions[i].getBytes("UTF-8");
        }
        ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
        localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        localByteBuffer.putShort((short)arrayOfByte1.length);
        localByteBuffer.put(arrayOfByte1);
        localByteBuffer.putShort((short)arrayOfByte2.length);
        localByteBuffer.put(arrayOfByte2);
        localByteBuffer.put((byte)arrayOfByte3.length);
        for ([B local[B : arrayOfByte3)
        {
          localByteBuffer.putShort((short)local[B.length);
          localByteBuffer.put(local[B);
        }
        localByteBuffer.put(this.minNumberOfOptions);
        localByteBuffer.put(this.maxNumberOfOptions);
        localByteBuffer.put((byte)(this.optionsAreBinary ? 1 : 0));
        
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
      localJSONObject.put("name", this.pollName);
      localJSONObject.put("description", this.pollDescription);
      JSONArray localJSONArray = new JSONArray();
      Collections.addAll(localJSONArray, this.pollOptions);
      localJSONObject.put("options", localJSONArray);
      localJSONObject.put("minNumberOfOptions", Byte.valueOf(this.minNumberOfOptions));
      localJSONObject.put("maxNumberOfOptions", Byte.valueOf(this.maxNumberOfOptions));
      localJSONObject.put("optionsAreBinary", Boolean.valueOf(this.optionsAreBinary));
      
      return localJSONObject;
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.Messaging.POLL_CREATION;
    }
    
    public String getPollName()
    {
      return this.pollName;
    }
    
    public String getPollDescription()
    {
      return this.pollDescription;
    }
    
    public String[] getPollOptions()
    {
      return this.pollOptions;
    }
    
    public byte getMinNumberOfOptions()
    {
      return this.minNumberOfOptions;
    }
    
    public byte getMaxNumberOfOptions()
    {
      return this.maxNumberOfOptions;
    }
    
    public boolean isOptionsAreBinary()
    {
      return this.optionsAreBinary;
    }
  }
  
  public static final class MessagingVoteCasting
    implements Attachment, Serializable
  {
    static final long serialVersionUID = 0L;
    private final Long pollId;
    private final byte[] pollVote;
    
    public MessagingVoteCasting(Long paramLong, byte[] paramArrayOfByte)
    {
      this.pollId = paramLong;
      this.pollVote = paramArrayOfByte;
    }
    
    public int getSize()
    {
      return 9 + this.pollVote.length;
    }
    
    public byte[] getBytes()
    {
      ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.putLong(this.pollId.longValue());
      localByteBuffer.put((byte)this.pollVote.length);
      localByteBuffer.put(this.pollVote);
      
      return localByteBuffer.array();
    }
    
    public JSONStreamAware getJSON()
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("pollId", Convert.toUnsignedLong(this.pollId));
      JSONArray localJSONArray = new JSONArray();
      for (byte b : this.pollVote) {
        localJSONArray.add(Byte.valueOf(b));
      }
      localJSONObject.put("vote", localJSONArray);
      
      return localJSONObject;
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.Messaging.VOTE_CASTING;
    }
    
    public Long getPollId()
    {
      return this.pollId;
    }
    
    public byte[] getPollVote()
    {
      return this.pollVote;
    }
  }
  
  public static final class ColoredCoinsAssetIssuance
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
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.ASSET_ISSUANCE;
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
  
  public static final class ColoredCoinsAssetTransfer
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
      localJSONObject.put("asset", Convert.toUnsignedLong(this.assetId));
      localJSONObject.put("quantity", Integer.valueOf(this.quantity));
      
      return localJSONObject;
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.ASSET_TRANSFER;
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
      localJSONObject.put("asset", Convert.toUnsignedLong(this.assetId));
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
  
  public static final class ColoredCoinsAskOrderPlacement
    extends Attachment.ColoredCoinsOrderPlacement
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsAskOrderPlacement(Long paramLong, int paramInt, long paramLong1)
    {
      super(paramInt, paramLong1, null);
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
    }
  }
  
  public static final class ColoredCoinsBidOrderPlacement
    extends Attachment.ColoredCoinsOrderPlacement
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsBidOrderPlacement(Long paramLong, int paramInt, long paramLong1)
    {
      super(paramInt, paramLong1, null);
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
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
      localJSONObject.put("order", Convert.toUnsignedLong(this.orderId));
      
      return localJSONObject;
    }
    
    public Long getOrderId()
    {
      return this.orderId;
    }
  }
  
  public static final class ColoredCoinsAskOrderCancellation
    extends Attachment.ColoredCoinsOrderCancellation
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsAskOrderCancellation(Long paramLong)
    {
      super(null);
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
    }
  }
  
  public static final class ColoredCoinsBidOrderCancellation
    extends Attachment.ColoredCoinsOrderCancellation
  {
    static final long serialVersionUID = 0L;
    
    public ColoredCoinsBidOrderCancellation(Long paramLong)
    {
      super(null);
    }
    
    public TransactionType getTransactionType()
    {
      return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
    }
  }
}