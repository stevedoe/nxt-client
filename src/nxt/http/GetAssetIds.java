package nxt.http;

import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Asset;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAssetIds
  extends HttpRequestHandler
{
  static final GetAssetIds instance = new GetAssetIds();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    JSONArray localJSONArray = new JSONArray();
    for (Object localObject = Asset.getAllAssets().iterator(); ((Iterator)localObject).hasNext();)
    {
      Asset localAsset = (Asset)((Iterator)localObject).next();
      localJSONArray.add(Convert.convert(localAsset.getId()));
    }
    localObject = new JSONObject();
    ((JSONObject)localObject).put("assetIds", localJSONArray);
    return localObject;
  }
}