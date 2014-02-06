package nxt.http;

import java.io.UnsupportedEncodingException;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class DecodeToken
  extends HttpRequestHandler
{
  static final DecodeToken instance = new DecodeToken();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("website");
    String str2 = paramHttpServletRequest.getParameter("token");
    if (str1 == null) {
      return JSONResponses.MISSING_WEBSITE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_TOKEN;
    }
    try
    {
      byte[] arrayOfByte1 = str1.trim().getBytes("UTF-8");
      byte[] arrayOfByte2 = new byte[100];
      int i = 0;int j = 0;
      try
      {
        for (; i < str2.length(); j += 5)
        {
          long l = Long.parseLong(str2.substring(i, i + 8), 32);
          arrayOfByte2[j] = ((byte)(int)l);
          arrayOfByte2[(j + 1)] = ((byte)(int)(l >> 8));
          arrayOfByte2[(j + 2)] = ((byte)(int)(l >> 16));
          arrayOfByte2[(j + 3)] = ((byte)(int)(l >> 24));
          arrayOfByte2[(j + 4)] = ((byte)(int)(l >> 32));i += 8;
        }
      }
      catch (NumberFormatException localNumberFormatException)
      {
        return JSONResponses.INCORRECT_TOKEN;
      }
      if (i != 160) {
        return JSONResponses.INCORRECT_TOKEN;
      }
      byte[] arrayOfByte3 = new byte[32];
      System.arraycopy(arrayOfByte2, 0, arrayOfByte3, 0, 32);
      int k = arrayOfByte2[32] & 0xFF | (arrayOfByte2[33] & 0xFF) << 8 | (arrayOfByte2[34] & 0xFF) << 16 | (arrayOfByte2[35] & 0xFF) << 24;
      byte[] arrayOfByte4 = new byte[64];
      System.arraycopy(arrayOfByte2, 36, arrayOfByte4, 0, 64);
      
      byte[] arrayOfByte5 = new byte[arrayOfByte1.length + 36];
      System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte1.length);
      System.arraycopy(arrayOfByte2, 0, arrayOfByte5, arrayOfByte1.length, 36);
      boolean bool = Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte3);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("account", Convert.convert(Account.getId(arrayOfByte3)));
      localJSONObject.put("timestamp", Integer.valueOf(k));
      localJSONObject.put("valid", Boolean.valueOf(bool));
      
      return localJSONObject;
    }
    catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
    return JSONResponses.INCORRECT_WEBSITE;
  }
}