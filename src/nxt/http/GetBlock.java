package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBlock
  extends HttpRequestHandler
{
  static final GetBlock instance = new GetBlock();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("block");
    if (str == null) {
      return JSONResponses.MISSING_BLOCK;
    }
    Block localBlock;
    try
    {
      localBlock = Blockchain.getBlock(Convert.parseUnsignedLong(str));
      if (localBlock == null) {
        return JSONResponses.UNKNOWN_BLOCK;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_BLOCK;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("height", Integer.valueOf(localBlock.getHeight()));
    localJSONObject.put("generator", Convert.convert(localBlock.getGeneratorAccountId()));
    localJSONObject.put("timestamp", Integer.valueOf(localBlock.getTimestamp()));
    localJSONObject.put("numberOfTransactions", Integer.valueOf(localBlock.getTransactionIds().length));
    localJSONObject.put("totalAmount", Integer.valueOf(localBlock.getTotalAmount()));
    localJSONObject.put("totalFee", Integer.valueOf(localBlock.getTotalFee()));
    localJSONObject.put("payloadLength", Integer.valueOf(localBlock.getPayloadLength()));
    localJSONObject.put("version", Integer.valueOf(localBlock.getVersion()));
    localJSONObject.put("baseTarget", Convert.convert(localBlock.getBaseTarget()));
    if (localBlock.getPreviousBlockId() != null) {
      localJSONObject.put("previousBlock", Convert.convert(localBlock.getPreviousBlockId()));
    }
    if (localBlock.getNextBlockId() != null) {
      localJSONObject.put("nextBlock", Convert.convert(localBlock.getNextBlockId()));
    }
    localJSONObject.put("payloadHash", Convert.convert(localBlock.getPayloadHash()));
    localJSONObject.put("generationSignature", Convert.convert(localBlock.getGenerationSignature()));
    if (localBlock.getVersion() > 1) {
      localJSONObject.put("previousBlockHash", Convert.convert(localBlock.getPreviousBlockHash()));
    }
    localJSONObject.put("blockSignature", Convert.convert(localBlock.getBlockSignature()));
    JSONArray localJSONArray = new JSONArray();
    for (Long localLong : localBlock.getTransactionIds()) {
      localJSONArray.add(Convert.convert(localLong));
    }
    localJSONObject.put("transactions", localJSONArray);
    
    return localJSONObject;
  }
}