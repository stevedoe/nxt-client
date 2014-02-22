package nxt;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class BlockImpl
  implements Block
{
  private final int version;
  private final int timestamp;
  private final Long previousBlockId;
  private final byte[] generatorPublicKey;
  private final byte[] previousBlockHash;
  private final int totalAmount;
  private final int totalFee;
  private final int payloadLength;
  private final byte[] generationSignature;
  private final byte[] payloadHash;
  private final List<Long> transactionIds;
  private final List<TransactionImpl> blockTransactions;
  private byte[] blockSignature;
  private BigInteger cumulativeDifficulty = BigInteger.ZERO;
  private long baseTarget = 153722867L;
  private volatile Long nextBlockId;
  private int height = -1;
  private volatile Long id;
  private volatile String stringId = null;
  private volatile Long generatorId;
  
  BlockImpl(int paramInt1, int paramInt2, Long paramLong, int paramInt3, int paramInt4, int paramInt5, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5, List<TransactionImpl> paramList)
    throws NxtException.ValidationException
  {
    if (paramList.size() > 255) {
      throw new NxtException.ValidationException("attempted to create a block with " + paramList.size() + " transactions");
    }
    if ((paramInt5 > 32640) || (paramInt5 < 0)) {
      throw new NxtException.ValidationException("attempted to create a block with payloadLength " + paramInt5);
    }
    this.version = paramInt1;
    this.timestamp = paramInt2;
    this.previousBlockId = paramLong;
    this.totalAmount = paramInt3;
    this.totalFee = paramInt4;
    this.payloadLength = paramInt5;
    this.payloadHash = paramArrayOfByte1;
    this.generatorPublicKey = paramArrayOfByte2;
    this.generationSignature = paramArrayOfByte3;
    this.blockSignature = paramArrayOfByte4;
    
    this.previousBlockHash = paramArrayOfByte5;
    this.blockTransactions = Collections.unmodifiableList(paramList);
    ArrayList localArrayList = new ArrayList(this.blockTransactions.size());
    Long localLong = Long.valueOf(-9223372036854775808L);
    for (Transaction localTransaction : this.blockTransactions)
    {
      if (localTransaction.getId().longValue() < localLong.longValue()) {
        throw new NxtException.ValidationException("Block transactions are not sorted!");
      }
      localArrayList.add(localTransaction.getId());
      localLong = localTransaction.getId();
    }
    this.transactionIds = Collections.unmodifiableList(localArrayList);
  }
  
  BlockImpl(int paramInt1, int paramInt2, Long paramLong1, int paramInt3, int paramInt4, int paramInt5, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4, byte[] paramArrayOfByte5, List<TransactionImpl> paramList, BigInteger paramBigInteger, long paramLong, Long paramLong2, int paramInt6, Long paramLong3)
    throws NxtException.ValidationException
  {
    this(paramInt1, paramInt2, paramLong1, paramInt3, paramInt4, paramInt5, paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, paramArrayOfByte4, paramArrayOfByte5, paramList);
    
    this.cumulativeDifficulty = paramBigInteger;
    this.baseTarget = paramLong;
    this.nextBlockId = paramLong2;
    this.height = paramInt6;
    this.id = paramLong3;
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
  
  public List<Long> getTransactionIds()
  {
    return this.transactionIds;
  }
  
  public byte[] getPayloadHash()
  {
    return this.payloadHash;
  }
  
  public byte[] getGenerationSignature()
  {
    return this.generationSignature;
  }
  
  public byte[] getBlockSignature()
  {
    return this.blockSignature;
  }
  
  public List<TransactionImpl> getTransactions()
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
  
  public int getHeight()
  {
    if (this.height == -1) {
      throw new IllegalStateException("Block height not yet set");
    }
    return this.height;
  }
  
  public Long getId()
  {
    if (this.id == null)
    {
      if (this.blockSignature == null) {
        throw new IllegalStateException("Block is not signed yet");
      }
      byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
      BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
      this.id = Long.valueOf(localBigInteger.longValue());
      this.stringId = localBigInteger.toString();
    }
    return this.id;
  }
  
  public String getStringId()
  {
    if (this.stringId == null)
    {
      getId();
      if (this.stringId == null) {
        this.stringId = Convert.toUnsignedLong(this.id);
      }
    }
    return this.stringId;
  }
  
  public Long getGeneratorId()
  {
    if (this.generatorId == null) {
      this.generatorId = Account.getId(this.generatorPublicKey);
    }
    return this.generatorId;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof BlockImpl)) && (getId().equals(((BlockImpl)paramObject).getId()));
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
    localByteBuffer.putInt(this.blockTransactions.size());
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
  
  public JSONObject getJSONObject()
  {
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("version", Integer.valueOf(this.version));
    localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
    localJSONObject.put("previousBlock", Convert.toUnsignedLong(this.previousBlockId));
    localJSONObject.put("numberOfTransactions", Integer.valueOf(this.blockTransactions.size()));
    localJSONObject.put("totalAmount", Integer.valueOf(this.totalAmount));
    localJSONObject.put("totalFee", Integer.valueOf(this.totalFee));
    localJSONObject.put("payloadLength", Integer.valueOf(this.payloadLength));
    localJSONObject.put("payloadHash", Convert.toHexString(this.payloadHash));
    localJSONObject.put("generatorPublicKey", Convert.toHexString(this.generatorPublicKey));
    localJSONObject.put("generationSignature", Convert.toHexString(this.generationSignature));
    if (this.version > 1) {
      localJSONObject.put("previousBlockHash", Convert.toHexString(this.previousBlockHash));
    }
    localJSONObject.put("blockSignature", Convert.toHexString(this.blockSignature));
    
    JSONArray localJSONArray = new JSONArray();
    for (Transaction localTransaction : this.blockTransactions) {
      localJSONArray.add(localTransaction.getJSONObject());
    }
    localJSONObject.put("transactions", localJSONArray);
    
    return localJSONObject;
  }
  
  void sign(String paramString)
  {
    if (this.blockSignature != null) {
      throw new IllegalStateException("Block already singed");
    }
    this.blockSignature = new byte[64];
    byte[] arrayOfByte1 = getBytes();
    byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
    this.blockSignature = Crypto.sign(arrayOfByte2, paramString);
  }
  
  boolean verifyBlockSignature()
  {
    Account localAccount = Account.getAccount(getGeneratorId());
    if (localAccount == null) {
      return false;
    }
    byte[] arrayOfByte1 = getBytes();
    byte[] arrayOfByte2 = new byte[arrayOfByte1.length - 64];
    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte2.length);
    
    return (Crypto.verify(this.blockSignature, arrayOfByte2, this.generatorPublicKey)) && (localAccount.setOrVerify(this.generatorPublicKey, this.height));
  }
  
  boolean verifyGenerationSignature()
    throws BlockchainProcessor.BlockOutOfOrderException
  {
    try
    {
      BlockImpl localBlockImpl = (BlockImpl)Nxt.getBlockchain().getBlock(this.previousBlockId);
      if (localBlockImpl == null) {
        throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing");
      }
      if ((this.version == 1) && (!Crypto.verify(this.generationSignature, localBlockImpl.generationSignature, this.generatorPublicKey))) {
        return false;
      }
      Account localAccount = Account.getAccount(getGeneratorId());
      if ((localAccount == null) || (localAccount.getEffectiveBalance() <= 0)) {
        return false;
      }
      int i = this.timestamp - localBlockImpl.timestamp;
      BigInteger localBigInteger1 = BigInteger.valueOf(Nxt.getBlockchain().getLastBlock().getBaseTarget()).multiply(BigInteger.valueOf(localAccount.getEffectiveBalance())).multiply(BigInteger.valueOf(i));
      
      MessageDigest localMessageDigest = Crypto.sha256();
      byte[] arrayOfByte;
      if (this.version == 1)
      {
        arrayOfByte = localMessageDigest.digest(this.generationSignature);
      }
      else
      {
        localMessageDigest.update(localBlockImpl.generationSignature);
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
    Account localAccount = Account.addOrGetAccount(getGeneratorId());
    if (!localAccount.setOrVerify(this.generatorPublicKey, this.height)) {
      throw new IllegalStateException("Generator public key mismatch");
    }
    localAccount.apply(this.height);
    localAccount.addToBalanceAndUnconfirmedBalance(this.totalFee * 100L);
  }
  
  void setPrevious(BlockImpl paramBlockImpl)
  {
    if (paramBlockImpl != null)
    {
      if (!paramBlockImpl.getId().equals(getPreviousBlockId())) {
        throw new IllegalStateException("Previous block id doesn't match");
      }
      this.height = (paramBlockImpl.getHeight() + 1);
      calculateBaseTarget(paramBlockImpl);
    }
    else
    {
      this.height = 0;
    }
    for (TransactionImpl localTransactionImpl : this.blockTransactions) {
      localTransactionImpl.setBlock(this);
    }
  }
  
  private void calculateBaseTarget(BlockImpl paramBlockImpl)
  {
    if ((getId().equals(Genesis.GENESIS_BLOCK_ID)) && (this.previousBlockId == null))
    {
      this.baseTarget = 153722867L;
      this.cumulativeDifficulty = BigInteger.ZERO;
    }
    else
    {
      long l1 = paramBlockImpl.baseTarget;
      long l2 = BigInteger.valueOf(l1).multiply(BigInteger.valueOf(this.timestamp - paramBlockImpl.timestamp)).divide(BigInteger.valueOf(60L)).longValue();
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
      this.cumulativeDifficulty = paramBlockImpl.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(this.baseTarget)));
    }
  }
}