package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class GetNewData
  extends UserServlet.UserRequestHandler
{
  static final GetNewData instance = new GetNewData();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    return null;
  }
}