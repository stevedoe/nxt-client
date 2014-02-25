package nxt.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

public final class APIServlet
  extends HttpServlet
{
  static abstract class APIRequestHandler
  {
    abstract JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
      throws NxtException;
    
    boolean requirePost()
    {
      return false;
    }
  }
  
  private static final boolean enforcePost = Nxt.getBooleanProperty("nxt.apiServerEnforcePOST").booleanValue();
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
    localHashMap.put("generateToken", GenerateToken.instance);
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
    process(paramHttpServletRequest, paramHttpServletResponse);
  }
  
  protected void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    process(paramHttpServletRequest, paramHttpServletResponse);
  }
  
  private void process(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws IOException
  {
    paramHttpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
    paramHttpServletResponse.setHeader("Pragma", "no-cache");
    paramHttpServletResponse.setDateHeader("Expires", 0L);
    
    JSONStreamAware localJSONStreamAware = JSON.emptyJSON;
    try
    {
      Object localObject1;
      Object localObject2;
      if ((API.allowedBotHosts != null) && (!API.allowedBotHosts.contains(paramHttpServletRequest.getRemoteHost())))
      {
        localJSONStreamAware = JSONResponses.ERROR_NOT_ALLOWED;
        



























        paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
        localObject1 = paramHttpServletResponse.getWriter();localObject2 = null;
        try
        {
          localJSONStreamAware.writeJSONString((Writer)localObject1);
        }
        catch (Throwable localThrowable2)
        {
          localObject2 = localThrowable2; throw localThrowable2;
        }
        finally
        {
          if (localObject1 != null) {
            if (localObject2 != null) {
              try
              {
                ((Writer)localObject1).close();
              }
              catch (Throwable localThrowable7)
              {
                ((Throwable)localObject2).addSuppressed(localThrowable7);
              }
            } else {
              ((Writer)localObject1).close();
            }
          }
        }
      }
      else
      {
        localObject1 = paramHttpServletRequest.getParameter("requestType");
        Object localObject3;
        if (localObject1 == null)
        {
          localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
          





















          paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
          localObject2 = paramHttpServletResponse.getWriter();localObject3 = null;
          try
          {
            localJSONStreamAware.writeJSONString((Writer)localObject2);
          }
          catch (Throwable localThrowable6)
          {
            localObject3 = localThrowable6; throw localThrowable6;
          }
          finally
          {
            if (localObject2 != null) {
              if (localObject3 != null) {
                try
                {
                  ((Writer)localObject2).close();
                }
                catch (Throwable localThrowable12)
                {
                  ((Throwable)localObject3).addSuppressed(localThrowable12);
                }
              } else {
                ((Writer)localObject2).close();
              }
            }
          }
        }
        else
        {
          localObject2 = (APIRequestHandler)apiRequestHandlers.get(localObject1);
          Object localObject5;
          if (localObject2 == null)
          {
            localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
            















            paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
            localObject3 = paramHttpServletResponse.getWriter();localObject5 = null;
            try
            {
              localJSONStreamAware.writeJSONString((Writer)localObject3);
            }
            catch (Throwable localThrowable9)
            {
              localObject5 = localThrowable9; throw localThrowable9;
            }
            finally
            {
              if (localObject3 != null) {
                if (localObject5 != null) {
                  try
                  {
                    ((Writer)localObject3).close();
                  }
                  catch (Throwable localThrowable13)
                  {
                    localObject5.addSuppressed(localThrowable13);
                  }
                } else {
                  ((Writer)localObject3).close();
                }
              }
            }
          }
          else if ((enforcePost) && (((APIRequestHandler)localObject2).requirePost()) && (!"POST".equals(paramHttpServletRequest.getMethod())))
          {
            localJSONStreamAware = JSONResponses.POST_REQUIRED;
            










            paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
            localObject3 = paramHttpServletResponse.getWriter();localObject5 = null;
            try
            {
              localJSONStreamAware.writeJSONString((Writer)localObject3);
            }
            catch (Throwable localThrowable11)
            {
              localObject5 = localThrowable11; throw localThrowable11;
            }
            finally
            {
              if (localObject3 != null) {
                if (localObject5 != null) {
                  try
                  {
                    ((Writer)localObject3).close();
                  }
                  catch (Throwable localThrowable14)
                  {
                    localObject5.addSuppressed(localThrowable14);
                  }
                } else {
                  ((Writer)localObject3).close();
                }
              }
            }
          }
          else
          {
            try
            {
              localJSONStreamAware = ((APIRequestHandler)localObject2).processRequest(paramHttpServletRequest);
            }
            catch (NxtException|RuntimeException localNxtException)
            {
              Logger.logDebugMessage("Error processing API request", localNxtException);
              localJSONStreamAware = JSONResponses.ERROR_INCORRECT_REQUEST;
            }
            paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
            localObject1 = paramHttpServletResponse.getWriter();localObject2 = null;
            try
            {
              localJSONStreamAware.writeJSONString((Writer)localObject1);
            }
            catch (Throwable localThrowable4)
            {
              localObject2 = localThrowable4; throw localThrowable4;
            }
            finally
            {
              if (localObject1 != null) {
                if (localObject2 != null) {
                  try
                  {
                    ((Writer)localObject1).close();
                  }
                  catch (Throwable localThrowable15)
                  {
                    ((Throwable)localObject2).addSuppressed(localThrowable15);
                  }
                } else {
                  ((Writer)localObject1).close();
                }
              }
            }
          }
        }
      }
    }
    finally
    {
      paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
      PrintWriter localPrintWriter = paramHttpServletResponse.getWriter();Object localObject11 = null;
      try
      {
        localJSONStreamAware.writeJSONString(localPrintWriter);
      }
      catch (Throwable localThrowable17)
      {
        localObject11 = localThrowable17;throw localThrowable17;
      }
      finally
      {
        if (localPrintWriter != null) {
          if (localObject11 != null) {
            try
            {
              localPrintWriter.close();
            }
            catch (Throwable localThrowable18)
            {
              localObject11.addSuppressed(localThrowable18);
            }
          } else {
            localPrintWriter.close();
          }
        }
      }
    }
  }
}