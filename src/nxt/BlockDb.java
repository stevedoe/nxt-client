package nxt;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

final class BlockDb
{
  /* Error */
  static BlockImpl findBlock(Long paramLong)
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
    //   53: invokestatic 9	nxt/BlockDb:findBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/BlockImpl;
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
    //   Java source line #15	-> byte code offset #0
    //   Java source line #16	-> byte code offset #6
    //   Java source line #15	-> byte code offset #15
    //   Java source line #17	-> byte code offset #18
    //   Java source line #18	-> byte code offset #29
    //   Java source line #19	-> byte code offset #37
    //   Java source line #20	-> byte code offset #40
    //   Java source line #21	-> byte code offset #50
    //   Java source line #23	-> byte code offset #58
    //   Java source line #24	-> byte code offset #65
    //   Java source line #25	-> byte code offset #69
    //   Java source line #15	-> byte code offset #142
    //   Java source line #25	-> byte code offset #151
    //   Java source line #15	-> byte code offset #192
    //   Java source line #25	-> byte code offset #197
    //   Java source line #26	-> byte code offset #237
    //   Java source line #27	-> byte code offset #250
    //   Java source line #28	-> byte code offset #251
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
    //   38	28	6	localBlockImpl1	BlockImpl
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
  static boolean hasBlock(Long paramLong)
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
    //   Java source line #33	-> byte code offset #0
    //   Java source line #34	-> byte code offset #6
    //   Java source line #33	-> byte code offset #15
    //   Java source line #35	-> byte code offset #18
    //   Java source line #36	-> byte code offset #29
    //   Java source line #37	-> byte code offset #37
    //   Java source line #38	-> byte code offset #46
    //   Java source line #33	-> byte code offset #119
    //   Java source line #38	-> byte code offset #128
    //   Java source line #33	-> byte code offset #169
    //   Java source line #38	-> byte code offset #174
    //   Java source line #39	-> byte code offset #214
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
  
  /* Error */
  static long findBlockIdAtHeight(int paramInt)
  {
    // Byte code:
    //   0: invokestatic 2	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_1
    //   4: aconst_null
    //   5: astore_2
    //   6: aload_1
    //   7: ldc 29
    //   9: invokeinterface 4 2 0
    //   14: astore_3
    //   15: aconst_null
    //   16: astore 4
    //   18: aload_3
    //   19: iconst_1
    //   20: iload_0
    //   21: invokeinterface 30 3 0
    //   26: aload_3
    //   27: invokeinterface 7 1 0
    //   32: astore 5
    //   34: aload 5
    //   36: invokeinterface 8 1 0
    //   41: ifne +42 -> 83
    //   44: aload 5
    //   46: invokeinterface 10 1 0
    //   51: new 16	java/lang/RuntimeException
    //   54: dup
    //   55: new 20	java/lang/StringBuilder
    //   58: dup
    //   59: invokespecial 21	java/lang/StringBuilder:<init>	()V
    //   62: ldc 31
    //   64: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   67: iload_0
    //   68: invokevirtual 32	java/lang/StringBuilder:append	(I)Ljava/lang/StringBuilder;
    //   71: ldc 33
    //   73: invokevirtual 23	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   76: invokevirtual 26	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   79: invokespecial 27	java/lang/RuntimeException:<init>	(Ljava/lang/String;)V
    //   82: athrow
    //   83: aload 5
    //   85: ldc 34
    //   87: invokeinterface 35 2 0
    //   92: lstore 6
    //   94: aload 5
    //   96: invokeinterface 10 1 0
    //   101: lload 6
    //   103: lstore 8
    //   105: aload_3
    //   106: ifnull +35 -> 141
    //   109: aload 4
    //   111: ifnull +24 -> 135
    //   114: aload_3
    //   115: invokeinterface 11 1 0
    //   120: goto +21 -> 141
    //   123: astore 10
    //   125: aload 4
    //   127: aload 10
    //   129: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   132: goto +9 -> 141
    //   135: aload_3
    //   136: invokeinterface 11 1 0
    //   141: aload_1
    //   142: ifnull +33 -> 175
    //   145: aload_2
    //   146: ifnull +23 -> 169
    //   149: aload_1
    //   150: invokeinterface 14 1 0
    //   155: goto +20 -> 175
    //   158: astore 10
    //   160: aload_2
    //   161: aload 10
    //   163: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   166: goto +9 -> 175
    //   169: aload_1
    //   170: invokeinterface 14 1 0
    //   175: lload 8
    //   177: lreturn
    //   178: astore 5
    //   180: aload 5
    //   182: astore 4
    //   184: aload 5
    //   186: athrow
    //   187: astore 11
    //   189: aload_3
    //   190: ifnull +35 -> 225
    //   193: aload 4
    //   195: ifnull +24 -> 219
    //   198: aload_3
    //   199: invokeinterface 11 1 0
    //   204: goto +21 -> 225
    //   207: astore 12
    //   209: aload 4
    //   211: aload 12
    //   213: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   216: goto +9 -> 225
    //   219: aload_3
    //   220: invokeinterface 11 1 0
    //   225: aload 11
    //   227: athrow
    //   228: astore_3
    //   229: aload_3
    //   230: astore_2
    //   231: aload_3
    //   232: athrow
    //   233: astore 13
    //   235: aload_1
    //   236: ifnull +33 -> 269
    //   239: aload_2
    //   240: ifnull +23 -> 263
    //   243: aload_1
    //   244: invokeinterface 14 1 0
    //   249: goto +20 -> 269
    //   252: astore 14
    //   254: aload_2
    //   255: aload 14
    //   257: invokevirtual 13	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   260: goto +9 -> 269
    //   263: aload_1
    //   264: invokeinterface 14 1 0
    //   269: aload 13
    //   271: athrow
    //   272: astore_1
    //   273: new 16	java/lang/RuntimeException
    //   276: dup
    //   277: aload_1
    //   278: invokevirtual 17	java/sql/SQLException:getMessage	()Ljava/lang/String;
    //   281: aload_1
    //   282: invokespecial 18	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   285: athrow
    // Line number table:
    //   Java source line #44	-> byte code offset #0
    //   Java source line #45	-> byte code offset #6
    //   Java source line #44	-> byte code offset #15
    //   Java source line #46	-> byte code offset #18
    //   Java source line #47	-> byte code offset #26
    //   Java source line #48	-> byte code offset #34
    //   Java source line #49	-> byte code offset #44
    //   Java source line #50	-> byte code offset #51
    //   Java source line #52	-> byte code offset #83
    //   Java source line #53	-> byte code offset #94
    //   Java source line #54	-> byte code offset #101
    //   Java source line #55	-> byte code offset #105
    //   Java source line #44	-> byte code offset #178
    //   Java source line #55	-> byte code offset #187
    //   Java source line #44	-> byte code offset #228
    //   Java source line #55	-> byte code offset #233
    //   Java source line #56	-> byte code offset #273
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	286	0	paramInt	int
    //   3	261	1	localConnection	Connection
    //   272	10	1	localSQLException	SQLException
    //   5	250	2	localObject1	Object
    //   14	206	3	localPreparedStatement	PreparedStatement
    //   228	4	3	localThrowable1	Throwable
    //   16	194	4	localObject2	Object
    //   32	63	5	localResultSet	ResultSet
    //   178	7	5	localThrowable2	Throwable
    //   92	10	6	l1	long
    //   123	5	10	localThrowable3	Throwable
    //   158	4	10	localThrowable4	Throwable
    //   187	39	11	localObject3	Object
    //   207	5	12	localThrowable5	Throwable
    //   233	37	13	localObject4	Object
    //   252	4	14	localThrowable6	Throwable
    // Exception table:
    //   from	to	target	type
    //   114	120	123	java/lang/Throwable
    //   149	155	158	java/lang/Throwable
    //   18	105	178	java/lang/Throwable
    //   18	105	187	finally
    //   178	189	187	finally
    //   198	204	207	java/lang/Throwable
    //   6	141	228	java/lang/Throwable
    //   178	228	228	java/lang/Throwable
    //   6	141	233	finally
    //   178	235	233	finally
    //   243	249	252	java/lang/Throwable
    //   0	175	272	java/sql/SQLException
    //   178	272	272	java/sql/SQLException
  }
  
  static BlockImpl findBlock(Connection paramConnection, ResultSet paramResultSet)
    throws NxtException.ValidationException
  {
    try
    {
      int i = paramResultSet.getInt("version");
      int j = paramResultSet.getInt("timestamp");
      Long localLong1 = Long.valueOf(paramResultSet.getLong("previous_block_id"));
      if (paramResultSet.wasNull()) {
        localLong1 = null;
      }
      int k = paramResultSet.getInt("total_amount");
      int m = paramResultSet.getInt("total_fee");
      int n = paramResultSet.getInt("payload_length");
      byte[] arrayOfByte1 = paramResultSet.getBytes("generator_public_key");
      byte[] arrayOfByte2 = paramResultSet.getBytes("previous_block_hash");
      BigInteger localBigInteger = new BigInteger(paramResultSet.getBytes("cumulative_difficulty"));
      long l = paramResultSet.getLong("base_target");
      Long localLong2 = Long.valueOf(paramResultSet.getLong("next_block_id"));
      if (paramResultSet.wasNull()) {
        localLong2 = null;
      }
      int i1 = paramResultSet.getInt("height");
      byte[] arrayOfByte3 = paramResultSet.getBytes("generation_signature");
      byte[] arrayOfByte4 = paramResultSet.getBytes("block_signature");
      byte[] arrayOfByte5 = paramResultSet.getBytes("payload_hash");
      
      Long localLong3 = Long.valueOf(paramResultSet.getLong("id"));
      List localList = TransactionDb.findBlockTransactions(paramConnection, localLong3);
      
      BlockImpl localBlockImpl = new BlockImpl(i, j, localLong1, k, m, n, arrayOfByte5, arrayOfByte1, arrayOfByte3, arrayOfByte4, arrayOfByte2, localList, localBigInteger, l, localLong2, i1, localLong3);
      for (TransactionImpl localTransactionImpl : localList) {
        localTransactionImpl.setBlock(localBlockImpl);
      }
      return localBlockImpl;
    }
    catch (SQLException localSQLException)
    {
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  static void saveBlock(Connection paramConnection, BlockImpl paramBlockImpl)
  {
    try
    {
      PreparedStatement localPreparedStatement = paramConnection.prepareStatement("INSERT INTO block (id, version, timestamp, previous_block_id, total_amount, total_fee, payload_length, generator_public_key, previous_block_hash, cumulative_difficulty, base_target, next_block_id, height, generation_signature, block_signature, payload_hash, generator_id)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");Object localObject1 = null;
      try
      {
        int i = 0;
        localPreparedStatement.setLong(++i, paramBlockImpl.getId().longValue());
        localPreparedStatement.setInt(++i, paramBlockImpl.getVersion());
        localPreparedStatement.setInt(++i, paramBlockImpl.getTimestamp());
        if (paramBlockImpl.getPreviousBlockId() != null) {
          localPreparedStatement.setLong(++i, paramBlockImpl.getPreviousBlockId().longValue());
        } else {
          localPreparedStatement.setNull(++i, -5);
        }
        localPreparedStatement.setInt(++i, paramBlockImpl.getTotalAmount());
        localPreparedStatement.setInt(++i, paramBlockImpl.getTotalFee());
        localPreparedStatement.setInt(++i, paramBlockImpl.getPayloadLength());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getGeneratorPublicKey());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getPreviousBlockHash());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getCumulativeDifficulty().toByteArray());
        localPreparedStatement.setLong(++i, paramBlockImpl.getBaseTarget());
        if (paramBlockImpl.getNextBlockId() != null) {
          localPreparedStatement.setLong(++i, paramBlockImpl.getNextBlockId().longValue());
        } else {
          localPreparedStatement.setNull(++i, -5);
        }
        localPreparedStatement.setInt(++i, paramBlockImpl.getHeight());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getGenerationSignature());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getBlockSignature());
        localPreparedStatement.setBytes(++i, paramBlockImpl.getPayloadHash());
        localPreparedStatement.setLong(++i, paramBlockImpl.getGeneratorId().longValue());
        localPreparedStatement.executeUpdate();
        TransactionDb.saveTransactions(paramConnection, paramBlockImpl.getTransactions());
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
            catch (Throwable localThrowable5)
            {
              localObject1.addSuppressed(localThrowable5);
            }
          } else {
            localPreparedStatement.close();
          }
        }
      }
      if (paramBlockImpl.getPreviousBlockId() != null)
      {
        localPreparedStatement = paramConnection.prepareStatement("UPDATE block SET next_block_id = ? WHERE id = ?");localObject1 = null;
        try
        {
          localPreparedStatement.setLong(1, paramBlockImpl.getId().longValue());
          localPreparedStatement.setLong(2, paramBlockImpl.getPreviousBlockId().longValue());
          localPreparedStatement.executeUpdate();
        }
        catch (Throwable localThrowable4)
        {
          localObject1 = localThrowable4;throw localThrowable4;
        }
        finally
        {
          if (localPreparedStatement != null) {
            if (localObject1 != null) {
              try
              {
                localPreparedStatement.close();
              }
              catch (Throwable localThrowable6)
              {
                localObject1.addSuppressed(localThrowable6);
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
  
  static void deleteBlock(Long paramLong)
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        PreparedStatement localPreparedStatement = localConnection.prepareStatement("DELETE FROM block WHERE id = ?");Object localObject2 = null;
        try
        {
          try
          {
            localPreparedStatement.setLong(1, paramLong.longValue());
            localPreparedStatement.executeUpdate();
            localConnection.commit();
          }
          catch (SQLException localSQLException2)
          {
            localConnection.rollback();
            throw localSQLException2;
          }
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localConnection != null) {
          if (localObject1 != null) {
            try
            {
              localConnection.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localConnection.close();
          }
        }
      }
    }
    catch (SQLException localSQLException1)
    {
      throw new RuntimeException(localSQLException1.toString(), localSQLException1);
    }
  }
  
  static void deleteAll()
  {
    try
    {
      Connection localConnection = Db.getConnection();Object localObject1 = null;
      try
      {
        Statement localStatement = localConnection.createStatement();Object localObject2 = null;
        try
        {
          try
          {
            localStatement.executeUpdate("SET REFERENTIAL_INTEGRITY FALSE");
            localStatement.executeUpdate("TRUNCATE TABLE transaction");
            localStatement.executeUpdate("TRUNCATE TABLE block");
            localStatement.executeUpdate("SET REFERENTIAL_INTEGRITY TRUE");
            localConnection.commit();
          }
          catch (SQLException localSQLException2)
          {
            localConnection.rollback();
            throw localSQLException2;
          }
        }
        catch (Throwable localThrowable4)
        {
          localObject2 = localThrowable4;throw localThrowable4;
        }
        finally {}
      }
      catch (Throwable localThrowable2)
      {
        localObject1 = localThrowable2;throw localThrowable2;
      }
      finally
      {
        if (localConnection != null) {
          if (localObject1 != null) {
            try
            {
              localConnection.close();
            }
            catch (Throwable localThrowable6)
            {
              localObject1.addSuppressed(localThrowable6);
            }
          } else {
            localConnection.close();
          }
        }
      }
    }
    catch (SQLException localSQLException1)
    {
      throw new RuntimeException(localSQLException1.toString(), localSQLException1);
    }
  }
}