package nxt.peer;

import nxt.Blockchain;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions
  extends HttpJSONRequestHandler
{
  static final ProcessTransactions instance = new ProcessTransactions();
  
  JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    Blockchain.processTransactions(paramJSONObject);
    
    return JSON.emptyJSON;
  }
}