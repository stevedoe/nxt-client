package nxt.user;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import nxt.Account;
import nxt.Block;
import nxt.Blockchain;
import nxt.Genesis;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class UnlockAccount
  extends UserRequestHandler
{
  static final UnlockAccount instance = new UnlockAccount();
  
  public JSONStreamAware processRequest(HttpServletRequest paramHttpServletRequest, User paramUser)
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
      Object localObject7;
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
      localObject5 = Blockchain.getLastBlock().getId();
      int i = 1;
      while (((JSONArray)localObject3).size() < 1000)
      {
        localObject7 = Blockchain.getBlock((Long)localObject5);
        Object localObject8;
        if ((((Block)localObject7).getTotalFee() > 0) && (Arrays.equals(((Block)localObject7).getGeneratorPublicKey(), (byte[])localObject4)))
        {
          localObject8 = new JSONObject();
          ((JSONObject)localObject8).put("index", ((Block)localObject7).getStringId());
          ((JSONObject)localObject8).put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
          ((JSONObject)localObject8).put("block", ((Block)localObject7).getStringId());
          ((JSONObject)localObject8).put("earnedAmount", Integer.valueOf(((Block)localObject7).getTotalFee()));
          ((JSONObject)localObject8).put("numberOfConfirmations", Integer.valueOf(i));
          ((JSONObject)localObject8).put("id", "-");
          
          ((JSONArray)localObject3).add(localObject8);
        }
        for (Object localObject9 : ((Block)localObject7).getTransactions())
        {
          JSONObject localJSONObject2;
          if (Arrays.equals(localObject9.getSenderPublicKey(), (byte[])localObject4))
          {
            localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", Integer.valueOf(localObject9.getIndex()));
            localJSONObject2.put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
            localJSONObject2.put("transactionTimestamp", Integer.valueOf(localObject9.getTimestamp()));
            localJSONObject2.put("account", Convert.convert(localObject9.getRecipientId()));
            localJSONObject2.put("sentAmount", Integer.valueOf(localObject9.getAmount()));
            if (((Long)localObject2).equals(localObject9.getRecipientId())) {
              localJSONObject2.put("receivedAmount", Integer.valueOf(localObject9.getAmount()));
            }
            localJSONObject2.put("fee", Integer.valueOf(localObject9.getFee()));
            localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i));
            localJSONObject2.put("id", localObject9.getStringId());
            
            ((JSONArray)localObject3).add(localJSONObject2);
          }
          else if (((Long)localObject2).equals(localObject9.getRecipientId()))
          {
            localJSONObject2 = new JSONObject();
            localJSONObject2.put("index", Integer.valueOf(localObject9.getIndex()));
            localJSONObject2.put("blockTimestamp", Integer.valueOf(((Block)localObject7).getTimestamp()));
            localJSONObject2.put("transactionTimestamp", Integer.valueOf(localObject9.getTimestamp()));
            localJSONObject2.put("account", Convert.convert(localObject9.getSenderAccountId()));
            localJSONObject2.put("receivedAmount", Integer.valueOf(localObject9.getAmount()));
            localJSONObject2.put("fee", Integer.valueOf(localObject9.getFee()));
            localJSONObject2.put("numberOfConfirmations", Integer.valueOf(i));
            localJSONObject2.put("id", localObject9.getStringId());
            
            ((JSONArray)localObject3).add(localJSONObject2);
          }
        }
        if (((Long)localObject5).equals(Genesis.GENESIS_BLOCK_ID)) {
          break;
        }
        localObject5 = ((Block)localObject7).getPreviousBlockId();
        i++;
      }
      if (((JSONArray)localObject3).size() > 0)
      {
        localObject7 = new JSONObject();
        ((JSONObject)localObject7).put("response", "processNewData");
        ((JSONObject)localObject7).put("addedMyTransactions", localObject3);
        paramUser.enqueue((JSONStreamAware)localObject7);
      }
    }
    return localJSONObject1;
  }
}