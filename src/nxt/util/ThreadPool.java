package nxt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool
{
  private static ScheduledExecutorService scheduledThreadPool;
  private static Map<Runnable, Integer> backgroundJobs = new HashMap();
  
  public static synchronized void scheduleThread(Runnable paramRunnable, int paramInt)
  {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started, no new jobs accepted");
    }
    backgroundJobs.put(paramRunnable, Integer.valueOf(paramInt));
  }
  
  public static synchronized void start()
  {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started");
    }
    Logger.logDebugMessage("Starting " + backgroundJobs.size() + " background jobs");
    scheduledThreadPool = Executors.newScheduledThreadPool(backgroundJobs.size());
    for (Map.Entry localEntry : backgroundJobs.entrySet()) {
      scheduledThreadPool.scheduleWithFixedDelay((Runnable)localEntry.getKey(), 0L, ((Integer)localEntry.getValue()).intValue(), TimeUnit.SECONDS);
    }
    backgroundJobs = null;
  }
  
  public static synchronized void shutdown()
  {
    Logger.logDebugMessage("Stopping background jobs...");
    shutdownExecutor(scheduledThreadPool);
    scheduledThreadPool = null;
    Logger.logDebugMessage("...Done");
  }
  
  public static void shutdownExecutor(ExecutorService paramExecutorService)
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