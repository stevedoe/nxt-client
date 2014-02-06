package nxt.user;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GenerateAuthorizationToken
  extends UserRequestHandler
{
  static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    if (!paramUser.getSecretPhrase().equals(str1)) {
      return JSONResponses.INVALID_SECRET_PHRASE;
    }
    byte[] arrayOfByte1 = paramHttpServletRequest.getParameter("website").trim().getBytes("UTF-8");
    byte[] arrayOfByte2 = new byte[arrayOfByte1.length + 32 + 4];
    System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte1.length);
    System.arraycopy(paramUser.getPublicKey(), 0, arrayOfByte2, arrayOfByte1.length, 32);
    int i = Convert.getEpochTime();
    arrayOfByte2[(arrayOfByte1.length + 32)] = ((byte)i);
    arrayOfByte2[(arrayOfByte1.length + 32 + 1)] = ((byte)(i >> 8));
    arrayOfByte2[(arrayOfByte1.length + 32 + 2)] = ((byte)(i >> 16));
    arrayOfByte2[(arrayOfByte1.length + 32 + 3)] = ((byte)(i >> 24));
    
    byte[] arrayOfByte3 = new byte[100];
    System.arraycopy(arrayOfByte2, arrayOfByte1.length, arrayOfByte3, 0, 36);
    System.arraycopy(Crypto.sign(arrayOfByte2, paramUser.getSecretPhrase()), 0, arrayOfByte3, 36, 64);
    String str2 = "";
    for (int j = 0; j < 100; j += 5)
    {
      long l = arrayOfByte3[j] & 0xFF | (arrayOfByte3[(j + 1)] & 0xFF) << 8 | (arrayOfByte3[(j + 2)] & 0xFF) << 16 | (arrayOfByte3[(j + 3)] & 0xFF) << 24 | (arrayOfByte3[(j + 4)] & 0xFF) << 32;
      if (l < 32L) {
        str2 = str2 + "0000000";
      } else if (l < 1024L) {
        str2 = str2 + "000000";
      } else if (l < 32768L) {
        str2 = str2 + "00000";
      } else if (l < 1048576L) {
        str2 = str2 + "0000";
      } else if (l < 33554432L) {
        str2 = str2 + "000";
      } else if (l < 1073741824L) {
        str2 = str2 + "00";
      } else if (l < 34359738368L) {
        str2 = str2 + "0";
      }
      str2 = str2 + Long.toString(l, 32);
    }
    JSONObject localJSONObject = new JSONObject();
    localJSONObject.put("response", "showAuthorizationToken");
    localJSONObject.put("token", str2);
    
    return localJSONObject;
  }
}