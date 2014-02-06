package nxt.user;

import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class JSONResponses
{
  static final JSONStreamAware INVALID_SECRET_PHRASE;
  static final JSONStreamAware LOCK_ACCOUNT;
  static final JSONStreamAware LOCAL_USERS_ONLY;
  static final JSONStreamAware NOTIFY_OF_ACCEPTED_TRANSACTION;
  
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
  }
}