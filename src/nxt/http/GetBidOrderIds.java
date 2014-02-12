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
  extends HttpRequestDispatcher.HttpRequestHandler
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
    JSONArray localJSONArray = new JSONArray();
    Iterator localIterator = Order.Bid.getSortedOrders(Long.valueOf(l)).iterator();
    while (localIterator.hasNext()) {
      localJSONArray.add(Convert.convert(((Order.Bid)localIterator.next()).getId()));
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("bidOrderIds", localJSONArray);
    return localJSONObject;
  }
}