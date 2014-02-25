package nxt.http;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Attachment.MessagingPollCreation;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class CreatePoll
  extends APIServlet.APIRequestHandler
{
  static final CreatePoll instance = new CreatePoll();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("name");
    String str3 = paramHttpServletRequest.getParameter("description");
    String str4 = paramHttpServletRequest.getParameter("minNumberOfOptions");
    String str5 = paramHttpServletRequest.getParameter("maxNumberOfOptions");
    String str6 = paramHttpServletRequest.getParameter("optionsAreBinary");
    String str7 = paramHttpServletRequest.getParameter("fee");
    String str8 = paramHttpServletRequest.getParameter("deadline");
    String str9 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_NAME;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_DESCRIPTION;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_MINNUMBEROFOPTIONS;
    }
    if (str5 == null) {
      return JSONResponses.MISSING_MAXNUMBEROFOPTIONS;
    }
    if (str6 == null) {
      return JSONResponses.MISSING_OPTIONSAREBINARY;
    }
    if (str7 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str8 == null) {
      return JSONResponses.MISSING_DEADLINE;
    }
    if (str2.length() > 100) {
      return JSONResponses.INCORRECT_POLL_NAME_LENGTH;
    }
    if (str3.length() > 1000) {
      return JSONResponses.INCORRECT_POLL_DESCRIPTION_LENGTH;
    }
    ArrayList localArrayList = new ArrayList();
    while (localArrayList.size() < 100)
    {
      String str10 = paramHttpServletRequest.getParameter("option" + localArrayList.size());
      if (str10 == null) {
        break;
      }
      if (str10.length() > 100) {
        return JSONResponses.INCORRECT_POLL_OPTION_LENGTH;
      }
      localArrayList.add(str10.trim());
    }
    byte b1;
    try
    {
      b1 = Byte.parseByte(str4);
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_MINNUMBEROFOPTIONS;
    }
    byte b2;
    try
    {
      b2 = Byte.parseByte(str5);
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_MAXNUMBEROFOPTIONS;
    }
    boolean bool;
    try
    {
      bool = Boolean.parseBoolean(str6);
    }
    catch (NumberFormatException localNumberFormatException3)
    {
      return JSONResponses.INCORRECT_OPTIONSAREBINARY;
    }
    int i;
    try
    {
      i = Integer.parseInt(str7);
      if ((i <= 0) || (i >= 1000000000L)) {
        return JSONResponses.INCORRECT_FEE;
      }
    }
    catch (NumberFormatException localNumberFormatException4)
    {
      return JSONResponses.INCORRECT_FEE;
    }
    short s;
    try
    {
      s = Short.parseShort(str8);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException5)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong;
    try
    {
      localLong = str9 == null ? null : Convert.parseUnsignedLong(str9);
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
    }
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    
    Account localAccount = Account.getAccount(arrayOfByte);
    if ((localAccount == null) || (i * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    int j = Convert.getEpochTime();
    
    Attachment.MessagingPollCreation localMessagingPollCreation = new Attachment.MessagingPollCreation(str2.trim(), str3.trim(), (String[])localArrayList.toArray(new String[localArrayList.size()]), b1, b2, bool);
    Transaction localTransaction = Nxt.getTransactionProcessor().newTransaction(j, s, arrayOfByte, Genesis.CREATOR_ID, 0, i, localLong, localMessagingPollCreation);
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