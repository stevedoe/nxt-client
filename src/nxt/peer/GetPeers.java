package nxt.peer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetPeers
  extends PeerServlet.PeerRequestHandler
{
  static final GetPeers instance = new GetPeers();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    for (Peer localPeer : Peers.getAllPeers()) {
      if ((!localPeer.isBlacklisted()) && (localPeer.getAnnouncedAddress() != null) && (localPeer.getState() == Peer.State.CONNECTED) && (localPeer.shareAddress())) {
        localJSONArray.add(localPeer.getAnnouncedAddress());
      }
    }
    localJSONObject.put("peers", localJSONArray);
    
    return localJSONObject;
  }
}