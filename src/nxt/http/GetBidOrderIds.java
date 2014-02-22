package nxt.http;

import java.util.Iterator;
import java.util.SortedSet;
import javax.servlet.http.HttpServletRequest;
import nxt.Asset;
import nxt.Order.Bid;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBidOrderIds
  extends APIServlet.APIRequestHandler
{
  static final GetBidOrderIds instance = new GetBidOrderIds();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("asset");
    if (str == null) {
      return JSONResponses.MISSING_ASSET;
    }
    long l;
    try
    {
      l = Convert.parseUnsignedLong(str).longValue();
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    if (Asset.getAsset(Long.valueOf(l)) == null) {
      return JSONResponses.UNKNOWN_ASSET;
    }
    int i;
    try
    {
      i = Integer.parseInt(paramHttpServletRequest.getParameter("limit"));
    }
    catch (NumberFormatException localNumberFormatException)
    {
      i = 2147483647;
    }
    JSONArray localJSONArray = new JSONArray();
    Iterator localIterator = Order.Bid.getSortedOrders(Long.valueOf(l)).iterator();
    while ((localIterator.hasNext()) && (i-- > 0)) {
      localJSONArray.add(Convert.toUnsignedLong(((Order.Bid)localIterator.next()).getId()));
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("bidOrderIds", localJSONArray);
    return localJSONObject;
  }
}