package nxt.http;

import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Generator;
import nxt.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetForging
  extends APIServlet.APIRequestHandler
{
  static final GetForging instance = new GetForging();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("secretPhrase");
    if (str == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    Account localAccount = Account.getAccount(Crypto.getPublicKey(str));
    if (localAccount == null) {
      return JSONResponses.UNKNOWN_ACCOUNT;
    }
    Generator localGenerator = Generator.getGenerator(str);
    if (localGenerator == null) {
      return JSONResponses.NOT_FORGING;
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("deadline", Long.valueOf(localGenerator.getDeadline()));
    return localJSONObject;
  }
}