package nxt.user;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.peer.Peers;
import org.json.simple.JSONStreamAware;

public final class RemoveActivePeer
  extends UserServlet.UserRequestHandler
{
  static final RemoveActivePeer instance = new RemoveActivePeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    if ((Users.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
      return JSONResponses.LOCAL_USERS_ONLY;
    }
    int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
    for (Peer localPeer : Peers.getAllPeers()) {
      if (Users.getIndex(localPeer) == i)
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