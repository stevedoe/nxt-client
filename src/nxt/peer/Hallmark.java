package nxt.peer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;
import nxt.Account;
import nxt.crypto.Crypto;
import nxt.util.Convert;

public final class Hallmark
{
  private final String hallmarkString;
  private final String host;
  private final int weight;
  private final int date;
  private final byte[] publicKey;
  private final Long accountId;
  private final byte[] signature;
  private final boolean isValid;
  
  public static int parseDate(String paramString)
  {
    return Integer.parseInt(paramString.substring(0, 4)) * 10000 + Integer.parseInt(paramString.substring(5, 7)) * 100 + Integer.parseInt(paramString.substring(8, 10));
  }
  
  public static String formatDate(int paramInt)
  {
    int i = paramInt / 10000;
    int j = paramInt % 10000 / 100;
    int k = paramInt % 100;
    return (i < 1000 ? "0" : i < 100 ? "00" : i < 10 ? "000" : "") + i + "-" + (j < 10 ? "0" : "") + j + "-" + (k < 10 ? "0" : "") + k;
  }
  
  public static String generateHallmark(String paramString1, String paramString2, int paramInt1, int paramInt2)
  {
    try
    {
      if ((paramString2.length() == 0) || (paramString2.length() > 100)) {
        throw new IllegalArgumentException("Hostname length should be between 1 and 100");
      }
      if ((paramInt1 <= 0) || (paramInt1 > 1000000000L)) {
        throw new IllegalArgumentException("Weight should be between 1 and 1000000000");
      }
      byte[] arrayOfByte1 = Crypto.getPublicKey(paramString1);
      byte[] arrayOfByte2 = paramString2.getBytes("UTF-8");
      
      ByteBuffer localByteBuffer = ByteBuffer.allocate(34 + arrayOfByte2.length + 4 + 4 + 1);
      localByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
      localByteBuffer.put(arrayOfByte1);
      localByteBuffer.putShort((short)arrayOfByte2.length);
      localByteBuffer.put(arrayOfByte2);
      localByteBuffer.putInt(paramInt1);
      localByteBuffer.putInt(paramInt2);
      
      byte[] arrayOfByte3 = localByteBuffer.array();
      byte[] arrayOfByte4;
      do
      {
        arrayOfByte3[(arrayOfByte3.length - 1)] = ((byte)ThreadLocalRandom.current().nextInt());
        arrayOfByte4 = Crypto.sign(arrayOfByte3, paramString1);
      } while (!Crypto.verify(arrayOfByte4, arrayOfByte3, arrayOfByte1));
      return Convert.toHexString(arrayOfByte3) + Convert.toHexString(arrayOfByte4);
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
      throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
    }
  }
  
  public static Hallmark parseHallmark(String paramString)
  {
    try
    {
      byte[] arrayOfByte1 = Convert.parseHexString(paramString);
      
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
      String str = new String(arrayOfByte3, "UTF-8");
      int j = localByteBuffer.getInt();
      int k = localByteBuffer.getInt();
      localByteBuffer.get();
      byte[] arrayOfByte4 = new byte[64];
      localByteBuffer.get(arrayOfByte4);
      
      byte[] arrayOfByte5 = new byte[arrayOfByte1.length - 64];
      System.arraycopy(arrayOfByte1, 0, arrayOfByte5, 0, arrayOfByte5.length);
      
      boolean bool = (str.length() < 100) && (j > 0) && (j <= 1000000000L) && (Crypto.verify(arrayOfByte4, arrayOfByte5, arrayOfByte2));
      
      return new Hallmark(paramString, arrayOfByte2, arrayOfByte4, str, j, k, bool);
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
      throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
    }
  }
  
  private Hallmark(String paramString1, byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, String paramString2, int paramInt1, int paramInt2, boolean paramBoolean)
  {
    this.hallmarkString = paramString1;
    this.host = paramString2;
    this.publicKey = paramArrayOfByte1;
    this.accountId = Account.getId(paramArrayOfByte1);
    this.signature = paramArrayOfByte2;
    this.weight = paramInt1;
    this.date = paramInt2;
    this.isValid = paramBoolean;
  }
  
  public String getHallmarkString()
  {
    return this.hallmarkString;
  }
  
  public String getHost()
  {
    return this.host;
  }
  
  public int getWeight()
  {
    return this.weight;
  }
  
  public int getDate()
  {
    return this.date;
  }
  
  public byte[] getSignature()
  {
    return this.signature;
  }
  
  public byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  public Long getAccountId()
  {
    return this.accountId;
  }
  
  public boolean isValid()
  {
    return this.isValid;
  }
}