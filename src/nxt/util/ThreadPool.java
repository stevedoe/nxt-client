package nxt.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadPool
{
  private static ScheduledExecutorService scheduledThreadPool;
  private static Map<Runnable, Integer> backgroundJobs = new HashMap();
  private static List<Runnable> runBeforeStartJobs = new ArrayList();
  
  public static synchronized void runBeforeStart(Runnable paramRunnable)
  {
    if (scheduledThreadPool != null) {
      throw new IllegalStateException("Executor service already started");
    }
    runBeforeStartJobs.add(paramRunnable);
  }
  
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
    Logger.logDebugMessage("Running final tasks...");
    for (Iterator localIterator = runBeforeStartJobs.iterator(); localIterator.hasNext();)
    {
      localObject = (Runnable)localIterator.next();
      ((Runnable)localObject).run();
    }
    Object localObject;
    runBeforeStartJobs = null;
    Logger.logDebugMessage("Starting " + backgroundJobs.size() + " background jobs");
    scheduledThreadPool = Executors.newScheduledThreadPool(backgroundJobs.size());
    for (localIterator = backgroundJobs.entrySet().iterator(); localIterator.hasNext();)
    {
      localObject = (Map.Entry)localIterator.next();
      scheduledThreadPool.scheduleWithFixedDelay((Runnable)((Map.Entry)localObject).getKey(), 0L, ((Integer)((Map.Entry)localObject).getValue()).intValue(), TimeUnit.SECONDS);
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