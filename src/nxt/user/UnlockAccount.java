package nxt.user;

import java.io.IOException;
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
        ((User)localObject2).lockAccount();
        if (!((User)localObject2).isInactive()) {
          ((User)localObject2).enqueue(JSONResponses.LOCK_ACCOUNT);
        }
      }
    }
    localObject1 = paramUser.unlockAccount(str);
    
    Object localObject2 = new JSONObject();
    ((JSONObject)localObject2).put("response", "unlockAccount");
    ((JSONObject)localObject2).put("account", Convert.convert((Long)localObject1));
    if (str.length() < 30) {
      ((JSONObject)localObject2).put("secretPhraseStrength", Integer.valueOf(1));
    } else {
      ((JSONObject)localObject2).put("secretPhraseStrength", Integer.valueOf(5));
    }
    Account localAccount = Account.getAccount((Long)localObject1);
    if (localAccount == null)
    {
      ((JSONObject)localObject2).put("balance", Integer.valueOf(0));
    }
    else
    {
      ((JSONObject)localObject2).put("balance", Long.valueOf(localAccount.getUnconfirmedBalance()));
      
      JSONArray localJSONArray = new JSONArray();
      byte[] arrayOfByte = localAccount.getPublicKey();
      for (Object localObject3 = Blockchain.getAllUnconfirmedTransactions().iterator(); ((Iterator)localObject3).hasNext();)
      {
        Transaction localTransaction1 = (Transaction)((Iterator)localObject3).next();
        if (Arrays.equals(localTransaction1.getSenderPublicKey(), arrayOfByte))
        {
          localObject4 = new JSONObject();
          ((JSONObject)localObject4).put("index", Integer.valueOf(localTransaction1.getIndex()));
          ((JSONObject)localObject4).put("transactionTimestamp", Integer.valueOf(localTransaction1.getTimestamp()));
          ((JSONObject)localObject4).put("deadline", Short.valueOf(localTransaction1.getDeadline()));
          ((JSONObject)localObject4).put("account", Convert.convert(localTransaction1.getRecipientId()));
          ((JSONObject)localObject4).put("sentAmount", Integer.valueOf(localTransaction1.getAmount()));
          if (((Long)localObject1).equals(localTransaction1.getRecipientId())) {
            ((JSONObject)localObject4).put("receivedAmount", Integer.valueOf(localTransaction1.getAmount()));
          }
          ((JSONObject)localObject4).put("fee", Integer.valueOf(localTransaction1.getFee()));
          ((JSONObject)localObject4).put("numberOfConfirmations", Integer.valueOf(0));
          ((JSONObject)localObject4).put("id", localTransaction1.getStringId());
          
          localJSONArray.add(localObject4);
        }
        else if (((Long)localObject1).equals(localTransaction1.getRecipientId()))
        {
          localObject4 = new JSONObject();
          ((JSONObject)localObject4).put("index", Integer.valueOf(localTransaction1.getIndex()));
          ((JSONObject)localObject4).put("transactionTimestamp", Integer.valueOf(localTransaction1.getTimestamp()));
          ((JSONObject)localObject4).put("deadline", Short.valueOf(localTransaction1.getDeadline()));
          ((JSONObject)localObject4).put("account", Convert.convert(localTransaction1.getSenderId()));
          ((JSONObject)localObject4).put("receivedAmount", Integer.valueOf(localTransaction1.getAmount()));
          ((JSONObject)localObject4).put("fee", Integer.valueOf(localTransaction1.getFee()));
          ((JSONObject)localObject4).put("numberOfConfirmations", Integer.valueOf(0));
          ((JSONObject)localObject4).put("id", localTransaction1.getStringId());
          
          localJSONArray.add(localObject4);
        }
      }
      localObject3 = new TreeMap();
      
      int i = Blockchain.getLastBlock().getHeight();
      Object localObject4 = Blockchain.getAllBlocks(localAccount, 0);Object localObject5 = null;
      JSONObject localJSONObject;
      try
      {
        while (((DbIterator)localObject4).hasNext())
        {
          Block localBlock = (Block)((DbIterator)localObject4).next();
          if (localBlock.getTotalFee() > 0)
          {
            localJSONObject = new JSONObject();
            localJSONObject.put("index", localBlock.getStringId());
            localJSONObject.put("blockTimestamp", Integer.valueOf(localBlock.getTimestamp()));
            localJSONObject.put("block", localBlock.getStringId());
            localJSONObject.put("earnedAmount", Integer.valueOf(localBlock.getTotalFee()));
            localJSONObject.put("numberOfConfirmations", Integer.valueOf(i - localBlock.getHeight()));
            localJSONObject.put("id", "-");
            ((SortedMap)localObject3).put(Integer.valueOf(-localBlock.getTimestamp()), localJSONObject);
          }
        }
      }
      catch (Throwable localThrowable2)
      {
        localObject5 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localObject4 != null) {
          if (localObject5 != null) {
            try
            {
              ((DbIterator)localObject4).close();
            }
            catch (Throwable localThrowable5)
            {
              ((Throwable)localObject5).addSuppressed(localThrowable5);
            }
          } else {
            ((DbIterator)localObject4).close();
          }
        }
      }
      localObject4 = Blockchain.getAllTransactions(localAccount, (byte)-1, (byte)-1, 0);localObject5 = null;
      try
      {
        while (((DbIterator)localObject4).hasNext())
        {
          Transaction localTransaction2 = (Transaction)((DbIterator)localObject4).next();
          if (localTransaction2.getSenderId().equals(localObject1))
          {
            localJSONObject = new JSONObject();
            localJSONObject.put("index", Integer.valueOf(localTransaction2.getIndex()));
            localJSONObject.put("blockTimestamp", Integer.valueOf(localTransaction2.getBlock().getTimestamp()));
            localJSONObject.put("transactionTimestamp", Integer.valueOf(localTransaction2.getTimestamp()));
            localJSONObject.put("account", Convert.convert(localTransaction2.getRecipientId()));
            localJSONObject.put("sentAmount", Integer.valueOf(localTransaction2.getAmount()));
            if (((Long)localObject1).equals(localTransaction2.getRecipientId())) {
              localJSONObject.put("receivedAmount", Integer.valueOf(localTransaction2.getAmount()));
            }
            localJSONObject.put("fee", Integer.valueOf(localTransaction2.getFee()));
            localJSONObject.put("numberOfConfirmations", Integer.valueOf(i - localTransaction2.getBlock().getHeight()));
            localJSONObject.put("id", localTransaction2.getStringId());
            ((SortedMap)localObject3).put(Integer.valueOf(-localTransaction2.getTimestamp()), localJSONObject);
          }
          else if (localTransaction2.getRecipientId().equals(localObject1))
          {
            localJSONObject = new JSONObject();
            localJSONObject.put("index", Integer.valueOf(localTransaction2.getIndex()));
            localJSONObject.put("blockTimestamp", Integer.valueOf(localTransaction2.getBlock().getTimestamp()));
            localJSONObject.put("transactionTimestamp", Integer.valueOf(localTransaction2.getTimestamp()));
            localJSONObject.put("account", Convert.convert(localTransaction2.getSenderId()));
            localJSONObject.put("receivedAmount", Integer.valueOf(localTransaction2.getAmount()));
            localJSONObject.put("fee", Integer.valueOf(localTransaction2.getFee()));
            localJSONObject.put("numberOfConfirmations", Integer.valueOf(i - localTransaction2.getBlock().getHeight()));
            localJSONObject.put("id", localTransaction2.getStringId());
            ((SortedMap)localObject3).put(Integer.valueOf(-localTransaction2.getTimestamp()), localJSONObject);
          }
        }
      }
      catch (Throwable localThrowable4)
      {
        localObject5 = localThrowable4;throw localThrowable4;
      }
      finally
      {
        if (localObject4 != null) {
          if (localObject5 != null) {
            try
            {
              ((DbIterator)localObject4).close();
            }
            catch (Throwable localThrowable6)
            {
              ((Throwable)localObject5).addSuppressed(localThrowable6);
            }
          } else {
            ((DbIterator)localObject4).close();
          }
        }
      }
      localObject4 = ((SortedMap)localObject3).values().iterator();
      while ((localJSONArray.size() < 1000) && (((Iterator)localObject4).hasNext())) {
        localJSONArray.add(((Iterator)localObject4).next());
      }
      if (localJSONArray.size() > 0)
      {
        localObject5 = new JSONObject();
        ((JSONObject)localObject5).put("response", "processNewData");
        ((JSONObject)localObject5).put("addedMyTransactions", localJSONArray);
        paramUser.enqueue((JSONStreamAware)localObject5);
      }
    }
    return localObject2;
  }
}