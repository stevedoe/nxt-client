package nxt.peer;

import java.util.ArrayList;
import java.util.List;
import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetNextBlocks
  extends HttpJSONRequestHandler
{
  static final GetNextBlocks instance = new GetNextBlocks();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    ArrayList localArrayList = new ArrayList();
    int i = 0;
    Block localBlock1 = Blockchain.getBlock(Convert.parseUnsignedLong((String)paramJSONObject.get("blockId")));
    while ((localBlock1 != null) && (localBlock1.getNextBlockId() != null))
    {
      localBlock1 = Blockchain.getBlock(localBlock1.getNextBlockId());
      if (localBlock1 != null)
      {
        int j = 224 + localBlock1.getPayloadLength();
        if (i + j > 1048576) {
          break;
        }
        localArrayList.add(localBlock1);
        i += j;
      }
    }
    JSONArray localJSONArray = new JSONArray();
    for (Block localBlock2 : localArrayList) {
      localJSONArray.add(localBlock2.getJSON());
    }
    localJSONObject.put("nextBlocks", localJSONArray);
    
    return localJSONObject;
  }
}