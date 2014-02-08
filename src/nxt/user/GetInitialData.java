package nxt.user;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Block;
import nxt.Blockchain;
import nxt.Genesis;
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
    for (Object localObject1 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Transaction)((Iterator)localObject1).next();
      
      localObject3 = new JSONObject();
      ((JSONObject)localObject3).put("index", Integer.valueOf(((Transaction)localObject2).getIndex()));
      ((JSONObject)localObject3).put("timestamp", Integer.valueOf(((Transaction)localObject2).getTimestamp()));
      ((JSONObject)localObject3).put("deadline", Short.valueOf(((Transaction)localObject2).getDeadline()));
      ((JSONObject)localObject3).put("recipient", Convert.convert(((Transaction)localObject2).getRecipientId()));
      ((JSONObject)localObject3).put("amount", Integer.valueOf(((Transaction)localObject2).getAmount()));
      ((JSONObject)localObject3).put("fee", Integer.valueOf(((Transaction)localObject2).getFee()));
      ((JSONObject)localObject3).put("sender", Convert.convert(((Transaction)localObject2).getSenderAccountId()));
      
      localJSONArray1.add(localObject3);
    }
    Object localObject2;
    for (localObject1 = Peer.getAllPeers().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Peer)((Iterator)localObject1).next();
      
      localObject3 = ((Peer)localObject2).getPeerAddress();
      if (((Peer)localObject2).isBlacklisted())
      {
        localJSONObject = new JSONObject();
        localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
        localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), (String)localObject3, 25, true));
        if (((Peer)localObject2).isWellKnown()) {
          localJSONObject.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONArray4.add(localJSONObject);
      }
      else if (((Peer)localObject2).getState() == Peer.State.NON_CONNECTED)
      {
        if (((Peer)localObject2).getAnnouncedAddress() != null)
        {
          localJSONObject = new JSONObject();
          localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
          localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), "", 25, true));
          if (((Peer)localObject2).isWellKnown()) {
            localJSONObject.put("wellKnown", Boolean.valueOf(true));
          }
          localJSONArray3.add(localJSONObject);
        }
      }
      else
      {
        localJSONObject = new JSONObject();
        localJSONObject.put("index", Integer.valueOf(((Peer)localObject2).getIndex()));
        if (((Peer)localObject2).getState() == Peer.State.DISCONNECTED) {
          localJSONObject.put("disconnected", Boolean.valueOf(true));
        }
        localJSONObject.put("address", Convert.truncate((String)localObject3, "", 25, true));
        localJSONObject.put("announcedAddress", Convert.truncate(((Peer)localObject2).getAnnouncedAddress(), "", 25, true));
        localJSONObject.put("weight", Integer.valueOf(((Peer)localObject2).getWeight()));
        localJSONObject.put("downloaded", Long.valueOf(((Peer)localObject2).getDownloadedVolume()));
        localJSONObject.put("uploaded", Long.valueOf(((Peer)localObject2).getUploadedVolume()));
        localJSONObject.put("software", ((Peer)localObject2).getSoftware());
        if (((Peer)localObject2).isWellKnown()) {
          localJSONObject.put("wellKnown", Boolean.valueOf(true));
        }
        localJSONArray2.add(localJSONObject);
      }
    }
    JSONObject localJSONObject;
    localObject1 = Blockchain.getLastBlock().getId();
    int i = 0;
    while (i < 60)
    {
      i++;
      
      localObject3 = Blockchain.getBlock((Long)localObject1);
      localJSONObject = new JSONObject();
      localJSONObject.put("index", Integer.valueOf(((Block)localObject3).getIndex()));
      localJSONObject.put("timestamp", Integer.valueOf(((Block)localObject3).getTimestamp()));
      localJSONObject.put("numberOfTransactions", Integer.valueOf(((Block)localObject3).getTransactionIds().length));
      localJSONObject.put("totalAmount", Integer.valueOf(((Block)localObject3).getTotalAmount()));
      localJSONObject.put("totalFee", Integer.valueOf(((Block)localObject3).getTotalFee()));
      localJSONObject.put("payloadLength", Integer.valueOf(((Block)localObject3).getPayloadLength()));
      localJSONObject.put("generator", Convert.convert(((Block)localObject3).getGeneratorAccountId()));
      localJSONObject.put("height", Integer.valueOf(((Block)localObject3).getHeight()));
      localJSONObject.put("version", Integer.valueOf(((Block)localObject3).getVersion()));
      localJSONObject.put("block", ((Block)localObject3).getStringId());
      localJSONObject.put("baseTarget", BigInteger.valueOf(((Block)localObject3).getBaseTarget()).multiply(BigInteger.valueOf(100000L)).divide(BigInteger.valueOf(153722867L)));
      

      localJSONArray5.add(localJSONObject);
      if (((Long)localObject1).equals(Genesis.GENESIS_BLOCK_ID)) {
        break;
      }
      localObject1 = ((Block)localObject3).getPreviousBlockId();
    }
    Object localObject3 = new JSONObject();
    ((JSONObject)localObject3).put("response", "processInitialData");
    ((JSONObject)localObject3).put("version", "0.6.2");
    if (localJSONArray1.size() > 0) {
      ((JSONObject)localObject3).put("unconfirmedTransactions", localJSONArray1);
    }
    if (localJSONArray2.size() > 0) {
      ((JSONObject)localObject3).put("activePeers", localJSONArray2);
    }
    if (localJSONArray3.size() > 0) {
      ((JSONObject)localObject3).put("knownPeers", localJSONArray3);
    }
    if (localJSONArray4.size() > 0) {
      ((JSONObject)localObject3).put("blacklistedPeers", localJSONArray4);
    }
    if (localJSONArray5.size() > 0) {
      ((JSONObject)localObject3).put("recentBlocks", localJSONArray5);
    }
    return localObject3;
  }
}