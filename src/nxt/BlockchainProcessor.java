package nxt;

import nxt.peer.Peer;
import nxt.util.Observable;
import org.json.simple.JSONObject;

public abstract interface BlockchainProcessor
  extends Observable<Block, Event>
{
  public abstract Peer getLastBlockchainFeeder();
  
  public abstract void processPeerBlock(JSONObject paramJSONObject)
    throws NxtException;
  
  public abstract void fullReset();
  
  public static enum Event
  {
    BLOCK_PUSHED,  BLOCK_POPPED,  BLOCK_GENERATED,  BLOCK_SCANNED;
    
    private Event() {}
  }
  
  public static class BlockNotAcceptedException
    extends NxtException
  {
    BlockNotAcceptedException(String paramString)
    {
      super();
    }
  }
  
  public static class BlockOutOfOrderException
    extends BlockchainProcessor.BlockNotAcceptedException
  {
    BlockOutOfOrderException(String paramString)
    {
      super();
    }
  }
}