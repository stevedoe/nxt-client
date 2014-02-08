package nxt;

import java.io.UnsupportedEncodingException;
import nxt.crypto.Crypto;
import nxt.util.Convert;

public final class Token
{
  private final byte[] publicKey;
  private final int timestamp;
  private final boolean isValid;
  
  public static String generateToken(String paramString1, String paramString2)
  {
    try
    {
      byte[] arrayOfByte1 = paramString2.getBytes("UTF-8");
      byte[] arrayOfByte2 = new byte[arrayOfByte1.length + 32 + 4];
      System.arraycopy(arrayOfByte1, 0, arrayOfByte2, 0, arrayOfByte1.length);
      System.arraycopy(Crypto.getPublicKey(paramString1), 0, arrayOfByte2, arrayOfByte1.length, 32);
      int i = Convert.getEpochTime();
      arrayOfByte2[(arrayOfByte1.length + 32)] = ((byte)i);
      arrayOfByte2[(arrayOfByte1.length + 32 + 1)] = ((byte)(i >> 8));
      arrayOfByte2[(arrayOfByte1.length + 32 + 2)] = ((byte)(i >> 16));
      arrayOfByte2[(arrayOfByte1.length + 32 + 3)] = ((byte)(i >> 24));
      
      byte[] arrayOfByte3 = new byte[100];
      System.arraycopy(arrayOfByte2, arrayOfByte1.length, arrayOfByte3, 0, 36);
      System.arraycopy(Crypto.sign(arrayOfByte2, paramString1), 0, arrayOfByte3, 36, 64);
      
      StringBuilder localStringBuilder = new StringBuilder();
      for (int j = 0; j < 100; j += 5)
      {
        long l = arrayOfByte3[j] & 0xFF | (arrayOfByte3[(j + 1)] & 0xFF) << 8 | (arrayOfByte3[(j + 2)] & 0xFF) << 16 | (arrayOfByte3[(j + 3)] & 0xFF) << 24 | (arrayOfByte3[(j + 4)] & 0xFF) << 32;
        if (l < 32L) {
          localStringBuilder.append("0000000");
        } else if (l < 1024L) {
          localStringBuilder.append("000000");
        } else if (l < 32768L) {
          localStringBuilder.append("00000");
        } else if (l < 1048576L) {
          localStringBuilder.append("0000");
        } else if (l < 33554432L) {
          localStringBuilder.append("000");
        } else if (l < 1073741824L) {
          localStringBuilder.append("00");
        } else if (l < 34359738368L) {
          localStringBuilder.append("0");
        }
        localStringBuilder.append(Long.toString(l, 32));
      }
      return localStringBuilder.toString();
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
      throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
    }
  }
  
  public static Token parseToken(String paramString1, String paramString2)
  {
    try
    {
      byte[] arrayOfByte1 = paramString2.getBytes("UTF-8");
      byte[] arrayOfByte2 = new byte[100];
      int i = 0;
      for (int j = 0; i < paramString1.length(); j += 5)
      {
        long l = Long.parseLong(paramString1.substring(i, i + 8), 32);
        arrayOfByte2[j] = ((byte)(int)l);
        arrayOfByte2[(j + 1)] = ((byte)(int)(l >> 8));
        arrayOfByte2[(j + 2)] = ((byte)(int)(l >> 16));
        arrayOfByte2[(j + 3)] = ((byte)(int)(l >> 24));
        arrayOfByte2[(j + 4)] = ((byte)(int)(l >> 32));i += 8;
      }
      if (i != 160) {
        throw new IllegalArgumentException("Invalid token string: " + paramString1);
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
      
      return new Token(arrayOfByte3, k, bool);
    }
    catch (UnsupportedEncodingException localUnsupportedEncodingException)
    {
      throw new RuntimeException(localUnsupportedEncodingException.toString(), localUnsupportedEncodingException);
    }
  }
  
  private Token(byte[] paramArrayOfByte, int paramInt, boolean paramBoolean)
  {
    this.publicKey = paramArrayOfByte;
    this.timestamp = paramInt;
    this.isValid = paramBoolean;
  }
  
  public byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  public int getTimestamp()
  {
    return this.timestamp;
  }
  
  public boolean isValid()
  {
    return this.isValid;
  }
}