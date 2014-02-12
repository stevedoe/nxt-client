package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Alias;
import nxt.Attachment.MessagingAliasAssignment;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class AssignAlias
  extends HttpRequestDispatcher.HttpRequestHandler
{
  static final AssignAlias instance = new AssignAlias();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("alias");
    String str3 = paramHttpServletRequest.getParameter("uri");
    String str4 = paramHttpServletRequest.getParameter("fee");
    String str5 = paramHttpServletRequest.getParameter("deadline");
    String str6 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_ALIAS;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_URI;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str5 == null) {
      return JSONResponses.MISSING_DEADLINE;
    }
    str2 = str2.trim();
    if ((str2.length() == 0) || (str2.length() > 100)) {
      return JSONResponses.INCORRECT_ALIAS_LENGTH;
    }
    String str7 = str2.toLowerCase();
    for (int i = 0; i < str7.length(); i++) {
      if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str7.charAt(i)) < 0) {
        return JSONResponses.INCORRECT_ALIAS;
      }
    }
    str3 = str3.trim();
    if (str3.length() > 1000) {
      return JSONResponses.INCORRECT_URI_LENGTH;
    }
    try
    {
      i = Integer.parseInt(str4);
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
      s = Short.parseShort(str5);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong = str6 == null ? null : Convert.parseUnsignedLong(str6);
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    Account localAccount = Account.getAccount(arrayOfByte);
    if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    Alias localAlias = Alias.getAlias(str7);
    JSONObject localJSONObject = new JSONObject();
    if ((localAlias != null) && (localAlias.getAccount() != localAccount))
    {
      localJSONObject.put("errorCode", Integer.valueOf(8));
      localJSONObject.put("errorDescription", "\"" + str2 + "\" is already used");
    }
    else
    {
      int j = Convert.getEpochTime();
      Attachment.MessagingAliasAssignment localMessagingAliasAssignment = new Attachment.MessagingAliasAssignment(str2, str3);
      Transaction localTransaction = Transaction.newTransaction(j, s, arrayOfByte, Genesis.CREATOR_ID, 0, i, localLong, localMessagingAliasAssignment);
      
      localTransaction.sign(str1);
      
      Blockchain.broadcast(localTransaction);
      
      localJSONObject.put("transaction", localTransaction.getStringId());
    }
    return localJSONObject;
  }
}