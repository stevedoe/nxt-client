package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetMilestoneBlockIds
  extends PeerServlet.PeerRequestHandler
{
  static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    try
    {
      JSONArray localJSONArray = new JSONArray();
      
      String str1 = (String)paramJSONObject.get("lastBlockId");
      if (str1 != null)
      {
        Long localLong1 = Convert.parseUnsignedLong(str1);
        Long localLong2 = Nxt.getBlockchain().getLastBlock().getId();
        if ((localLong2.equals(localLong1)) || (Nxt.getBlockchain().hasBlock(localLong1)))
        {
          localJSONArray.add(str1);
          localJSONObject.put("milestoneBlockIds", localJSONArray);
          if (localLong2.equals(localLong1)) {
            localJSONObject.put("last", Boolean.TRUE);
          }
          return localJSONObject;
        }
      }
      String str2 = (String)paramJSONObject.get("lastMilestoneBlockId");
      int i;
      int j;
      int k;
      if (str2 != null)
      {
        Block localBlock = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(str2));
        if (localBlock == null) {
          throw new IllegalStateException("Don't have block " + str2);
        }
        i = localBlock.getHeight();
        j = Math.min(1440, Nxt.getBlockchain().getLastBlock().getHeight() - i);
        i = Math.max(i - j, 0);
        k = 10;
      }
      else if (str1 != null)
      {
        i = Nxt.getBlockchain().getLastBlock().getHeight();
        j = 10;
        k = 10;
      }
      else
      {
        paramPeer.blacklist();
        localJSONObject.put("error", "Old getMilestoneBlockIds protocol not supported, please upgrade");
        return localJSONObject;
      }
      long l = Nxt.getBlockchain().getBlockIdAtHeight(i);
      while ((i > 0) && (k-- > 0))
      {
        localJSONArray.add(Convert.toUnsignedLong(l));
        l = Nxt.getBlockchain().getBlockIdAtHeight(i);
        i -= j;
      }
      localJSONObject.put("milestoneBlockIds", localJSONArray);
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logDebugMessage(localRuntimeException.toString());
      localJSONObject.put("error", localRuntimeException.toString());
    }
    return localJSONObject;
  }
}