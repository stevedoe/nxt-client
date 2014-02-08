package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

final class GetPeers
  extends HttpJSONRequestHandler
{
  static final GetPeers instance = new GetPeers();
  
  JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    for (Peer localPeer : Peer.getAllPeers()) {
      if ((!localPeer.isBlacklisted()) && (localPeer.getAnnouncedAddress() != null) && (localPeer.getState() == Peer.State.CONNECTED) && (localPeer.shareAddress())) {
        localJSONArray.add(localPeer.getAnnouncedAddress());
      }
    }
    localJSONObject.put("peers", localJSONArray);
    
    return localJSONObject;
  }
}