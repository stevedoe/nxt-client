package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAccountPublicKey
  extends HttpRequestHandler
{
  static final GetAccountPublicKey instance = new GetAccountPublicKey();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("account");
    if (str == null) {
      return JSONResponses.MISSING_ACCOUNT;
    }
    Account localAccount;
    try
    {
      localAccount = Account.getAccount(Convert.parseUnsignedLong(str));
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    if (localAccount == null) {
      return JSONResponses.UNKNOWN_ACCOUNT;
    }
    if (localAccount.getPublicKey() != null)
    {
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("publicKey", Convert.convert(localAccount.getPublicKey()));
      return localJSONObject;
    }
    return JSON.emptyJSON;
  }
}