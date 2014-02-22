package nxt;

import java.math.BigInteger;
import java.util.List;
import org.json.simple.JSONObject;

public abstract interface Block
{
  public abstract int getVersion();
  
  public abstract Long getId();
  
  public abstract String getStringId();
  
  public abstract int getHeight();
  
  public abstract int getTimestamp();
  
  public abstract Long getGeneratorId();
  
  public abstract byte[] getGeneratorPublicKey();
  
  public abstract Long getPreviousBlockId();
  
  public abstract byte[] getPreviousBlockHash();
  
  public abstract Long getNextBlockId();
  
  public abstract int getTotalAmount();
  
  public abstract int getTotalFee();
  
  public abstract int getPayloadLength();
  
  public abstract byte[] getPayloadHash();
  
  public abstract List<Long> getTransactionIds();
  
  public abstract List<? extends Transaction> getTransactions();
  
  public abstract byte[] getGenerationSignature();
  
  public abstract byte[] getBlockSignature();
  
  public abstract long getBaseTarget();
  
  public abstract BigInteger getCumulativeDifficulty();
  
  public abstract JSONObject getJSONObject();
}