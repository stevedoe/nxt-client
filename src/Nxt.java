import java.io.ObjectStreamException;
import java.io.Serializable;
import java.math.BigInteger;
import nxt.Attachment;
import nxt.Attachment.ColoredCoinsAskOrderCancellation;
import nxt.Attachment.ColoredCoinsAskOrderPlacement;
import nxt.Attachment.ColoredCoinsAssetIssuance;
import nxt.Attachment.ColoredCoinsAssetTransfer;
import nxt.Attachment.ColoredCoinsBidOrderCancellation;
import nxt.Attachment.ColoredCoinsBidOrderPlacement;
import nxt.Attachment.MessagingAliasAssignment;
import nxt.Attachment.MessagingArbitraryMessage;
import nxt.Block;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.util.Convert;

final class Nxt
{
  static class Transaction
    implements Serializable
  {
    static final long serialVersionUID = 0L;
    byte type;
    byte subtype;
    int timestamp;
    short deadline;
    byte[] senderPublicKey;
    long recipient;
    int amount;
    int fee;
    long referencedTransaction;
    byte[] signature;
    Attachment attachment;
    int index;
    long block;
    int height;
    
    public Object readResolve()
      throws ObjectStreamException
    {
      try
      {
        Transaction localTransaction = this.attachment != null ? Transaction.newTransaction(this.timestamp, this.deadline, this.senderPublicKey, Long.valueOf(this.recipient), this.amount, this.fee, Convert.zeroToNull(this.referencedTransaction), this.attachment) : Transaction.newTransaction(this.timestamp, this.deadline, this.senderPublicKey, Long.valueOf(this.recipient), this.amount, this.fee, Convert.zeroToNull(this.referencedTransaction));
        



        localTransaction.signature = this.signature;
        localTransaction.index = this.index;
        localTransaction.blockId = Convert.zeroToNull(this.block);
        localTransaction.height = this.height;
        return localTransaction;
      }
      catch (NxtException.ValidationException localValidationException)
      {
        throw new RuntimeException(localValidationException.getMessage(), localValidationException);
      }
    }
    
    public static abstract interface Attachment {}
    
    static class MessagingArbitraryMessageAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      byte[] message;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.MessagingArbitraryMessage(this.message);
      }
    }
    
    static class MessagingAliasAssignmentAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      String alias;
      String uri;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.MessagingAliasAssignment(this.alias, this.uri);
      }
    }
    
    static class ColoredCoinsAssetIssuanceAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      String name;
      String description;
      int quantity;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsAssetIssuance(this.name, this.description, this.quantity);
      }
    }
    
    static class ColoredCoinsAssetTransferAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsAssetTransfer(Convert.zeroToNull(this.asset), this.quantity);
      }
    }
    
    static class ColoredCoinsAskOrderPlacementAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      long price;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsAskOrderPlacement(Convert.zeroToNull(this.asset), this.quantity, this.price);
      }
    }
    
    static class ColoredCoinsBidOrderPlacementAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long asset;
      int quantity;
      long price;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsBidOrderPlacement(Convert.zeroToNull(this.asset), this.quantity, this.price);
      }
    }
    
    static class ColoredCoinsAskOrderCancellationAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long order;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsAskOrderCancellation(Convert.zeroToNull(this.order));
      }
    }
    
    static class ColoredCoinsBidOrderCancellationAttachment
      implements Nxt.Transaction.Attachment, Serializable
    {
      static final long serialVersionUID = 0L;
      long order;
      
      public Object readResolve()
        throws ObjectStreamException
      {
        return new Attachment.ColoredCoinsBidOrderCancellation(Convert.zeroToNull(this.order));
      }
    }
  }
  
  static class Block
    implements Serializable
  {
    static final long serialVersionUID = 0L;
    static final long[] emptyLong = new long[0];
    int version;
    int timestamp;
    long previousBlock;
    int totalAmount;
    int totalFee;
    int payloadLength;
    byte[] payloadHash;
    byte[] generatorPublicKey;
    byte[] generationSignature;
    byte[] blockSignature;
    byte[] previousBlockHash;
    int index;
    long[] transactions;
    long baseTarget;
    int height;
    long nextBlock;
    BigInteger cumulativeDifficulty;
    
    public Object readResolve()
      throws ObjectStreamException
    {
      try
      {
        Block localBlock = new Block(this.version, this.timestamp, Convert.zeroToNull(this.previousBlock), this.transactions.length, this.totalAmount, this.totalFee, this.payloadLength, this.payloadHash, this.generatorPublicKey, this.generationSignature, this.blockSignature, this.previousBlockHash);
        
        localBlock.index = this.index;
        for (int i = 0; i < this.transactions.length; i++) {
          localBlock.getTransactionIds()[i] = Long.valueOf(this.transactions[i]);
        }
        localBlock.baseTarget = this.baseTarget;
        localBlock.height = this.height;
        localBlock.nextBlockId = Convert.zeroToNull(this.nextBlock);
        localBlock.cumulativeDifficulty = this.cumulativeDifficulty;
        return localBlock;
      }
      catch (NxtException.ValidationException localValidationException)
      {
        throw new RuntimeException(localValidationException.getMessage(), localValidationException);
      }
    }
  }
}