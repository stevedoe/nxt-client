package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Poll;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPollIds
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetPollIds instance = new GetPollIds();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Poll.getAllPolls().iterator(); ((Iterator)localObject).hasNext();)
    {
      Poll localPoll = (Poll)((Iterator)localObject).next();
      localJSONArray.add(Convert.toUnsignedLong(localPoll.getId()));
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("pollIds", localJSONArray);
    return localObject;
  }
}