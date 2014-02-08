package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPeer
  extends HttpRequestHandler
{
  static final GetPeer instance = new GetPeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("peer");
    if (str == null) {
      return JSONResponses.MISSING_PEER;
    }
    Peer localPeer = Peer.getPeer(str);
    if (localPeer == null) {
      return JSONResponses.UNKNOWN_PEER;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("state", Integer.valueOf(localPeer.getState().ordinal()));
    localJSONObject.put("announcedAddress", localPeer.getAnnouncedAddress());
    if (localPeer.getHallmark() != null) {
      localJSONObject.put("hallmark", localPeer.getHallmark());
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