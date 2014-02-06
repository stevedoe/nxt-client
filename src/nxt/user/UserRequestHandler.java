package nxt.user;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import nxt.NxtException;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public abstract class UserRequestHandler
{
  private static final Map<String, UserRequestHandler> userRequestHandlers;
  
  static
  {
    HashMap localHashMap = new HashMap();
    
    localHashMap.put("generateAuthorizationToken", GenerateAuthorizationToken.instance);
    localHashMap.put("getInitialData", GetInitialData.instance);
    localHashMap.put("getNewData", GetNewData.instance);
    localHashMap.put("lockAccount", LockAccount.instance);
    localHashMap.put("removeActivePeer", RemoveActivePeer.instance);
    localHashMap.put("removeBlacklistedPeer", RemoveBlacklistedPeer.instance);
    localHashMap.put("removeKnownPeer", RemoveKnownPeer.instance);
    localHashMap.put("sendMoney", SendMoney.instance);
    localHashMap.put("unlockAccount", UnlockAccount.instance);
    
    userRequestHandlers = Collections.unmodifiableMap(localHashMap);
  }
  
  abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws NxtException, IOException;
  
  public static void process(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws ServletException, IOException
  {
    try
    {
      String str = paramHttpServletRequest.getParameter("requestType");
      if (str != null)
      {
        localObject = (UserRequestHandler)userRequestHandlers.get(str);
        if (localObject != null)
        {
          JSONStreamAware localJSONStreamAware = ((UserRequestHandler)localObject).processRequest(paramHttpServletRequest, paramUser);
          if (localJSONStreamAware != null) {
            paramUser.enqueue(localJSONStreamAware);
          }
          return;
        }
      }
      localObject = new JSONObject();
      ((JSONObject)localObject).put("response", "showMessage");
      ((JSONObject)localObject).put("message", "Incorrect request!");
      paramUser.enqueue((JSONStreamAware)localObject);
    }
    catch (Exception localException)
    {
      Logger.logMessage("Error processing GET request", localException);
      Object localObject = new JSONObject();
      ((JSONObject)localObject).put("response", "showMessage");
      ((JSONObject)localObject).put("message", localException.toString());
      paramUser.enqueue((JSONStreamAware)localObject);
    }
  }
}