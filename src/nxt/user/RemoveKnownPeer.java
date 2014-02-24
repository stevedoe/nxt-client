package nxt.user;

import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import nxt.peer.Peer;
import org.json.simple.JSONStreamAware;

public final class RemoveKnownPeer
  extends UserServlet.UserRequestHandler
{
  static final RemoveKnownPeer instance = new RemoveKnownPeer();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    if ((Users.allowedUserHosts == null) && (!InetAddress.getByName(paramHttpServletRequest.getRemoteAddr()).isLoopbackAddress())) {
      return JSONResponses.LOCAL_USERS_ONLY;
    }
    int i = Integer.parseInt(paramHttpServletRequest.getParameter("peer"));
    Peer localPeer = Users.getPeer(i);
    if (localPeer != null) {
      localPeer.remove();
    }
    return null;
  }
}