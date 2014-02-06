package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetMilestoneBlockIds
  extends HttpJSONRequestHandler
{
  static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    Block localBlock = Blockchain.getLastBlock();
    int i = localBlock.getHeight() * 4 / 1461 + 1;
    for (; (localBlock != null) && (localBlock.getHeight() > 0); goto 64)
    {
      localJSONArray.add(localBlock.getStringId());
      int j = 0;
      if ((j < i) && (localBlock != null) && (localBlock.getHeight() > 0))
      {
        localBlock = Blockchain.getBlock(localBlock.getPreviousBlockId());j++;
      }
    }
    localJSONObject.put("milestoneBlockIds", localJSONArray);
    
    return localJSONObject;
  }
}