package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAliasIds
  extends HttpRequestHandler
{
  static final GetAliasIds instance = new GetAliasIds();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("timestamp");
    if (str == null) {
      return JSONResponses.MISSING_TIMESTAMP;
    }
    int i;
    try
    {
      i = Integer.parseInt(str);
      if (i < 0) {
        return JSONResponses.INCORRECT_TIMESTAMP;
      }
    }
    catch (NumberFormatException localNumberFormatException)
    {
      return JSONResponses.INCORRECT_TIMESTAMP;
    }
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Alias.getAllAliases().iterator(); ((Iterator)localObject).hasNext();)
    {
      Alias localAlias = (Alias)((Iterator)localObject).next();
      if (localAlias.getTimestamp() >= i) {
        localJSONArray.add(Convert.convert(localAlias.getId()));
      }
    }
    localObject = new JSONObject();
    
    ((JSONObject)localObject).put("aliasIds", localJSONArray);
    return localObject;
  }
}