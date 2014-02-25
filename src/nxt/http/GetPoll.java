package nxt.http;

import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPoll
  extends APIServlet.APIRequestHandler
{
  static final GetPoll instance = new GetPoll();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("poll");
    if (str == null) {
      return JSONResponses.MISSING_POLL;
    }
    Poll localPoll;
    try
    {
      localPoll = Poll.getPoll(Convert.parseUnsignedLong(str));
      if (localPoll == null) {
        return JSONResponses.UNKNOWN_POLL;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_POLL;
    }
    JSONObject localJSONObject = new JSONObject();
    if (localPoll.getName().length() > 0) {
      localJSONObject.put("name", localPoll.getName());
    }
    if (localPoll.getDescription().length() > 0) {
      localJSONObject.put("description", localPoll.getDescription());
    }
    JSONArray localJSONArray1 = new JSONArray();
    Collections.addAll(localJSONArray1, localPoll.getOptions());
    localJSONObject.put("options", localJSONArray1);
    localJSONObject.put("minNumberOfOptions", Byte.valueOf(localPoll.getMinNumberOfOptions()));
    localJSONObject.put("maxNumberOfOptions", Byte.valueOf(localPoll.getMaxNumberOfOptions()));
    localJSONObject.put("optionsAreBinary", Boolean.valueOf(localPoll.isOptionsAreBinary()));
    JSONArray localJSONArray2 = new JSONArray();
    for (Long localLong : localPoll.getVoters().keySet()) {
      localJSONArray2.add(Convert.toUnsignedLong(localLong));
    }
    localJSONObject.put("voters", localJSONArray2);
    
    return localJSONObject;
  }
}