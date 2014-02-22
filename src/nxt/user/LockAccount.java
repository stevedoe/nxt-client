package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class LockAccount
  extends UserServlet.UserRequestHandler
{
  static final LockAccount instance = new LockAccount();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    paramUser.lockAccount();
    
    return JSONResponses.LOCK_ACCOUNT;
  }
}