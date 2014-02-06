package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

final class LockAccount
  extends UserRequestHandler
{
  static final LockAccount instance = new LockAccount();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    paramUser.deinitializeKeyPair();
    
    return JSONResponses.LOCK_ACCOUNT;
  }
}