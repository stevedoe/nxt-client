package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Order.Ask;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAskOrder
  extends HttpRequestHandler
{
  static final GetAskOrder instance = new GetAskOrder();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("order");
    if (str == null) {
      return JSONResponses.MISSING_ORDER;
    }
    Order.Ask localAsk;
    try
    {
      localAsk = Order.Ask.getAskOrder(Convert.parseUnsignedLong(str));
      if (localAsk == null) {
        return JSONResponses.UNKNOWN_ORDER;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ORDER;
    }
    JSONObject localJSONObject = new JSONObject();
    
    localJSONObject.put("account", Convert.convert(localAsk.getAccount().getId()));
    localJSONObject.put("asset", Convert.convert(localAsk.getAssetId()));
    localJSONObject.put("quantity", Integer.valueOf(localAsk.getQuantity()));
    localJSONObject.put("price", Long.valueOf(localAsk.getPrice()));
    return localJSONObject;
  }
}