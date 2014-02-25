package nxt.http;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nxt.Asset;
import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import nxt.Trade;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetTrades
  extends APIServlet.APIRequestHandler
{
  static final GetTrades instance = new GetTrades();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("asset");
    if (str == null) {
      return JSONResponses.MISSING_ASSET;
    }
    Long localLong;
    try
    {
      localLong = Convert.parseUnsignedLong(str);
      if (Asset.getAsset(localLong) == null) {
        return JSONResponses.UNKNOWN_ASSET;
      }
    }
    catch (RuntimeException localRuntimeException1)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    int i;
    try
    {
      i = Integer.parseInt(paramHttpServletRequest.getParameter("firstIndex"));
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      i = 0;
    }
    int j;
    try
    {
      j = Integer.parseInt(paramHttpServletRequest.getParameter("lastIndex"));
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      j = 2147483647;
    }
    JSONObject localJSONObject1 = new JSONObject();
    
    JSONArray localJSONArray = new JSONArray();
    try
    {
      List localList = Trade.getTrades(localLong);
      for (int k = i; k <= j; k++)
      {
        Trade localTrade = (Trade)localList.get(k);
        
        JSONObject localJSONObject2 = new JSONObject();
        localJSONObject2.put("timestamp", Integer.valueOf(Nxt.getBlockchain().getBlock(localTrade.getBlockId()).getTimestamp()));
        localJSONObject2.put("askOrderId", Convert.toUnsignedLong(localTrade.getAskOrderId()));
        localJSONObject2.put("bidOrderId", Convert.toUnsignedLong(localTrade.getBidOrderId()));
        localJSONObject2.put("quantity", Integer.valueOf(localTrade.getQuantity()));
        localJSONObject2.put("price", Long.valueOf(localTrade.getPrice()));
        
        localJSONArray.add(localJSONObject2);
      }
    }
    catch (RuntimeException localRuntimeException2)
    {
      localJSONObject1.put("error", localRuntimeException2.toString());
    }
    localJSONObject1.put("trades", localJSONArray);
    
    return localJSONObject1;
  }
}