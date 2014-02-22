package nxt.peer;

import nxt.Block;
import nxt.Blockchain;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessBlock
  extends PeerServlet.PeerRequestHandler
{
  static final ProcessBlock instance = new ProcessBlock();
  private static final JSONStreamAware ACCEPTED;
  private static final JSONStreamAware NOT_ACCEPTED;
  
  static
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("accepted", Boolean.valueOf(true));
    ACCEPTED = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("accepted", Boolean.valueOf(false));
    NOT_ACCEPTED = JSON.prepare(localJSONObject);
  }
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    try
    {
      if (!Nxt.getBlockchain().getLastBlock().getId().equals(Convert.parseUnsignedLong((String)paramJSONObject.get("previousBlock")))) {
        return NOT_ACCEPTED;
      }
      Nxt.getBlockchainProcessor().processPeerBlock(paramJSONObject);
      return ACCEPTED;
    }
    catch (NxtException localNxtException)
    {
      if (paramPeer != null) {
        paramPeer.blacklist(localNxtException);
      }
    }
    return NOT_ACCEPTED;
  }
}