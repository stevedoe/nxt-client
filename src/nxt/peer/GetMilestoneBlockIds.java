package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
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
    long l = localBlock.getId().longValue();
    int i = localBlock.getHeight();
    int j = i * 4 / 1461 + 1;
    while (i > 0)
    {
      localJSONArray.add(Convert.convert(l));
      l = Blockchain.getBlockIdAtHeight(i);
      i -= j;
    }
    localJSONObject.put("milestoneBlockIds", localJSONArray);
    
    return localJSONObject;
  }
}