package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Order.Bid;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBidOrder
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetBidOrder instance = new GetBidOrder();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("order");
    if (str == null) {
      return JSONResponses.MISSING_ORDER;
    }
    Order.Bid localBid;
    try
    {
      localBid = Order.Bid.getBidOrder(Convert.parseUnsignedLong(str));
      if (localBid == null) {
        return JSONResponses.UNKNOWN_ORDER;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ORDER;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("account", Convert.convert(localBid.getAccount().getId()));
    localJSONObject.put("asset", Convert.convert(localBid.getAssetId()));
    localJSONObject.put("quantity", Integer.valueOf(localBid.getQuantity()));
    localJSONObject.put("price", Long.valueOf(localBid.getPrice()));
    
    return localJSONObject;
  }
}