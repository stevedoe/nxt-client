package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GenerateToken
  extends APIServlet.APIRequestHandler
{
  static final GenerateToken instance = new GenerateToken();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("website");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_WEBSITE;
    }
    try
    {
      String str3 = Token.generateToken(str1, str2.trim());
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("token", str3);
      
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_WEBSITE;
  }
  
  boolean requirePost()
  {
    return true;
  }
}