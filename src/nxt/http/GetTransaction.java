package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetTransaction
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetTransaction instance = new GetTransaction();
  
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
      localTransaction = Blockchain.getTransaction(localLong);
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_TRANSACTION;
    }
    JSONObject localJSONObject;
    if (localTransaction == null)
    {
      localTransaction = Blockchain.getUnconfirmedTransaction(localLong);
      if (localTransaction == null) {
        return JSONResponses.UNKNOWN_TRANSACTION;
      }
      localJSONObject = localTransaction.getJSONObject();
      localJSONObject.put("sender", Convert.convert(localTransaction.getSenderId()));
    }
    else
    {
      localJSONObject = localTransaction.getJSONObject();
      localJSONObject.put("sender", Convert.convert(localTransaction.getSenderId()));
      Block localBlock = localTransaction.getBlock();
      localJSONObject.put("block", localBlock.getStringId());
      localJSONObject.put("confirmations", Integer.valueOf(Blockchain.getLastBlock().getHeight() - localBlock.getHeight() + 1));
    }
    return localJSONObject;
  }
}