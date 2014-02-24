package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Nxt;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SendMoney
  extends APIServlet.APIRequestHandler
{
  static final SendMoney instance = new SendMoney();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("recipient");
    String str3 = paramHttpServletRequest.getParameter("amount");
    String str4 = paramHttpServletRequest.getParameter("fee");
    String str5 = paramHttpServletRequest.getParameter("deadline");
    String str6 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if ((str2 == null) || ("0".equals(str2))) {
      return JSONResponses.MISSING_RECIPIENT;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_AMOUNT;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str5 == null) {
      return JSONResponses.MISSING_DEADLINE;
    }
    Long localLong1;
    try
    {
      localLong1 = Convert.parseUnsignedLong(str2);
    }
    catch (RuntimeException localRuntimeException1)
    {
      return JSONResponses.INCORRECT_RECIPIENT;
    }
    int i;
    try
    {
      i = Integer.parseInt(str3);
      if ((i <= 0) || (i >= 1000000000L)) {
        return JSONResponses.INCORRECT_AMOUNT;
      }
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_AMOUNT;
    }
    int j;
    try
    {
      j = Integer.parseInt(str4);
      if ((j <= 0) || (j >= 1000000000L)) {
        return JSONResponses.INCORRECT_FEE;
      }
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_FEE;
    }
    short s;
    try
    {
      s = Short.parseShort(str5);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException3)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong2;
    try
    {
      localLong2 = str6 == null ? null : Convert.parseUnsignedLong(str6);
    }
    catch (RuntimeException localRuntimeException2)
    {
      return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
    }
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    
    Account localAccount = Account.getAccount(arrayOfByte);
    if ((localAccount == null) || ((i + j) * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    Transaction localTransaction = Nxt.getTransactionProcessor().newTransaction(Convert.getEpochTime(), s, arrayOfByte, localLong1, i, j, localLong2);
    
    localTransaction.sign(str1);
    
    Nxt.getTransactionProcessor().broadcast(localTransaction);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("transaction", localTransaction.getStringId());
    localJSONObject.put("bytes", Convert.toHexString(localTransaction.getBytes()));
    
    return localJSONObject;
  }
  
  boolean requirePost()
  {
    return true;
  }
}