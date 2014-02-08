package nxt.peer;

import java.util.ArrayList;
import java.util.Iterator;
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
  
  JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    ArrayList localArrayList = new ArrayList();
    int i = 0;
    Long localLong = Convert.parseUnsignedLong((String)paramJSONObject.get("blockId"));
    List localList = Blockchain.getBlocksAfter(localLong, 1440);
    for (Object localObject1 = localList.iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Block)((Iterator)localObject1).next();
      int j = 224 + ((Block)localObject2).getPayloadLength();
      if (i + j > 1048576) {
        break;
      }
      localArrayList.add(localObject2);
      i += j;
    }
    localObject1 = new JSONArray();
    for (Object localObject2 = localArrayList.iterator(); ((Iterator)localObject2).hasNext();)
    {
      Block localBlock = (Block)((Iterator)localObject2).next();
      ((JSONArray)localObject1).add(localBlock.getJSON());
    }
    localJSONObject.put("nextBlocks", localObject1);
    
    return localJSONObject;
  }
}