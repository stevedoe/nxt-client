package nxt.user;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class UnlockAccount
  extends UserRequestHandler
{
  static final UnlockAccount instance = new UnlockAccount();
  
  JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
    throws IOException
  {
    String str = paramHttpServletRequest.getParameter("secretPhrase");
    for (Object localObject1 = User.getAllUsers().iterator(); ((Iterator)localObject1).hasNext();)
    {
      localObject2 = (User)((Iterator)localObject1).next();
      if (str.equals(((User)localObject2).getSecretPhrase()))
      {
        ((User)localObject2).deinitializeKeyPair();
        if (!((User)localObject2).isInactive()) {
          ((User)localObject2).enqueue(JSONResponses.LOCK_ACCOUNT);
        }
      }
    }
    localObject1 = paramUser.initializeKeyPair(str);
    Object localObject2 = Long.valueOf(((BigInteger)localObject1).longValue());
    
    JSONObject localJSONObject1 = new JSONObject();
    localJSONObject1.put("response", "unlockAccount");
    localJSONObject1.put("account", ((BigInteger)localObject1).toString());
    if (str.length() < 30) {
      localJSONObject1.put("secretPhraseStrength", Integer.valueOf(1));
    } else {
      localJSONObject1.put("secretPhraseStrength", Integer.valueOf(5));
    }
    Account localAccount = Account.getAccount((Long)localObject2);
    if (localAccount == null)
    {
      localJSONObject1.put("balance", Integer.valueOf(0));
    }
    else
    {
      localJSONObject1.put("balance", Long.valueOf(localAccount.getUnconfirmedBalance()));
      
      long l = localAccount.getEffectiveBalance();
      Object localObject6;
      if (l > 0L)
      {
        localObject3 = new JSONObject();
        ((JSONObject)localObject3).put("response", "setBlockGenerationDeadline");
        
        localObject4 = Blockchain.getLastBlock();
        localObject5 = Crypto.sha256();
        if (((Block)localObject4).getHeight() < 30000)
        {
          localObject7 = Crypto.sign(((Block)localObject4).getGenerationSignature(), paramUser.getSecretPhrase());
          localObject6 = ((MessageDigest)localObject5).digest((byte[])localObject7);
        }
        else
        {
          ((MessageDigest)localObject5).update(((Block)localObject4).getGenerationSignature());
          localObject6 = ((MessageDigest)localObject5).digest(paramUser.getPublicKey());
        }
        localObject7 = new BigInteger(1, new byte[] { localObject6[7], localObject6[6], localObject6[5], localObject6[4], localObject6[3], localObject6[2], localObject6[1], localObject6[0] });
        ((JSONObject)localObject3).put("deadline", Long.valueOf(((BigInteger)localObject7).divide(BigInteger.valueOf(((Block)localObject4).getBaseTarget()).multiply(BigInteger.valueOf(l))).longValue() - (Convert.getEpochTime() - ((Block)localObject4).getTimestamp())));
        
        paramUser.enqueue((JSONStreamAware)localObject3);
      }
      Object localObject3 = new JSONArray();
      Object localObject4 = localAccount.getPublicKey();
      for (Object localObject5 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject5).hasNext();)
      {
        localObject6 = (Transaction)((Iterator)localObject5).next();
        if (Arrays.equals(((Transaction)localObject6).getSenderPublicKey(), (byte[])localObject4))
        {
          localObject7 = new JSONObject();
          ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
          ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
          ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
          ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getRecipientId()));
          ((JSONObject)localObject7).put("sentAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
          if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId())) {
            ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
          }
          ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
          ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
          ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
          
          ((JSONArray)localObject3).add(localObject7);
        }
        else if (((Long)localObject2).equals(((Transaction)localObject6).getRecipientId()))
        {
          localObject7 = new JSONObject();
          ((JSONObject)localObject7).put("index", Integer.valueOf(((Transaction)localObject6).getIndex()));
          ((JSONObject)localObject7).put("transactionTimestamp", Integer.valueOf(((Transaction)localObject6).getTimestamp()));
          ((JSONObject)localObject7).put("deadline", Short.valueOf(((Transaction)localObject6).getDeadline()));
          ((JSONObject)localObject7).put("account", Convert.convert(((Transaction)localObject6).getSenderAccountId()));
          ((JSONObject)localObject7).put("receivedAmount", Integer.valueOf(((Transaction)localObject6).getAmount()));
          ((JSONObject)localObject7).put("fee", Integer.valueOf(((Transaction)localObject6).getFee()));
          ((JSONObject)localObject7).put("numberOfConfirmations", Integer.valueOf(0));
          ((JSONObject)localObject7).put("id", ((Transaction)localObject6).getStringId());
          
          ((JSONArray)localObject3).add(localObject7);
        }
      }
      localObject5 = new TreeMap();
      
      int i = Blockchain.getLastBlock().getHeight();
      Object localObject7 = Blockchain.getAllBlocks(localAccount, 0);Object localObject8 = null;
      JSONObject localJSONObject2;
      try
      {
        while (((DbIterator)localObject7).hasNext())
        {
          Block localBlock = (Block)((DbIterator)localObject7).next();
          if (localBlock.getTotalFee() > 0)
          {
            localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", localBlock.getStringId());
            localJSONObject2.put("blockTimestamp", Integer.valueOf(localBlock.getTimestamp()));
            localJSONObject2.put("block", localBlock.getStringId());
            localJSONObject2.put("earnedAmount", Integer.valueOf(localBlock.getTotalFee()));
            localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localBlock.getHeight()));
            localJSONObject2.put("id", "-");
            ((SortedMap)localObject5).put(Integer.valueOf(-localBlock.getTimestamp()), localJSONObject2);
          }
        }
      }
      catch (Throwable localThrowable2)
      {
        localObject8 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localObject7 != null) {
          if (localObject8 != null) {
            try
            {
              ((DbIterator)localObject7).close();
            }
            catch (Throwable localThrowable5)
            {
              ((Throwable)localObject8).addSuppressed(localThrowable5);
            }
          } else {
            ((DbIterator)localObject7).close();
          }
        }
      }
      localObject7 = Blockchain.getAllTransactions(localAccount, (byte)-1, (byte)-1, 0);localObject8 = null;
      try
      {
        while (((DbIterator)localObject7).hasNext())
        {
          Transaction localTransaction = (Transaction)((DbIterator)localObject7).next();
          if (localTransaction.getSenderAccountId().equals(localObject2))
          {
            localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
            localJSONObject2.put("blockTimestamp", Integer.valueOf(localTransaction.getBlock().getTimestamp()));
            localJSONObject2.put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
            localJSONObject2.put("account", Convert.convert(localTransaction.getRecipientId()));
            localJSONObject2.put("sentAmount", Integer.valueOf(localTransaction.getAmount()));
            if (((Long)localObject2).equals(localTransaction.getRecipientId())) {
              localJSONObject2.put("receivedAmount", Integer.valueOf(localTransaction.getAmount()));
            }
            localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
            localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localTransaction.getBlock().getHeight()));
            localJSONObject2.put("id", localTransaction.getStringId());
            ((SortedMap)localObject5).put(Integer.valueOf(-localTransaction.getTimestamp()), localJSONObject2);
          }
          else if (localTransaction.getRecipientId().equals(localObject2))
          {
            localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", Integer.valueOf(localTransaction.getIndex()));
            localJSONObject2.put("blockTimestamp", Integer.valueOf(localTransaction.getBlock().getTimestamp()));
            localJSONObject2.put("transactionTimestamp", Integer.valueOf(localTransaction.getTimestamp()));
            localJSONObject2.put("account", Convert.convert(localTransaction.getSenderAccountId()));
            localJSONObject2.put("receivedAmount", Integer.valueOf(localTransaction.getAmount()));
            localJSONObject2.put("fee", Integer.valueOf(localTransaction.getFee()));
            localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i - localTransaction.getBlock().getHeight()));
            localJSONObject2.put("id", localTransaction.getStringId());
            ((SortedMap)localObject5).put(Integer.valueOf(-localTransaction.getTimestamp()), localJSONObject2);
          }
        }
      }
      catch (Throwable localThrowable4)
      {
        localObject8 = localThrowable4;throw localThrowable4;
      }
      finally
      {
        if (localObject7 != null) {
          if (localObject8 != null) {
            try
            {
              ((DbIterator)localObject7).close();
            }
            catch (Throwable localThrowable6)
            {
              ((Throwable)localObject8).addSuppressed(localThrowable6);
            }
          } else {
            ((DbIterator)localObject7).close();
          }
        }
      }
      localObject7 = ((SortedMap)localObject5).values().iterator();
      while ((((JSONArray)localObject3).size() < 1000) && (((Iterator)localObject7).hasNext())) {
        ((JSONArray)localObject3).add(((Iterator)localObject7).next());
      }
      if (((JSONArray)localObject3).size() > 0)
      {
        localObject8 = new JSONObject();
        ((JSONObject)localObject8).put("response", "processNewData");
        ((JSONObject)localObject8).put("addedMyTransactions", localObject3);
        paramUser.enqueue((JSONStreamAware)localObject8);
      }
    }
    return localJSONObject1;
  }
}