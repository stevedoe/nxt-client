package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Attachment.ColoredCoinsAssetTransfer;
import nxt.Blockchain;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class TransferAsset
  extends HttpRequestHandler
{
  static final TransferAsset instance = new TransferAsset();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("recipient");
    String str3 = paramHttpServletRequest.getParameter("asset");
    String str4 = paramHttpServletRequest.getParameter("quantity");
    String str5 = paramHttpServletRequest.getParameter("fee");
    String str6 = paramHttpServletRequest.getParameter("deadline");
    String str7 = paramHttpServletRequest.getParameter("referencedTransaction");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if ((str2 == null) || ("0".equals(str2))) {
      return JSONResponses.MISSING_RECIPIENT;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_ASSET;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_QUANTITY;
    }
    if (str5 == null) {
      return JSONResponses.MISSING_FEE;
    }
    if (str6 == null) {
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
    Long localLong2;
    try
    {
      localLong2 = Convert.parseUnsignedLong(str3);
    }
    catch (RuntimeException localRuntimeException2)
    {
      return JSONResponses.INCORRECT_ASSET;
    }
    int i;
    try
    {
      i = Integer.parseInt(str4);
      if ((i <= 0) || (i >= 1000000000L)) {
        return JSONResponses.INCORRECT_QUANTITY;
      }
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_QUANTITY;
    }
    int j;
    try
    {
      j = Integer.parseInt(str5);
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
      s = Short.parseShort(str6);
      if ((s < 1) || (s > 1440)) {
        return JSONResponses.INCORRECT_DEADLINE;
      }
    }
    catch (NumberFormatException localNumberFormatException3)
    {
      return JSONResponses.INCORRECT_DEADLINE;
    }
    Long localLong3;
    try
    {
      localLong3 = str7 == null ? null : Convert.parseUnsignedLong(str7);
    }
    catch (RuntimeException localRuntimeException3)
    {
      return JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
    }
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    
    Account localAccount = Account.getAccount(arrayOfByte);
    if ((localAccount == null) || (j * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    Integer localInteger = localAccount.getUnconfirmedAssetBalance(localLong2);
    if ((localInteger == null) || (i > localInteger.intValue())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    int k = Convert.getEpochTime();
    
    Attachment.ColoredCoinsAssetTransfer localColoredCoinsAssetTransfer = new Attachment.ColoredCoinsAssetTransfer(localLong2, i);
    Transaction localTransaction = Transaction.newTransaction(k, s, arrayOfByte, localLong1, 0, j, localLong3, localColoredCoinsAssetTransfer);
    
    localTransaction.sign(str1);
    
    Blockchain.broadcast(localTransaction);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("transaction", localTransaction.getStringId());
    return localJSONObject;
  }
}