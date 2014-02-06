package nxt.http;

import java.util.Iterator;
import java.util.SortedSet;
import javax.servlet.http.HttpServletRequest;
import nxt.Asset;
import nxt.Order.Ask;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAskOrderIds
  extends HttpRequestHandler
{
  static final GetAskOrderIds instance = new GetAskOrderIds();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
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
    Iterator localIterator = Order.Ask.getSortedOrders(Long.valueOf(l)).iterator();
    while (localIterator.hasNext()) {
      localJSONArray.add(Convert.convert(((Order.Ask)localIterator.next()).getId()));
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("askOrderIds", localJSONArray);
    return localJSONObject;
  }
}