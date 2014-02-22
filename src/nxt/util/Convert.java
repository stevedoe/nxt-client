package nxt.util;

import java.math.BigInteger;
import nxt.Nxt;

public final class Convert
{
  private static final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
  public static final BigInteger two64 = new BigInteger("18446744073709551616");
  
  public static byte[] parseHexString(String paramString)
  {
    byte[] arrayOfByte = new byte[paramString.length() / 2];
    for (int i = 0; i < arrayOfByte.length; i++)
    {
      int j = paramString.charAt(i * 2);
      j = j > 96 ? j - 87 : j - 48;
      int k = paramString.charAt(i * 2 + 1);
      k = k > 96 ? k - 87 : k - 48;
      if ((j < 0) || (k < 0) || (j > 15) || (k > 15)) {
        throw new NumberFormatException("Invalid hex number: " + paramString);
      }
      arrayOfByte[i] = ((byte)((j << 4) + k));
    }
    return arrayOfByte;
  }
  
  public static String toHexString(byte[] paramArrayOfByte)
  {
    char[] arrayOfChar = new char[paramArrayOfByte.length * 2];
    for (int i = 0; i < paramArrayOfByte.length; i++)
    {
      arrayOfChar[(i * 2)] = hexChars[(paramArrayOfByte[i] >> 4 & 0xF)];
      arrayOfChar[(i * 2 + 1)] = hexChars[(paramArrayOfByte[i] & 0xF)];
    }
    return String.valueOf(arrayOfChar);
  }
  
  public static String toUnsignedLong(long paramLong)
  {
    if (paramLong >= 0L) {
      return String.valueOf(paramLong);
    }
    BigInteger localBigInteger = BigInteger.valueOf(paramLong).add(two64);
    return localBigInteger.toString();
  }
  
  public static String toUnsignedLong(Long paramLong)
  {
    return toUnsignedLong(nullToZero(paramLong));
  }
  
  public static Long parseUnsignedLong(String paramString)
  {
    if (paramString == null) {
      throw new IllegalArgumentException("trying to parse null");
    }
    BigInteger localBigInteger = new BigInteger(paramString.trim());
    if ((localBigInteger.signum() < 0) || (localBigInteger.compareTo(two64) != -1)) {
      throw new IllegalArgumentException("overflow: " + paramString);
    }
    return zeroToNull(localBigInteger.longValue());
  }
  
  public static int getEpochTime()
  {
    return (int)((System.currentTimeMillis() - Nxt.EPOCH_BEGINNING + 500L) / 1000L);
  }
  
  public static Long zeroToNull(long paramLong)
  {
    return paramLong == 0L ? null : Long.valueOf(paramLong);
  }
  
  public static long nullToZero(Long paramLong)
  {
    return paramLong == null ? 0L : paramLong.longValue();
  }
  
  public static int nullToZero(Integer paramInteger)
  {
    return paramInteger == null ? 0 : paramInteger.intValue();
  }
  
  public static String truncate(String paramString1, String paramString2, int paramInt, boolean paramBoolean)
  {
    return paramString1.length() > paramInt ? paramString1.substring(0, paramBoolean ? paramInt - 3 : paramInt) + (paramBoolean ? "..." : "") : paramString1 == null ? paramString2 : paramString1;
  }
}