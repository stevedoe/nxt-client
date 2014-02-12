package nxt;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import nxt.peer.Peer;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class ThreadPools
{
  private static final ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(8);
  private static final ExecutorService sendToPeersService = Executors.newFixedThreadPool(10);
  
  public static Future<JSONObject> sendInParallel(Peer paramPeer, final JSONStreamAware paramJSONStreamAware)
  {
    sendToPeersService.submit(new Callable()
    {
      public JSONObject call()
      {
        return this.val$peer.send(paramJSONStreamAware);
      }
    });
  }
  
  static void start()
  {
    scheduledThreadPool.scheduleWithFixedDelay(Peer.peerConnectingThread, 0L, 5L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Peer.peerUnBlacklistingThread, 0L, 1L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Peer.getMorePeersThread, 0L, 5L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Blockchain.processTransactionsThread, 0L, 5L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Blockchain.removeUnconfirmedTransactionsThread, 0L, 1L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Blockchain.getMoreBlocksThread, 0L, 1L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Blockchain.rebroadcastTransactionsThread, 0L, 60L, TimeUnit.SECONDS);
    
    scheduledThreadPool.scheduleWithFixedDelay(Generator.generateBlockThread, 0L, 1L, TimeUnit.SECONDS);
  }
  
  static void shutdown()
  {
    shutdownExecutor(scheduledThreadPool);
    shutdownExecutor(sendToPeersService);
  }
  
  private static void shutdownExecutor(ExecutorService paramExecutorService)
  {
    paramExecutorService.shutdown();
    try
    {
      paramExecutorService.awaitTermination(10L, TimeUnit.SECONDS);
    }
    catch (InterruptedException localInterruptedException)
    {
      Thread.currentThread().interrupt();
    }
    if (!paramExecutorService.isTerminated())
    {
      Logger.logMessage("some threads didn't terminate, forcing shutdown");
      paramExecutorService.shutdownNow();
    }
  }
}