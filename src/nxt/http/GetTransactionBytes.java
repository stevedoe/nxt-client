package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetTransactionBytes
  extends APIServlet.APIRequestHandler
{
  static final GetTransactionBytes instance = new GetTransactionBytes();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("transaction");
    if (str == null) {
      return JSONResponses.MISSING_TRANSACTION;
    }
    Long localLong;
    Transaction localTransaction;
    try
    {
      localLong = Convert.parseUnsignedLong(str);
      localTransaction = Nxt.getBlockchain().getTransaction(localLong);
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_TRANSACTION;
    }
    JSONObject localJSONObject = new JSONObject();
    if (localTransaction == null)
    {
      localTransaction = Nxt.getTransactionProcessor().getUnconfirmedTransaction(localLong);
      if (localTransaction == null) {
        return JSONResponses.UNKNOWN_TRANSACTION;
      }
      localJSONObject.put("bytes", Convert.toHexString(localTransaction.getBytes()));
    }
    else
    {
      localJSONObject.put("bytes", Convert.toHexString(localTransaction.getBytes()));
      Block localBlock = localTransaction.getBlock();
      localJSONObject.put("confirmations", Integer.valueOf(Nxt.getBlockchain().getLastBlock().getHeight() - localBlock.getHeight() + 1));
    }
    return localJSONObject;
  }
}