package nxt;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import nxt.util.Convert;

public final class Vote
{
  private static final ConcurrentMap<Long, Vote> votes = new ConcurrentHashMap();
  private final Long id;
  private final Long pollId;
  private final Long voterId;
  private final byte[] vote;
  
  private Vote(Long paramLong1, Long paramLong2, Long paramLong3, byte[] paramArrayOfByte)
  {
    this.id = paramLong1;
    this.pollId = paramLong2;
    this.voterId = paramLong3;
    this.vote = paramArrayOfByte;
  }
  
  static Vote addVote(Long paramLong1, Long paramLong2, Long paramLong3, byte[] paramArrayOfByte)
  {
    Vote localVote = new Vote(paramLong1, paramLong2, paramLong3, paramArrayOfByte);
    if (votes.putIfAbsent(paramLong1, localVote) != null) {
      throw new IllegalStateException("Vote with id " + Convert.toUnsignedLong(paramLong1) + " already exists");
    }
    return localVote;
  }
  
  public static Map<Long, Vote> getVotes()
  {
    return Collections.unmodifiableMap(votes);
  }
  
  static void clear()
  {
    votes.clear();
  }
  
  public static Vote getVote(Long paramLong)
  {
    return (Vote)votes.get(paramLong);
  }
  
  public Long getId()
  {
    return this.id;
  }
  
  public Long getPollId()
  {
    return this.pollId;
  }
  
  public Long getVoterId()
  {
    return this.voterId;
  }
  
  public byte[] getVote()
  {
    return this.vote;
  }
}