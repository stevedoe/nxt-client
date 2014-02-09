package nxt.user;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.peer.Peer.State;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetInitialData
  extends UserRequestHandler
{
  static final GetInitialData instance = new GetInitialData();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    JSONArray localJSONArray1 = new JSONArray();
    JSONArray localJSONArray2 = new JSONArray();JSONArray localJSONArray3 = new JSONArray();JSONArray localJSONArray4 = new JSONArray();
    JSONArray localJSONArray5 = new JSONArray();
    for (Iterator localIterator = Blockchain.getAllUnconfirmedTransactions().iterator(); localIterator.hasNext();)
    {
      localObject1 = (Transaction)localIterator.next();
      
      localObject2 = new JSONObject();
      ((JSONObject)localObject2).put("index", Integer.valueOf(((Transaction)localObject1).getIndex()));
      ((JSONObject)localObject2).put("timestamp", Integer.valueOf(((Transaction)localObject1).getTimestamp()));
      ((JSONObject)localObject2).put("deadline", Short.valueOf(((Transaction)localObject1).getDeadline()));
      ((JSONObject)localObject2).put("recipient", Convert.convert(((Transaction)localObject1).getRecipientId()));
      ((JSONObject)localObject2).put("amount", Integer.valueOf(((Transaction)localObject1).getAmount()));
      ((JSONObject)localObject2).put("fee", Integer.valueOf(((Transaction)localObject1).getFee()));
      ((JSONObject)localObject2).put("sender", Convert.convert(((Transaction)localObject1).getSenderAccountId()));
      
      localJSONArray1.add(localObject2);
    }
    Object localObject2;
    for (localIterator = Peer.getAllPeers().iterator(); localIterator.hasNext();)
    {
      localObject1 = (Peer)localIterator.next();
      
      localObject2 = ((Peer)localObject1).getPeerAddress();
      if (((Peer)localObject1).isBlacklisted())
      {
        localObject3 = new JSONObject();
        ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
        ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), (String)localObject2, 25, true));
        if (((Peer)localObject1).isWellKnown()) {
          ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
        }
        localJSONArray4.add(localObject3);
      }
      else if (((Peer)localObject1).getState() == Peer.State.NON_CONNECTED)
      {
        if (((Peer)localObject1).getAnnouncedAddress() != null)
        {
          localObject3 = new JSONObject();
          ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
          ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), "", 25, true));
          if (((Peer)localObject1).isWellKnown()) {
            ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
          }
          localJSONArray3.add(localObject3);
        }
      }
      else
      {
        localObject3 = new JSONObject();
        ((JSONObject)localObject3).put("index", Integer.valueOf(((Peer)localObject1).getIndex()));
        if (((Peer)localObject1).getState() == Peer.State.DISCONNECTED) {
          ((JSONObject)localObject3).put("disconnected", Boolean.valueOf(true));
        }
        ((JSONObject)localObject3).put("address", Convert.truncate((String)localObject2, "", 25, true));
        ((JSONObject)localObject3).put("announcedAddress", Convert.truncate(((Peer)localObject1).getAnnouncedAddress(), "", 25, true));
        ((JSONObject)localObject3).put("weight", Integer.valueOf(((Peer)localObject1).getWeight()));
        ((JSONObject)localObject3).put("downloaded", Long.valueOf(((Peer)localObject1).getDownloadedVolume()));
        ((JSONObject)localObject3).put("uploaded", Long.valueOf(((Peer)localObject1).getUploadedVolume()));
        ((JSONObject)localObject3).put("software", ((Peer)localObject1).getSoftware());
        if (((Peer)localObject1).isWellKnown()) {
          ((JSONObject)localObject3).put("wellKnown", Boolean.valueOf(true));
        }
        localJSONArray2.add(localObject3);
      }
    }
    Object localObject3;
    int i = Blockchain.getLastBlock().getHeight();
    Object localObject1 = Blockchain.getBlocksFromHeight(Math.max(0, i - 59));
    for (int j = ((List)localObject1).size() - 1; j >= 0; j--)
    {
      localObject3 = (Block)((List)localObject1).get(j);
      JSONObject localJSONObject2 = new JSONObject();
      localJSONObject2.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
      localJSONObject2.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
      localJSONObject2.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
      localJSONObject2.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
      localJSONObject2.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
      localJSONObject2.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
      localJSONObject2.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
      localJSONObject2.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
      localJSONObject2.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
      localJSONObject2.put("block", ((Block)localObject3).getStringId());
      localJSONObject2.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
      

      localJSONArray5.add(localJSONObject2);
    }
    JSONObject localJSONObject1 = new JSONObject();
    localJSONObject1.put("response", "processInitialData");
    localJSONObject1.put("version", "0.7.2");
    if (localJSONArray1.size() > 0) {
      localJSONObject1.put("unconfirmedTransactions", localJSONArray1);
    }
    if (localJSONArray2.size() > 0) {
      localJSONObject1.put("activePeers", localJSONArray2);
    }
    if (localJSONArray3.size() > 0) {
      localJSONObject1.put("knownPeers", localJSONArray3);
    }
    if (localJSONArray4.size() > 0) {
      localJSONObject1.put("blacklistedPeers", localJSONArray4);
    }
    if (localJSONArray5.size() > 0) {
      localJSONObject1.put("recentBlocks", localJSONArray5);
    }
    return localJSONObject1;
  }
}