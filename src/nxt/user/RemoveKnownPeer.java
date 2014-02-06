package nxt.user;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import nxt.Nxt;
import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

final class RemoveKnownPeer
  extends UserRequestHandler
{
  static final RemoveKnownPeer instance = new RemoveKnownPeer();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
      return JSONResponses.LOCAL_USERS_ONLY;
    }
    int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
    for (Peer localPeer : Peer.getAllPeers()) {
      if (localPeer.getIndex() == i)
      {
        localPeer.removePeer();
        break;
      }
    }
    return null;
  }
}