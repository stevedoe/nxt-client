package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAlias
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetAlias instance = new GetAlias();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("alias");
    if (str == null) {
      return JSONResponses.MISSING_ALIAS;
    }
    Alias localAlias;
    try
    {
      localAlias = Alias.getAlias(Convert.parseUnsignedLong(str));
      if (localAlias == null) {
        return JSONResponses.UNKNOWN_ALIAS;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ALIAS;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("account", Convert.toUnsignedLong(localAlias.getAccount().getId()));
    localJSONObject.put("alias", localAlias.getAliasName());
    if (localAlias.getURI().length() > 0) {
      localJSONObject.put("uri", localAlias.getURI());
    }
    localJSONObject.put("timestamp", Integer.valueOf(localAlias.getTimestamp()));
    return localJSONObject;
  }
}