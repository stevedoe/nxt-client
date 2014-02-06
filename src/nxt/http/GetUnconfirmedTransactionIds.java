package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Blockchain;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetUnconfirmedTransactionIds
  extends HttpRequestHandler
{
  static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject).hasNext();)
    {
      Transaction localTransaction = (Transaction)((Iterator)localObject).next();
      localJSONArray.add(localTransaction.getStringId());
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("unconfirmedTransactionIds", localJSONArray);
    return localObject;
  }
}