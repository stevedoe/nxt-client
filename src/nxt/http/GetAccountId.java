package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountId
  extends APIServlet.APIRequestHandler
{
  static final GetAccountId instance = new GetAccountId();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("secretPhrase");
    if (str == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    byte[] arrayOfByte = Crypto.getPublicKey(str);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("accountId", Convert.toUnsignedLong(Account.getId(arrayOfByte)));
    
    return localJSONObject;
  }
  
  boolean requirePost()
  {
    return true;
  }
}