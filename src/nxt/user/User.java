package nxt.user;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import nxt.Account;
import nxt.Generator;
import nxt.crypto.Crypto;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class User
{
  private volatile String secretPhrase;
  private volatile byte[] publicKey;
  private volatile boolean isInactive;
  private final String userId;
  private final ConcurrentLinkedQueue<JSONStreamAware> pendingResponses = new ConcurrentLinkedQueue();
  private AsyncContext asyncContext;
  
  User(String paramString)
  {
    this.userId = paramString;
  }
  
  String getUserId()
  {
    return this.userId;
  }
  
  byte[] getPublicKey()
  {
    return this.publicKey;
  }
  
  String getSecretPhrase()
  {
    return this.secretPhrase;
  }
  
  boolean isInactive()
  {
    return this.isInactive;
  }
  
  void setInactive(boolean paramBoolean)
  {
    this.isInactive = paramBoolean;
  }
  
  void enqueue(JSONStreamAware paramJSONStreamAware)
  {
    this.pendingResponses.offer(paramJSONStreamAware);
  }
  
  void lockAccount()
  {
    Generator.stopForging(this.secretPhrase);
    this.secretPhrase = null;
  }
  
  Long unlockAccount(String paramString)
  {
    this.publicKey = Crypto.getPublicKey(paramString);
    this.secretPhrase = paramString;
    Generator.startForging(paramString, this.publicKey);
    return Account.getId(this.publicKey);
  }
  
  synchronized void processPendingResponses(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse)
    throws IOException
  {
    JSONArray localJSONArray = new JSONArray();
    JSONStreamAware localJSONStreamAware;
    while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
      localJSONArray.add(localJSONStreamAware);
    }
    Object localObject1;
    Object localObject2;
    if (localJSONArray.size() > 0)
    {
      localObject1 = new JSONObject();
      ((JSONObject)localObject1).put("responses", localJSONArray);
      Object localObject3;
      if (this.asyncContext != null)
      {
        this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        localObject2 = this.asyncContext.getResponse().getWriter();localObject3 = null;
        try
        {
          ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
        }
        catch (Throwable localThrowable4)
        {
          localObject3 = localThrowable4;throw localThrowable4;
        }
        finally
        {
          if (localObject2 != null) {
            if (localObject3 != null) {
              try
              {
                ((Writer)localObject2).close();
              }
              catch (Throwable localThrowable7)
              {
                localObject3.addSuppressed(localThrowable7);
              }
            } else {
              ((Writer)localObject2).close();
            }
          }
        }
        this.asyncContext.complete();
        this.asyncContext = paramHttpServletRequest.startAsync();
        this.asyncContext.addListener(new UserAsyncListener(null));
        this.asyncContext.setTimeout(5000L);
      }
      else
      {
        paramHttpServletResponse.setContentType("text/plain; charset=UTF-8");
        localObject2 = paramHttpServletResponse.getWriter();localObject3 = null;
        try
        {
          ((JSONObject)localObject1).writeJSONString((Writer)localObject2);
        }
        catch (Throwable localThrowable6)
        {
          localObject3 = localThrowable6;throw localThrowable6;
        }
        finally
        {
          if (localObject2 != null) {
            if (localObject3 != null) {
              try
              {
                ((Writer)localObject2).close();
              }
              catch (Throwable localThrowable8)
              {
                localObject3.addSuppressed(localThrowable8);
              }
            } else {
              ((Writer)localObject2).close();
            }
          }
        }
      }
    }
    else
    {
      if (this.asyncContext != null)
      {
        this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        localObject1 = this.asyncContext.getResponse().getWriter();localObject2 = null;
        try
        {
          JSON.emptyJSON.writeJSONString((Writer)localObject1);
        }
        catch (Throwable localThrowable2)
        {
          localObject2 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localObject1 != null) {
            if (localObject2 != null) {
              try
              {
                ((Writer)localObject1).close();
              }
              catch (Throwable localThrowable9)
              {
                ((Throwable)localObject2).addSuppressed(localThrowable9);
              }
            } else {
              ((Writer)localObject1).close();
            }
          }
        }
        this.asyncContext.complete();
      }
      this.asyncContext = paramHttpServletRequest.startAsync();
      this.asyncContext.addListener(new UserAsyncListener(null));
      this.asyncContext.setTimeout(5000L);
    }
  }
  
  synchronized void send(JSONStreamAware paramJSONStreamAware)
  {
    if (this.asyncContext == null)
    {
      if (this.isInactive) {
        return;
      }
      if (this.pendingResponses.size() > 1000)
      {
        this.pendingResponses.clear();
        
        this.isInactive = true;
        if (this.secretPhrase == null) {
          Users.remove(this);
        }
        return;
      }
      this.pendingResponses.offer(paramJSONStreamAware);
    }
    else
    {
      JSONArray localJSONArray = new JSONArray();
      JSONStreamAware localJSONStreamAware;
      while ((localJSONStreamAware = (JSONStreamAware)this.pendingResponses.poll()) != null) {
        localJSONArray.add(localJSONStreamAware);
      }
      localJSONArray.add(paramJSONStreamAware);
      
      JSONObject localJSONObject = new JSONObject();
      localJSONObject.put("responses", localJSONArray);
      
      this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
      try
      {
        PrintWriter localPrintWriter = this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          localJSONObject.writeJSONString(localPrintWriter);
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localPrintWriter != null) {
            if (localObject1 != null) {
              try
              {
                localPrintWriter.close();
              }
              catch (Throwable localThrowable3)
              {
                localObject1.addSuppressed(localThrowable3);
              }
            } else {
              localPrintWriter.close();
            }
          }
        }
      }
      catch (IOException localIOException)
      {
        Logger.logMessage("Error sending response to user", localIOException);
      }
      this.asyncContext.complete();
      this.asyncContext = null;
    }
  }
  
  private final class UserAsyncListener
    implements AsyncListener
  {
    private UserAsyncListener() {}
    
    public void onComplete(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onError(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      synchronized (User.this)
      {
        User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        
        PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          JSON.emptyJSON.writeJSONString(localPrintWriter);
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localPrintWriter != null) {
            if (localObject1 != null) {
              try
              {
                localPrintWriter.close();
              }
              catch (Throwable localThrowable3)
              {
                localObject1.addSuppressed(localThrowable3);
              }
            } else {
              localPrintWriter.close();
            }
          }
        }
        User.this.asyncContext.complete();
        User.this.asyncContext = null;
      }
    }
    
    public void onStartAsync(AsyncEvent paramAsyncEvent)
      throws IOException
    {}
    
    public void onTimeout(AsyncEvent paramAsyncEvent)
      throws IOException
    {
      synchronized (User.this)
      {
        User.this.asyncContext.getResponse().setContentType("text/plain; charset=UTF-8");
        
        PrintWriter localPrintWriter = User.this.asyncContext.getResponse().getWriter();Object localObject1 = null;
        try
        {
          JSON.emptyJSON.writeJSONString(localPrintWriter);
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localPrintWriter != null) {
            if (localObject1 != null) {
              try
              {
                localPrintWriter.close();
              }
              catch (Throwable localThrowable3)
              {
                localObject1.addSuppressed(localThrowable3);
              }
            } else {
              localPrintWriter.close();
            }
          }
        }
        User.this.asyncContext.complete();
        User.this.asyncContext = null;
      }
    }
  }
}