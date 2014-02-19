package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Token;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class DecodeToken
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final DecodeToken instance = new DecodeToken();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("website");
    String str2 = paramHttpServletRequest.getParameter("token");
    if (str1 == null) {
      return JSONResponses.MISSING_WEBSITE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_TOKEN;
    }
    try
    {
      Token localToken = Token.parseToken(str2, str1.trim());
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("account", Convert.toUnsignedLong(Account.getId(localToken.getPublicKey())));
      localJSONObject.put("timestamp", Integer.valueOf(localToken.getTimestamp()));
      localJSONObject.put("valid", Boolean.valueOf(localToken.isValid()));
      
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_WEBSITE;
  }
}