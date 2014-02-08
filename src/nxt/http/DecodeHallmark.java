package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.peer.Hallmark;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class DecodeHallmark
  extends HttpRequestHandler
{
  static final DecodeHallmark instance = new DecodeHallmark();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("hallmark");
    if (str1 == null) {
      return JSONResponses.MISSING_HALLMARK;
    }
    try
    {
      Hallmark localHallmark = Hallmark.parseHallmark(str1);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("account", Convert.convert(Account.getId(localHallmark.getPublicKey())));
      localJSONObject.put("host", localHallmark.getHost());
      localJSONObject.put("weight", Integer.valueOf(localHallmark.getWeight()));
      String str2 = Hallmark.formatDate(localHallmark.getDate());
      localJSONObject.put("date", str2);
      localJSONObject.put("valid", Boolean.valueOf(localHallmark.isValid()));
      
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_HALLMARK;
  }
}