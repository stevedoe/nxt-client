package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBalance
  extends APIServlet.APIRequestHandler
{
  static final GetBalance instance = new GetBalance();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
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
    JSONObject localJSONObject = new JSONObject();
    if (localAccount == null)
    {
      localJSONObject.put("balance", Integer.valueOf(0));
      localJSONObject.put("unconfirmedBalance", Integer.valueOf(0));
      localJSONObject.put("effectiveBalance", Integer.valueOf(0));
    }
    else
    {
      synchronized (localAccount)
      {
        localJSONObject.put("balance", Long.valueOf(localAccount.getBalance()));
        localJSONObject.put("unconfirmedBalance", Long.valueOf(localAccount.getUnconfirmedBalance()));
        localJSONObject.put("effectiveBalance", Long.valueOf(localAccount.getEffectiveBalance() * 100L));
      }
    }
    return localJSONObject;
  }
}