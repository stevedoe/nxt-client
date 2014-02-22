package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

final class TransactionDb
{
  /* Error */
  static Transaction findTransaction(Long paramLong)
  {
    // Byte code:
    //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_1
    //   4: aconst_null
    //   5: astore_2
    //   6: aload_1
    //   7: ldc 3
    //   9: invokeinterface 4 2 0
    //   14: astore_3
    //   15: aconst_null
    //   16: astore 4
    //   18: aload_3
    //   19: iconst_1
    //   20: aload_0
    //   21: invokevirtual 5	java/lang/Long:longValue	()J
    //   24: invokeinterface 6 4 0
    //   29: aload_3
    //   30: invokeinterface 7 1 0
    //   35: astore 5
    //   37: aconst_null
    //   38: astore 6
    //   40: aload 5
    //   42: invokeinterface 8 1 0
    //   47: ifeq +11 -> 58
    //   50: aload_1
    //   51: aload 5
    //   53: invokestatic 9	nxt/TransactionDb:findTransaction	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/TransactionImpl;
    //   56: astore 6
    //   58: aload 5
    //   60: invokeinterface 10 1 0
    //   65: aload 6
    //   67: astore 7
    //   69: aload_3
    //   70: ifnull +35 -> 105
    //   73: aload 4
    //   75: ifnull +24 -> 99
    //   78: aload_3
    //   79: invokeinterface 11 1 0
    //   84: goto +21 -> 105
    //   87: astore 8
    //   89: aload 4
    //   91: aload 8
    //   93: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   96: goto +9 -> 105
    //   99: aload_3
    //   100: invokeinterface 11 1 0
    //   105: aload_1
    //   106: ifnull +33 -> 139
    //   109: aload_2
    //   110: ifnull +23 -> 133
    //   113: aload_1
    //   114: invokeinterface 14 1 0
    //   119: goto +20 -> 139
    //   122: astore 8
    //   124: aload_2
    //   125: aload 8
    //   127: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   130: goto +9 -> 139
    //   133: aload_1
    //   134: invokeinterface 14 1 0
    //   139: aload 7
    //   141: areturn
    //   142: astore 5
    //   144: aload 5
    //   146: astore 4
    //   148: aload 5
    //   150: athrow
    //   151: astore 9
    //   153: aload_3
    //   154: ifnull +35 -> 189
    //   157: aload 4
    //   159: ifnull +24 -> 183
    //   162: aload_3
    //   163: invokeinterface 11 1 0
    //   168: goto +21 -> 189
    //   171: astore 10
    //   173: aload 4
    //   175: aload 10
    //   177: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   180: goto +9 -> 189
    //   183: aload_3
    //   184: invokeinterface 11 1 0
    //   189: aload 9
    //   191: athrow
    //   192: astore_3
    //   193: aload_3
    //   194: astore_2
    //   195: aload_3
    //   196: athrow
    //   197: astore 11
    //   199: aload_1
    //   200: ifnull +33 -> 233
    //   203: aload_2
    //   204: ifnull +23 -> 227
    //   207: aload_1
    //   208: invokeinterface 14 1 0
    //   213: goto +20 -> 233
    //   216: astore 12
    //   218: aload_2
    //   219: aload 12
    //   221: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   224: goto +9 -> 233
    //   227: aload_1
    //   228: invokeinterface 14 1 0
    //   233: aload 11
    //   235: athrow
    //   236: astore_1
    //   237: new 16	java/lang/RuntimeException
    //   240: dup
    //   241: aload_1
    //   242: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
    //   245: aload_1
    //   246: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   249: athrow
    //   250: astore_1
    //   251: new 16	java/lang/RuntimeException
    //   254: dup
    //   255: new 20	java/lang/StringBuilder
    //   258: dup
    //   259: invokespecial 21	java/lang/StringBuilder:<init>	()V
    //   262: ldc 22
    //   264: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   267: aload_0
    //   268: invokevirtual 24	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   271: ldc 25
    //   273: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   276: invokevirtual 26	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   279: invokespecial 27	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
    //   282: athrow
    // Line number table:
    //   Java source line #14	-> byte code offset #0
    //   Java source line #15	-> byte code offset #6
    //   Java source line #14	-> byte code offset #15
    //   Java source line #16	-> byte code offset #18
    //   Java source line #17	-> byte code offset #29
    //   Java source line #18	-> byte code offset #37
    //   Java source line #19	-> byte code offset #40
    //   Java source line #20	-> byte code offset #50
    //   Java source line #22	-> byte code offset #58
    //   Java source line #23	-> byte code offset #65
    //   Java source line #24	-> byte code offset #69
    //   Java source line #14	-> byte code offset #142
    //   Java source line #24	-> byte code offset #151
    //   Java source line #14	-> byte code offset #192
    //   Java source line #24	-> byte code offset #197
    //   Java source line #25	-> byte code offset #237
    //   Java source line #26	-> byte code offset #250
    //   Java source line #27	-> byte code offset #251
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	283	0	paramLong	Long
    //   3	225	1	localConnection	Connection
    //   236	10	1	localSQLException	SQLException
    //   250	1	1	localValidationException	NxtException.ValidationException
    //   5	214	2	localObject1	Object
    //   14	170	3	localPreparedStatement	PreparedStatement
    //   192	4	3	localThrowable1	Throwable
    //   16	158	4	localObject2	Object
    //   35	24	5	localResultSet	ResultSet
    //   142	7	5	localThrowable2	Throwable
    //   38	28	6	localTransactionImpl1	TransactionImpl
    //   87	5	8	localThrowable3	Throwable
    //   122	4	8	localThrowable4	Throwable
    //   151	39	9	localObject3	Object
    //   171	5	10	localThrowable5	Throwable
    //   197	37	11	localObject4	Object
    //   216	4	12	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   78	84	87	java/lang/Throwable
    //   113	119	122	java/lang/Throwable
    //   18	69	142	java/lang/Throwable
    //   18	69	151	finally
    //   142	153	151	finally
    //   162	168	171	java/lang/Throwable
    //   6	105	192	java/lang/Throwable
    //   142	192	192	java/lang/Throwable
    //   6	105	197	finally
    //   142	199	197	finally
    //   207	213	216	java/lang/Throwable
    //   0	139	236	java/sql/SQLException
    //   142	236	236	java/sql/SQLException
    //   0	139	250	nxt/NxtException$ValidationException
    //   142	236	250	nxt/NxtException$ValidationException
  }
  
  /* Error */
  static boolean hasTransaction(Long paramLong)
  {
    // Byte code:
    //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_1
    //   4: aconst_null
    //   5: astore_2
    //   6: aload_1
    //   7: ldc 28
    //   9: invokeinterface 4 2 0
    //   14: astore_3
    //   15: aconst_null
    //   16: astore 4
    //   18: aload_3
    //   19: iconst_1
    //   20: aload_0
    //   21: invokevirtual 5	java/lang/Long:longValue	()J
    //   24: invokeinterface 6 4 0
    //   29: aload_3
    //   30: invokeinterface 7 1 0
    //   35: astore 5
    //   37: aload 5
    //   39: invokeinterface 8 1 0
    //   44: istore 6
    //   46: aload_3
    //   47: ifnull +35 -> 82
    //   50: aload 4
    //   52: ifnull +24 -> 76
    //   55: aload_3
    //   56: invokeinterface 11 1 0
    //   61: goto +21 -> 82
    //   64: astore 7
    //   66: aload 4
    //   68: aload 7
    //   70: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   73: goto +9 -> 82
    //   76: aload_3
    //   77: invokeinterface 11 1 0
    //   82: aload_1
    //   83: ifnull +33 -> 116
    //   86: aload_2
    //   87: ifnull +23 -> 110
    //   90: aload_1
    //   91: invokeinterface 14 1 0
    //   96: goto +20 -> 116
    //   99: astore 7
    //   101: aload_2
    //   102: aload 7
    //   104: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   107: goto +9 -> 116
    //   110: aload_1
    //   111: invokeinterface 14 1 0
    //   116: iload 6
    //   118: ireturn
    //   119: astore 5
    //   121: aload 5
    //   123: astore 4
    //   125: aload 5
    //   127: athrow
    //   128: astore 8
    //   130: aload_3
    //   131: ifnull +35 -> 166
    //   134: aload 4
    //   136: ifnull +24 -> 160
    //   139: aload_3
    //   140: invokeinterface 11 1 0
    //   145: goto +21 -> 166
    //   148: astore 9
    //   150: aload 4
    //   152: aload 9
    //   154: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   157: goto +9 -> 166
    //   160: aload_3
    //   161: invokeinterface 11 1 0
    //   166: aload 8
    //   168: athrow
    //   169: astore_3
    //   170: aload_3
    //   171: astore_2
    //   172: aload_3
    //   173: athrow
    //   174: astore 10
    //   176: aload_1
    //   177: ifnull +33 -> 210
    //   180: aload_2
    //   181: ifnull +23 -> 204
    //   184: aload_1
    //   185: invokeinterface 14 1 0
    //   190: goto +20 -> 210
    //   193: astore 11
    //   195: aload_2
    //   196: aload 11
    //   198: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   201: goto +9 -> 210
    //   204: aload_1
    //   205: invokeinterface 14 1 0
    //   210: aload 10
    //   212: athrow
    //   213: astore_1
    //   214: new 16	java/lang/RuntimeException
    //   217: dup
    //   218: aload_1
    //   219: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
    //   222: aload_1
    //   223: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   226: athrow
    // Line number table:
    //   Java source line #32	-> byte code offset #0
    //   Java source line #33	-> byte code offset #6
    //   Java source line #32	-> byte code offset #15
    //   Java source line #34	-> byte code offset #18
    //   Java source line #35	-> byte code offset #29
    //   Java source line #36	-> byte code offset #37
    //   Java source line #37	-> byte code offset #46
    //   Java source line #32	-> byte code offset #119
    //   Java source line #37	-> byte code offset #128
    //   Java source line #32	-> byte code offset #169
    //   Java source line #37	-> byte code offset #174
    //   Java source line #38	-> byte code offset #214
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	227	0	paramLong	Long
    //   3	202	1	localConnection	Connection
    //   213	10	1	localSQLException	SQLException
    //   5	191	2	localObject1	Object
    //   14	147	3	localPreparedStatement	PreparedStatement
    //   169	4	3	localThrowable1	Throwable
    //   16	135	4	localObject2	Object
    //   35	3	5	localResultSet	ResultSet
    //   119	7	5	localThrowable2	Throwable
    //   64	5	7	localThrowable3	Throwable
    //   99	4	7	localThrowable4	Throwable
    //   128	39	8	localObject3	Object
    //   148	5	9	localThrowable5	Throwable
    //   174	37	10	localObject4	Object
    //   193	4	11	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   55	61	64	java/lang/Throwable
    //   90	96	99	java/lang/Throwable
    //   18	46	119	java/lang/Throwable
    //   18	46	128	finally
    //   119	130	128	finally
    //   139	145	148	java/lang/Throwable
    //   6	82	169	java/lang/Throwable
    //   119	169	169	java/lang/Throwable
    //   6	82	174	finally
    //   119	176	174	finally
    //   184	190	193	java/lang/Throwable
    //   0	116	213	java/sql/SQLException
    //   119	213	213	java/sql/SQLException
  }
  
  static TransactionImpl findTransaction(Connection paramConnection, ResultSet paramResultSet)
    throws NxtException.ValidationException
  {
    try
    {
      byte b1 = paramResultSet.getByte("type");
      byte b2 = paramResultSet.getByte("subtype");
      int i = paramResultSet.getInt("timestamp");
      short s = paramResultSet.getShort("deadline");
      byte[] arrayOfByte1 = paramResultSet.getBytes("sender_public_key");
      Long localLong1 = Long.valueOf(paramResultSet.getLong("recipient_id"));
      int j = paramResultSet.getInt("amount");
      int k = paramResultSet.getInt("fee");
      Long localLong2 = Long.valueOf(paramResultSet.getLong("referenced_transaction_id"));
      if (paramResultSet.wasNull()) {
        localLong2 = null;
      }
      byte[] arrayOfByte2 = paramResultSet.getBytes("signature");
      Long localLong3 = Long.valueOf(paramResultSet.getLong("block_id"));
      int m = paramResultSet.getInt("height");
      Long localLong4 = Long.valueOf(paramResultSet.getLong("id"));
      Long localLong5 = Long.valueOf(paramResultSet.getLong("sender_id"));
      Attachment localAttachment = (Attachment)paramResultSet.getObject("attachment");
      
      TransactionType localTransactionType = TransactionType.findTransactionType(b1, b2);
      return new TransactionImpl(localTransactionType, i, s, arrayOfByte1, localLong1, j, k, localLong2, arrayOfByte2, localLong3, m, localLong4, localLong5, localAttachment);
    }
    catch (SQLException localSQLException)
    {
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  /* Error */
  static List<TransactionImpl> findBlockTransactions(Connection paramConnection, Long paramLong)
  {
    // Byte code:
    //   0: new 57	java/util/ArrayList
    //   3: dup
    //   4: invokespecial 58	java/util/ArrayList:<init>	()V
    //   7: astore_2
    //   8: aload_0
    //   9: ldc 59
    //   11: invokeinterface 4 2 0
    //   16: astore_3
    //   17: aconst_null
    //   18: astore 4
    //   20: aload_3
    //   21: iconst_1
    //   22: aload_1
    //   23: invokevirtual 5	java/lang/Long:longValue	()J
    //   26: invokeinterface 6 4 0
    //   31: aload_3
    //   32: invokeinterface 7 1 0
    //   37: astore 5
    //   39: aload 5
    //   41: invokeinterface 8 1 0
    //   46: ifeq +19 -> 65
    //   49: aload_2
    //   50: aload_0
    //   51: aload 5
    //   53: invokestatic 9	nxt/TransactionDb:findTransaction	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/TransactionImpl;
    //   56: invokeinterface 60 2 0
    //   61: pop
    //   62: goto -23 -> 39
    //   65: aload 5
    //   67: invokeinterface 10 1 0
    //   72: aload_2
    //   73: astore 6
    //   75: aload_3
    //   76: ifnull +35 -> 111
    //   79: aload 4
    //   81: ifnull +24 -> 105
    //   84: aload_3
    //   85: invokeinterface 11 1 0
    //   90: goto +21 -> 111
    //   93: astore 7
    //   95: aload 4
    //   97: aload 7
    //   99: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   102: goto +9 -> 111
    //   105: aload_3
    //   106: invokeinterface 11 1 0
    //   111: aload 6
    //   113: areturn
    //   114: astore 5
    //   116: aload 5
    //   118: astore 4
    //   120: aload 5
    //   122: athrow
    //   123: astore 8
    //   125: aload_3
    //   126: ifnull +35 -> 161
    //   129: aload 4
    //   131: ifnull +24 -> 155
    //   134: aload_3
    //   135: invokeinterface 11 1 0
    //   140: goto +21 -> 161
    //   143: astore 9
    //   145: aload 4
    //   147: aload 9
    //   149: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   152: goto +9 -> 161
    //   155: aload_3
    //   156: invokeinterface 11 1 0
    //   161: aload 8
    //   163: athrow
    //   164: astore_3
    //   165: new 16	java/lang/RuntimeException
    //   168: dup
    //   169: aload_3
    //   170: invokevirtual 56	java/sql/SQLException:toString	()Ljava/lang/String;
    //   173: aload_3
    //   174: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   177: athrow
    //   178: astore_3
    //   179: new 16	java/lang/RuntimeException
    //   182: dup
    //   183: new 20	java/lang/StringBuilder
    //   186: dup
    //   187: invokespecial 21	java/lang/StringBuilder:<init>	()V
    //   190: ldc 61
    //   192: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   195: aload_1
    //   196: invokevirtual 24	java/lang/StringBuilder:append	(Ljava/lang/Object;)Ljava/lang/StringBuilder;
    //   199: ldc 62
    //   201: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   204: invokevirtual 26	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   207: invokespecial 27	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
    //   210: athrow
    // Line number table:
    //   Java source line #74	-> byte code offset #0
    //   Java source line #75	-> byte code offset #8
    //   Java source line #76	-> byte code offset #20
    //   Java source line #77	-> byte code offset #31
    //   Java source line #78	-> byte code offset #39
    //   Java source line #79	-> byte code offset #49
    //   Java source line #81	-> byte code offset #65
    //   Java source line #82	-> byte code offset #72
    //   Java source line #83	-> byte code offset #75
    //   Java source line #75	-> byte code offset #114
    //   Java source line #83	-> byte code offset #123
    //   Java source line #84	-> byte code offset #165
    //   Java source line #85	-> byte code offset #178
    //   Java source line #86	-> byte code offset #179
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	211	0	paramConnection	Connection
    //   0	211	1	paramLong	Long
    //   7	66	2	localArrayList1	java.util.ArrayList
    //   16	140	3	localPreparedStatement	PreparedStatement
    //   164	10	3	localSQLException	SQLException
    //   178	1	3	localValidationException	NxtException.ValidationException
    //   18	128	4	localObject1	Object
    //   37	29	5	localResultSet	ResultSet
    //   114	7	5	localThrowable1	Throwable
    //   93	5	7	localThrowable2	Throwable
    //   123	39	8	localObject2	Object
    //   143	5	9	localThrowable3	Throwable
    // Exception table:
    //   from	to	target	type
    //   84	90	93	java/lang/Throwable
    //   20	75	114	java/lang/Throwable
    //   20	75	123	finally
    //   114	125	123	finally
    //   134	140	143	java/lang/Throwable
    //   8	111	164	java/sql/SQLException
    //   114	164	164	java/sql/SQLException
    //   8	111	178	nxt/NxtException$ValidationException
    //   114	164	178	nxt/NxtException$ValidationException
  }
  
  static void saveTransactions(Connection paramConnection, List<TransactionImpl> paramList)
  {
    try
    {
      for (Transaction localTransaction : paramList)
      {
        PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO transaction (id, deadline, sender_public_key, recipient_id, amount, fee, referenced_transaction_id, height, block_id, signature, timestamp, type, subtype, sender_id, attachment)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");Object localObject1 = null;
        try
        {
          int i = 0;
          localPreparedStatement.setLong(++i, localTransaction.getId().longValue());
          localPreparedStatement.setShort(++i, localTransaction.getDeadline());
          localPreparedStatement.setBytes(++i, localTransaction.getSenderPublicKey());
          localPreparedStatement.setLong(++i, localTransaction.getRecipientId().longValue());
          localPreparedStatement.setInt(++i, localTransaction.getAmount());
          localPreparedStatement.setInt(++i, localTransaction.getFee());
          if (localTransaction.getReferencedTransactionId() != null) {
            localPreparedStatement.setLong(++i, localTransaction.getReferencedTransactionId().longValue());
          } else {
            localPreparedStatement.setNull(++i, -5);
          }
          localPreparedStatement.setInt(++i, localTransaction.getHeight());
          localPreparedStatement.setLong(++i, localTransaction.getBlock().getId().longValue());
          localPreparedStatement.setBytes(++i, localTransaction.getSignature());
          localPreparedStatement.setInt(++i, localTransaction.getTimestamp());
          localPreparedStatement.setByte(++i, localTransaction.getType().getType());
          localPreparedStatement.setByte(++i, localTransaction.getType().getSubtype());
          localPreparedStatement.setLong(++i, localTransaction.getSenderId().longValue());
          if (localTransaction.getAttachment() != null) {
            localPreparedStatement.setObject(++i, localTransaction.getAttachment());
          } else {
            localPreparedStatement.setNull(++i, 2000);
          }
          localPreparedStatement.executeUpdate();
        }
        catch (Throwable localThrowable2)
        {
          localObject1 = localThrowable2;throw localThrowable2;
        }
        finally
        {
          if (localPreparedStatement != null) {
            if (localObject1 != null) {
              try
              {
                localPreparedStatement.close();
              }
              catch (Throwable localThrowable3)
              {
                localObject1.addSuppressed(localThrowable3);
              }
            } else {
              localPreparedStatement.close();
            }
          }
        }
      }
    }
    catch (SQLException localSQLException)
    {
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
}