package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Alias;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class ListAccountAliases
  extends APIServlet.APIRequestHandler
{
  static final ListAccountAliases instance = new ListAccountAliases();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str = paramHttpServletRequest.getParameter("account");
    if (str == null) {
      return JSONResponses.MISSING_ACCOUNT;
    }
    Account localAccount;
    try
    {
      Long localLong = Convert.parseUnsignedLong(str);
      localAccount = Account.getAccount(localLong);
      if (localAccount == null) {
        return JSONResponses.UNKNOWN_ACCOUNT;
      }
    }
    catch (RuntimeException localRuntimeException)
    {
      return JSONResponses.INCORRECT_ACCOUNT;
    }
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Alias.getAllAliases().iterator(); ((Iterator)localObject).hasNext();)
    {
      Alias localAlias = (Alias)((Iterator)localObject).next();
      if (localAlias.getAccount().equals(localAccount))
      {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("alias", localAlias.getAliasName());
        localJSONObject.put("uri", localAlias.getURI());
        localJSONArray.add(localJSONObject);
      }
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("aliases", localJSONArray);
    
    return localObject;
  }
}