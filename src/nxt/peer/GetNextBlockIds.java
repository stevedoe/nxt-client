package nxt.peer;

import java.util.List;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetNextBlockIds
  extends PeerServlet.PeerRequestHandler
{
  static final GetNextBlockIds instance = new GetNextBlockIds();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    Long localLong1 = Convert.parseUnsignedLong((String)paramJSONObject.get("blockId"));
    List localList = Nxt.getBlockchain().getBlockIdsAfter(localLong1, 1440);
    for (Long localLong2 : localList) {
      localJSONArray.add(Convert.toUnsignedLong(localLong2));
    }
    localJSONObject.put("nextBlockIds", localJSONArray);
    
    return localJSONObject;
  }
}