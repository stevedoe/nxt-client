package nxt.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream
  extends FilterOutputStream
{
  private long count;
  
  public CountingOutputStream(OutputStream paramOutputStream)
  {
    super(paramOutputStream);
  }
  
  public void write(int paramInt)
    throws IOException
  {
    this.count += 1L;
    super.write(paramInt);
  }
  
  public long getCount()
  {
    return this.count;
  }
}