package nxt.peer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInfo
  extends PeerServlet.PeerRequestHandler
{
  static final GetInfo instance = new GetInfo();
  
  JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    PeerImpl localPeerImpl = (PeerImpl)paramPeer;
    String str1 = (String)paramJSONObject.get("announcedAddress");
    if (str1 != null)
    {
      str1 = str1.trim();
      if (str1.length() > 0) {
        localPeerImpl.setAnnouncedAddress(str1);
      }
    }
    String str2 = (String)paramJSONObject.get("application");
    if (str2 == null) {
      str2 = "?";
    }
    localPeerImpl.setApplication(str2.trim());
    
    String str3 = (String)paramJSONObject.get("version");
    if (str3 == null) {
      str3 = "?";
    }
    localPeerImpl.setVersion(str3.trim());
    
    String str4 = (String)paramJSONObject.get("platform");
    if (str4 == null) {
      str4 = "?";
    }
    localPeerImpl.setPlatform(str4.trim());
    
    localPeerImpl.setShareAddress(Boolean.TRUE.equals(paramJSONObject.get("shareAddress")));
    
    localPeerImpl.setState(Peer.State.CONNECTED);
    
    return Peers.myPeerInfoResponse;
  }
}