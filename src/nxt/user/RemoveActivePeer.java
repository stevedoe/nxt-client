package nxt.user;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import nxt.Nxt;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import org.json.simple.JSONStreamAware;

final class RemoveActivePeer
  extends UserRequestHandler
{
  static final RemoveActivePeer instance = new RemoveActivePeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
      return JSONResponses.LOCAL_USERS_ONLY;
    }
    int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
    for (Peer localPeer : Peer.getAllPeers()) {
      if (User.getIndex(localPeer) == i)
      {
        if ((localPeer.isBlacklisted()) || (localPeer.getState() == Peer.State.NON_CONNECTED)) {
          break;
        }
        localPeer.deactivate(); break;
      }
    }
    return null;
  }
}