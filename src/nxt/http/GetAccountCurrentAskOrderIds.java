package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Order.Ask;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountCurrentAskOrderIds
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetAccountCurrentAskOrderIds instance = new GetAccountCurrentAskOrderIds();
  
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
    catch (RuntimeException localRuntimeException1)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    Long localLong = null;
    try
    {
      localLong = Convert.parseUnsignedLong(paramHttpServletRequest.getParameter("asset"));
    }
    catch (RuntimeException localRuntimeException2) {}
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Order.Ask.getAllAskOrders().iterator(); ((Iterator)localObject).hasNext();)
    {
      Order.Ask localAsk = (Order.Ask)((Iterator)localObject).next();
      if (((localLong == null) || (localAsk.getAssetId().equals(localLong))) && (localAsk.getAccount().equals(localAccount))) {
        localJSONArray.add(Convert.toUnsignedLong(localAsk.getId()));
      }
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("askOrderIds", localJSONArray);
    return localObject;
  }
}