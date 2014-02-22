package nxt;

import org.json.simple.JSONObject;

public abstract interface Transaction
  extends Comparable<Transaction>
{
  public abstract Long getId();
  
  public abstract String getStringId();
  
  public abstract Long getSenderId();
  
  public abstract byte[] getSenderPublicKey();
  
  public abstract Long getRecipientId();
  
  public abstract int getHeight();
  
  public abstract Block getBlock();
  
  public abstract int getTimestamp();
  
  public abstract short getDeadline();
  
  public abstract int getExpiration();
  
  public abstract int getAmount();
  
  public abstract int getFee();
  
  public abstract Long getReferencedTransactionId();
  
  public abstract byte[] getSignature();
  
  public abstract String getHash();
  
  public abstract TransactionType getType();
  
  public abstract Attachment getAttachment();
  
  public abstract void sign(String paramString);
  
  public abstract JSONObject getJSONObject();
  
  public abstract byte[] getBytes();
}