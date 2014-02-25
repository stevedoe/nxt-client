package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.peer.Hallmark;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPeer
  extends APIServlet.APIRequestHandler
{
  static final GetPeer instance = new GetPeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("peer");
    if (str == null) {
      return JSONResponses.MISSING_PEER;
    }
    Peer localPeer = Peers.getPeer(str);
    if (localPeer == null) {
      return JSONResponses.UNKNOWN_PEER;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("state", Integer.valueOf(localPeer.getState().ordinal()));
    localJSONObject.put("announcedAddress", localPeer.getAnnouncedAddress());
    localJSONObject.put("shareAddress", Boolean.valueOf(localPeer.shareAddress()));
    if (localPeer.getHallmark() != null) {
      localJSONObject.put("hallmark", localPeer.getHallmark().getHallmarkString());
    }
    localJSONObject.put("weight", Integer.valueOf(localPeer.getWeight()));
    localJSONObject.put("downloadedVolume", Long.valueOf(localPeer.getDownloadedVolume()));
    localJSONObject.put("uploadedVolume", Long.valueOf(localPeer.getUploadedVolume()));
    localJSONObject.put("application", localPeer.getApplication());
    localJSONObject.put("version", localPeer.getVersion());
    localJSONObject.put("platform", localPeer.getPlatform());
    
    return localJSONObject;
  }
}