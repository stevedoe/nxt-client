package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.util.Convert;
import nxt.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountBlockIds
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final GetAccountBlockIds instance = new GetAccountBlockIds();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
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
    JSONArray localJSONArray = new JSONArray();
    Object localObject1 = Blockchain.getAllBlocks(localAccount, i);Object localObject2 = null;
    try
    {
      while (((DbIterator)localObject1).hasNext())
      {
        Block localBlock = (Block)((DbIterator)localObject1).next();
        localJSONArray.add(localBlock.getStringId());
      }
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
            ((DbIterator)localObject1).close();
          }
          catch (Throwable localThrowable3)
          {
            localObject2.addSuppressed(localThrowable3);
          }
        } else {
          ((DbIterator)localObject1).close();
        }
      }
    }
    localObject1 = new JSONObject();
    ((JSONObject)localObject1).put("blockIds", localJSONArray);
    
    return localObject1;
  }
}