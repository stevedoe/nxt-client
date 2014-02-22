package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAliasId
  extends APIServlet.APIRequestHandler
{
  static final GetAliasId instance = new GetAliasId();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("alias");
    if (str == null) {
      return JSONResponses.MISSING_ALIAS;
    }
    Alias localAlias = Alias.getAlias(str.toLowerCase());
    if (localAlias == null) {
      return JSONResponses.UNKNOWN_ALIAS;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("id", Convert.toUnsignedLong(localAlias.getId()));
    return localJSONObject;
  }
}