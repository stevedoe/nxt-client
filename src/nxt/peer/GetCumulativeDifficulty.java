package nxt.peer;

import java.math.BigInteger;
import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty
  extends PeerServlet.PeerRequestHandler
{
  static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("cumulativeDifficulty", Nxt.getBlockchain().getLastBlock().getCumulativeDifficulty().toString());
    
    return localJSONObject;
  }
}