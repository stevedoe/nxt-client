package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Asset;
import nxt.Attachment.ColoredCoinsAssetIssuance;
import nxt.Genesis;
import nxt.Nxt;
import nxt.NxtException.ValidationException;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class IssueAsset
  extends APIServlet.APIRequestHandler
{
  static final IssueAsset instance = new IssueAsset();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
    throws NxtException.ValidationException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("name");
    String str3 = paramHttpServletRequest.getParameter("description");
    String str4 = paramHttpServletRequest.getParameter("quantity");
    String str5 = paramHttpServletRequest.getParameter("fee");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_NAME;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_QUANTITY;
    }
    if (str5 == null) {
      return JSONResponses.MISSING_FEE;
    }
    str2 = str2.trim();
    if ((str2.length() < 3) || (str2.length() > 10)) {
      return JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
    }
    String str6 = str2.toLowerCase();
    for (int i = 0; i < str6.length(); i++) {
      if ("0123456789abcdefghijklmnopqrstuvwxyz".indexOf(str6.charAt(i)) < 0) {
        return JSONResponses.INCORRECT_ASSET_NAME;
      }
    }
    if (Asset.getAsset(str6) != null) {
      return JSONResponses.ASSET_NAME_ALREADY_USED;
    }
    if ((str3 != null) && (str3.length() > 1000)) {
      return JSONResponses.INCORRECT_ASSET_DESCRIPTION;
    }
    try
    {
      i = Integer.parseInt(str4);
      if ((i <= 0) || (i > 1000000000L)) {
        return JSONResponses.INCORRECT_ASSET_QUANTITY;
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
      if (j < 1000) {
        return JSONResponses.INCORRECT_ASSET_ISSUANCE_FEE;
      }
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_FEE;
    }
    byte[] arrayOfByte = Crypto.getPublicKey(str1);
    Account localAccount = Account.getAccount(arrayOfByte);
    if ((localAccount == null) || (j * 100L > localAccount.getUnconfirmedBalance())) {
      return JSONResponses.NOT_ENOUGH_FUNDS;
    }
    int k = Convert.getEpochTime();
    Attachment.ColoredCoinsAssetIssuance localColoredCoinsAssetIssuance = new Attachment.ColoredCoinsAssetIssuance(str2, str3, i);
    Transaction localTransaction = Nxt.getTransactionProcessor().newTransaction(k, (short)1440, arrayOfByte, Genesis.CREATOR_ID, 0, j, null, localColoredCoinsAssetIssuance);
    
    localTransaction.sign(str1);
    
    Nxt.getTransactionProcessor().broadcast(localTransaction);
    
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("transaction", localTransaction.getStringId());
    return localJSONObject;
  }
}