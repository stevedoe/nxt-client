package nxt.peer;

import java.util.List;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetNextBlockIds
  extends HttpJSONRequestHandler
{
  static final GetNextBlockIds instance = new GetNextBlockIds();
  
  JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("blockId"));
    List localList = Blockchain.getBlockIdsAfter(localLong1, 1440);
    for (Long localLong2 : localList) {
      localJSONArray.add(Convert.toUnsignedLong(localLong2));
    }
    localJSONObject.put("nextBlockIds", localJSONArray);
    
    return localJSONObject;
  }
}