package nxt.peer;

import nxt.Nxt;
import nxt.TransactionProcessor;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions
  extends PeerServlet.PeerRequestHandler
{
  static final ProcessTransactions instance = new ProcessTransactions();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    Nxt.getTransactionProcessor().processPeerTransactions(paramJSONObject);
    
    return JSON.emptyJSON;
  }
}