package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class StartForging
  extends APIServlet.APIRequestHandler
{
  static final StartForging instance = new StartForging();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("secretPhrase");
    if (str == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    Generator localGenerator = Generator.startForging(str);
    if (localGenerator == null) {
      return JSONResponses.UNKNOWN_ACCOUNT;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("deadline", Long.valueOf(localGenerator.getDeadline()));
    return localJSONObject;
  }
}