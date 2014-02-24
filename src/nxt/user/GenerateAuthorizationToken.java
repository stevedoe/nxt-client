package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import nxt.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GenerateAuthorizationToken
  extends UserServlet.UserRequestHandler
{
  static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    if (!paramUser.getSecretPhrase().equals(str1)) {
      return JSONResponses.INVALID_SECRET_PHRASE;
    }
    String str2 = Token.generateToken(str1, paramHttpServletRequest.getParameter("website").trim());
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("response", "showAuthorizationToken");
    localJSONObject.put("token", str2);
    
    return localJSONObject;
  }
  
  boolean requirePost()
  {
    return true;
  }
}