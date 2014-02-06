package nxt.peer;

import nxt.Blockchain;
import nxt.Transaction;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetUnconfirmedTransactions
  extends HttpJSONRequestHandler
{
  static final GetUnconfirmedTransactions instance = new GetUnconfirmedTransactions();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    for (Transaction localTransaction : Blockchain.getAllUnconfirmedTransactions()) {
      localJSONArray.add(localTransaction.getJSONObject());
    }
    localJSONObject.put("unconfirmedTransactions", localJSONArray);
    

    return localJSONObject;
  }
}