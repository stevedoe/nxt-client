package nxt.peer;

import java.math.BigInteger;
import nxt.Block;
import nxt.Blockchain;
import nxt.Nxt;
import org.json.simple.JSONObject;

final class GetInfo
  extends HttpJSONRequestHandler
{
  static final GetInfo instance = new GetInfo();
  
  JSONObject processJSONRequest(JSONObject paramJSONObject, Peer paramPeer)
  {
    JSONObject localJSONObject = new JSONObject();
    if (paramPeer != null)
    {
      String str1 = (String)paramJSONObject.get("announcedAddress");
      if (str1 != null)
      {
        str1 = str1.trim();
        if (str1.length() > 0) {
          paramPeer.setAnnouncedAddress(str1);
        }
      }
      String str2 = (String)paramJSONObject.get("application");
      if (str2 == null) {
        str2 = "?";
      }
      paramPeer.setApplication(str2.trim());
      
      String str3 = (String)paramJSONObject.get("version");
      if (str3 == null) {
        str3 = "?";
      }
      paramPeer.setVersion(str3.trim());
      
      String str4 = (String)paramJSONObject.get("platform");
      if (str4 == null) {
        str4 = "?";
      }
      paramPeer.setPlatform(str4.trim());
      
      paramPeer.setShareAddress(Boolean.TRUE.equals(paramJSONObject.get("shareAddress")));
    }
    if ((Nxt.myHallmark != null) && (Nxt.myHallmark.length() > 0)) {
      localJSONObject.put("hallmark", Nxt.myHallmark);
    }
    localJSONObject.put("application", "NRS");
    localJSONObject.put("version", "0.7.3");
    localJSONObject.put("platform", Nxt.myPlatform);
    localJSONObject.put("shareAddress", Boolean.valueOf(Nxt.shareMyAddress));
    
    localJSONObject.put("cumulativeDifficulty", Blockchain.getLastBlock().getCumulativeDifficulty().toString());
    
    return localJSONObject;
  }
}