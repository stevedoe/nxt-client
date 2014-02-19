package nxt.http;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Attachment.MessagingVoteCasting;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.NxtException;
import nxt.Poll;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class CastVote
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final CastVote instance = new CastVote();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException, IOException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("poll");
    String str3 = paramHttpServletRequest.getParameter("fee");
    String str4 = paramHttpServletRequest.getParameter("deadline");
    String str5 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_POLL;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_DEADLINE;
    }
    int i = 0;
    Poll localPoll;
    try
    {
      localPoll = Poll.getPoll(Convert.parseUnsignedLong(str2));
      if (localPoll != null) {
        i = localPoll.getOptions().length;
      }
    }
    catch (RuntimeException localRuntimeException1)
    {
      return JSONResponses.INCORRECT_POLL;
    }
    byte[] arrayOfByte1 = new byte[i];
    try
    {
      for (int j = 0; j < i; j++)
      {
        String str6 = paramHttpServletRequest.getParameter("vote" + j);
        if (str6 != null) {
          arrayOfByte1[j] = Byte.parseByte(str6);
        }
      }
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_VOTE;
    }
    int k;
    try
    {
      k = Integer.parseInt(str3);
      if ((k <= 0) || (k >= 1000000000L)) {
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
      s = Short.parseShort(str4);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException3)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong;
    try
    {
      localLong = str5 == null ? null : Convert.parseUnsignedLong(str5);
    }
    catch (RuntimeException localRuntimeException2)
    {
      return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
    }
    byte[] arrayOfByte2 = Crypto.getPublicKey(str1);
    
    Account localAccount = Account.getAccount(arrayOfByte2);
    if ((localAccount == null) || (k * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    int m = Convert.getEpochTime();
    
    Attachment.MessagingVoteCasting localMessagingVoteCasting = new Attachment.MessagingVoteCasting(localPoll.getId(), arrayOfByte1);
    Transaction localTransaction = Transaction.newTransaction(m, s, arrayOfByte2, Genesis.CREATOR_ID, 0, k, localLong, localMessagingVoteCasting);
    localTransaction.sign(str1);
    
    Blockchain.broadcast(localTransaction);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("transaction", localTransaction.getStringId());
    localJSONObject.put("bytes", Convert.toHexString(localTransaction.getBytes()));
    
    return localJSONObject;
  }
}