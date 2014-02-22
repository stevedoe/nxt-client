package nxt.http;

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
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject2 : localPoll.getOptions()) {
      localJSONArray.add(localObject2);
    }
    localJSONObject.put("options", localJSONArray);
    localJSONObject.put("minNumberOfOptions", Byte.valueOf(localPoll.getMinNumberOfOptions()));
    localJSONObject.put("maxNumberOfOptions", Byte.valueOf(localPoll.getMaxNumberOfOptions()));
    localJSONObject.put("optionsAreBinary", Boolean.valueOf(localPoll.isOptionsAreBinary()));
    ??? = new JSONArray();
    for (Long localLong : localPoll.getVoters().keySet()) {
      ((JSONArray)???).add(Convert.toUnsignedLong(localLong));
    }
    localJSONObject.put("voters", ???);
    
    return localJSONObject;
  }
}