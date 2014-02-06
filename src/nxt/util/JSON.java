package nxt.util;

import java.io.IOException;
import java.io.Writer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class JSON
{
  public static final JSONStreamAware emptyJSON = prepare(new JSONObject());
  
  public static JSONStreamAware prepare(JSONObject paramJSONObject)
  {
    new JSONStreamAware()
    {
      private final char[] jsonChars = this.val$json.toJSONString().toCharArray();
      
      public void writeJSONString(Writer paramAnonymousWriter)
        throws IOException
      {
        paramAnonymousWriter.write(this.jsonChars);
      }
    };
  }
  
  public static JSONStreamAware prepareRequest(JSONObject paramJSONObject)
  {
    paramJSONObject.put("protocol", Integer.valueOf(1));
    return prepare(paramJSONObject);
  }
}