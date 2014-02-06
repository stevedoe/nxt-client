package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Blockchain;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class BroadcastTransaction
  extends HttpRequestHandler
{
  static final BroadcastTransaction instance = new BroadcastTransaction();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str = paramHttpServletRequest.getParameter("transactionBytes");
    if (str == null) {
      return JSONResponses.MISSING_TRANSACTION_BYTES;
    }
    try
    {
      byte[] arrayOfByte = Convert.convert(str);
      Transaction localTransaction = Transaction.getTransaction(arrayOfByte);
      
      Blockchain.broadcast(localTransaction);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("transaction", localTransaction.getStringId());
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_TRANSACTION_BYTES;
  }
}