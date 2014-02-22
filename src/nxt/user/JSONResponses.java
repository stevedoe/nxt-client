package nxt.user;

import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class JSONResponses
{
  public static final JSONStreamAware INVALID_SECRET_PHRASE;
  public static final JSONStreamAware LOCK_ACCOUNT;
  public static final JSONStreamAware LOCAL_USERS_ONLY;
  public static final JSONStreamAware NOTIFY_OF_ACCEPTED_TRANSACTION;
  public static final JSONStreamAware DENY_ACCESS;
  public static final JSONStreamAware INCORRECT_REQUEST;
  
  static
  {
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("response", "showMessage");
    localJSONObject.put("message", "Invalid secret phrase!");
    INVALID_SECRET_PHRASE = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("response", "lockAccount");
    LOCK_ACCOUNT = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("response", "showMessage");
    localJSONObject.put("message", "This operation is allowed to local host users only!");
    LOCAL_USERS_ONLY = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("response", "notifyOfAcceptedTransaction");
    NOTIFY_OF_ACCEPTED_TRANSACTION = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("response", "denyAccess");
    DENY_ACCESS = JSON.prepare(localJSONObject);
    



    localJSONObject = new JSONObject();
    localJSONObject.put("response", "showMessage");
    localJSONObject.put("message", "Incorrect request!");
    INCORRECT_REQUEST = JSON.prepare(localJSONObject);
  }
}