package nxt.http;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import nxt.Nxt;
import nxt.util.Logger;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public final class API
{
  static final Set<String> allowedBotHosts;
  
  public static void init() {}
  
  static
  {
    String str1 = Nxt.getStringProperty("nxt.allowedBotHosts");
    Object localObject1;
    if (!str1.equals("*"))
    {
      HashSet localHashSet = new HashSet();
      for (localObject1 : str1.split(";"))
      {
        localObject1 = ((String)localObject1).trim();
        if (((String)localObject1).length() > 0) {
          localHashSet.add(localObject1);
        }
      }
      allowedBotHosts = Collections.unmodifiableSet(localHashSet);
    }
    else
    {
      allowedBotHosts = null;
    }
    boolean bool1 = Nxt.getBooleanProperty("nxt.enableAPIServer").booleanValue();
    if (bool1) {
      try
      {
        int i = Nxt.getIntProperty("nxt.apiServerPort");
        String str2 = Nxt.getStringProperty("nxt.apiServerHost");
        Server localServer = new Server();
        

        boolean bool2 = Nxt.getBooleanProperty("nxt.apiSSL").booleanValue();
        if (bool2)
        {
          Logger.logMessage("Using SSL (https) for the API server");
          localObject2 = new HttpConfiguration();
          ((HttpConfiguration)localObject2).setSecureScheme("https");
          ((HttpConfiguration)localObject2).setSecurePort(i);
          ((HttpConfiguration)localObject2).addCustomizer(new SecureRequestCustomizer());
          localObject3 = new SslContextFactory();
          ((SslContextFactory)localObject3).setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
          ((SslContextFactory)localObject3).setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
          ((SslContextFactory)localObject3).setExcludeCipherSuites(new String[] { "SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA" });
          

          localObject1 = new ServerConnector(localServer, new ConnectionFactory[] { new SslConnectionFactory((SslContextFactory)localObject3, "http/1.1"), new HttpConnectionFactory((HttpConfiguration)localObject2) });
        }
        else
        {
          localObject1 = new ServerConnector(localServer);
        }
        ((ServerConnector)localObject1).setPort(i);
        ((ServerConnector)localObject1).setHost(str2);
        ((ServerConnector)localObject1).setIdleTimeout(Nxt.getIntProperty("nxt.apiServerIdleTimeout"));
        localServer.addConnector((Connector)localObject1);
        
        Object localObject2 = new HandlerList();
        
        Object localObject3 = Nxt.getStringProperty("nxt.apiResourceBase");
        if (localObject3 != null)
        {
          localObject4 = new ResourceHandler();
          ((ResourceHandler)localObject4).setDirectoriesListed(true);
          ((ResourceHandler)localObject4).setWelcomeFiles(new String[] { "index.html" });
          ((ResourceHandler)localObject4).setResourceBase((String)localObject3);
          ((HandlerList)localObject2).addHandler((Handler)localObject4);
        }
        Object localObject4 = Nxt.getStringProperty("nxt.javadocResourceBase");
        Object localObject6;
        if (localObject4 != null)
        {
          localObject5 = new ContextHandler("/doc");
          localObject6 = new ResourceHandler();
          ((ResourceHandler)localObject6).setDirectoriesListed(false);
          ((ResourceHandler)localObject6).setWelcomeFiles(new String[] { "index.html" });
          ((ResourceHandler)localObject6).setResourceBase((String)localObject4);
          ((ContextHandler)localObject5).setHandler((Handler)localObject6);
          ((HandlerList)localObject2).addHandler((Handler)localObject5);
        }
        Object localObject5 = new ServletHandler();
        ((ServletHandler)localObject5).addServletWithMapping(APIServlet.class, "/nxt");
        if (Nxt.getBooleanProperty("nxt.apiServerCORS").booleanValue())
        {
          localObject6 = ((ServletHandler)localObject5).addFilterWithMapping(CrossOriginFilter.class, "/*", 0);
          ((FilterHolder)localObject6).setInitParameter("allowedHeaders", "*");
          ((FilterHolder)localObject6).setAsyncSupported(true);
        }
        ((HandlerList)localObject2).addHandler((Handler)localObject5);
        ((HandlerList)localObject2).addHandler(new DefaultHandler());
        
        localServer.setHandler((Handler)localObject2);
        localServer.setStopAtShutdown(true);
        localServer.start();
        Logger.logMessage("Started API server at " + str2 + ":" + i);
      }
      catch (Exception localException)
      {
        Logger.logDebugMessage("Failed to start API server", localException);
        throw new RuntimeException(localException.toString(), localException);
      }
    } else {
      Logger.logMessage("API server not enabled");
    }
  }
}