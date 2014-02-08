package nxt.http;

import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetMyInfo
  extends HttpRequestHandler
{
  static final GetMyInfo instance = new GetMyInfo();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("host", paramHttpServletRequest.getRemoteHost());
    localJSONObject.put("address", paramHttpServletRequest.getRemoteAddr());
    return localJSONObject;
  }
}