package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetGuaranteedBalance
  extends APIServlet.APIRequestHandler
{
  static final GetGuaranteedBalance instance = new GetGuaranteedBalance();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("account");
    String str2 = paramHttpServletRequest.getParameter("numberOfConfirmations");
    if (str1 == null) {
      return JSONResponses.MISSING_ACCOUNT;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_NUMBER_OF_CONFIRMATIONS;
    }
    Account localAccount;
    try
    {
      localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    JSONObject localJSONObject = new JSONObject();
    if (localAccount == null) {
      localJSONObject.put("guaranteedBalance", Integer.valueOf(0));
    } else {
      try
      {
        int i = Integer.parseInt(str2);
        localJSONObject.put("guaranteedBalance", Long.valueOf(localAccount.getGuaranteedBalance(i)));
      }
      catch (NumberFormatException localNumberFormatException)
      {
        return JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
      }
    }
    return localJSONObject;
  }
}