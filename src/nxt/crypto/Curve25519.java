package nxt.crypto;

final class Curve25519
{
  public static final int KEY_SIZE = 32;
  public static final byte[] ZERO = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  public static final byte[] PRIME = { -19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 127 };
  public static final byte[] ORDER = { -19, -45, -11, 92, 26, 99, 18, 88, -42, -100, -9, -94, -34, -7, -34, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16 };
  private static final int P25 = 33554431;
  private static final int P26 = 67108863;
  
  public static void clamp(byte[] paramArrayOfByte)
  {
    paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] & 0x7F));
    paramArrayOfByte[31] = ((byte)(paramArrayOfByte[31] | 0x40)); int 
      tmp22_21 = 0;paramArrayOfByte[tmp22_21] = ((byte)(paramArrayOfByte[tmp22_21] & 0xF8));
  }
  
  public static void keygen(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
  {
    clamp(paramArrayOfByte3);
    core(paramArrayOfByte1, paramArrayOfByte2, paramArrayOfByte3, null);
  }
  
  public static void curve(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3)
  {
    core(paramArrayOfByte1, null, paramArrayOfByte2, paramArrayOfByte3);
  }
  
  public static boolean sign(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
  {
    byte[] arrayOfByte1 = new byte[65];
    byte[] arrayOfByte2 = new byte[33];
    for (int j = 0; j < 32; j++) {
      paramArrayOfByte1[j] = 0;
    }
    j = mula_small(paramArrayOfByte1, paramArrayOfByte3, 0, paramArrayOfByte2, 32, -1);
    mula_small(paramArrayOfByte1, paramArrayOfByte1, 0, ORDER, 32, (15 - paramArrayOfByte1[31]) / 16);
    mula32(arrayOfByte1, paramArrayOfByte1, paramArrayOfByte4, 32, 1);
    divmod(arrayOfByte2, arrayOfByte1, 64, ORDER, 32);
    int i = 0;
    for (j = 0; j < 32; j++) {
      i |= (paramArrayOfByte1[j] = arrayOfByte1[j]);
    }
    return i != 0;
  }
  
  public static void verify(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
  {
    byte[] arrayOfByte = new byte[32];
    
    long10[] arrayOflong101 = { new long10(), new long10() };
    long10[] arrayOflong102 = { new long10(), new long10() };
    long10[] arrayOflong103 = { new long10(), new long10(), new long10() };
    long10[] arrayOflong104 = { new long10(), new long10(), new long10() };
    long10[] arrayOflong105 = { new long10(), new long10(), new long10() };
    long10[] arrayOflong106 = { new long10(), new long10(), new long10() };
    
    int i = 0;int j = 0;int k = 0;int m = 0;
    


    set(arrayOflong101[0], 9);
    unpack(arrayOflong101[1], paramArrayOfByte4);
    





    x_to_y2(arrayOflong105[0], arrayOflong106[0], arrayOflong101[1]);
    sqrt(arrayOflong105[0], arrayOflong106[0]);
    int i1 = is_negative(arrayOflong105[0]);
    arrayOflong106[0]._0 += 39420360L;
    mul(arrayOflong106[1], BASE_2Y, arrayOflong105[0]);
    sub(arrayOflong105[i1], arrayOflong106[0], arrayOflong106[1]);
    add(arrayOflong105[(1 - i1)], arrayOflong106[0], arrayOflong106[1]);
    cpy(arrayOflong106[0], arrayOflong101[1]);
    arrayOflong106[0]._0 -= 9L;
    sqr(arrayOflong106[1], arrayOflong106[0]);
    recip(arrayOflong106[0], arrayOflong106[1], 0);
    mul(arrayOflong102[0], arrayOflong105[0], arrayOflong106[0]);
    sub(arrayOflong102[0], arrayOflong102[0], arrayOflong101[1]);
    arrayOflong102[0]._0 -= 486671L;
    mul(arrayOflong102[1], arrayOflong105[1], arrayOflong106[0]);
    sub(arrayOflong102[1], arrayOflong102[1], arrayOflong101[1]);
    arrayOflong102[1]._0 -= 486671L;
    mul_small(arrayOflong102[0], arrayOflong102[0], 1L);
    mul_small(arrayOflong102[1], arrayOflong102[1], 1L);
    for (int n = 0; n < 32; n++)
    {
      i = i >> 8 ^ paramArrayOfByte2[n] & 0xFF ^ (paramArrayOfByte2[n] & 0xFF) << 1;
      j = j >> 8 ^ paramArrayOfByte3[n] & 0xFF ^ (paramArrayOfByte3[n] & 0xFF) << 1;
      m = i ^ j ^ 0xFFFFFFFF;
      k = m & (k & 0x80) >> 7 ^ i;
      k ^= m & (k & 0x1) << 1;
      k ^= m & (k & 0x2) << 1;
      k ^= m & (k & 0x4) << 1;
      k ^= m & (k & 0x8) << 1;
      k ^= m & (k & 0x10) << 1;
      k ^= m & (k & 0x20) << 1;
      k ^= m & (k & 0x40) << 1;
      arrayOfByte[n] = ((byte)k);
    }
    k = (m & (k & 0x80) << 1 ^ i) >> 8;
    

    set(arrayOflong103[0], 1);
    cpy(arrayOflong103[1], arrayOflong101[k]);
    cpy(arrayOflong103[2], arrayOflong102[0]);
    set(arrayOflong104[0], 0);
    set(arrayOflong104[1], 1);
    set(arrayOflong104[2], 1);
    






    i = 0;
    j = 0;
    for (n = 32; n-- != 0;)
    {
      i = i << 8 | paramArrayOfByte2[n] & 0xFF;
      j = j << 8 | paramArrayOfByte3[n] & 0xFF;
      k = k << 8 | arrayOfByte[n] & 0xFF;
      for (i1 = 8; i1-- != 0;)
      {
        mont_prep(arrayOflong105[0], arrayOflong106[0], arrayOflong103[0], arrayOflong104[0]);
        mont_prep(arrayOflong105[1], arrayOflong106[1], arrayOflong103[1], arrayOflong104[1]);
        mont_prep(arrayOflong105[2], arrayOflong106[2], arrayOflong103[2], arrayOflong104[2]);
        
        i2 = ((i ^ i >> 1) >> i1 & 0x1) + ((j ^ j >> 1) >> i1 & 0x1);
        
        mont_dbl(arrayOflong103[2], arrayOflong104[2], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[0], arrayOflong104[0]);
        
        i2 = k >> i1 & 0x2 ^ (k >> i1 & 0x1) << 1;
        mont_add(arrayOflong105[1], arrayOflong106[1], arrayOflong105[i2], arrayOflong106[i2], arrayOflong103[1], arrayOflong104[1], arrayOflong101[(k >> i1 & 0x1)]);
        

        mont_add(arrayOflong105[2], arrayOflong106[2], arrayOflong105[0], arrayOflong106[0], arrayOflong103[2], arrayOflong104[2], arrayOflong102[(((i ^ j) >> i1 & 0x2) >> 1)]);
      }
    }
    int i2 = (i & 0x1) + (j & 0x1);
    recip(arrayOflong105[0], arrayOflong104[i2], 0);
    mul(arrayOflong105[1], arrayOflong103[i2], arrayOflong105[0]);
    
    pack(arrayOflong105[1], paramArrayOfByte1);
  }
  
  private static final class long10
  {
    public long _0;
    public long _1;
    public long _2;
    public long _3;
    public long _4;
    public long _5;
    public long _6;
    public long _7;
    public long _8;
    public long _9;
    
    public long10() {}
    
    public long10(long paramLong1, long paramLong2, long paramLong3, long paramLong4, long paramLong5, long paramLong6, long paramLong7, long paramLong8, long paramLong9, long paramLong10)
    {
      this._0 = paramLong1;this._1 = paramLong2;this._2 = paramLong3;
      this._3 = paramLong4;this._4 = paramLong5;this._5 = paramLong6;
      this._6 = paramLong7;this._7 = paramLong8;this._8 = paramLong9;
      this._9 = paramLong10;
    }
  }
  
  private static void cpy32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2)
  {
    for (int i = 0; i < 32; i++) {
      paramArrayOfByte1[i] = paramArrayOfByte2[i];
    }
  }
  
  private static int mula_small(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2, int paramInt3)
  {
    int i = 0;
    for (int j = 0; j < paramInt2; j++)
    {
      i += (paramArrayOfByte2[(j + paramInt1)] & 0xFF) + paramInt3 * (paramArrayOfByte3[j] & 0xFF);
      paramArrayOfByte1[(j + paramInt1)] = ((byte)i);
      i >>= 8;
    }
    return i;
  }
  
  private static int mula32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, int paramInt1, int paramInt2)
  {
    int i = 0;
    for (int j = 0; j < paramInt1; j++)
    {
      int k = paramInt2 * (paramArrayOfByte3[j] & 0xFF);
      i += mula_small(paramArrayOfByte1, paramArrayOfByte1, j, paramArrayOfByte2, 31, k) + (paramArrayOfByte1[(j + 31)] & 0xFF) + k * (paramArrayOfByte2[31] & 0xFF);
      
      paramArrayOfByte1[(j + 31)] = ((byte)i);
      i >>= 8;
    }
    paramArrayOfByte1[(j + 31)] = ((byte)(i + (paramArrayOfByte1[(j + 31)] & 0xFF)));
    return i >> 8;
  }
  
  private static void divmod(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, int paramInt1, byte[] paramArrayOfByte3, int paramInt2)
  {
    int i = 0;
    int j = (paramArrayOfByte3[(paramInt2 - 1)] & 0xFF) << 8;
    if (paramInt2 > 1) {
      j |= paramArrayOfByte3[(paramInt2 - 2)] & 0xFF;
    }
    while (paramInt1-- >= paramInt2)
    {
      int k = i << 16 | (paramArrayOfByte2[paramInt1] & 0xFF) << 8;
      if (paramInt1 > 0) {
        k |= paramArrayOfByte2[(paramInt1 - 1)] & 0xFF;
      }
      k /= j;
      i += mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -k);
      paramArrayOfByte1[(paramInt1 - paramInt2 + 1)] = ((byte)(k + i & 0xFF));
      mula_small(paramArrayOfByte2, paramArrayOfByte2, paramInt1 - paramInt2 + 1, paramArrayOfByte3, paramInt2, -i);
      i = paramArrayOfByte2[paramInt1] & 0xFF;
      paramArrayOfByte2[paramInt1] = 0;
    }
    paramArrayOfByte2[(paramInt2 - 1)] = ((byte)i);
  }
  
  private static int numsize(byte[] paramArrayOfByte, int paramInt)
  {
    while ((paramInt-- != 0) && (paramArrayOfByte[paramInt] == 0)) {}
    return paramInt + 1;
  }
  
  private static byte[] egcd32(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
  {
    int j = 32;
    for (int m = 0; m < 32; m++)
    {
      int tmp21_20 = 0;paramArrayOfByte2[m] = tmp21_20;paramArrayOfByte1[m] = tmp21_20;
    }
    paramArrayOfByte1[0] = 1;
    int i = numsize(paramArrayOfByte3, 32);
    if (i == 0) {
      return paramArrayOfByte2;
    }
    byte[] arrayOfByte = new byte[32];
    for (;;)
    {
      int k = j - i + 1;
      divmod(arrayOfByte, paramArrayOfByte4, j, paramArrayOfByte3, i);
      j = numsize(paramArrayOfByte4, j);
      if (j == 0) {
        return paramArrayOfByte1;
      }
      mula32(paramArrayOfByte2, paramArrayOfByte1, arrayOfByte, k, -1);
      
      k = i - j + 1;
      divmod(arrayOfByte, paramArrayOfByte3, i, paramArrayOfByte4, j);
      i = numsize(paramArrayOfByte3, i);
      if (i == 0) {
        return paramArrayOfByte2;
      }
      mula32(paramArrayOfByte1, paramArrayOfByte2, arrayOfByte, k, -1);
    }
  }
  
  private static void unpack(long10 paramlong10, byte[] paramArrayOfByte)
  {
    paramlong10._0 = (paramArrayOfByte[0] & 0xFF | (paramArrayOfByte[1] & 0xFF) << 8 | (paramArrayOfByte[2] & 0xFF) << 16 | (paramArrayOfByte[3] & 0xFF & 0x3) << 24);
    
    paramlong10._1 = ((paramArrayOfByte[3] & 0xFF & 0xFFFFFFFC) >> 2 | (paramArrayOfByte[4] & 0xFF) << 6 | (paramArrayOfByte[5] & 0xFF) << 14 | (paramArrayOfByte[6] & 0xFF & 0x7) << 22);
    
    paramlong10._2 = ((paramArrayOfByte[6] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[7] & 0xFF) << 5 | (paramArrayOfByte[8] & 0xFF) << 13 | (paramArrayOfByte[9] & 0xFF & 0x1F) << 21);
    
    paramlong10._3 = ((paramArrayOfByte[9] & 0xFF & 0xFFFFFFE0) >> 5 | (paramArrayOfByte[10] & 0xFF) << 3 | (paramArrayOfByte[11] & 0xFF) << 11 | (paramArrayOfByte[12] & 0xFF & 0x3F) << 19);
    
    paramlong10._4 = ((paramArrayOfByte[12] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[13] & 0xFF) << 2 | (paramArrayOfByte[14] & 0xFF) << 10 | (paramArrayOfByte[15] & 0xFF) << 18);
    
    paramlong10._5 = (paramArrayOfByte[16] & 0xFF | (paramArrayOfByte[17] & 0xFF) << 8 | (paramArrayOfByte[18] & 0xFF) << 16 | (paramArrayOfByte[19] & 0xFF & 0x1) << 24);
    
    paramlong10._6 = ((paramArrayOfByte[19] & 0xFF & 0xFFFFFFFE) >> 1 | (paramArrayOfByte[20] & 0xFF) << 7 | (paramArrayOfByte[21] & 0xFF) << 15 | (paramArrayOfByte[22] & 0xFF & 0x7) << 23);
    
    paramlong10._7 = ((paramArrayOfByte[22] & 0xFF & 0xFFFFFFF8) >> 3 | (paramArrayOfByte[23] & 0xFF) << 5 | (paramArrayOfByte[24] & 0xFF) << 13 | (paramArrayOfByte[25] & 0xFF & 0xF) << 21);
    
    paramlong10._8 = ((paramArrayOfByte[25] & 0xFF & 0xFFFFFFF0) >> 4 | (paramArrayOfByte[26] & 0xFF) << 4 | (paramArrayOfByte[27] & 0xFF) << 12 | (paramArrayOfByte[28] & 0xFF & 0x3F) << 20);
    
    paramlong10._9 = ((paramArrayOfByte[28] & 0xFF & 0xFFFFFFC0) >> 6 | (paramArrayOfByte[29] & 0xFF) << 2 | (paramArrayOfByte[30] & 0xFF) << 10 | (paramArrayOfByte[31] & 0xFF) << 18);
  }
  
  private static boolean is_overflow(long10 paramlong10)
  {
    return ((paramlong10._0 > 67108844L) && ((paramlong10._1 & paramlong10._3 & paramlong10._5 & paramlong10._7 & paramlong10._9) == 33554431L) && ((paramlong10._2 & paramlong10._4 & paramlong10._6 & paramlong10._8) == 67108863L)) || (paramlong10._9 > 33554431L);
  }
  
  private static void pack(long10 paramlong10, byte[] paramArrayOfByte)
  {
    int i = 0;int j = 0;
    
    i = (is_overflow(paramlong10) ? 1 : 0) - (paramlong10._9 < 0L ? 1 : 0);
    j = i * -33554432;
    i *= 19;
    long l = i + paramlong10._0 + (paramlong10._1 << 26);
    paramArrayOfByte[0] = ((byte)(int)l);
    paramArrayOfByte[1] = ((byte)(int)(l >> 8));
    paramArrayOfByte[2] = ((byte)(int)(l >> 16));
    paramArrayOfByte[3] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._2 << 19);
    paramArrayOfByte[4] = ((byte)(int)l);
    paramArrayOfByte[5] = ((byte)(int)(l >> 8));
    paramArrayOfByte[6] = ((byte)(int)(l >> 16));
    paramArrayOfByte[7] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._3 << 13);
    paramArrayOfByte[8] = ((byte)(int)l);
    paramArrayOfByte[9] = ((byte)(int)(l >> 8));
    paramArrayOfByte[10] = ((byte)(int)(l >> 16));
    paramArrayOfByte[11] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._4 << 6);
    paramArrayOfByte[12] = ((byte)(int)l);
    paramArrayOfByte[13] = ((byte)(int)(l >> 8));
    paramArrayOfByte[14] = ((byte)(int)(l >> 16));
    paramArrayOfByte[15] = ((byte)(int)(l >> 24));
    l = (l >> 32) + paramlong10._5 + (paramlong10._6 << 25);
    paramArrayOfByte[16] = ((byte)(int)l);
    paramArrayOfByte[17] = ((byte)(int)(l >> 8));
    paramArrayOfByte[18] = ((byte)(int)(l >> 16));
    paramArrayOfByte[19] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._7 << 19);
    paramArrayOfByte[20] = ((byte)(int)l);
    paramArrayOfByte[21] = ((byte)(int)(l >> 8));
    paramArrayOfByte[22] = ((byte)(int)(l >> 16));
    paramArrayOfByte[23] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._8 << 12);
    paramArrayOfByte[24] = ((byte)(int)l);
    paramArrayOfByte[25] = ((byte)(int)(l >> 8));
    paramArrayOfByte[26] = ((byte)(int)(l >> 16));
    paramArrayOfByte[27] = ((byte)(int)(l >> 24));
    l = (l >> 32) + (paramlong10._9 + j << 6);
    paramArrayOfByte[28] = ((byte)(int)l);
    paramArrayOfByte[29] = ((byte)(int)(l >> 8));
    paramArrayOfByte[30] = ((byte)(int)(l >> 16));
    paramArrayOfByte[31] = ((byte)(int)(l >> 24));
  }
  
  private static void cpy(long10 paramlong101, long10 paramlong102)
  {
    paramlong101._0 = paramlong102._0;paramlong101._1 = paramlong102._1;
    paramlong101._2 = paramlong102._2;paramlong101._3 = paramlong102._3;
    paramlong101._4 = paramlong102._4;paramlong101._5 = paramlong102._5;
    paramlong101._6 = paramlong102._6;paramlong101._7 = paramlong102._7;
    paramlong101._8 = paramlong102._8;paramlong101._9 = paramlong102._9;
  }
  
  private static void set(long10 paramlong10, int paramInt)
  {
    paramlong10._0 = paramInt;paramlong10._1 = 0L;
    paramlong10._2 = 0L;paramlong10._3 = 0L;
    paramlong10._4 = 0L;paramlong10._5 = 0L;
    paramlong10._6 = 0L;paramlong10._7 = 0L;
    paramlong10._8 = 0L;paramlong10._9 = 0L;
  }
  
  private static void add(long10 paramlong101, long10 paramlong102, long10 paramlong103)
  {
    paramlong102._0 += paramlong103._0;paramlong102._1 += paramlong103._1;
    paramlong102._2 += paramlong103._2;paramlong102._3 += paramlong103._3;
    paramlong102._4 += paramlong103._4;paramlong102._5 += paramlong103._5;
    paramlong102._6 += paramlong103._6;paramlong102._7 += paramlong103._7;
    paramlong102._8 += paramlong103._8;paramlong102._9 += paramlong103._9;
  }
  
  private static void sub(long10 paramlong101, long10 paramlong102, long10 paramlong103)
  {
    paramlong102._0 -= paramlong103._0;paramlong102._1 -= paramlong103._1;
    paramlong102._2 -= paramlong103._2;paramlong102._3 -= paramlong103._3;
    paramlong102._4 -= paramlong103._4;paramlong102._5 -= paramlong103._5;
    paramlong102._6 -= paramlong103._6;paramlong102._7 -= paramlong103._7;
    paramlong102._8 -= paramlong103._8;paramlong102._9 -= paramlong103._9;
  }
  
  private static long10 mul_small(long10 paramlong101, long10 paramlong102, long paramLong)
  {
    long l = paramlong102._8 * paramLong;
    paramlong101._8 = (l & 0x3FFFFFF);
    l = (l >> 26) + paramlong102._9 * paramLong;
    paramlong101._9 = (l & 0x1FFFFFF);
    l = 19L * (l >> 25) + paramlong102._0 * paramLong;
    paramlong101._0 = (l & 0x3FFFFFF);
    l = (l >> 26) + paramlong102._1 * paramLong;
    paramlong101._1 = (l & 0x1FFFFFF);
    l = (l >> 25) + paramlong102._2 * paramLong;
    paramlong101._2 = (l & 0x3FFFFFF);
    l = (l >> 26) + paramlong102._3 * paramLong;
    paramlong101._3 = (l & 0x1FFFFFF);
    l = (l >> 25) + paramlong102._4 * paramLong;
    paramlong101._4 = (l & 0x3FFFFFF);
    l = (l >> 26) + paramlong102._5 * paramLong;
    paramlong101._5 = (l & 0x1FFFFFF);
    l = (l >> 25) + paramlong102._6 * paramLong;
    paramlong101._6 = (l & 0x3FFFFFF);
    l = (l >> 26) + paramlong102._7 * paramLong;
    paramlong101._7 = (l & 0x1FFFFFF);
    l = (l >> 25) + paramlong101._8;
    paramlong101._8 = (l & 0x3FFFFFF);
    paramlong101._9 += (l >> 26);
    return paramlong101;
  }
  
  private static long10 mul(long10 paramlong101, long10 paramlong102, long10 paramlong103)
  {
    long l1 = paramlong102._0;long l2 = paramlong102._1;long l3 = paramlong102._2;long l4 = paramlong102._3;long l5 = paramlong102._4;
    long l6 = paramlong102._5;long l7 = paramlong102._6;long l8 = paramlong102._7;long l9 = paramlong102._8;long l10 = paramlong102._9;
    
    long l11 = paramlong103._0;long l12 = paramlong103._1;long l13 = paramlong103._2;long l14 = paramlong103._3;long l15 = paramlong103._4;
    long l16 = paramlong103._5;long l17 = paramlong103._6;long l18 = paramlong103._7;long l19 = paramlong103._8;long l20 = paramlong103._9;
    
    long l21 = l1 * l19 + l3 * l17 + l5 * l15 + l7 * l13 + l9 * l11 + 2L * (l2 * l18 + l4 * l16 + l6 * l14 + l8 * l12) + 38L * (l10 * l20);
    


    paramlong101._8 = (l21 & 0x3FFFFFF);
    l21 = (l21 >> 26) + l1 * l20 + l2 * l19 + l3 * l18 + l4 * l17 + l5 * l16 + l6 * l15 + l7 * l14 + l8 * l13 + l9 * l12 + l10 * l11;
    


    paramlong101._9 = (l21 & 0x1FFFFFF);
    l21 = l1 * l11 + 19L * ((l21 >> 25) + l3 * l19 + l5 * l17 + l7 * l15 + l9 * l13) + 38L * (l2 * l20 + l4 * l18 + l6 * l16 + l8 * l14 + l10 * l12);
    


    paramlong101._0 = (l21 & 0x3FFFFFF);
    l21 = (l21 >> 26) + l1 * l12 + l2 * l11 + 19L * (l3 * l20 + l4 * l19 + l5 * l18 + l6 * l17 + l7 * l16 + l8 * l15 + l9 * l14 + l10 * l13);
    


    paramlong101._1 = (l21 & 0x1FFFFFF);
    l21 = (l21 >> 25) + l1 * l13 + l3 * l11 + 19L * (l5 * l19 + l7 * l17 + l9 * l15) + 2L * (l2 * l12) + 38L * (l4 * l20 + l6 * l18 + l8 * l16 + l10 * l14);
    


    paramlong101._2 = (l21 & 0x3FFFFFF);
    l21 = (l21 >> 26) + l1 * l14 + l2 * l13 + l3 * l12 + l4 * l11 + 19L * (l5 * l20 + l6 * l19 + l7 * l18 + l8 * l17 + l9 * l16 + l10 * l15);
    


    paramlong101._3 = (l21 & 0x1FFFFFF);
    l21 = (l21 >> 25) + l1 * l15 + l3 * l13 + l5 * l11 + 19L * (l7 * l19 + l9 * l17) + 2L * (l2 * l14 + l4 * l12) + 38L * (l6 * l20 + l8 * l18 + l10 * l16);
    


    paramlong101._4 = (l21 & 0x3FFFFFF);
    l21 = (l21 >> 26) + l1 * l16 + l2 * l15 + l3 * l14 + l4 * l13 + l5 * l12 + l6 * l11 + 19L * (l7 * l20 + l8 * l19 + l9 * l18 + l10 * l17);
    


    paramlong101._5 = (l21 & 0x1FFFFFF);
    l21 = (l21 >> 25) + l1 * l17 + l3 * l15 + l5 * l13 + l7 * l11 + 19L * (l9 * l19) + 2L * (l2 * l16 + l4 * l14 + l6 * l12) + 38L * (l8 * l20 + l10 * l18);
    


    paramlong101._6 = (l21 & 0x3FFFFFF);
    l21 = (l21 >> 26) + l1 * l18 + l2 * l17 + l3 * l16 + l4 * l15 + l5 * l14 + l6 * l13 + l7 * l12 + l8 * l11 + 19L * (l9 * l20 + l10 * l19);
    


    paramlong101._7 = (l21 & 0x1FFFFFF);
    l21 = (l21 >> 25) + paramlong101._8;
    paramlong101._8 = (l21 & 0x3FFFFFF);
    paramlong101._9 += (l21 >> 26);
    return paramlong101;
  }
  
  private static long10 sqr(long10 paramlong101, long10 paramlong102)
  {
    long l1 = paramlong102._0;long l2 = paramlong102._1;long l3 = paramlong102._2;long l4 = paramlong102._3;long l5 = paramlong102._4;
    long l6 = paramlong102._5;long l7 = paramlong102._6;long l8 = paramlong102._7;long l9 = paramlong102._8;long l10 = paramlong102._9;
    
    long l11 = l5 * l5 + 2L * (l1 * l9 + l3 * l7) + 38L * (l10 * l10) + 4L * (l2 * l8 + l4 * l6);
    
    paramlong101._8 = (l11 & 0x3FFFFFF);
    l11 = (l11 >> 26) + 2L * (l1 * l10 + l2 * l9 + l3 * l8 + l4 * l7 + l5 * l6);
    
    paramlong101._9 = (l11 & 0x1FFFFFF);
    l11 = 19L * (l11 >> 25) + l1 * l1 + 38L * (l3 * l9 + l5 * l7 + l6 * l6) + 76L * (l2 * l10 + l4 * l8);
    

    paramlong101._0 = (l11 & 0x3FFFFFF);
    l11 = (l11 >> 26) + 2L * (l1 * l2) + 38L * (l3 * l10 + l4 * l9 + l5 * l8 + l6 * l7);
    
    paramlong101._1 = (l11 & 0x1FFFFFF);
    l11 = (l11 >> 25) + 19L * (l7 * l7) + 2L * (l1 * l3 + l2 * l2) + 38L * (l5 * l9) + 76L * (l4 * l10 + l6 * l8);
    

    paramlong101._2 = (l11 & 0x3FFFFFF);
    l11 = (l11 >> 26) + 2L * (l1 * l4 + l2 * l3) + 38L * (l5 * l10 + l6 * l9 + l7 * l8);
    
    paramlong101._3 = (l11 & 0x1FFFFFF);
    l11 = (l11 >> 25) + l3 * l3 + 2L * (l1 * l5) + 38L * (l7 * l9 + l8 * l8) + 4L * (l2 * l4) + 76L * (l6 * l10);
    

    paramlong101._4 = (l11 & 0x3FFFFFF);
    l11 = (l11 >> 26) + 2L * (l1 * l6 + l2 * l5 + l3 * l4) + 38L * (l7 * l10 + l8 * l9);
    
    paramlong101._5 = (l11 & 0x1FFFFFF);
    l11 = (l11 >> 25) + 19L * (l9 * l9) + 2L * (l1 * l7 + l3 * l5 + l4 * l4) + 4L * (l2 * l6) + 76L * (l8 * l10);
    

    paramlong101._6 = (l11 & 0x3FFFFFF);
    l11 = (l11 >> 26) + 2L * (l1 * l8 + l2 * l7 + l3 * l6 + l4 * l5) + 38L * (l9 * l10);
    
    paramlong101._7 = (l11 & 0x1FFFFFF);
    l11 = (l11 >> 25) + paramlong101._8;
    paramlong101._8 = (l11 & 0x3FFFFFF);
    paramlong101._9 += (l11 >> 26);
    return paramlong101;
  }
  
  private static void recip(long10 paramlong101, long10 paramlong102, int paramInt)
  {
    long10 locallong101 = new long10();
    long10 locallong102 = new long10();
    long10 locallong103 = new long10();
    long10 locallong104 = new long10();
    long10 locallong105 = new long10();
    

    sqr(locallong102, paramlong102);
    sqr(locallong103, locallong102);
    sqr(locallong101, locallong103);
    mul(locallong103, locallong101, paramlong102);
    mul(locallong101, locallong103, locallong102);
    sqr(locallong102, locallong101);
    mul(locallong104, locallong102, locallong103);
    
    sqr(locallong102, locallong104);
    sqr(locallong103, locallong102);
    sqr(locallong102, locallong103);
    sqr(locallong103, locallong102);
    sqr(locallong102, locallong103);
    mul(locallong103, locallong102, locallong104);
    sqr(locallong102, locallong103);
    sqr(locallong104, locallong102);
    for (int i = 1; i < 5; i++)
    {
      sqr(locallong102, locallong104);
      sqr(locallong104, locallong102);
    }
    mul(locallong102, locallong104, locallong103);
    sqr(locallong104, locallong102);
    sqr(locallong105, locallong104);
    for (i = 1; i < 10; i++)
    {
      sqr(locallong104, locallong105);
      sqr(locallong105, locallong104);
    }
    mul(locallong104, locallong105, locallong102);
    for (i = 0; i < 5; i++)
    {
      sqr(locallong102, locallong104);
      sqr(locallong104, locallong102);
    }
    mul(locallong102, locallong104, locallong103);
    sqr(locallong103, locallong102);
    sqr(locallong104, locallong103);
    for (i = 1; i < 25; i++)
    {
      sqr(locallong103, locallong104);
      sqr(locallong104, locallong103);
    }
    mul(locallong103, locallong104, locallong102);
    sqr(locallong104, locallong103);
    sqr(locallong105, locallong104);
    for (i = 1; i < 50; i++)
    {
      sqr(locallong104, locallong105);
      sqr(locallong105, locallong104);
    }
    mul(locallong104, locallong105, locallong103);
    for (i = 0; i < 25; i++)
    {
      sqr(locallong105, locallong104);
      sqr(locallong104, locallong105);
    }
    mul(locallong103, locallong104, locallong102);
    sqr(locallong102, locallong103);
    sqr(locallong103, locallong102);
    if (paramInt != 0)
    {
      mul(paramlong101, paramlong102, locallong103);
    }
    else
    {
      sqr(locallong102, locallong103);
      sqr(locallong103, locallong102);
      sqr(locallong102, locallong103);
      mul(paramlong101, locallong102, locallong101);
    }
  }
  
  private static int is_negative(long10 paramlong10)
  {
    return (int)(((is_overflow(paramlong10)) || (paramlong10._9 < 0L) ? 1 : 0) ^ paramlong10._0 & 1L);
  }
  
  private static void sqrt(long10 paramlong101, long10 paramlong102)
  {
    long10 locallong101 = new long10();long10 locallong102 = new long10();long10 locallong103 = new long10();
    add(locallong102, paramlong102, paramlong102);
    recip(locallong101, locallong102, 1);
    sqr(paramlong101, locallong101);
    mul(locallong103, locallong102, paramlong101);
    locallong103._0 -= 1L;
    mul(locallong102, locallong101, locallong103);
    mul(paramlong101, paramlong102, locallong102);
  }
  
  private static void mont_prep(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104)
  {
    add(paramlong101, paramlong103, paramlong104);
    sub(paramlong102, paramlong103, paramlong104);
  }
  
  private static void mont_add(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104, long10 paramlong105, long10 paramlong106, long10 paramlong107)
  {
    mul(paramlong105, paramlong102, paramlong103);
    mul(paramlong106, paramlong101, paramlong104);
    add(paramlong101, paramlong105, paramlong106);
    sub(paramlong102, paramlong105, paramlong106);
    sqr(paramlong105, paramlong101);
    sqr(paramlong101, paramlong102);
    mul(paramlong106, paramlong101, paramlong107);
  }
  
  private static void mont_dbl(long10 paramlong101, long10 paramlong102, long10 paramlong103, long10 paramlong104, long10 paramlong105, long10 paramlong106)
  {
    sqr(paramlong101, paramlong103);
    sqr(paramlong102, paramlong104);
    mul(paramlong105, paramlong101, paramlong102);
    sub(paramlong102, paramlong101, paramlong102);
    mul_small(paramlong106, paramlong102, 121665L);
    add(paramlong101, paramlong101, paramlong106);
    mul(paramlong106, paramlong101, paramlong102);
  }
  
  private static void x_to_y2(long10 paramlong101, long10 paramlong102, long10 paramlong103)
  {
    sqr(paramlong101, paramlong103);
    mul_small(paramlong102, paramlong103, 486662L);
    add(paramlong101, paramlong101, paramlong102);
    paramlong101._0 += 1L;
    mul(paramlong102, paramlong101, paramlong103);
  }
  
  private static void core(byte[] paramArrayOfByte1, byte[] paramArrayOfByte2, byte[] paramArrayOfByte3, byte[] paramArrayOfByte4)
  {
    long10 locallong101 = new long10();
    long10 locallong102 = new long10();
    long10 locallong103 = new long10();
    long10 locallong104 = new long10();
    long10 locallong105 = new long10();
    
    long10[] arrayOflong101 = { new long10(), new long10() };
    long10[] arrayOflong102 = { new long10(), new long10() };
    if (paramArrayOfByte4 != null) {
      unpack(locallong101, paramArrayOfByte4);
    } else {
      set(locallong101, 9);
    }
    set(arrayOflong101[0], 1);
    set(arrayOflong102[0], 0);
    

    cpy(arrayOflong101[1], locallong101);
    set(arrayOflong102[1], 1);
    for (int i = 32; i-- != 0;)
    {
      if (i == 0) {
        i = 0;
      }
      for (j = 8; j-- != 0;)
      {
        int k = (paramArrayOfByte3[i] & 0xFF) >> j & 0x1;
        int m = (paramArrayOfByte3[i] & 0xFF ^ 0xFFFFFFFF) >> j & 0x1;
        localObject = arrayOflong101[m];
        long10 locallong106 = arrayOflong102[m];
        long10 locallong107 = arrayOflong101[k];
        long10 locallong108 = arrayOflong102[k];
        


        mont_prep(locallong102, locallong103, (long10)localObject, locallong106);
        mont_prep(locallong104, locallong105, locallong107, locallong108);
        mont_add(locallong102, locallong103, locallong104, locallong105, (long10)localObject, locallong106, locallong101);
        mont_dbl(locallong102, locallong103, locallong104, locallong105, locallong107, locallong108);
      }
    }
    int j;
    Object localObject;
    recip(locallong102, arrayOflong102[0], 0);
    mul(locallong101, arrayOflong101[0], locallong102);
    pack(locallong101, paramArrayOfByte1);
    if (paramArrayOfByte2 != null)
    {
      x_to_y2(locallong103, locallong102, locallong101);
      recip(locallong104, arrayOflong102[1], 0);
      mul(locallong103, arrayOflong101[1], locallong104);
      add(locallong103, locallong103, locallong101);
      locallong103._0 += 486671L;
      locallong101._0 -= 9L;
      sqr(locallong104, locallong101);
      mul(locallong101, locallong103, locallong104);
      sub(locallong101, locallong101, locallong102);
      locallong101._0 -= 39420360L;
      mul(locallong102, locallong101, BASE_R2Y);
      if (is_negative(locallong102) != 0) {
        cpy32(paramArrayOfByte2, paramArrayOfByte3);
      } else {
        mula_small(paramArrayOfByte2, ORDER_TIMES_8, 0, paramArrayOfByte3, 32, -1);
      }
      byte[] arrayOfByte1 = new byte[32];
      byte[] arrayOfByte2 = new byte[64];
      localObject = new byte[64];
      cpy32(arrayOfByte1, ORDER);
      cpy32(paramArrayOfByte2, egcd32(arrayOfByte2, (byte[])localObject, paramArrayOfByte2, arrayOfByte1));
      if ((paramArrayOfByte2[31] & 0x80) != 0) {
        mula_small(paramArrayOfByte2, paramArrayOfByte2, 0, ORDER, 32, 1);
      }
    }
  }
  
  private static final byte[] ORDER_TIMES_8 = { 104, -97, -82, -25, -46, 24, -109, -64, -78, -26, -68, 23, -11, -50, -9, -90, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -128 };
  private static final long10 BASE_2Y = new long10(39999547L, 18689728L, 59995525L, 1648697L, 57546132L, 24010086L, 19059592L, 5425144L, 63499247L, 16420658L);
  private static final long10 BASE_R2Y = new long10(5744L, 8160848L, 4790893L, 13779497L, 35730846L, 12541209L, 49101323L, 30047407L, 40071253L, 6226132L);
}