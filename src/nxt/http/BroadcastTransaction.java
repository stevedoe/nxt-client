package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Nxt;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class BroadcastTransaction
  extends APIServlet.APIRequestHandler
{
  static final BroadcastTransaction instance = new BroadcastTransaction();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str = paramHttpServletRequest.getParameter("transactionBytes");
    if (str == null) {
      return JSONResponses.MISSING_TRANSACTION_BYTES;
    }
    try
    {
      byte[] arrayOfByte = Convert.parseHexString(str);
      Transaction localTransaction = Nxt.getTransactionProcessor().parseTransaction(arrayOfByte);
      
      Nxt.getTransactionProcessor().broadcast(localTransaction);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("transaction", localTransaction.getStringId());
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_TRANSACTION_BYTES;
  }
  
  boolean requirePost()
  {
    return true;
  }
}