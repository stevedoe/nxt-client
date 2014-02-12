package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Order.Bid;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountCurrentBidOrderIds
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetAccountCurrentBidOrderIds instance = new GetAccountCurrentBidOrderIds();
  
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
    for (Object localObject = Order.Bid.getAllBidOrders().iterator(); ((Iterator)localObject).hasNext();)
    {
      Order.Bid localBid = (Order.Bid)((Iterator)localObject).next();
      if (((localLong == null) || (localBid.getAssetId().equals(localLong))) && (localBid.getAccount().equals(localAccount))) {
        localJSONArray.add(Convert.convert(localBid.getId()));
      }
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("bidOrderIds", localJSONArray);
    return localObject;
  }
}