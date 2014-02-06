package nxt.http;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.Transaction.Type;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetAccountTransactionIds
  extends HttpRequestHandler
{
  static final GetAccountTransactionIds instance = new GetAccountTransactionIds();
  
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
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_TIMESTAMP;
    }
    int j;
    try
    {
      j = Integer.parseInt(paramHttpServletRequest.getParameter("type"));
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      j = -1;
    }
    int k;
    try
    {
      k = Integer.parseInt(paramHttpServletRequest.getParameter("subtype"));
    }
    catch (NumberFormatException localNumberFormatException3)
    {
      k = -1;
    }
    PriorityQueue localPriorityQueue = new PriorityQueue(11, Transaction.timestampComparator);
    byte[] arrayOfByte = localAccount.getPublicKey();
    for (Object localObject1 = Blockchain.getAllTransactions().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (Transaction)((Iterator)localObject1).next();
      if (((((Transaction)localObject2).getRecipientId().equals(localAccount.getId())) || (Arrays.equals(((Transaction)localObject2).getSenderPublicKey(), arrayOfByte))) && ((j < 0) || (((Transaction)localObject2).getType().getType() == j)) && ((k < 0) || (((Transaction)localObject2).getType().getSubtype() == k)) && (((Transaction)localObject2).getBlock().getTimestamp() >= i)) {
        localPriorityQueue.offer(localObject2);
      }
    }
    localObject1 = new JSONArray();
    while (!localPriorityQueue.isEmpty()) {
      ((JSONArray)localObject1).add(((Transaction)localPriorityQueue.poll()).getStringId());
    }
    Object localObject2 = new JSONObject();
    ((JSONObject)localObject2).put("transactionIds", localObject1);
    return localObject2;
  }
}