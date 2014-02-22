package nxt;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

final class TransactionImpl
  implements Transaction
{
  private final short deadline;
  private final byte[] senderPublicKey;
  private final Long recipientId;
  private final int amount;
  private final int fee;
  private final Long referencedTransactionId;
  private final TransactionType type;
  private int height = 2147483647;
  private Long blockId;
  private volatile Block block;
  private byte[] signature;
  private int timestamp;
  private Attachment attachment;
  private volatile Long id;
  private volatile String stringId = null;
  private volatile Long senderId;
  private volatile String hash;
  static final int TRANSACTION_BYTES_LENGTH = 128;
  
  TransactionImpl(TransactionType paramTransactionType, int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
    throws NxtException.ValidationException
  {
    if ((paramInt1 == 0) && (Arrays.equals(paramArrayOfByte1, Genesis.CREATOR_PUBLIC_KEY)) ? (paramShort == 0) || (paramInt3 == 0) : (paramShort < 1) || (paramInt3 <= 0) || (paramInt3 > 1000000000L) || (paramInt2 < 0) || (paramInt2 > 1000000000L) || (paramTransactionType == null)) {
      throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + paramTransactionType + ", timestamp: " + paramInt1 + ", deadline: " + paramShort + ", fee: " + paramInt3 + ", amount: " + paramInt2);
    }
    this.timestamp = paramInt1;
    this.deadline = paramShort;
    this.senderPublicKey = paramArrayOfByte1;
    this.recipientId = paramLong1;
    this.amount = paramInt2;
    this.fee = paramInt3;
    this.referencedTransactionId = paramLong2;
    this.signature = paramArrayOfByte2;
    this.type = paramTransactionType;
  }
  
  TransactionImpl(TransactionType paramTransactionType, int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2, Long paramLong3, int paramInt4, Long paramLong4, Long paramLong5, Attachment paramAttachment)
    throws NxtException.ValidationException
  {
    this(paramTransactionType, paramInt1, paramShort, paramArrayOfByte1, paramLong1, paramInt2, paramInt3, paramLong2, paramArrayOfByte2);
    this.blockId = paramLong3;
    this.height = paramInt4;
    this.id = paramLong4;
    this.senderId = paramLong5;
    this.attachment = paramAttachment;
  }
  
  public short getDeadline()
  {
    return this.deadline;
  }
  
  public byte[] getSenderPublicKey()
  {
    return this.senderPublicKey;
  }
  
  public Long getRecipientId()
  {
    return this.recipientId;
  }
  
  public int getAmount()
  {
    return this.amount;
  }
  
  public int getFee()
  {
    return this.fee;
  }
  
  public Long getReferencedTransactionId()
  {
    return this.referencedTransactionId;
  }
  
  public int getHeight()
  {
    return this.height;
  }
  
  public byte[] getSignature()
  {
    return this.signature;
  }
  
  public TransactionType getType()
  {
    return this.type;
  }
  
  public Block getBlock()
  {
    if (this.block == null) {
      this.block = BlockDb.findBlock(this.blockId);
    }
    return this.block;
  }
  
  void setBlock(Block paramBlock)
  {
    this.block = paramBlock;
    this.blockId = paramBlock.getId();
    this.height = paramBlock.getHeight();
  }
  
  public int getTimestamp()
  {
    return this.timestamp;
  }
  
  public int getExpiration()
  {
    return this.timestamp + this.deadline * 60;
  }
  
  public Attachment getAttachment()
  {
    return this.attachment;
  }
  
  void setAttachment(Attachment paramAttachment)
  {
    this.attachment = paramAttachment;
  }
  
  public Long getId()
  {
    if (this.id == null)
    {
      if (this.signature == null) {
        throw new IllegalStateException("Transaction is not signed yet");
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
  
  public Long getSenderId()
  {
    if (this.senderId == null) {
      this.senderId = Account.getId(this.senderPublicKey);
    }
    return this.senderId;
  }
  
  public int compareTo(Transaction paramTransaction)
  {
    if (this.height < paramTransaction.getHeight()) {
      return -1;
    }
    if (this.height > paramTransaction.getHeight()) {
      return 1;
    }
    if (this.fee * ((TransactionImpl)paramTransaction).getSize() > paramTransaction.getFee() * getSize()) {
      return -1;
    }
    if (this.fee * ((TransactionImpl)paramTransaction).getSize() < paramTransaction.getFee() * getSize()) {
      return 1;
    }
    if (this.timestamp < paramTransaction.getTimestamp()) {
      return -1;
    }
    if (this.timestamp > paramTransaction.getTimestamp()) {
      return 1;
    }
    return 0;
  }
  
  int getSize()
  {
    return 128 + (this.attachment == null ? 0 : this.attachment.getSize());
  }
  
  public byte[] getBytes()
  {
    ByteBuffer localByteBuffer = ByteBuffer.allocate(getSize());
    localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    localByteBuffer.put(this.type.getType());
    localByteBuffer.put(this.type.getSubtype());
    localByteBuffer.putInt(this.timestamp);
    localByteBuffer.putShort(this.deadline);
    localByteBuffer.put(this.senderPublicKey);
    localByteBuffer.putLong(Convert.nullToZero(this.recipientId));
    localByteBuffer.putInt(this.amount);
    localByteBuffer.putInt(this.fee);
    localByteBuffer.putLong(Convert.nullToZero(this.referencedTransactionId));
    localByteBuffer.put(this.signature);
    if (this.attachment != null) {
      localByteBuffer.put(this.attachment.getBytes());
    }
    return localByteBuffer.array();
  }
  
  public JSONObject getJSONObject()
  {
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("type", Byte.valueOf(this.type.getType()));
    localJSONObject.put("subtype", Byte.valueOf(this.type.getSubtype()));
    localJSONObject.put("timestamp", Integer.valueOf(this.timestamp));
    localJSONObject.put("deadline", Short.valueOf(this.deadline));
    localJSONObject.put("senderPublicKey", Convert.toHexString(this.senderPublicKey));
    localJSONObject.put("recipient", Convert.toUnsignedLong(this.recipientId));
    localJSONObject.put("amount", Integer.valueOf(this.amount));
    localJSONObject.put("fee", Integer.valueOf(this.fee));
    localJSONObject.put("referencedTransaction", Convert.toUnsignedLong(this.referencedTransactionId));
    localJSONObject.put("signature", Convert.toHexString(this.signature));
    if (this.attachment != null) {
      localJSONObject.put("attachment", this.attachment.getJSON());
    }
    return localJSONObject;
  }
  
  public void sign(String paramString)
  {
    if (this.signature != null) {
      throw new IllegalStateException("Transaction already signed");
    }
    this.signature = new byte[64];
    this.signature = Crypto.sign(getBytes(), paramString);
    try
    {
      while (!verify())
      {
        this.timestamp += 1;
        
        this.signature = new byte[64];
        this.signature = Crypto.sign(getBytes(), paramString);
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logMessage("Error signing transaction", localRuntimeException);
    }
  }
  
  public String getHash()
  {
    if (this.hash == null)
    {
      byte[] arrayOfByte = getBytes();
      for (int i = 64; i < 128; i++) {
        arrayOfByte[i] = 0;
      }
      this.hash = Convert.toHexString(Crypto.sha256().digest(arrayOfByte));
    }
    return this.hash;
  }
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof TransactionImpl)) && (getId().equals(((Transaction)paramObject).getId()));
  }
  
  public int hashCode()
  {
    return getId().hashCode();
  }
  
  boolean verify()
  {
    Account localAccount = Account.getAccount(getSenderId());
    if (localAccount == null) {
      return false;
    }
    byte[] arrayOfByte = getBytes();
    for (int i = 64; i < 128; i++) {
      arrayOfByte[i] = 0;
    }
    return (Crypto.verify(this.signature, arrayOfByte, this.senderPublicKey)) && (localAccount.setOrVerify(this.senderPublicKey, getHeight()));
  }
  
  void validateAttachment()
    throws NxtException.ValidationException
  {
    this.type.validateAttachment(this);
  }
  
  boolean isDoubleSpending()
  {
    Account localAccount = Account.getAccount(getSenderId());
    if (localAccount == null) {
      return true;
    }
    synchronized (localAccount)
    {
      return this.type.isDoubleSpending(this, localAccount, this.amount + this.fee);
    }
  }
  
  void apply()
  {
    Account localAccount1 = Account.getAccount(getSenderId());
    if (!localAccount1.setOrVerify(this.senderPublicKey, getHeight())) {
      throw new RuntimeException("sender public key mismatch");
    }
    localAccount1.apply(getHeight());
    Account localAccount2 = Account.getAccount(this.recipientId);
    if (localAccount2 == null) {
      localAccount2 = Account.addOrGetAccount(this.recipientId);
    }
    localAccount1.addToBalanceAndUnconfirmedBalance(-(this.amount + this.fee) * 100L);
    this.type.apply(this, localAccount1, localAccount2);
  }
  
  void undo()
    throws TransactionType.UndoNotSupportedException
  {
    Account localAccount1 = Account.getAccount(this.senderId);
    localAccount1.undo(getHeight());
    localAccount1.addToBalance((this.amount + this.fee) * 100L);
    Account localAccount2 = Account.getAccount(this.recipientId);
    this.type.undo(this, localAccount1, localAccount2);
  }
  
  void updateTotals(Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1)
  {
    Long localLong1 = getSenderId();
    Long localLong2 = (Long)paramMap.get(localLong1);
    if (localLong2 == null) {
      localLong2 = Long.valueOf(0L);
    }
    paramMap.put(localLong1, Long.valueOf(localLong2.longValue() + (this.amount + this.fee) * 100L));
    this.type.updateTotals(this, paramMap, paramMap1, localLong2);
  }
  
  boolean isDuplicate(Map<TransactionType, Set<String>> paramMap)
  {
    return this.type.isDuplicate(this, paramMap);
  }
}