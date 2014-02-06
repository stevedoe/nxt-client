package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAliasId
  extends HttpRequestHandler
{
  static final GetAliasId instance = new GetAliasId();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
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
    localJSONObject.put("id", Convert.convert(localAlias.getId()));
    return localJSONObject;
  }
}