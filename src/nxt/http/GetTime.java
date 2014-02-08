package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetTime
  extends HttpRequestHandler
{
  static final GetTime instance = new GetTime();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("time", Integer.valueOf(Convert.getEpochTime()));
    
    return localJSONObject;
  }
}