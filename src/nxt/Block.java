package nxt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class Block
  implements Serializable
{
  static final long serialVersionUID = 0L;
  static final Long[] emptyLong = new Long[0];
  static final Transaction[] emptyTransactions = new Transaction[0];
  public static final Comparator<Block> heightComparator = new Comparator()
  {
    public int compare(Block paramAnonymousBlock1, Block paramAnonymousBlock2)
    {
      return paramAnonymousBlock1.height > paramAnonymousBlock2.height ? 1 : paramAnonymousBlock1.height < paramAnonymousBlock2.height ? -1 : 0;
    }
  };
  private final int version;
  private final int timestamp;
  private final Long previousBlockId;
  private final byte[] generatorPublicKey;
  private final byte[] previousBlockHash;
  private final int totalAmount;
  private final int totalFee;
  private final int payloadLength;
  final Long[] transactionIds;
  transient Transaction[] blockTransactions;
  
  static Block getBlock(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    try
    {
      int i = ((Long)paramJSONObject.get("version")).intValue();
      int j = ((Long)paramJSONObject.get("timestamp")).intValue();
      Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("previousBlock"));
      int k = ((Long)paramJSONObject.get("numberOfTransactions")).intValue();
      int m = ((Long)paramJSONObject.get("totalAmount")).intValue();
      int n = ((Long)paramJSONObject.get("totalFee")).intValue();
      int i1 = ((Long)paramJSONObject.get("payloadLength")).intValue();
      byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("payloadHash"));
      byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("generatorPublicKey"));
      byte[] arrayOfByte3 = Convert.convert((String)paramJSONObject.get("generationSignature"));
      byte[] arrayOfByte4 = Convert.convert((String)paramJSONObject.get("blockSignature"));
      byte[] arrayOfByte5 = i == 1 ? null : Convert.convert((String)paramJSONObject.get("previousBlockHash"));
      if ((k > 255) || (i1 > 32640)) {
        throw new NxtException.ValidationException("Invalid number of transactions or payload length");
      }
      return new Block(i, j, localLong, k, m, n, i1, arrayOfByte1, arrayOfByte2, arrayOfByte3, arrayOfByte4, arrayOfByte5);
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString(), localRuntimeException);
    }
  }
  
  public BigInteger cumulativeDifficulty = BigInteger.ZERO;
  public long baseTarget = 153722867L;
  public volatile Long nextBlockId;
  public int index;
  public int height;
  private byte[] generationSignature;
  private byte[] blockSignature;
  private byte[] payloadHash;
  private volatile transient Long id;
  private volatile transient String stringId = null;
  private volatile transient Long generatorAccountId;
  private transient SoftReference<JSONStreamAware> jsonRef;
  
  Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
    throws NxtException.ValidationException
  {
    this(paramInt1, paramInt2, paramLong, paramInt3, paramInt4, paramInt5, paramInt6, paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, paramArrayOfByte4, null);
  }
  
  public Block(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, int paramInt6, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5)
    throws NxtException.ValidationException
  {
    if ((paramInt3 > 255) || (paramInt3 < 0)) {
      throw new NxtException.ValidationException("attempted to create a block with " + paramInt3 + " transactions");
    }
    if ((paramInt6 > 32640) || (paramInt6 < 0)) {
      throw new NxtException.ValidationException("attempted to create a block with payloadLength " + paramInt6);
    }
    this.version = paramInt1;
    this.timestamp = paramInt2;
    this.previousBlockId = paramLong;
    this.totalAmount = paramInt4;
    this.totalFee = paramInt5;
    this.payloadLength = paramInt6;
    this.payloadHash = paramArrayOfByte1;
    this.generatorPublicKey = paramArrayOfByte2;
    this.generationSignature = paramArrayOfByte3;
    this.blockSignature = paramArrayOfByte4;
    
    this.previousBlockHash = paramArrayOfByte5;
    this.transactionIds = (paramInt3 == 0 ? emptyLong : new Long[paramInt3]);
    this.blockTransactions = (paramInt3 == 0 ? emptyTransactions : new Transaction[paramInt3]);
  }
  
  public int getVersion()
  {
    return this.version;
  }
  
  public int getTimestamp()
  {
    return this.timestamp;
  }
  
  public Long getPreviousBlockId()
  {
    return this.previousBlockId;
  }
  
  public byte[] getGeneratorPublicKey()
  {
    return this.generatorPublicKey;
  }
  
  public byte[] getPreviousBlockHash()
  {
    return this.previousBlockHash;
  }
  
  public int getTotalAmount()
  {
    return this.totalAmount;
  }
  
  public int getTotalFee()
  {
    return this.totalFee;
  }
  
  public int getPayloadLength()
  {
    return this.payloadLength;
  }
  
  public Long[] getTransactionIds()
  {
    return this.transactionIds;
  }
  
  public byte[] getPayloadHash()
  {
    return this.payloadHash;
  }
  
  void setPayloadHash(byte[] paramArrayOfByte)
  {
    this.payloadHash = paramArrayOfByte;
  }
  
  public byte[] getGenerationSignature()
  {
    return this.generationSignature;
  }
  
  void setGenerationSignature(byte[] paramArrayOfByte)
  {
    this.generationSignature = paramArrayOfByte;
  }
  
  public byte[] getBlockSignature()
  {
    return this.blockSignature;
  }
  
  void setBlockSignature(byte[] paramArrayOfByte)
  {
    this.blockSignature = paramArrayOfByte;
  }
  
  public Transaction[] getTransactions()
  {
    return this.blockTransactions;
  }
  
  public long getBaseTarget()
  {
    return this.baseTarget;
  }
  
  public BigInteger getCumulativeDifficulty()
  {
    return this.cumulativeDifficulty;
  }
  
  public Long getNextBlockId()
  {
    return this.nextBlockId;
  }
  
  public int getIndex()
  {
    return this.index;
  }
  
  void setIndex(int paramInt)
  {
    this.index = paramInt;
  }
  
  public int getHeight()
  {
    return this.height;
  }
  
  void setHeight(int paramInt)
  {
    this.height = paramInt;
  }
  
  public Long getId()
  {
    calculateIds();
    return this.id;
  }
  
  public String getStringId()
  {
    calculateIds();
    return this.stringId;
  }
  
  public Long getGeneratorAccountId()
  {
    calculateIds();
    return this.generatorAccountId;
  }
  
  public synchronized JSONStreamAware getJSON()
  {
    if (this.jsonRef != null)
    {
      localJSONStreamAware = (JSONStreamAware)this.jsonRef.get();
      if (localJSONStreamAware != null) {
        return localJSONStreamAware;
      }
    }
    JSONStreamAware localJSONStreamAware = JSON.prepare(getJSONObject());
    this.jsonRef = new SoftReference(localJSONStreamAware);
    return localJSONStreamAware;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof Block)) && (getId().equals(((Block)paramObject).getId()));
  }
  
  public int hashCode()
  {
    return getId().hashCode();
  }
  
  byte[] getBytes()
  {
    ByteBuffer localByteBuffer = ByteBuffer.allocate(224);
    localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    localByteBuffer.putInt(this.version);
    localByteBuffer.putInt(this.timestamp);
    localByteBuffer.putLong(Convert.nullToZero(this.previousBlockId));
    localByteBuffer.putInt(this.transactionIds.length);
    localByteBuffer.putInt(this.totalAmount);
    localByteBuffer.putInt(this.totalFee);
    localByteBuffer.putInt(this.payloadLength);
    localByteBuffer.put(this.payloadHash);
    localByteBuffer.put(this.generatorPublicKey);
    localByteBuffer.put(this.generationSignature);
    if (this.version > 1) {
      localByteBuffer.put(this.previousBlockHash);
    }
    localByteBuffer.put(this.blockSignature);
    return localByteBuffer.array();
  }
  
  JSONObject getJSONObject()
  {
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("version", Integer.valueOf(this.version));
    localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
    localJSONObject.put("previousBlock", Convert.convert(this.previousBlockId));
    localJSONObject.put("numberOfTransactions", Integer.valueOf(this.transactionIds.length));
    localJSONObject.put("totalAmount", Integer.valueOf(this.totalAmount));
    localJSONObject.put("totalFee", Integer.valueOf(this.totalFee));
    localJSONObject.put("payloadLength", Integer.valueOf(this.payloadLength));
    localJSONObject.put("payloadHash", Convert.convert(this.payloadHash));
    localJSONObject.put("generatorPublicKey", Convert.convert(this.generatorPublicKey));
    localJSONObject.put("generationSignature", Convert.convert(this.generationSignature));
    if (this.version > 1) {
      localJSONObject.put("previousBlockHash", Convert.convert(this.previousBlockHash));
    }
    localJSONObject.put("blockSignature", Convert.convert(this.blockSignature));
    
    JSONArray localJSONArray = new JSONArray();
    for (Transaction localTransaction : this.blockTransactions) {
      localJSONArray.add(localTransaction.getJSONObject());
    }
    localJSONObject.put("transactions", localJSONArray);
    
    return localJSONObject;
  }
  
  boolean verifyBlockSignature()
  {
    Account localAccount = Account.getAccount(getGeneratorAccountId());
    if (localAccount == null) {
      return false;
    }
    byte[] arrayOfByte1 = getBytes();
    byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
    
    return (Crypto.verify(this.blockSignature, arrayOfByte2, this.generatorPublicKey)) && (localAccount.setOrVerify(this.generatorPublicKey));
  }
  
  boolean verifyGenerationSignature()
  {
    try
    {
      Block localBlock = Blockchain.getBlock(this.previousBlockId);
      if (localBlock == null) {
        return false;
      }
      if ((this.version == 1) && (!Crypto.verify(this.generationSignature, localBlock.generationSignature, this.generatorPublicKey))) {
        return false;
      }
      Account localAccount = Account.getAccount(getGeneratorAccountId());
      if ((localAccount == null) || (localAccount.getEffectiveBalance() <= 0)) {
        return false;
      }
      int i = this.timestamp - localBlock.timestamp;
      BigInteger localBigInteger1 = BigInteger.valueOf(Blockchain.getLastBlock().baseTarget).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
      
      MessageDigest localMessageDigest = Crypto.sha256();
      byte[] arrayOfByte;
      if (this.version == 1)
      {
        arrayOfByte = localMessageDigest.digest(this.generationSignature);
      }
      else
      {
        localMessageDigest.update(localBlock.generationSignature);
        arrayOfByte = localMessageDigest.digest(this.generatorPublicKey);
        if (!Arrays.equals(this.generationSignature, arrayOfByte)) {
          return false;
        }
      }
      BigInteger localBigInteger2 = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      
      return localBigInteger2.compareTo(localBigInteger1) < 0;
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logMessage("Error verifying block generation signature", localRuntimeException);
    }
    return false;
  }
  
  void apply()
  {
    for (int i = 0; i < this.transactionIds.length; i++)
    {
      this.blockTransactions[i] = Blockchain.getTransaction(this.transactionIds[i]);
      if (this.blockTransactions[i] == null) {
        throw new IllegalStateException("Missing transaction " + Convert.convert(this.transactionIds[i]));
      }
    }
    if ((this.previousBlockId == null) && (getId().equals(Genesis.GENESIS_BLOCK_ID)))
    {
      calculateBaseTarget();
      Blockchain.addBlock(this);
    }
    else
    {
      localObject = Blockchain.getLastBlock();
      
      ((Block)localObject).nextBlockId = getId();
      this.height = (((Block)localObject).height + 1);
      calculateBaseTarget();
      Blockchain.addBlock(this);
    }
    Object localObject = Account.addOrGetAccount(getGeneratorAccountId());
    if (!((Account)localObject).setOrVerify(this.generatorPublicKey)) {
      throw new IllegalStateException("Generator public key mismatch");
    }
    ((Account)localObject).addToBalanceAndUnconfirmedBalance(this.totalFee * 100L);
    for (Transaction localTransaction : this.blockTransactions)
    {
      localTransaction.setHeight(this.height);
      localTransaction.setBlockId(getId());
      localTransaction.apply();
    }
    Blockchain.purgeExpiredHashes(this.timestamp);
  }
  
  private void calculateBaseTarget()
  {
    if ((getId().equals(Genesis.GENESIS_BLOCK_ID)) && (this.previousBlockId == null))
    {
      this.baseTarget = 153722867L;
      this.cumulativeDifficulty = BigInteger.ZERO;
    }
    else
    {
      Block localBlock = Blockchain.getBlock(this.previousBlockId);
      long l1 = localBlock.baseTarget;
      long l2 = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(this.timestamp - localBlock.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
      if ((l2 < 0L) || (l2 > 153722867000000000L)) {
        l2 = 153722867000000000L;
      }
      if (l2 < l1 / 2L) {
        l2 = l1 / 2L;
      }
      if (l2 == 0L) {
        l2 = 1L;
      }
      long l3 = l1 * 2L;
      if (l3 < 0L) {
        l3 = 153722867000000000L;
      }
      if (l2 > l3) {
        l2 = l3;
      }
      this.baseTarget = l2;
      this.cumulativeDifficulty = localBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(this.baseTarget)));
    }
  }
  
  private void calculateIds()
  {
    if (this.stringId != null) {
      return;
    }
    byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
    BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
    this.id = Long.valueOf(localBigInteger.longValue());
    this.stringId = localBigInteger.toString();
    this.generatorAccountId = Account.getId(this.generatorPublicKey);
  }
  
  private void readObject(ObjectInputStream paramObjectInputStream)
    throws IOException, ClassNotFoundException
  {
    paramObjectInputStream.defaultReadObject();
    this.blockTransactions = (this.transactionIds.length == 0 ? emptyTransactions : new Transaction[this.transactionIds.length]);
    for (int i = 0; i < this.transactionIds.length; i++) {
      this.blockTransactions[i] = Blockchain.getTransaction(this.transactionIds[i]);
    }
  }
}