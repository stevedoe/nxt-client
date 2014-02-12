package nxt.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream
  extends FilterInputStream
{
  private long count;
  
  public CountingInputStream(InputStream paramInputStream)
  {
    super(paramInputStream);
  }
  
  public int read()
    throws IOException
  {
    int i = super.read();
    if (i >= 0) {
      this.count += 1L;
    }
    return i;
  }
  
  public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
    throws IOException
  {
    int i = super.read(paramArrayOfByte, paramInt1, paramInt2);
    if (i >= 0) {
      this.count += i;
    }
    return i;
  }
  
  public long skip(long paramLong)
    throws IOException
  {
    long l = super.skip(paramLong);
    if (l >= 0L) {
      this.count += l;
    }
    return l;
  }
  
  public long getCount()
  {
    return this.count;
  }
}