package nxt.http;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAccountBlockIds
  extends HttpRequestHandler
{
  static final GetAccountBlockIds instance = new GetAccountBlockIds();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("account");
    String str2 = paramHttpServletRequest.getParameter("timestamp");
    if (str1 == null) {
      return JSONResponses.MISSING_ACCOUNT;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_TIMESTAMP;
    }
    Account localAccount;
    try
    {
      localAccount = Account.getAccount(Convert.parseUnsignedLong(str1));
      if (localAccount == null) {
        return JSONResponses.UNKNOWN_ACCOUNT;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    int i;
    try
    {
      i = Integer.parseInt(str2);
      if (i < 0) {
        return JSONResponses.INCORRECT_TIMESTAMP;
      }
    }
    catch (NumberFormatException localNumberFormatException)
    {
      return JSONResponses.INCORRECT_TIMESTAMP;
    }
    PriorityQueue localPriorityQueue = new PriorityQueue(11, Block.heightComparator);
    byte[] arrayOfByte = localAccount.getPublicKey();
    for (Object localObject1 = Blockchain.getAllBlocks().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Block)((Iterator)localObject1).next();
      if ((((Block)localObject2).getTimestamp() >= i) && (Arrays.equals(((Block)localObject2).getGeneratorPublicKey(), arrayOfByte))) {
        localPriorityQueue.offer(localObject2);
      }
    }
    localObject1 = new JSONArray();
    while (!localPriorityQueue.isEmpty()) {
      ((JSONArray)localObject1).add(((Block)localPriorityQueue.poll()).getStringId());
    }
    Object localObject2 = new JSONObject();
    ((JSONObject)localObject2).put("blockIds", localObject1);
    
    return localObject2;
  }
}