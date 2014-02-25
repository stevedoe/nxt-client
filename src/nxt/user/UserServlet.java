package nxt.user;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class UserServlet
  extends HttpServlet
{
  static abstract class UserRequestHandler
  {
    abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
      throws NxtException, IOException;
    
    boolean requirePost()
    {
      return false;
    }
  }
  
  private static final boolean enforcePost = Nxt.getBooleanProperty("nxt.uiServerEnforcePOST").booleanValue();
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
  
  protected void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    process(paramHttpServletRequest, paramHttpServletResponse);
  }
  
  protected void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    process(paramHttpServletRequest, paramHttpServletResponse);
  }
  
  private void process(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    paramHttpServletResponse.setHeader("Pragma", "no-cache");
    paramHttpServletResponse.setDateHeader("Expires", 0L);
    
    User localUser = null;
    try
    {
      String str = paramHttpServletRequest.getParameter("user");
      if (str == null) {
        return;
      }
      localUser = Users.getUser(str);
      if ((Users.allowedUserHosts != null) && (!Users.allowedUserHosts.contains(paramHttpServletRequest.getRemoteHost())))
      {
        localUser.enqueue(JSONResponses.DENY_ACCESS);
      }
      else
      {
        localObject1 = paramHttpServletRequest.getParameter("requestType");
        if (localObject1 == null)
        {
          localUser.enqueue(JSONResponses.INCORRECT_REQUEST);
        }
        else
        {
          UserRequestHandler localUserRequestHandler = (UserRequestHandler)userRequestHandlers.get(localObject1);
          if (localUserRequestHandler == null)
          {
            localUser.enqueue(JSONResponses.INCORRECT_REQUEST);
          }
          else if ((enforcePost) && (localUserRequestHandler.requirePost()) && (!"POST".equals(paramHttpServletRequest.getMethod())))
          {
            localUser.enqueue(JSONResponses.POST_REQUIRED);
          }
          else
          {
            JSONStreamAware localJSONStreamAware = localUserRequestHandler.processRequest(paramHttpServletRequest, localUser);
            if (localJSONStreamAware != null) {
              localUser.enqueue(localJSONStreamAware);
            }
          }
        }
      }
    }
    catch (RuntimeException|NxtException localRuntimeException)
    {
      Object localObject1;
      Logger.logMessage("Error processing GET request", localRuntimeException);
      if (localUser != null)
      {
        localObject1 = new JSONObject();
        ((JSONObject)localObject1).put("response", "showMessage");
        ((JSONObject)localObject1).put("message", localRuntimeException.toString());
        localUser.enqueue((JSONStreamAware)localObject1);
      }
    }
    finally
    {
      if (localUser != null) {
        localUser.processPendingResponses(paramHttpServletRequest, paramHttpServletResponse);
      }
    }
  }
}