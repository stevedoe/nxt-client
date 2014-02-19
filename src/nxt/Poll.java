package nxt;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.util.Convert;

public final class Poll
{
  private static final ConcurrentMap<Long, Poll> polls = new ConcurrentHashMap();
  private static final Collection<Poll> allPolls = Collections.unmodifiableCollection(polls.values());
  private final Long id;
  private final String name;
  private final String description;
  private final String[] options;
  private final byte minNumberOfOptions;
  private final byte maxNumberOfOptions;
  private final boolean optionsAreBinary;
  private final ConcurrentMap<Long, Long> voters;
  
  private Poll(Long paramLong, String paramString1, String paramString2, String[] paramArrayOfString, byte paramByte1, byte paramByte2, boolean paramBoolean)
  {
    this.id = paramLong;
    this.name = paramString1;
    this.description = paramString2;
    this.options = paramArrayOfString;
    this.minNumberOfOptions = paramByte1;
    this.maxNumberOfOptions = paramByte2;
    this.optionsAreBinary = paramBoolean;
    this.voters = new ConcurrentHashMap();
  }
  
  static void addPoll(Long paramLong, String paramString1, String paramString2, String[] paramArrayOfString, byte paramByte1, byte paramByte2, boolean paramBoolean)
  {
    if (polls.putIfAbsent(paramLong, new Poll(paramLong, paramString1, paramString2, paramArrayOfString, paramByte1, paramByte2, paramBoolean)) != null) {
      throw new IllegalStateException("Poll with id " + Convert.toUnsignedLong(paramLong) + " already exists");
    }
  }
  
  public static Collection<Poll> getAllPolls()
  {
    return allPolls;
  }
  
  static void clear()
  {
    polls.clear();
  }
  
  public static Poll getPoll(Long paramLong)
  {
    return (Poll)polls.get(paramLong);
  }
  
  public Long getId()
  {
    return this.id;
  }
  
  public String getName()
  {
    return this.name;
  }
  
  public String getDescription()
  {
    return this.description;
  }
  
  public String[] getOptions()
  {
    return this.options;
  }
  
  public byte getMinNumberOfOptions()
  {
    return this.minNumberOfOptions;
  }
  
  public byte getMaxNumberOfOptions()
  {
    return this.maxNumberOfOptions;
  }
  
  public boolean isOptionsAreBinary()
  {
    return this.optionsAreBinary;
  }
  
  public Map<Long, Long> getVoters()
  {
    return Collections.unmodifiableMap(this.voters);
  }
  
  void addVoter(Long paramLong1, Long paramLong2)
  {
    this.voters.put(paramLong1, paramLong2);
  }
}