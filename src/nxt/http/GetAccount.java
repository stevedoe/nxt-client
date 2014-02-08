package nxt.http;

import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccount
  extends HttpRequestHandler
{
  static final GetAccount instance = new GetAccount();
  
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
      if (localAccount == null) {
        return JSONResponses.UNKNOWN_ACCOUNT;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    JSONObject localJSONObject1 = new JSONObject();
    synchronized (localAccount)
    {
      if (localAccount.getPublicKey() != null) {
        localJSONObject1.put("publicKey", Convert.convert(localAccount.getPublicKey()));
      }
      localJSONObject1.put("balance", Long.valueOf(localAccount.getBalance()));
      localJSONObject1.put("effectiveBalance", Long.valueOf(localAccount.getEffectiveBalance() * 100L));
      
      JSONArray localJSONArray = new JSONArray();
      for (Map.Entry localEntry : localAccount.getAssetBalances().entrySet())
      {
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("asset", Convert.convert((Long)localEntry.getKey()));
        localJSONObject2.put("balance", localEntry.getValue());
        localJSONArray.add(localJSONObject2);
      }
      if (localJSONArray.size() > 0) {
        localJSONObject1.put("assetBalances", localJSONArray);
      }
    }
    return localJSONObject1;
  }
}