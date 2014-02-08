package nxt.user;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import nxt.Nxt;
import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

final class RemoveBlacklistedPeer
  extends UserRequestHandler
{
  static final RemoveBlacklistedPeer instance = new RemoveBlacklistedPeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    if ((Nxt.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
      return JSONResponses.LOCAL_USERS_ONLY;
    }
    int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
    for (Peer localPeer : Peer.getAllPeers()) {
      if (localPeer.getIndex() == i)
      {
        if (!localPeer.isBlacklisted()) {
          break;
        }
        localPeer.removeBlacklistedStatus(); break;
      }
    }
    return null;
  }
}