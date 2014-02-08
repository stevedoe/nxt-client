package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.peer.Hallmark;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class MarkHost
  extends HttpRequestHandler
{
  static final MarkHost instance = new MarkHost();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("host");
    String str3 = paramHttpServletRequest.getParameter("weight");
    String str4 = paramHttpServletRequest.getParameter("date");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_HOST;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_WEIGHT;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_DATE;
    }
    if (str2.length() > 100) {
      return JSONResponses.INCORRECT_HOST;
    }
    int i;
    try
    {
      i = Integer.parseInt(str3);
      if ((i <= 0) || (i > 1000000000L)) {
        return JSONResponses.INCORRECT_WEIGHT;
      }
    }
    catch (NumberFormatException localNumberFormatException)
    {
      return JSONResponses.INCORRECT_WEIGHT;
    }
    try
    {
      String str5 = Hallmark.generateHallmark(str1, str2, i, Hallmark.parseDate(str4));
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("hallmark", str5);
      return localJSONObject;
    }
    catch (RuntimeException localRuntimeException) {}
    return JSONResponses.INCORRECT_DATE;
  }
}