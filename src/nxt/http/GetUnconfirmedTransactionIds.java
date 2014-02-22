package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetUnconfirmedTransactionIds
  extends APIServlet.APIRequestHandler
{
  static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject).hasNext();)
    {
      Transaction localTransaction = (Transaction)((Iterator)localObject).next();
      localJSONArray.add(localTransaction.getStringId());
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("unconfirmedTransactionIds", localJSONArray);
    return localObject;
  }
}