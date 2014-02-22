package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Alias;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAliasURI
  extends APIServlet.APIRequestHandler
{
  static final GetAliasURI instance = new GetAliasURI();
  
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
    if (localAlias.getURI().length() > 0)
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("uri", localAlias.getURI());
      return localJSONObject;
    }
    return JSON.emptyJSON;
  }
}