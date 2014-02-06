package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetNextBlockIds
  extends HttpJSONRequestHandler
{
  static final GetNextBlockIds instance = new GetNextBlockIds();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    Block localBlock = Blockchain.getBlock(Convert.parseUnsignedLong((String)paramJSONObject.get("blockId")));
    while ((localBlock != null) && (localBlock.getNextBlockId() != null) && (localJSONArray.size() < 1440))
    {
      localBlock = Blockchain.getBlock(localBlock.getNextBlockId());
      if (localBlock != null) {
        localJSONArray.add(localBlock.getStringId());
      }
    }
    localJSONObject.put("nextBlockIds", localJSONArray);
    
    return localJSONObject;
  }
}