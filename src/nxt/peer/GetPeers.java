package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetPeers
  extends HttpJSONRequestHandler
{
  static final GetPeers instance = new GetPeers();
  
  public JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    for (Peer localPeer : Peer.getAllPeers()) {
      if ((localPeer.getBlacklistingTime() == 0L) && (localPeer.getAnnouncedAddress().length() > 0) && (localPeer.getState() == Peer.State.CONNECTED) && (localPeer.shareAddress())) {
        localJSONArray.add(localPeer.getAnnouncedAddress());
      }
    }
    localJSONObject.put("peers", localJSONArray);
    
    return localJSONObject;
  }
}