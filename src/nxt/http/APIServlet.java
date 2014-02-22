package nxt.http;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.NxtException;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

public final class APIServlet
  extends HttpServlet
{
  private static final Map<String, APIRequestHandler> apiRequestHandlers;
  
  static
  {
    HashMap localHashMap = new HashMap();
    
    localHashMap.put("assignAlias", AssignAlias.instance);
    localHashMap.put("broadcastTransaction", BroadcastTransaction.instance);
    localHashMap.put("cancelAskOrder", CancelAskOrder.instance);
    localHashMap.put("cancelBidOrder", CancelBidOrder.instance);
    localHashMap.put("castVote", CastVote.instance);
    localHashMap.put("createPoll", CreatePoll.instance);
    localHashMap.put("decodeHallmark", DecodeHallmark.instance);
    localHashMap.put("decodeToken", DecodeToken.instance);
    localHashMap.put("getAccount", GetAccount.instance);
    localHashMap.put("getAccountBlockIds", GetAccountBlockIds.instance);
    localHashMap.put("getAccountId", GetAccountId.instance);
    localHashMap.put("getAccountPublicKey", GetAccountPublicKey.instance);
    localHashMap.put("getAccountTransactionIds", GetAccountTransactionIds.instance);
    localHashMap.put("getAlias", GetAlias.instance);
    localHashMap.put("getAliasId", GetAliasId.instance);
    localHashMap.put("getAliasIds", GetAliasIds.instance);
    localHashMap.put("getAliasURI", GetAliasURI.instance);
    localHashMap.put("getAsset", GetAsset.instance);
    localHashMap.put("getAssetIds", GetAssetIds.instance);
    localHashMap.put("getBalance", GetBalance.instance);
    localHashMap.put("getBlock", GetBlock.instance);
    localHashMap.put("getConstants", GetConstants.instance);
    localHashMap.put("getGuaranteedBalance", GetGuaranteedBalance.instance);
    localHashMap.put("getMyInfo", GetMyInfo.instance);
    localHashMap.put("getPeer", GetPeer.instance);
    localHashMap.put("getPeers", GetPeers.instance);
    localHashMap.put("getPoll", GetPoll.instance);
    localHashMap.put("getPollIds", GetPollIds.instance);
    localHashMap.put("getState", GetState.instance);
    localHashMap.put("getTime", GetTime.instance);
    localHashMap.put("getTrades", GetTrades.instance);
    localHashMap.put("getTransaction", GetTransaction.instance);
    localHashMap.put("getTransactionBytes", GetTransactionBytes.instance);
    localHashMap.put("getUnconfirmedTransactionIds", GetUnconfirmedTransactionIds.instance);
    localHashMap.put("getAccountCurrentAskOrderIds", GetAccountCurrentAskOrderIds.instance);
    localHashMap.put("getAccountCurrentBidOrderIds", GetAccountCurrentBidOrderIds.instance);
    localHashMap.put("getAskOrder", GetAskOrder.instance);
    localHashMap.put("getAskOrderIds", GetAskOrderIds.instance);
    localHashMap.put("getBidOrder", GetBidOrder.instance);
    localHashMap.put("getBidOrderIds", GetBidOrderIds.instance);
    localHashMap.put("issueAsset", IssueAsset.instance);
    localHashMap.put("listAccountAliases", ListAccountAliases.instance);
    localHashMap.put("markHost", MarkHost.instance);
    localHashMap.put("placeAskOrder", PlaceAskOrder.instance);
    localHashMap.put("placeBidOrder", PlaceBidOrder.instance);
    localHashMap.put("sendMessage", SendMessage.instance);
    localHashMap.put("sendMoney", SendMoney.instance);
    localHashMap.put("startForging", StartForging.instance);
    localHashMap.put("stopForging", StopForging.instance);
    localHashMap.put("getForging", GetForging.instance);
    localHashMap.put("transferAsset", TransferAsset.instance);
    
    apiRequestHandlers = Collections.unmodifiableMap(localHashMap);
  }
  
  protected void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    paramHttpServletResponse.setHeader("Pragma", "no-cache");
    paramHttpServletResponse.setDateHeader("Expires", 0L);
    JSONStreamAware localJSONStreamAware;
    if ((API.allowedBotHosts != null) && (!API.allowedBotHosts.contains(paramHttpServletRequest.getRemoteHost())))
    {
      localJSONStreamAware = JSONResponses.ERROR_NOT_ALLOWED;
    }
    else
    {
      localObject1 = paramHttpServletRequest.getParameter("requestType");
      if (localObject1 == null)
      {
        localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
      }
      else
      {
        localObject2 = (APIRequestHandler)apiRequestHandlers.get(localObject1);
        if (localObject2 != null) {
          try
          {
            localJSONStreamAware = ((APIRequestHandler)localObject2).processRequest(paramHttpServletRequest);
          }
          catch (NxtException|RuntimeException localNxtException)
          {
            Logger.logDebugMessage("Error processing API request", localNxtException);
            localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
          }
        } else {
          localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
        }
      }
    }
    paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
    
    Object localObject1 = paramHttpServletResponse.getWriter();Object localObject2 = null;
    try
    {
      localJSONStreamAware.writeJSONString((Writer)localObject1);
    }
    catch (Throwable localThrowable2)
    {
      localObject2 = localThrowable2;throw localThrowable2;
    }
    finally
    {
      if (localObject1 != null) {
        if (localObject2 != null) {
          try
          {
            ((Writer)localObject1).close();
          }
          catch (Throwable localThrowable3)
          {
            ((Throwable)localObject2).addSuppressed(localThrowable3);
          }
        } else {
          ((Writer)localObject1).close();
        }
      }
    }
  }
  
  static abstract class APIRequestHandler
  {
    abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
      throws NxtException, IOException;
  }
}