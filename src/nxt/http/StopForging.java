package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class StopForging
  extends APIServlet.APIRequestHandler
{
  static final StopForging instance = new StopForging();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("secretPhrase");
    if (str == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    Generator localGenerator = Generator.stopForging(str);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("foundAndStopped", Boolean.valueOf(localGenerator != null));
    return localJSONObject;
  }
}