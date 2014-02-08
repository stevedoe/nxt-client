package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.peer.Peer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPeers
  extends HttpRequestHandler
{
  static final GetPeers instance = new GetPeers();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Peer.getAllPeers().iterator(); ((Iterator)localObject).hasNext();)
    {
      Peer localPeer = (Peer)((Iterator)localObject).next();
      localJSONArray.add(localPeer.getPeerAddress());
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("peers", localJSONArray);
    return localObject;
  }
}