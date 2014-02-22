package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Attachment.ColoredCoinsBidOrderCancellation;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException.ValidationException;
import nxt.Order.Bid;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class CancelBidOrder
  extends APIServlet.APIRequestHandler
{
  static final CancelBidOrder instance = new CancelBidOrder();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("order");
    String str3 = paramHttpServletRequest.getParameter("fee");
    String str4 = paramHttpServletRequest.getParameter("deadline");
    String str5 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_ORDER;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_DEADLINE;
    }
    Long localLong1;
    try
    {
      localLong1 = Convert.parseUnsignedLong(str2);
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ORDER;
    }
    int i;
    try
    {
      i = Integer.parseInt(str3);
      if ((i <= 0) || (i >= 1000000000L)) {
        return JSONResponses.INCORRECT_FEE;
      }
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_FEE;
    }
    short s;
    try
    {
      s = Short.parseShort(str4);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong2 = str5 == null ? null : Convert.parseUnsignedLong(str5);
    
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    Long localLong3 = Account.getId(arrayOfByte);
    
    Order.Bid localBid = Order.Bid.getBidOrder(localLong1);
    if ((localBid == null) || (!localBid.getAccount().getId().equals(localLong3))) {
      return JSONResponses.UNKNOWN_ORDER;
    }
    Account localAccount = Account.getAccount(localLong3);
    if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    int j = Convert.getEpochTime();
    Attachment.ColoredCoinsBidOrderCancellation localColoredCoinsBidOrderCancellation = new Attachment.ColoredCoinsBidOrderCancellation(localLong1);
    Transaction localTransaction = Nxt.getTransactionProcessor().newTransaction(j, s, arrayOfByte, Genesis.CREATOR_ID, 0, i, localLong2, localColoredCoinsBidOrderCancellation);
    
    localTransaction.sign(str1);
    
    Nxt.getTransactionProcessor().broadcast(localTransaction);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("transaction", localTransaction.getStringId());
    return localJSONObject;
  }
}