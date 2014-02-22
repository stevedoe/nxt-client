package nxt;

import java.util.Collection;
import java.util.List;
import nxt.util.Observable;
import org.json.simple.JSONObject;

public abstract interface TransactionProcessor
  extends Observable<List<Transaction>, Event>
{
  public abstract Collection<? extends Transaction> getAllUnconfirmedTransactions();
  
  public abstract Transaction getUnconfirmedTransaction(Long paramLong);
  
  public abstract void broadcast(Transaction paramTransaction);
  
  public abstract void processPeerTransactions(JSONObject paramJSONObject);
  
  public abstract Transaction parseTransaction(byte[] paramArrayOfByte)
    throws NxtException.ValidationException;
  
  public abstract Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2)
    throws NxtException.ValidationException;
  
  public abstract Transaction newTransaction(int paramInt1, short paramShort, byte[] paramArrayOfByte, Long paramLong1, int paramInt2, int paramInt3, Long paramLong2, Attachment paramAttachment)
    throws NxtException.ValidationException;
  
  public static enum Event
  {
    REMOVED_UNCONFIRMED_TRANSACTIONS,  ADDED_UNCONFIRMED_TRANSACTIONS,  ADDED_CONFIRMED_TRANSACTIONS,  ADDED_DOUBLESPENDING_TRANSACTIONS;
    
    private Event() {}
  }
}