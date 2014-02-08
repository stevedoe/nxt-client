package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

final class GetNewData
  extends UserRequestHandler
{
  static final GetNewData instance = new GetNewData();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    return null;
  }
}