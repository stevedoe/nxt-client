package nxt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;

public final class Transaction
  implements Comparable<Transaction>, Serializable
{
  static final long serialVersionUID = 0L;
  private static final byte TYPE_PAYMENT = 0;
  private static final byte TYPE_MESSAGING = 1;
  private static final byte TYPE_COLORED_COINS = 2;
  private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;
  private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
  private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
  private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
  private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
  private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
  private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
  private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
  private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
  public static final Comparator<Transaction> timestampComparator = new Comparator()
  {
    public int compare(Transaction paramAnonymousTransaction1, Transaction paramAnonymousTransaction2)
    {
      return paramAnonymousTransaction1.timestamp > paramAnonymousTransaction2.timestamp ? 1 : paramAnonymousTransaction1.timestamp < paramAnonymousTransaction2.timestamp ? -1 : 0;
    }
  };
  private final short deadline;
  private final byte[] senderPublicKey;
  private final Long recipientId;
  private final int amount;
  private final int fee;
  private final Long referencedTransactionId;
  public int index;
  public int height;
  public Long blockId;
  public byte[] signature;
  private int timestamp;
  private transient Type type;
  private Attachment attachment;
  private volatile transient Long id;
  
  public static Transaction getTransaction(byte[] paramArrayOfByte)
    throws NxtException.ValidationException
  {
    try
    {
      ByteBuffer localByteBuffer = ByteBuffer.wrap(paramArrayOfByte);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      
      byte b1 = localByteBuffer.get();
      byte b2 = localByteBuffer.get();
      int i = localByteBuffer.getInt();
      short s = localByteBuffer.getShort();
      byte[] arrayOfByte1 = new byte[32];
      localByteBuffer.get(arrayOfByte1);
      Long localLong1 = Long.valueOf(localByteBuffer.getLong());
      int j = localByteBuffer.getInt();
      int k = localByteBuffer.getInt();
      Long localLong2 = Convert.zeroToNull(localByteBuffer.getLong());
      byte[] arrayOfByte2 = new byte[64];
      localByteBuffer.get(arrayOfByte2);
      
      Type localType = findTransactionType(b1, b2);
      Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
      if (!localType.loadAttachment(localTransaction, localByteBuffer)) {
        throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localTransaction.attachment.getJSON());
      }
      return localTransaction;
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString());
    }
  }
  
  public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2)
    throws NxtException.ValidationException
  {
    return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
  }
  
  public static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, Attachment paramAttachment)
    throws NxtException.ValidationException
  {
    Transaction localTransaction = new Transaction(paramAttachment.getTransactionType(), paramInt1, paramShort, paramArrayOfByte, paramLong1, paramInt2, paramInt3, paramLong2, null);
    
    localTransaction.attachment = paramAttachment;
    return localTransaction;
  }
  
  static Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
    throws NxtException.ValidationException
  {
    return new Transaction(Transaction.Type.Payment.ORDINARY, paramInt1, paramShort, paramArrayOfByte1, paramLong1, paramInt2, paramInt3, paramLong2, paramArrayOfByte2);
  }
  
  static Transaction getTransaction(JSONObject paramJSONObject)
    throws NxtException.ValidationException
  {
    try
    {
      byte b1 = ((Long)paramJSONObject.get("type")).byteValue();
      byte b2 = ((Long)paramJSONObject.get("subtype")).byteValue();
      int i = ((Long)paramJSONObject.get("timestamp")).intValue();
      short s = ((Long)paramJSONObject.get("deadline")).shortValue();
      byte[] arrayOfByte1 = Convert.convert((String)paramJSONObject.get("senderPublicKey"));
      Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("recipient"));
      if (localLong1 == null) {
        localLong1 = Long.valueOf(0L);
      }
      int j = ((Long)paramJSONObject.get("amount")).intValue();
      int k = ((Long)paramJSONObject.get("fee")).intValue();
      Long localLong2 = Convert.parseUnsignedLong((String)paramJSONObject.get("referencedTransaction"));
      byte[] arrayOfByte2 = Convert.convert((String)paramJSONObject.get("signature"));
      
      Type localType = findTransactionType(b1, b2);
      Transaction localTransaction = new Transaction(localType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2);
      

      JSONObject localJSONObject = (JSONObject)paramJSONObject.get("attachment");
      if (!localType.loadAttachment(localTransaction, localJSONObject)) {
        throw new NxtException.ValidationException("Invalid transaction attachment:\n" + localJSONObject.toJSONString());
      }
      return localTransaction;
    }
    catch (RuntimeException localRuntimeException)
    {
      throw new NxtException.ValidationException(localRuntimeException.toString());
    }
  }
  
  private volatile transient String stringId = null;
  private volatile transient Long senderAccountId;
  private volatile transient String hash;
  private static final int TRANSACTION_BYTES_LENGTH = 128;
  
  private Transaction(Type paramType, int paramInt1, short paramShort, byte[] paramArrayOfByte1, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, byte[] paramArrayOfByte2)
    throws NxtException.ValidationException
  {
    if ((paramInt1 == 0) && (Arrays.equals(paramArrayOfByte1, Genesis.CREATOR_PUBLIC_KEY)) ? (paramShort == 0) || (paramInt3 == 0) : (paramShort < 1) || (paramInt3 <= 0) || (paramInt3 > 1000000000L) || (paramInt2 < 0) || (paramInt2 > 1000000000L) || (paramType == null)) {
      throw new NxtException.ValidationException("Invalid transaction parameters:\n type: " + paramType + ", timestamp: " + paramInt1 + ", deadline: " + paramShort + ", fee: " + paramInt3 + ", amount: " + paramInt2);
    }
    this.timestamp = paramInt1;
    this.deadline = paramShort;
    this.senderPublicKey = paramArrayOfByte1;
    this.recipientId = paramLong1;
    this.amount = paramInt2;
    this.fee = paramInt3;
    this.referencedTransactionId = paramLong2;
    this.signature = paramArrayOfByte2;
    this.type = paramType;
    this.height = 2147483647;
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
  
  public Type getType()
  {
    return this.type;
  }
  
  public Block getBlock()
  {
    return Blockchain.getBlock(this.blockId);
  }
  
  void setBlockId(Long paramLong)
  {
    this.blockId = paramLong;
  }
  
  void setHeight(int paramInt)
  {
    this.height = paramInt;
  }
  
  public int getIndex()
  {
    return this.index;
  }
  
  void setIndex(int paramInt)
  {
    this.index = paramInt;
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
  
  public Long getSenderAccountId()
  {
    calculateIds();
    return this.senderAccountId;
  }
  
  public int compareTo(Transaction paramTransaction)
  {
    if (this.height < paramTransaction.height) {
      return -1;
    }
    if (this.height > paramTransaction.height) {
      return 1;
    }
    if (this.fee * paramTransaction.getSize() > paramTransaction.fee * getSize()) {
      return -1;
    }
    if (this.fee * paramTransaction.getSize() < paramTransaction.fee * getSize()) {
      return 1;
    }
    if (this.timestamp < paramTransaction.timestamp) {
      return -1;
    }
    if (this.timestamp > paramTransaction.timestamp) {
      return 1;
    }
    if (this.index < paramTransaction.index) {
      return -1;
    }
    if (this.index > paramTransaction.index) {
      return 1;
    }
    return 0;
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
    localJSONObject.put("senderPublicKey", Convert.convert(this.senderPublicKey));
    localJSONObject.put("recipient", Convert.convert(this.recipientId));
    localJSONObject.put("amount", Integer.valueOf(this.amount));
    localJSONObject.put("fee", Integer.valueOf(this.fee));
    localJSONObject.put("referencedTransaction", Convert.convert(this.referencedTransactionId));
    localJSONObject.put("signature", Convert.convert(this.signature));
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
  
  public boolean equals(Object paramObject)
  {
    return ((paramObject instanceof Transaction)) && (getId().equals(((Transaction)paramObject).getId()));
  }
  
  public int hashCode()
  {
    return getId().hashCode();
  }
  
  boolean verify()
  {
    Account localAccount = Account.getAccount(getSenderAccountId());
    if (localAccount == null) {
      return false;
    }
    byte[] arrayOfByte = getBytes();
    for (int i = 64; i < 128; i++) {
      arrayOfByte[i] = 0;
    }
    return (Crypto.verify(this.signature, arrayOfByte, this.senderPublicKey)) && (localAccount.setOrVerify(this.senderPublicKey));
  }
  
  boolean isDoubleSpending()
  {
    Account localAccount = Account.getAccount(getSenderAccountId());
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
    Account localAccount1 = Account.getAccount(getSenderAccountId());
    if (!localAccount1.setOrVerify(this.senderPublicKey)) {
      throw new RuntimeException("sender public key mismatch");
    }
    Blockchain.transactionHashes.put(getHash(), this);
    Account localAccount2 = Account.getAccount(this.recipientId);
    if (localAccount2 == null) {
      localAccount2 = Account.addOrGetAccount(this.recipientId);
    }
    localAccount1.addToBalanceAndUnconfirmedBalance(-(this.amount + this.fee) * 100L);
    this.type.apply(this, localAccount1, localAccount2);
  }
  
  void undo()
    throws Transaction.UndoNotSupportedException
  {
    Account localAccount1 = Account.getAccount(this.senderAccountId);
    localAccount1.addToBalance((this.amount + this.fee) * 100L);
    Account localAccount2 = Account.getAccount(this.recipientId);
    this.type.undo(this, localAccount1, localAccount2);
  }
  
  void updateTotals(Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1)
  {
    Long localLong1 = getSenderAccountId();
    Long localLong2 = (Long)paramMap.get(localLong1);
    if (localLong2 == null) {
      localLong2 = Long.valueOf(0L);
    }
    paramMap.put(localLong1, Long.valueOf(localLong2.longValue() + (this.amount + this.fee) * 100L));
    this.type.updateTotals(this, paramMap, paramMap1, localLong2);
  }
  
  boolean isDuplicate(Map<Type, Set<String>> paramMap)
  {
    return this.type.isDuplicate(this, paramMap);
  }
  
  int getSize()
  {
    return 128 + (this.attachment == null ? 0 : this.attachment.getSize());
  }
  
  String getHash()
  {
    if (this.hash == null)
    {
      byte[] arrayOfByte = getBytes();
      for (int i = 64; i < 128; i++) {
        arrayOfByte[i] = 0;
      }
      this.hash = Convert.convert(Crypto.sha256().digest(arrayOfByte));
    }
    return this.hash;
  }
  
  private void calculateIds()
  {
    if (this.stringId != null) {
      return;
    }
    byte[] arrayOfByte = Crypto.sha256().digest(getBytes());
    BigInteger localBigInteger = new BigInteger(1, new byte[] { arrayOfByte[7], arrayOfByte[6], arrayOfByte[5], arrayOfByte[4], arrayOfByte[3], arrayOfByte[2], arrayOfByte[1], arrayOfByte[0] });
    this.id = Long.valueOf(localBigInteger.longValue());
    this.senderAccountId = Account.getId(this.senderPublicKey);
    this.stringId = localBigInteger.toString();
  }
  
  private void writeObject(ObjectOutputStream paramObjectOutputStream)
    throws IOException
  {
    paramObjectOutputStream.defaultWriteObject();
    paramObjectOutputStream.write(this.type.getType());
    paramObjectOutputStream.write(this.type.getSubtype());
  }
  
  private void readObject(ObjectInputStream paramObjectInputStream)
    throws IOException, ClassNotFoundException
  {
    paramObjectInputStream.defaultReadObject();
    this.type = findTransactionType(paramObjectInputStream.readByte(), paramObjectInputStream.readByte());
  }
  
  public static Type findTransactionType(byte paramByte1, byte paramByte2)
  {
    switch (paramByte1)
    {
    case 0: 
      switch (paramByte2)
      {
      case 0: 
        return Transaction.Type.Payment.ORDINARY;
      }
      return null;
    case 1: 
      switch (paramByte2)
      {
      case 0: 
        return Transaction.Type.Messaging.ARBITRARY_MESSAGE;
      case 1: 
        return Transaction.Type.Messaging.ALIAS_ASSIGNMENT;
      }
      return null;
    case 2: 
      switch (paramByte2)
      {
      case 0: 
        return Transaction.Type.ColoredCoins.ASSET_ISSUANCE;
      case 1: 
        return Transaction.Type.ColoredCoins.ASSET_TRANSFER;
      case 2: 
        return Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT;
      case 3: 
        return Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT;
      case 4: 
        return Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION;
      case 5: 
        return Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION;
      }
      return null;
    }
    return null;
  }
  
  public static abstract class Type
  {
    public abstract byte getType();
    
    public abstract byte getSubtype();
    
    abstract boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
      throws NxtException.ValidationException;
    
    abstract boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
      throws NxtException.ValidationException;
    
    final boolean isDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
    {
      if (paramAccount.getUnconfirmedBalance() < paramInt * 100L) {
        return true;
      }
      paramAccount.addToUnconfirmedBalance(-paramInt * 100L);
      return checkDoubleSpending(paramTransaction, paramAccount, paramInt);
    }
    
    abstract boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt);
    
    abstract void apply(Transaction paramTransaction, Account paramAccount1, Account paramAccount2);
    
    abstract void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
      throws Transaction.UndoNotSupportedException;
    
    abstract void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong);
    
    boolean isDuplicate(Transaction paramTransaction, Map<Type, Set<String>> paramMap)
    {
      return false;
    }
    
    public static abstract class Payment
      extends Transaction.Type
    {
      public final byte getType()
      {
        return 0;
      }
      
      public static final Transaction.Type ORDINARY = new Payment()
      {
        public final byte getSubtype()
        {
          return 0;
        }
        
        final boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
        {
          return validateAttachment(paramAnonymousTransaction);
        }
        
        final boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
        {
          return validateAttachment(paramAnonymousTransaction);
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(paramAnonymousTransaction.amount * 100L);
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          paramAnonymousAccount2.addToBalanceAndUnconfirmedBalance(-paramAnonymousTransaction.amount * 100L);
        }
        
        void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
        
        boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
        {
          return false;
        }
        
        private boolean validateAttachment(Transaction paramAnonymousTransaction)
        {
          return (paramAnonymousTransaction.amount > 0) && (paramAnonymousTransaction.amount < 1000000000L);
        }
      };
    }
    
    public static abstract class Messaging
      extends Transaction.Type
    {
      public final byte getType()
      {
        return 1;
      }
      
      boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
      {
        return false;
      }
      
      public static final Transaction.Type ARBITRARY_MESSAGE = new Messaging()
      {
        public final byte getSubtype()
        {
          return 0;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
          throws NxtException.ValidationException
        {
          int i = paramAnonymousByteBuffer.getInt();
          if (i <= 1000)
          {
            byte[] arrayOfByte = new byte[i];
            paramAnonymousByteBuffer.get(arrayOfByte);
            paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(arrayOfByte);
            return validateAttachment(paramAnonymousTransaction);
          }
          return false;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
          throws NxtException.ValidationException
        {
          String str = (String)paramAnonymousJSONObject.get("message");
          paramAnonymousTransaction.attachment = new Attachment.MessagingArbitraryMessage(Convert.convert(str));
          return validateAttachment(paramAnonymousTransaction);
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2) {}
        
        private boolean validateAttachment(Transaction paramAnonymousTransaction)
          throws NxtException.ValidationException
        {
          if (Blockchain.getLastBlock().getHeight() < 40000) {
            throw new Transaction.NotYetEnabledException("Arbitrary messages not yet enabled at height " + Blockchain.getLastBlock().getHeight());
          }
          try
          {
            Attachment.MessagingArbitraryMessage localMessagingArbitraryMessage = (Attachment.MessagingArbitraryMessage)paramAnonymousTransaction.attachment;
            return (paramAnonymousTransaction.amount == 0) && (localMessagingArbitraryMessage.getMessage().length <= 1000);
          }
          catch (RuntimeException localRuntimeException)
          {
            Logger.logDebugMessage("Error validating arbitrary message", localRuntimeException);
          }
          return false;
        }
      };
      public static final Transaction.Type ALIAS_ASSIGNMENT = new Messaging()
      {
        public final byte getSubtype()
        {
          return 1;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
          throws NxtException.ValidationException
        {
          int i = paramAnonymousByteBuffer.get();
          if (i > 300) {
            throw new NxtException.ValidationException("Max alias length exceeded");
          }
          byte[] arrayOfByte1 = new byte[i];
          paramAnonymousByteBuffer.get(arrayOfByte1);
          int j = paramAnonymousByteBuffer.getShort();
          if (j > 3000) {
            throw new NxtException.ValidationException("Max alias URI length exceeded");
          }
          byte[] arrayOfByte2 = new byte[j];
          paramAnonymousByteBuffer.get(arrayOfByte2);
          try
          {
            paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(new String(arrayOfByte1, "UTF-8"), new String(arrayOfByte2, "UTF-8"));
            
            return validateAttachment(paramAnonymousTransaction);
          }
          catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
          {
            Logger.logDebugMessage("Error parsing alias assignment", localRuntimeException);
          }
          return false;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
          throws NxtException.ValidationException
        {
          String str1 = (String)paramAnonymousJSONObject.get("alias");
          String str2 = (String)paramAnonymousJSONObject.get("uri");
          paramAnonymousTransaction.attachment = new Attachment.MessagingAliasAssignment(str1, str2);
          return validateAttachment(paramAnonymousTransaction);
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
          Block localBlock = paramAnonymousTransaction.getBlock();
          Alias.addOrUpdateAlias(paramAnonymousAccount1, paramAnonymousTransaction.getId(), localMessagingAliasAssignment.getAliasName(), localMessagingAliasAssignment.getAliasURI(), localBlock.getTimestamp());
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
          throws Transaction.UndoNotSupportedException
        {
          throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Reversal of alias assignment not supported");
        }
        
        boolean isDuplicate(Transaction paramAnonymousTransaction, Map<Transaction.Type, Set<String>> paramAnonymousMap)
        {
          Object localObject = (Set)paramAnonymousMap.get(this);
          if (localObject == null)
          {
            localObject = new HashSet();
            paramAnonymousMap.put(this, localObject);
          }
          Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
          return !((Set)localObject).add(localMessagingAliasAssignment.getAliasName().toLowerCase());
        }
        
        private boolean validateAttachment(Transaction paramAnonymousTransaction)
          throws NxtException.ValidationException
        {
          if (Blockchain.getLastBlock().getHeight() < 22000) {
            throw new Transaction.NotYetEnabledException("Aliases not yet enabled at height " + Blockchain.getLastBlock().getHeight());
          }
          try
          {
            Attachment.MessagingAliasAssignment localMessagingAliasAssignment = (Attachment.MessagingAliasAssignment)paramAnonymousTransaction.attachment;
            if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (localMessagingAliasAssignment.getAliasName().length() == 0) || (localMessagingAliasAssignment.getAliasName().length() > 100) || (localMessagingAliasAssignment.getAliasURI().length() > 1000)) {
              return false;
            }
            String str = localMessagingAliasAssignment.getAliasName().toLowerCase();
            for (int i = 0; i < str.length(); i++) {
              if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
                return false;
              }
            }
            Alias localAlias = Alias.getAlias(str);
            return (localAlias == null) || (Arrays.equals(localAlias.getAccount().getPublicKey(), paramAnonymousTransaction.senderPublicKey));
          }
          catch (RuntimeException localRuntimeException)
          {
            Logger.logDebugMessage("Error in alias assignment validation", localRuntimeException);
          }
          return false;
        }
      };
      
      void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
    }
    
    public static abstract class ColoredCoins
      extends Transaction.Type
    {
      public final byte getType()
      {
        return 2;
      }
      
      public static final Transaction.Type ASSET_ISSUANCE = new ColoredCoins()
      {
        public final byte getSubtype()
        {
          return 0;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
          throws NxtException.ValidationException
        {
          int i = paramAnonymousByteBuffer.get();
          if (i > 30) {
            throw new NxtException.ValidationException("Max asset name length exceeded");
          }
          byte[] arrayOfByte1 = new byte[i];
          paramAnonymousByteBuffer.get(arrayOfByte1);
          int j = paramAnonymousByteBuffer.getShort();
          if (j > 300) {
            throw new NxtException.ValidationException("Max asset description length exceeded");
          }
          byte[] arrayOfByte2 = new byte[j];
          paramAnonymousByteBuffer.get(arrayOfByte2);
          int k = paramAnonymousByteBuffer.getInt();
          try
          {
            paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(new String(arrayOfByte1, "UTF-8").intern(), new String(arrayOfByte2, "UTF-8").intern(), k);
            
            return validateAttachment(paramAnonymousTransaction);
          }
          catch (RuntimeException|UnsupportedEncodingException localRuntimeException)
          {
            Logger.logDebugMessage("Error in asset issuance", localRuntimeException);
          }
          return false;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
        {
          String str1 = (String)paramAnonymousJSONObject.get("name");
          String str2 = (String)paramAnonymousJSONObject.get("description");
          int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetIssuance(str1.trim(), str2.trim(), i);
          return validateAttachment(paramAnonymousTransaction);
        }
        
        boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
        {
          return false;
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
          Long localLong = paramAnonymousTransaction.getId();
          Asset.addAsset(localLong, paramAnonymousTransaction.getSenderAccountId(), localColoredCoinsAssetIssuance.getName(), localColoredCoinsAssetIssuance.getDescription(), localColoredCoinsAssetIssuance.getQuantity());
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, localColoredCoinsAssetIssuance.getQuantity());
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
          Long localLong = paramAnonymousTransaction.getId();
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localLong, -localColoredCoinsAssetIssuance.getQuantity());
          Asset.removeAsset(localLong);
        }
        
        void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong) {}
        
        private boolean validateAttachment(Transaction paramAnonymousTransaction)
        {
          try
          {
            Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = (Attachment.ColoredCoinsAssetIssuance)paramAnonymousTransaction.attachment;
            if ((!Genesis.CREATOR_ID.equals(paramAnonymousTransaction.recipientId)) || (paramAnonymousTransaction.amount != 0) || (paramAnonymousTransaction.fee < 1000) || (localColoredCoinsAssetIssuance.getName().length() < 3) || (localColoredCoinsAssetIssuance.getName().length() > 10) || (localColoredCoinsAssetIssuance.getDescription().length() > 1000) || (localColoredCoinsAssetIssuance.getQuantity() <= 0) || (localColoredCoinsAssetIssuance.getQuantity() > 1000000000L)) {
              return false;
            }
            String str = localColoredCoinsAssetIssuance.getName().toLowerCase();
            for (int i = 0; i < str.length(); i++) {
              if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str.charAt(i)) < 0) {
                return false;
              }
            }
            return Asset.getAsset(str) == null;
          }
          catch (RuntimeException localRuntimeException)
          {
            Logger.logDebugMessage("Error validating colored coins asset issuance", localRuntimeException);
          }
          return false;
        }
      };
      public static final Transaction.Type ASSET_TRANSFER = new ColoredCoins()
      {
        public final byte getSubtype()
        {
          return 1;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
        {
          Long localLong = Convert.zeroToNull(paramAnonymousByteBuffer.getLong());
          int i = paramAnonymousByteBuffer.getInt();
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
          return validateAttachment(paramAnonymousTransaction);
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
        {
          Long localLong = Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("asset"));
          int i = ((Long)paramAnonymousJSONObject.get("quantity")).intValue();
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAssetTransfer(localLong, i);
          return validateAttachment(paramAnonymousTransaction);
        }
        
        boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
        {
          Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
          Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId());
          if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAssetTransfer.getQuantity()))
          {
            paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
            return true;
          }
          paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
          return false;
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
          paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), localColoredCoinsAssetTransfer.getQuantity());
          paramAnonymousAccount2.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAssetTransfer.getAssetId(), -localColoredCoinsAssetTransfer.getQuantity());
        }
        
        void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
        {
          Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
          Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
          if (localObject == null)
          {
            localObject = new HashMap();
            paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
          }
          Long localLong = (Long)((Map)localObject).get(localColoredCoinsAssetTransfer.getAssetId());
          if (localLong == null) {
            localLong = Long.valueOf(0L);
          }
          ((Map)localObject).put(localColoredCoinsAssetTransfer.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAssetTransfer.getQuantity()));
        }
        
        private boolean validateAttachment(Transaction paramAnonymousTransaction)
        {
          Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = (Attachment.ColoredCoinsAssetTransfer)paramAnonymousTransaction.attachment;
          return (paramAnonymousTransaction.amount == 0) && (localColoredCoinsAssetTransfer.getQuantity() > 0) && (localColoredCoinsAssetTransfer.getQuantity() <= 1000000000L);
        }
      };
      
      static abstract class ColoredCoinsOrderPlacement
        extends Transaction.Type.ColoredCoins
      {
        abstract Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramLong, int paramInt, long paramLong1);
        
        final boolean loadAttachment(Transaction paramTransaction, ByteBuffer paramByteBuffer)
        {
          Long localLong = Convert.zeroToNull(paramByteBuffer.getLong());
          int i = paramByteBuffer.getInt();
          long l = paramByteBuffer.getLong();
          paramTransaction.attachment = makeAttachment(localLong, i, l);
          return validateAttachment(paramTransaction);
        }
        
        final boolean loadAttachment(Transaction paramTransaction, JSONObject paramJSONObject)
        {
          Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("asset"));
          int i = ((Long)paramJSONObject.get("quantity")).intValue();
          long l = ((Long)paramJSONObject.get("price")).longValue();
          paramTransaction.attachment = makeAttachment(localLong, i, l);
          return validateAttachment(paramTransaction);
        }
        
        private boolean validateAttachment(Transaction paramTransaction)
        {
          Attachment.ColoredCoinsOrderPlacement localColoredCoinsOrderPlacement = (Attachment.ColoredCoinsOrderPlacement)paramTransaction.attachment;
          return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0) && (localColoredCoinsOrderPlacement.getQuantity() > 0) && (localColoredCoinsOrderPlacement.getQuantity() <= 1000000000L) && (localColoredCoinsOrderPlacement.getPrice() > 0L) && (localColoredCoinsOrderPlacement.getPrice() <= 100000000000L);
        }
      }
      
      public static final Transaction.Type ASK_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
      {
        public final byte getSubtype()
        {
          return 2;
        }
        
        final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
        {
          return new Attachment.ColoredCoinsAskOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
        }
        
        boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
        {
          Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
          Integer localInteger = paramAnonymousAccount.getUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId());
          if ((localInteger == null) || (localInteger.intValue() < localColoredCoinsAskOrderPlacement.getQuantity()))
          {
            paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
            return true;
          }
          paramAnonymousAccount.addToUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
          return false;
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), -localColoredCoinsAskOrderPlacement.getQuantity());
          Order.Ask.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity(), localColoredCoinsAskOrderPlacement.getPrice());
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
          throws Transaction.UndoNotSupportedException
        {
          Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
          Order.Ask localAsk = Order.Ask.removeOrder(paramAnonymousTransaction.getId());
          if ((localAsk == null) || (localAsk.getQuantity() != localColoredCoinsAskOrderPlacement.getQuantity()) || (!localAsk.getAssetId().equals(localColoredCoinsAskOrderPlacement.getAssetId()))) {
            throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Ask order already filled");
          }
          paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localColoredCoinsAskOrderPlacement.getAssetId(), localColoredCoinsAskOrderPlacement.getQuantity());
        }
        
        void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
        {
          Attachment.ColoredCoinsAskOrderPlacement localColoredCoinsAskOrderPlacement = (Attachment.ColoredCoinsAskOrderPlacement)paramAnonymousTransaction.attachment;
          Object localObject = (Map)paramAnonymousMap1.get(paramAnonymousTransaction.getSenderAccountId());
          if (localObject == null)
          {
            localObject = new HashMap();
            paramAnonymousMap1.put(paramAnonymousTransaction.getSenderAccountId(), localObject);
          }
          Long localLong = (Long)((Map)localObject).get(localColoredCoinsAskOrderPlacement.getAssetId());
          if (localLong == null) {
            localLong = Long.valueOf(0L);
          }
          ((Map)localObject).put(localColoredCoinsAskOrderPlacement.getAssetId(), Long.valueOf(localLong.longValue() + localColoredCoinsAskOrderPlacement.getQuantity()));
        }
      };
      public static final Transaction.Type BID_ORDER_PLACEMENT = new ColoredCoinsOrderPlacement()
      {
        public final byte getSubtype()
        {
          return 3;
        }
        
        final Attachment.ColoredCoinsOrderPlacement makeAttachment(Long paramAnonymousLong, int paramAnonymousInt, long paramAnonymousLong1)
        {
          return new Attachment.ColoredCoinsBidOrderPlacement(paramAnonymousLong, paramAnonymousInt, paramAnonymousLong1);
        }
        
        boolean checkDoubleSpending(Transaction paramAnonymousTransaction, Account paramAnonymousAccount, int paramAnonymousInt)
        {
          Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
          if (paramAnonymousAccount.getUnconfirmedBalance() < localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice())
          {
            paramAnonymousAccount.addToUnconfirmedBalance(paramAnonymousInt * 100L);
            return true;
          }
          paramAnonymousAccount.addToUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
          return false;
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
          paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(-localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
          Order.Bid.addOrder(paramAnonymousTransaction.getId(), paramAnonymousAccount1, localColoredCoinsBidOrderPlacement.getAssetId(), localColoredCoinsBidOrderPlacement.getQuantity(), localColoredCoinsBidOrderPlacement.getPrice());
        }
        
        void undo(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
          throws Transaction.UndoNotSupportedException
        {
          Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
          Order.Bid localBid = Order.Bid.removeOrder(paramAnonymousTransaction.getId());
          if ((localBid == null) || (localBid.getQuantity() != localColoredCoinsBidOrderPlacement.getQuantity()) || (!localBid.getAssetId().equals(localColoredCoinsBidOrderPlacement.getAssetId()))) {
            throw new Transaction.UndoNotSupportedException(paramAnonymousTransaction, "Bid order already filled");
          }
          paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice());
        }
        
        void updateTotals(Transaction paramAnonymousTransaction, Map<Long, Long> paramAnonymousMap, Map<Long, Map<Long, Long>> paramAnonymousMap1, Long paramAnonymousLong)
        {
          Attachment.ColoredCoinsBidOrderPlacement localColoredCoinsBidOrderPlacement = (Attachment.ColoredCoinsBidOrderPlacement)paramAnonymousTransaction.attachment;
          paramAnonymousMap.put(paramAnonymousTransaction.getSenderAccountId(), Long.valueOf(paramAnonymousLong.longValue() + localColoredCoinsBidOrderPlacement.getQuantity() * localColoredCoinsBidOrderPlacement.getPrice()));
        }
      };
      
      static abstract class ColoredCoinsOrderCancellation
        extends Transaction.Type.ColoredCoins
      {
        final boolean validateAttachment(Transaction paramTransaction)
        {
          return (Genesis.CREATOR_ID.equals(paramTransaction.recipientId)) && (paramTransaction.amount == 0);
        }
        
        final boolean checkDoubleSpending(Transaction paramTransaction, Account paramAccount, int paramInt)
        {
          return false;
        }
        
        final void updateTotals(Transaction paramTransaction, Map<Long, Long> paramMap, Map<Long, Map<Long, Long>> paramMap1, Long paramLong) {}
        
        final void undo(Transaction paramTransaction, Account paramAccount1, Account paramAccount2)
          throws Transaction.UndoNotSupportedException
        {
          throw new Transaction.UndoNotSupportedException(paramTransaction, "Reversal of order cancellation not supported");
        }
      }
      
      public static final Transaction.Type ASK_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
      {
        public final byte getSubtype()
        {
          return 4;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
        {
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
          return validateAttachment(paramAnonymousTransaction);
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
        {
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsAskOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
          return validateAttachment(paramAnonymousTransaction);
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsAskOrderCancellation localColoredCoinsAskOrderCancellation = (Attachment.ColoredCoinsAskOrderCancellation)paramAnonymousTransaction.attachment;
          Order.Ask localAsk = Order.Ask.removeOrder(localColoredCoinsAskOrderCancellation.getOrderId());
          if (localAsk != null) {
            paramAnonymousAccount1.addToAssetAndUnconfirmedAssetBalance(localAsk.getAssetId(), localAsk.getQuantity());
          }
        }
      };
      public static final Transaction.Type BID_ORDER_CANCELLATION = new ColoredCoinsOrderCancellation()
      {
        public final byte getSubtype()
        {
          return 5;
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, ByteBuffer paramAnonymousByteBuffer)
        {
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(paramAnonymousByteBuffer.getLong()));
          return validateAttachment(paramAnonymousTransaction);
        }
        
        boolean loadAttachment(Transaction paramAnonymousTransaction, JSONObject paramAnonymousJSONObject)
        {
          paramAnonymousTransaction.attachment = new Attachment.ColoredCoinsBidOrderCancellation(Convert.parseUnsignedLong((String)paramAnonymousJSONObject.get("order")));
          return validateAttachment(paramAnonymousTransaction);
        }
        
        void apply(Transaction paramAnonymousTransaction, Account paramAnonymousAccount1, Account paramAnonymousAccount2)
        {
          Attachment.ColoredCoinsBidOrderCancellation localColoredCoinsBidOrderCancellation = (Attachment.ColoredCoinsBidOrderCancellation)paramAnonymousTransaction.attachment;
          Order.Bid localBid = Order.Bid.removeOrder(localColoredCoinsBidOrderCancellation.getOrderId());
          if (localBid != null) {
            paramAnonymousAccount1.addToBalanceAndUnconfirmedBalance(localBid.getQuantity() * localBid.getPrice());
          }
        }
      };
    }
  }
  
  public static final class UndoNotSupportedException
    extends NxtException
  {
    private final Transaction transaction;
    
    public UndoNotSupportedException(Transaction paramTransaction, String paramString)
    {
      super();
      this.transaction = paramTransaction;
    }
    
    public Transaction getTransaction()
    {
      return this.transaction;
    }
  }
  
  public static final class NotYetEnabledException
    extends NxtException.ValidationException
  {
    public NotYetEnabledException(String paramString)
    {
      super();
    }
    
    public NotYetEnabledException(String paramString, Throwable paramThrowable)
    {
      super(paramThrowable);
    }
  }
}