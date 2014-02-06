package nxt.http;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class DecodeHallmark
  extends HttpRequestHandler
{
  static final DecodeHallmark instance = new DecodeHallmark();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("hallmark");
    if (str1 == null) {
      return JSONResponses.MISSING_HALLMARK;
    }
    try
    {
      byte[] arrayOfByte1 = Convert.convert(str1);
      
      ByteBuffer localByteBuffer = ByteBuffer.wrap(arrayOfByte1);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      
      byte[] arrayOfByte2 = new byte[32];
      localByteBuffer.get(arrayOfByte2);
      int i = localByteBuffer.getShort();
      if (i > 300) {
        throw new IllegalArgumentException("Invalid host length");
      }
      byte[] arrayOfByte3 = new byte[i];
      localByteBuffer.get(arrayOfByte3);
      String str2 = new String(arrayOfByte3, "UTF-8");
      int j = localByteBuffer.getInt();
      int k = localByteBuffer.getInt();
      localByteBuffer.get();
      byte[] arrayOfByte4 = new byte[64];
      localByteBuffer.get(arrayOfByte4);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("account", Convert.convert(Account.getId(arrayOfByte2)));
      localJSONObject.put("host", str2);
      localJSONObject.put("weight", Integer.valueOf(j));
      int m = k / 10000;
      int n = k % 10000 / 100;
      int i1 = k % 100;
      localJSONObject.put("date", (m < 1000 ? "0" : m < 100 ? "00" : m < 10 ? "000" : "") + m + "-" + (n < 10 ? "0" : "") + n + "-" + (i1 < 10 ? "0" : "") + i1);
      byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
      System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
      localJSONObject.put("valid", Boolean.valueOf((str2.length() > 100) || (j <= 0) || (j > 1000000000L) ? false : Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2)));
      return localJSONObject;
    }
    catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
    return JSONResponses.INCORRECT_HALLMARK;
  }
}