package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Genesis;
import nxt.Transaction.Type;
import nxt.Transaction.Type.ColoredCoins;
import nxt.Transaction.Type.Messaging;
import nxt.Transaction.Type.Payment;
import nxt.util.Convert;
import nxt.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetConstants
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetConstants instance = new GetConstants();
  private static final JSONStreamAware CONSTANTS;
  
  static
  {
    JSONObject localJSONObject1 = new JSONObject();
    localJSONObject1.put("genesisBlockId", Convert.convert(Genesis.GENESIS_BLOCK_ID));
    localJSONObject1.put("genesisAccountId", Convert.convert(Genesis.CREATOR_ID));
    localJSONObject1.put("maxBlockPayloadLength", Integer.valueOf(32640));
    localJSONObject1.put("maxArbitraryMessageLength", Integer.valueOf(1000));
    
    JSONArray localJSONArray1 = new JSONArray();
    JSONObject localJSONObject2 = new JSONObject();
    localJSONObject2.put("value", Byte.valueOf(Transaction.Type.Payment.ORDINARY.getType()));
    localJSONObject2.put("description", "Payment");
    JSONArray localJSONArray2 = new JSONArray();
    JSONObject localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Payment.ORDINARY.getSubtype()));
    localJSONObject3.put("description", "Ordinary payment");
    localJSONArray2.add(localJSONObject3);
    localJSONObject2.put("subtypes", localJSONArray2);
    localJSONArray1.add(localJSONObject2);
    localJSONObject2 = new JSONObject();
    localJSONObject2.put("value", Byte.valueOf(Transaction.Type.Messaging.ARBITRARY_MESSAGE.getType()));
    localJSONObject2.put("description", "Messaging");
    localJSONArray2 = new JSONArray();
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.ARBITRARY_MESSAGE.getSubtype()));
    localJSONObject3.put("description", "Arbitrary message");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.ALIAS_ASSIGNMENT.getSubtype()));
    localJSONObject3.put("description", "Alias assignment");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.POLL_CREATION.getSubtype()));
    localJSONObject3.put("description", "Poll creation");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.Messaging.VOTE_CASTING.getSubtype()));
    localJSONObject3.put("description", "Vote casting");
    localJSONArray2.add(localJSONObject3);
    localJSONObject2.put("subtypes", localJSONArray2);
    localJSONArray1.add(localJSONObject2);
    localJSONObject2 = new JSONObject();
    localJSONObject2.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_ISSUANCE.getType()));
    localJSONObject2.put("description", "Colored coins");
    localJSONArray2 = new JSONArray();
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_ISSUANCE.getSubtype()));
    localJSONObject3.put("description", "Asset issuance");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASSET_TRANSFER.getSubtype()));
    localJSONObject3.put("description", "Asset transfer");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASK_ORDER_PLACEMENT.getSubtype()));
    localJSONObject3.put("description", "Ask order placement");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.BID_ORDER_PLACEMENT.getSubtype()));
    localJSONObject3.put("description", "Bid order placement");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.ASK_ORDER_CANCELLATION.getSubtype()));
    localJSONObject3.put("description", "Ask order cancellation");
    localJSONArray2.add(localJSONObject3);
    localJSONObject3 = new JSONObject();
    localJSONObject3.put("value", Byte.valueOf(Transaction.Type.ColoredCoins.BID_ORDER_CANCELLATION.getSubtype()));
    localJSONObject3.put("description", "Bid order cancellation");
    localJSONArray2.add(localJSONObject3);
    localJSONObject2.put("subtypes", localJSONArray2);
    localJSONArray1.add(localJSONObject2);
    localJSONObject1.put("transactionTypes", localJSONArray1);
    
    JSONArray localJSONArray3 = new JSONArray();
    JSONObject localJSONObject4 = new JSONObject();
    localJSONObject4.put("value", Integer.valueOf(0));
    localJSONObject4.put("description", "Non-connected");
    localJSONArray3.add(localJSONObject4);
    localJSONObject4 = new JSONObject();
    localJSONObject4.put("value", Integer.valueOf(1));
    localJSONObject4.put("description", "Connected");
    localJSONArray3.add(localJSONObject4);
    localJSONObject4 = new JSONObject();
    localJSONObject4.put("value", Integer.valueOf(2));
    localJSONObject4.put("description", "Disconnected");
    localJSONArray3.add(localJSONObject4);
    localJSONObject1.put("peerStates", localJSONArray3);
    
    CONSTANTS = JSON.prepare(localJSONObject1);
  }
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    return CONSTANTS;
  }
}