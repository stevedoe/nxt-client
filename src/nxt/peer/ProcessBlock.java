package nxt.peer;

import nxt.Blockchain;
import nxt.NxtException.ValidationException;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessBlock
  extends HttpJSONRequestHandler
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
  
  public JSONStreamAware processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    try
    {
      return Blockchain.pushBlock(paramJSONObject) ? ACCEPTED : NOT_ACCEPTED;
    }
    catch (NxtException.ValidationException localValidationException)
    {
      if (paramPeer != null) {
        paramPeer.blacklist(localValidationException);
      }
    }
    return NOT_ACCEPTED;
  }
}