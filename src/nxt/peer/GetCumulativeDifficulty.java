package nxt.peer;

import java.math.BigInteger;
import nxt.Block;
import nxt.Blockchain;
import org.json.simple.JSONObject;

final class GetCumulativeDifficulty
  extends HttpJSONRequestHandler
{
  static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
    
    return localJSONObject;
  }
}