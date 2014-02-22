package nxt.peer;

import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public abstract interface Peer
  extends Comparable<Peer>
{
  public abstract String getPeerAddress();
  
  public abstract String getAnnouncedAddress();
  
  public abstract State getState();
  
  public abstract String getVersion();
  
  public abstract String getApplication();
  
  public abstract String getPlatform();
  
  public abstract String getSoftware();
  
  public abstract Hallmark getHallmark();
  
  public abstract int getWeight();
  
  public abstract boolean shareAddress();
  
  public abstract boolean isWellKnown();
  
  public abstract boolean isBlacklisted();
  
  public abstract void blacklist(NxtException paramNxtException);
  
  public abstract void blacklist();
  
  public abstract void unBlacklist();
  
  public abstract void deactivate();
  
  public abstract void remove();
  
  public abstract long getDownloadedVolume();
  
  public abstract long getUploadedVolume();
  
  public abstract JSONObject send(JSONStreamAware paramJSONStreamAware);
  
  public static enum State
  {
    NON_CONNECTED,  CONNECTED,  DISCONNECTED;
    
    private State() {}
  }
}