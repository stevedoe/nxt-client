package nxt.http;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletRequest;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class MarkHost
  extends HttpRequestHandler
{
  static final MarkHost instance = new MarkHost();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest)
  {
    String str1 = paramHttpServletRequest.getParameter("secretPhrase");
    String str2 = paramHttpServletRequest.getParameter("host");
    String str3 = paramHttpServletRequest.getParameter("weight");
    String str4 = paramHttpServletRequest.getParameter("date");
    if (str1 == null) {
      return JSONResponses.MISSING_SECRET_PHRASE;
    }
    if (str2 == null) {
      return JSONResponses.MISSING_HOST;
    }
    if (str3 == null) {
      return JSONResponses.MISSING_WEIGHT;
    }
    if (str4 == null) {
      return JSONResponses.MISSING_DATE;
    }
    if (str2.length() > 100) {
      return JSONResponses.INCORRECT_HOST;
    }
    int i;
    try
    {
      i = Integer.parseInt(str3);
      if ((i <= 0) || (i > 1000000000L)) {
        return JSONResponses.INCORRECT_WEIGHT;
      }
    }
    catch (NumberFormatException localNumberFormatException1)
    {
      return JSONResponses.INCORRECT_WEIGHT;
    }
    int j;
    try
    {
      j = Integer.parseInt(str4.substring(0, 4)) * 10000 + Integer.parseInt(str4.substring(5, 7)) * 100 + Integer.parseInt(str4.substring(8, 10));
    }
    catch (NumberFormatException localNumberFormatException2)
    {
      return JSONResponses.INCORRECT_DATE;
    }
    try
    {
      byte[] arrayOfByte1 = Crypto.getPublicKey(str1);
      byte[] arrayOfByte2 = str2.getBytes("UTF-8");
      
      ByteBuffer localByteBuffer = ByteBuffer.allocate(34 + arrayOfByte2.length + 4 + 4 + 1);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.put(arrayOfByte1);
      localByteBuffer.putShort((short)arrayOfByte2.length);
      localByteBuffer.put(arrayOfByte2);
      localByteBuffer.putInt(i);
      localByteBuffer.putInt(j);
      
      byte[] arrayOfByte3 = localByteBuffer.array();
      byte[] arrayOfByte4;
      do
      {
        arrayOfByte3[(arrayOfByte3.length - 1)] = ((byte)ThreadLocalRandom.current().nextInt());
        arrayOfByte4 = Crypto.sign(arrayOfByte3, str1);
      } while (!Crypto.verify(arrayOfByte4, arrayOfByte3, arrayOfByte1));
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("hallmark", Convert.convert(arrayOfByte3) + Convert.convert(arrayOfByte4));
      return localJSONObject;
    }
    catch (RuntimeException|UnsupportedEncodingException localRuntimeException) {}
    return JSONResponses.INCORRECT_HOST;
  }
}