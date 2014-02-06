package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Blockchain;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class SendMoney
  extends UserRequestHandler
{
  static final SendMoney instance = new SendMoney();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws NxtException.ValidationException, IOException
  {
    if (paramUser.getSecretPhrase() == null) {
      return null;
    }
    String str1 = paramHttpServletRequest.getParameter("recipient");
    String str2 = paramHttpServletRequest.getParameter("amount");
    String str3 = paramHttpServletRequest.getParameter("fee");
    String str4 = paramHttpServletRequest.getParameter("deadline");
    String str5 = paramHttpServletRequest.getParameter("secretPhrase");
    

    int i = 0;
    int j = 0;
    short s = 0;
    Long localLong;
    try
    {
      localLong = Convert.parseUnsignedLong(str1);
      if (localLong == null) {
        throw new IllegalArgumentException("invalid recipient");
      }
      i = Integer.parseInt(str2.trim());
      j = Integer.parseInt(str3.trim());
      s = (short)(int)(Double.parseDouble(str4) * 60.0D);
    }
    catch (RuntimeException localRuntimeException)
    {
      localObject2 = new JSONObject();
      ((JSONObject)localObject2).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject2).put("message", "One of the fields is filled incorrectly!");
      ((JSONObject)localObject2).put("recipient", str1);
      ((JSONObject)localObject2).put("amount", str2);
      ((JSONObject)localObject2).put("fee", str3);
      ((JSONObject)localObject2).put("deadline", str4);
      
      return localObject2;
    }
    if (!paramUser.getSecretPhrase().equals(str5))
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject1).put("message", "Wrong secret phrase!");
      ((JSONObject)localObject1).put("recipient", str1);
      ((JSONObject)localObject1).put("amount", str2);
      ((JSONObject)localObject1).put("fee", str3);
      ((JSONObject)localObject1).put("deadline", str4);
      
      return localObject1;
    }
    if ((i <= 0) || (i > 1000000000L))
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject1).put("message", "\"Amount\" must be greater than 0!");
      ((JSONObject)localObject1).put("recipient", str1);
      ((JSONObject)localObject1).put("amount", str2);
      ((JSONObject)localObject1).put("fee", str3);
      ((JSONObject)localObject1).put("deadline", str4);
      
      return localObject1;
    }
    if ((j <= 0) || (j > 1000000000L))
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject1).put("message", "\"Fee\" must be greater than 0!");
      ((JSONObject)localObject1).put("recipient", str1);
      ((JSONObject)localObject1).put("amount", str2);
      ((JSONObject)localObject1).put("fee", str3);
      ((JSONObject)localObject1).put("deadline", str4);
      
      return localObject1;
    }
    if ((s < 1) || (s > 1440))
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject1).put("message", "\"Deadline\" must be greater or equal to 1 minute and less than 24 hours!");
      ((JSONObject)localObject1).put("recipient", str1);
      ((JSONObject)localObject1).put("amount", str2);
      ((JSONObject)localObject1).put("fee", str3);
      ((JSONObject)localObject1).put("deadline", str4);
      
      return localObject1;
    }
    Object localObject1 = Account.getAccount(paramUser.getPublicKey());
    if ((localObject1 == null) || ((i + j) * 100L > ((Account)localObject1).getUnconfirmedBalance()))
    {
      localObject2 = new JSONObject();
      ((JSONObject)localObject2).put("response", "notifyOfIncorrectTransaction");
      ((JSONObject)localObject2).put("message", "Not enough funds!");
      ((JSONObject)localObject2).put("recipient", str1);
      ((JSONObject)localObject2).put("amount", str2);
      ((JSONObject)localObject2).put("fee", str3);
      ((JSONObject)localObject2).put("deadline", str4);
      
      return localObject2;
    }
    Object localObject2 = Transaction.newTransaction(Convert.getEpochTime(), s, paramUser.getPublicKey(), localLong, i, j, null);
    ((Transaction)localObject2).sign(paramUser.getSecretPhrase());
    
    Blockchain.broadcast((Transaction)localObject2);
    
    return JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;
  }
}