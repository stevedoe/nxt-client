package nxt;

public abstract class NxtException
  extends Exception
{
  protected NxtException() {}
  
  protected NxtException(String paramString)
  {
    super(paramString);
  }
  
  protected NxtException(String paramString, Throwable paramThrowable)
  {
    super(paramString, paramThrowable);
  }
  
  protected NxtException(Throwable paramThrowable)
  {
    super(paramThrowable);
  }
  
  public static class ValidationException
    extends NxtException
  {
    ValidationException(String paramString)
    {
      super();
    }
    
    ValidationException(String paramString, Throwable paramThrowable)
    {
      super(paramThrowable);
    }
  }
}