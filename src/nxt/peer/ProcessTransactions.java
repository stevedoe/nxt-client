package nxt.peer;

import nxt.Blockchain;
import nxt.NxtException.ValidationException;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessTransactions
  extends HttpJSONRequestHandler
{
  static final ProcessTransactions instance = new ProcessTransactions();
  
  public JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    try
    {
      Blockchain.processTransactions(paramJSONObject);
    }
    catch (NxtException.ValidationException localValidationException)
    {
      paramPeer.blacklist(localValidationException);
    }
    return JSON.emptyJSON;
  }
}