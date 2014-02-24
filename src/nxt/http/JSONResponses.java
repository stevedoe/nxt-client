package nxt.http;

import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class JSONResponses
{
  public static final JSONStreamAware INCORRECT_ALIAS_LENGTH = incorrect("alias", "(length must be in [1..100] range)");
  public static final JSONStreamAware INCORRECT_ALIAS = incorrect("alias", "(must contain only digits and latin letters)");
  public static final JSONStreamAware INCORRECT_URI_LENGTH = incorrect("uri", "(length must be not longer than 1000 characters)");
  public static final JSONStreamAware MISSING_SECRET_PHRASE = missing("secretPhrase");
  public static final JSONStreamAware MISSING_ALIAS = missing("alias");
  public static final JSONStreamAware MISSING_URI = missing("uri");
  public static final JSONStreamAware MISSING_FEE = missing("fee");
  public static final JSONStreamAware MISSING_DEADLINE = missing("deadline");
  public static final JSONStreamAware INCORRECT_DEADLINE = incorrect("deadline");
  public static final JSONStreamAware INCORRECT_FEE = incorrect("fee");
  public static final JSONStreamAware MISSING_TRANSACTION_BYTES = missing("transactionBytes");
  public static final JSONStreamAware INCORRECT_TRANSACTION_BYTES = incorrect("transactionBytes");
  public static final JSONStreamAware MISSING_ORDER = missing("order");
  public static final JSONStreamAware INCORRECT_ORDER = incorrect("order");
  public static final JSONStreamAware UNKNOWN_ORDER = unknown("order");
  public static final JSONStreamAware MISSING_HALLMARK = missing("hallmark");
  public static final JSONStreamAware INCORRECT_HALLMARK = incorrect("hallmark");
  public static final JSONStreamAware MISSING_WEBSITE = missing("website");
  public static final JSONStreamAware INCORRECT_WEBSITE = incorrect("website");
  public static final JSONStreamAware MISSING_TOKEN = missing("token");
  public static final JSONStreamAware INCORRECT_TOKEN = incorrect("token");
  public static final JSONStreamAware MISSING_ACCOUNT = missing("account");
  public static final JSONStreamAware INCORRECT_ACCOUNT = incorrect("account");
  public static final JSONStreamAware MISSING_TIMESTAMP = missing("timestamp");
  public static final JSONStreamAware INCORRECT_TIMESTAMP = incorrect("timestamp");
  public static final JSONStreamAware UNKNOWN_ACCOUNT = unknown("account");
  public static final JSONStreamAware UNKNOWN_ALIAS = unknown("alias");
  public static final JSONStreamAware MISSING_ASSET = missing("asset");
  public static final JSONStreamAware UNKNOWN_ASSET = unknown("asset");
  public static final JSONStreamAware INCORRECT_ASSET = incorrect("asset");
  public static final JSONStreamAware MISSING_BLOCK = missing("block");
  public static final JSONStreamAware UNKNOWN_BLOCK = unknown("block");
  public static final JSONStreamAware INCORRECT_BLOCK = incorrect("block");
  public static final JSONStreamAware MISSING_NUMBER_OF_CONFIRMATIONS = missing("numberOfConfirmations");
  public static final JSONStreamAware INCORRECT_NUMBER_OF_CONFIRMATIONS = incorrect("numberOfConfirmations");
  public static final JSONStreamAware MISSING_PEER = missing("peer");
  public static final JSONStreamAware UNKNOWN_PEER = unknown("peer");
  public static final JSONStreamAware MISSING_TRANSACTION = missing("transaction");
  public static final JSONStreamAware UNKNOWN_TRANSACTION = unknown("transaction");
  public static final JSONStreamAware INCORRECT_TRANSACTION = incorrect("transaction");
  public static final JSONStreamAware INCORRECT_ASSET_ISSUANCE_FEE = incorrect("fee", "(must be not less than 1'000)");
  public static final JSONStreamAware INCORRECT_ASSET_DESCRIPTION = incorrect("description", "(length must be not longer than 1000 characters)");
  public static final JSONStreamAware INCORRECT_ASSET_NAME = incorrect("name", "(must contain only digits and latin letters)");
  public static final JSONStreamAware INCORRECT_ASSET_NAME_LENGTH = incorrect("name", "(length must be in [3..10] range)");
  public static final JSONStreamAware MISSING_NAME = missing("name");
  public static final JSONStreamAware MISSING_QUANTITY = missing("quantity");
  public static final JSONStreamAware INCORRECT_QUANTITY = incorrect("quantity");
  public static final JSONStreamAware INCORRECT_ASSET_QUANTITY = incorrect("quantity", "(must be in [1..1'000'000'000] range)");
  public static final JSONStreamAware MISSING_HOST = missing("host");
  public static final JSONStreamAware MISSING_DATE = missing("date");
  public static final JSONStreamAware MISSING_WEIGHT = missing("weight");
  public static final JSONStreamAware INCORRECT_HOST = incorrect("host", "(the length exceeds 100 chars limit)");
  public static final JSONStreamAware INCORRECT_WEIGHT = incorrect("weight");
  public static final JSONStreamAware INCORRECT_DATE = incorrect("date");
  public static final JSONStreamAware MISSING_PRICE = missing("price");
  public static final JSONStreamAware INCORRECT_PRICE = incorrect("price");
  public static final JSONStreamAware INCORRECT_REFERENCED_TRANSACTION = incorrect("referencedTransaction");
  public static final JSONStreamAware MISSING_MESSAGE = missing("message");
  public static final JSONStreamAware MISSING_RECIPIENT = missing("recipient");
  public static final JSONStreamAware INCORRECT_RECIPIENT = incorrect("recipient");
  public static final JSONStreamAware INCORRECT_ARBITRARY_MESSAGE = incorrect("message", "(length must be not longer than 1000 bytes)");
  public static final JSONStreamAware MISSING_AMOUNT = missing("amount");
  public static final JSONStreamAware INCORRECT_AMOUNT = incorrect("amount");
  public static final JSONStreamAware MISSING_DESCRIPTION = missing("description");
  public static final JSONStreamAware MISSING_MINNUMBEROFOPTIONS = missing("minNumberOfOptions");
  public static final JSONStreamAware MISSING_MAXNUMBEROFOPTIONS = missing("maxNumberOfOptions");
  public static final JSONStreamAware MISSING_OPTIONSAREBINARY = missing("optionsAreBinary");
  public static final JSONStreamAware MISSING_POLL = missing("poll");
  public static final JSONStreamAware INCORRECT_POLL_NAME_LENGTH = incorrect("name", "(length must be not longer than 100 characters)");
  public static final JSONStreamAware INCORRECT_POLL_DESCRIPTION_LENGTH = incorrect("description", "(length must be not longer than 1000 characters)");
  public static final JSONStreamAware INCORRECT_POLL_OPTION_LENGTH = incorrect("option", "(length must be not longer than 100 characters)");
  public static final JSONStreamAware INCORRECT_MINNUMBEROFOPTIONS = incorrect("minNumberOfOptions");
  public static final JSONStreamAware INCORRECT_MAXNUMBEROFOPTIONS = incorrect("maxNumberOfOptions");
  public static final JSONStreamAware INCORRECT_OPTIONSAREBINARY = incorrect("optionsAreBinary");
  public static final JSONStreamAware INCORRECT_POLL = incorrect("poll");
  public static final JSONStreamAware INCORRECT_VOTE = incorrect("vote");
  public static final JSONStreamAware UNKNOWN_POLL = unknown("poll");
  public static final JSONStreamAware NOT_ENOUGH_FUNDS;
  public static final JSONStreamAware ASSET_NAME_ALREADY_USED;
  public static final JSONStreamAware ERROR_NOT_ALLOWED;
  public static final JSONStreamAware ERROR_INCORRECT_REQUEST;
  public static final JSONStreamAware NOT_FORGING;
  public static final JSONStreamAware POST_REQUIRED;
  
  static
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(6));
    localJSONObject.put("errorDescription", "Not enough funds");
    NOT_ENOUGH_FUNDS = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(8));
    localJSONObject.put("errorDescription", "Asset name is already used");
    ASSET_NAME_ALREADY_USED = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(7));
    localJSONObject.put("errorDescription", "Not allowed");
    ERROR_NOT_ALLOWED = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(1));
    localJSONObject.put("errorDescription", "Incorrect request");
    ERROR_INCORRECT_REQUEST = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(5));
    localJSONObject.put("errorDescription", "Account is not forging");
    NOT_FORGING = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(1));
    localJSONObject.put("errorDescription", "This request is only accepted using POST!");
    POST_REQUIRED = JSON.prepare(localJSONObject);
  }
  
  private static JSONStreamAware missing(String paramString)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(3));
    localJSONObject.put("errorDescription", "\"" + paramString + "\"" + " not specified");
    return JSON.prepare(localJSONObject);
  }
  
  private static JSONStreamAware incorrect(String paramString)
  {
    return incorrect(paramString, null);
  }
  
  private static JSONStreamAware incorrect(String paramString1, String paramString2)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(4));
    localJSONObject.put("errorDescription", "Incorrect \"" + paramString1 + (paramString2 != null ? "\" " + paramString2 : "\""));
    return JSON.prepare(localJSONObject);
  }
  
  private static JSONStreamAware unknown(String paramString)
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("errorCode", Integer.valueOf(5));
    localJSONObject.put("errorDescription", "Unknown " + paramString);
    return JSON.prepare(localJSONObject);
  }
}