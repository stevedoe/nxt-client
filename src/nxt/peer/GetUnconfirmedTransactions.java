package nxt.peer;

import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetUnconfirmedTransactions
  extends PeerServlet.PeerRequestHandler
{
  static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    for (Transaction localTransaction : Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
      localJSONArray.add(localTransaction.getJSONObject());
    }
    localJSONObject.put("unconfirmedTransactions", localJSONArray);
    

    return localJSONObject;
  }
}