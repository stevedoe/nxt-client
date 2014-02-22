package nxt;

import java.util.List;
import nxt.util.DbIterator;

public abstract interface Blockchain
{
  public abstract Block getLastBlock();
  
  public abstract Block getBlock(Long paramLong);
  
  public abstract boolean hasBlock(Long paramLong);
  
  public abstract int getBlockCount();
  
  public abstract DbIterator<? extends Block> getAllBlocks();
  
  public abstract DbIterator<? extends Block> getAllBlocks(Account paramAccount, int paramInt);
  
  public abstract List<Long> getBlockIdsAfter(Long paramLong, int paramInt);
  
  public abstract List<? extends Block> getBlocksAfter(Long paramLong, int paramInt);
  
  public abstract long getBlockIdAtHeight(int paramInt);
  
  public abstract List<? extends Block> getBlocksFromHeight(int paramInt);
  
  public abstract Transaction getTransaction(Long paramLong);
  
  public abstract boolean hasTransaction(Long paramLong);
  
  public abstract int getTransactionCount();
  
  public abstract DbIterator<? extends Transaction> getAllTransactions();
  
  public abstract DbIterator<? extends Transaction> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt);
  
  public abstract DbIterator<? extends Transaction> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt, Boolean paramBoolean);
}