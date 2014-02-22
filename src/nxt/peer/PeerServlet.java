package nxt.peer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.util.CountingInputStream;
import nxt.util.CountingOutputStream;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

public final class PeerServlet
  extends HttpServlet
{
  private static final Map<String, PeerRequestHandler> peerRequestHandlers;
  private static final JSONStreamAware UNSUPPORTED_REQUEST_TYPE;
  private static final JSONStreamAware UNSUPPORTED_PROTOCOL;
  
  static
  {
    Object localObject = new HashMap();
    ((Map)localObject).put("getCumulativeDifficulty", GetCumulativeDifficulty.instance);
    ((Map)localObject).put("getInfo", GetInfo.instance);
    ((Map)localObject).put("getMilestoneBlockIds", GetMilestoneBlockIds.instance);
    ((Map)localObject).put("getNextBlockIds", GetNextBlockIds.instance);
    ((Map)localObject).put("getNextBlocks", GetNextBlocks.instance);
    ((Map)localObject).put("getPeers", GetPeers.instance);
    ((Map)localObject).put("getUnconfirmedTransactions", GetUnconfirmedTransactions.instance);
    ((Map)localObject).put("processBlock", ProcessBlock.instance);
    ((Map)localObject).put("processTransactions", ProcessTransactions.instance);
    peerRequestHandlers = Collections.unmodifiableMap((Map)localObject);
    



    localObject = new JSONObject();
    ((JSONObject)localObject).put("error", "Unsupported request type!");
    UNSUPPORTED_REQUEST_TYPE = JSON.prepare((JSONObject)localObject);
    



    localObject = new JSONObject();
    ((JSONObject)localObject).put("error", "Unsupported protocol!");
    UNSUPPORTED_PROTOCOL = JSON.prepare((JSONObject)localObject);
  }
  
  protected void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws ServletException, IOException
  {
    PeerImpl localPeerImpl = null;
    Object localObject1;
    try
    {
      localPeerImpl = Peers.addPeer(paramHttpServletRequest.getRemoteHost(), null);
      if (localPeerImpl.isBlacklisted()) {
        return;
      }
      localObject2 = new CountingInputStream(paramHttpServletRequest.getInputStream());
      localObject3 = new BufferedReader(new InputStreamReader((InputStream)localObject2, "UTF-8"));Object localObject4 = null;
      JSONObject localJSONObject;
      try
      {
        localJSONObject = (JSONObject)JSONValue.parse((Reader)localObject3);
      }
      catch (Throwable localThrowable4)
      {
        localObject4 = localThrowable4;throw localThrowable4;
      }
      finally
      {
        if (localObject3 != null) {
          if (localObject4 != null) {
            try
            {
              ((Reader)localObject3).close();
            }
            catch (Throwable localThrowable5)
            {
              localObject4.addSuppressed(localThrowable5);
            }
          } else {
            ((Reader)localObject3).close();
          }
        }
      }
      if (localJSONObject == null) {
        return;
      }
      if (localPeerImpl.getState() == Peer.State.DISCONNECTED) {
        localPeerImpl.setState(Peer.State.CONNECTED);
      }
      localPeerImpl.updateDownloadedVolume(((CountingInputStream)localObject2).getCount());
      if (!localPeerImpl.analyzeHallmark(localPeerImpl.getPeerAddress(), (String)localJSONObject.get("hallmark")))
      {
        localPeerImpl.blacklist();
        return;
      }
      if ((localJSONObject.get("protocol") != null) && (((Number)localJSONObject.get("protocol")).intValue() == 1))
      {
        localObject3 = (PeerRequestHandler)peerRequestHandlers.get(localJSONObject.get("requestType"));
        if (localObject3 != null) {
          localObject1 = ((PeerRequestHandler)localObject3).processRequest(localJSONObject, localPeerImpl);
        } else {
          localObject1 = UNSUPPORTED_REQUEST_TYPE;
        }
      }
      else
      {
        Logger.logDebugMessage("Unsupported protocol " + localJSONObject.get("protocol"));
        localObject1 = UNSUPPORTED_PROTOCOL;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      Logger.logDebugMessage("Error processing POST request", localRuntimeException);
      localObject2 = new JSONObject();
      ((JSONObject)localObject2).put("error", localRuntimeException.toString());
      localObject1 = localObject2;
    }
    paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
    CountingOutputStream localCountingOutputStream = new CountingOutputStream(paramHttpServletResponse.getOutputStream());
    Object localObject2 = new BufferedWriter(new OutputStreamWriter(localCountingOutputStream, "UTF-8"));Object localObject3 = null;
    try
    {
      ((JSONStreamAware)localObject1).writeJSONString((Writer)localObject2);
    }
    catch (Throwable localThrowable2)
    {
      localObject3 = localThrowable2;throw localThrowable2;
    }
    finally
    {
      if (localObject2 != null) {
        if (localObject3 != null) {
          try
          {
            ((Writer)localObject2).close();
          }
          catch (Throwable localThrowable6)
          {
            ((Throwable)localObject3).addSuppressed(localThrowable6);
          }
        } else {
          ((Writer)localObject2).close();
        }
      }
    }
    if (localPeerImpl != null) {
      localPeerImpl.updateUploadedVolume(localCountingOutputStream.getCount());
    }
  }
  
  static abstract class PeerRequestHandler
  {
    abstract JSONStreamAware processRequest(JSONObject paramJSONObject, Peer paramPeer);
  }
}