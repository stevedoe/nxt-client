package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import nxt.util.DbIterator;
import nxt.util.DbIterator.ResultSetReader;
import nxt.util.DbUtils;

final class BlockchainImpl
  implements Blockchain
{
  private static final BlockchainImpl instance = new BlockchainImpl();
  
  static BlockchainImpl getInstance()
  {
    return instance;
  }
  
  private final AtomicReference<BlockImpl> lastBlock = new AtomicReference();
  
  public BlockImpl getLastBlock()
  {
    return (BlockImpl)this.lastBlock.get();
  }
  
  void setLastBlock(BlockImpl paramBlockImpl)
  {
    this.lastBlock.set(paramBlockImpl);
  }
  
  void setLastBlock(BlockImpl paramBlockImpl1, BlockImpl paramBlockImpl2)
  {
    if (!this.lastBlock.compareAndSet(paramBlockImpl1, paramBlockImpl2)) {
      throw new IllegalStateException("Last block is no longer previous block");
    }
  }
  
  public BlockImpl getBlock(Long paramLong)
  {
    return BlockDb.findBlock(paramLong);
  }
  
  public boolean hasBlock(Long paramLong)
  {
    return BlockDb.hasBlock(paramLong);
  }
  
  /* Error */
  public int getBlockCount()
  {
    // Byte code:
    //   0: invokestatic 15	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_1
    //   4: aconst_null
    //   5: astore_2
    //   6: aload_1
    //   7: ldc 16
    //   9: invokeinterface 17 2 0
    //   14: astore_3
    //   15: aconst_null
    //   16: astore 4
    //   18: aload_3
    //   19: invokeinterface 18 1 0
    //   24: astore 5
    //   26: aload 5
    //   28: invokeinterface 19 1 0
    //   33: pop
    //   34: aload 5
    //   36: iconst_1
    //   37: invokeinterface 20 2 0
    //   42: istore 6
    //   44: aload_3
    //   45: ifnull +35 -> 80
    //   48: aload 4
    //   50: ifnull +24 -> 74
    //   53: aload_3
    //   54: invokeinterface 21 1 0
    //   59: goto +21 -> 80
    //   62: astore 7
    //   64: aload 4
    //   66: aload 7
    //   68: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   71: goto +9 -> 80
    //   74: aload_3
    //   75: invokeinterface 21 1 0
    //   80: aload_1
    //   81: ifnull +33 -> 114
    //   84: aload_2
    //   85: ifnull +23 -> 108
    //   88: aload_1
    //   89: invokeinterface 24 1 0
    //   94: goto +20 -> 114
    //   97: astore 7
    //   99: aload_2
    //   100: aload 7
    //   102: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   105: goto +9 -> 114
    //   108: aload_1
    //   109: invokeinterface 24 1 0
    //   114: iload 6
    //   116: ireturn
    //   117: astore 5
    //   119: aload 5
    //   121: astore 4
    //   123: aload 5
    //   125: athrow
    //   126: astore 8
    //   128: aload_3
    //   129: ifnull +35 -> 164
    //   132: aload 4
    //   134: ifnull +24 -> 158
    //   137: aload_3
    //   138: invokeinterface 21 1 0
    //   143: goto +21 -> 164
    //   146: astore 9
    //   148: aload 4
    //   150: aload 9
    //   152: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   155: goto +9 -> 164
    //   158: aload_3
    //   159: invokeinterface 21 1 0
    //   164: aload 8
    //   166: athrow
    //   167: astore_3
    //   168: aload_3
    //   169: astore_2
    //   170: aload_3
    //   171: athrow
    //   172: astore 10
    //   174: aload_1
    //   175: ifnull +33 -> 208
    //   178: aload_2
    //   179: ifnull +23 -> 202
    //   182: aload_1
    //   183: invokeinterface 24 1 0
    //   188: goto +20 -> 208
    //   191: astore 11
    //   193: aload_2
    //   194: aload 11
    //   196: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   199: goto +9 -> 208
    //   202: aload_1
    //   203: invokeinterface 24 1 0
    //   208: aload 10
    //   210: athrow
    //   211: astore_1
    //   212: new 26	java/lang/RuntimeException
    //   215: dup
    //   216: aload_1
    //   217: invokevirtual 27	java/sql/SQLException:toString	()Ljava/lang/String;
    //   220: aload_1
    //   221: invokespecial 28	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   224: athrow
    // Line number table:
    //   Java source line #54	-> byte code offset #0
    //   Java source line #55	-> byte code offset #18
    //   Java source line #56	-> byte code offset #26
    //   Java source line #57	-> byte code offset #34
    //   Java source line #58	-> byte code offset #44
    //   Java source line #54	-> byte code offset #117
    //   Java source line #58	-> byte code offset #126
    //   Java source line #54	-> byte code offset #167
    //   Java source line #58	-> byte code offset #172
    //   Java source line #59	-> byte code offset #212
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	225	0	this	BlockchainImpl
    //   3	200	1	localConnection	Connection
    //   211	10	1	localSQLException	SQLException
    //   5	189	2	localObject1	Object
    //   14	145	3	localPreparedStatement	PreparedStatement
    //   167	4	3	localThrowable1	java.lang.Throwable
    //   16	133	4	localObject2	Object
    //   24	11	5	localResultSet	ResultSet
    //   117	7	5	localThrowable2	java.lang.Throwable
    //   62	5	7	localThrowable3	java.lang.Throwable
    //   97	4	7	localThrowable4	java.lang.Throwable
    //   126	39	8	localObject3	Object
    //   146	5	9	localThrowable5	java.lang.Throwable
    //   172	37	10	localObject4	Object
    //   191	4	11	localThrowable6	java.lang.Throwable
    // Exception table:
    //   from	to	target	type
    //   53	59	62	java/lang/Throwable
    //   88	94	97	java/lang/Throwable
    //   18	44	117	java/lang/Throwable
    //   18	44	126	finally
    //   117	128	126	finally
    //   137	143	146	java/lang/Throwable
    //   6	80	167	java/lang/Throwable
    //   117	167	167	java/lang/Throwable
    //   6	80	172	finally
    //   117	174	172	finally
    //   182	188	191	java/lang/Throwable
    //   0	114	211	java/sql/SQLException
    //   117	211	211	java/sql/SQLException
  }
  
  public DbIterator<BlockImpl> getAllBlocks()
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block ORDER BY db_id ASC");
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public BlockImpl get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return BlockDb.findBlock(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  public DbIterator<BlockImpl> getAllBlocks(Account paramAccount, int paramInt)
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM block WHERE timestamp >= ? AND generator_id = ? ORDER BY db_id ASC");
      localPreparedStatement.setInt(1, paramInt);
      localPreparedStatement.setLong(2, paramAccount.getId().longValue());
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public BlockImpl get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return BlockDb.findBlock(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  /* Error */
  public java.util.List<Long> getBlockIdsAfter(Long paramLong, int paramInt)
  {
    // Byte code:
    //   0: iload_2
    //   1: sipush 1440
    //   4: if_icmple +13 -> 17
    //   7: new 43	java/lang/IllegalArgumentException
    //   10: dup
    //   11: ldc 44
    //   13: invokespecial 45	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   16: athrow
    //   17: invokestatic 15	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   20: astore_3
    //   21: aconst_null
    //   22: astore 4
    //   24: aload_3
    //   25: ldc 46
    //   27: invokeinterface 17 2 0
    //   32: astore 5
    //   34: aconst_null
    //   35: astore 6
    //   37: aload_3
    //   38: ldc 47
    //   40: invokeinterface 17 2 0
    //   45: astore 7
    //   47: aconst_null
    //   48: astore 8
    //   50: aload 5
    //   52: iconst_1
    //   53: aload_1
    //   54: invokevirtual 39	java/lang/Long:longValue	()J
    //   57: invokeinterface 40 4 0
    //   62: aload 5
    //   64: invokeinterface 18 1 0
    //   69: astore 9
    //   71: aload 9
    //   73: invokeinterface 19 1 0
    //   78: ifne +132 -> 210
    //   81: aload 9
    //   83: invokeinterface 48 1 0
    //   88: invokestatic 49	java/util/Collections:emptyList	()Ljava/util/List;
    //   91: astore 10
    //   93: aload 7
    //   95: ifnull +37 -> 132
    //   98: aload 8
    //   100: ifnull +25 -> 125
    //   103: aload 7
    //   105: invokeinterface 21 1 0
    //   110: goto +22 -> 132
    //   113: astore 11
    //   115: aload 8
    //   117: aload 11
    //   119: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   122: goto +10 -> 132
    //   125: aload 7
    //   127: invokeinterface 21 1 0
    //   132: aload 5
    //   134: ifnull +37 -> 171
    //   137: aload 6
    //   139: ifnull +25 -> 164
    //   142: aload 5
    //   144: invokeinterface 21 1 0
    //   149: goto +22 -> 171
    //   152: astore 11
    //   154: aload 6
    //   156: aload 11
    //   158: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   161: goto +10 -> 171
    //   164: aload 5
    //   166: invokeinterface 21 1 0
    //   171: aload_3
    //   172: ifnull +35 -> 207
    //   175: aload 4
    //   177: ifnull +24 -> 201
    //   180: aload_3
    //   181: invokeinterface 24 1 0
    //   186: goto +21 -> 207
    //   189: astore 11
    //   191: aload 4
    //   193: aload 11
    //   195: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   198: goto +9 -> 207
    //   201: aload_3
    //   202: invokeinterface 24 1 0
    //   207: aload 10
    //   209: areturn
    //   210: new 50	java/util/ArrayList
    //   213: dup
    //   214: invokespecial 51	java/util/ArrayList:<init>	()V
    //   217: astore 10
    //   219: aload 9
    //   221: ldc 52
    //   223: invokeinterface 53 2 0
    //   228: istore 11
    //   230: aload 7
    //   232: iconst_1
    //   233: iload 11
    //   235: invokeinterface 37 3 0
    //   240: aload 7
    //   242: iconst_2
    //   243: iload_2
    //   244: invokeinterface 37 3 0
    //   249: aload 7
    //   251: invokeinterface 18 1 0
    //   256: astore 9
    //   258: aload 9
    //   260: invokeinterface 19 1 0
    //   265: ifeq +26 -> 291
    //   268: aload 10
    //   270: aload 9
    //   272: ldc 54
    //   274: invokeinterface 55 2 0
    //   279: invokestatic 56	java/lang/Long:valueOf	(J)Ljava/lang/Long;
    //   282: invokeinterface 57 2 0
    //   287: pop
    //   288: goto -30 -> 258
    //   291: aload 9
    //   293: invokeinterface 48 1 0
    //   298: aload 10
    //   300: astore 12
    //   302: aload 7
    //   304: ifnull +37 -> 341
    //   307: aload 8
    //   309: ifnull +25 -> 334
    //   312: aload 7
    //   314: invokeinterface 21 1 0
    //   319: goto +22 -> 341
    //   322: astore 13
    //   324: aload 8
    //   326: aload 13
    //   328: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   331: goto +10 -> 341
    //   334: aload 7
    //   336: invokeinterface 21 1 0
    //   341: aload 5
    //   343: ifnull +37 -> 380
    //   346: aload 6
    //   348: ifnull +25 -> 373
    //   351: aload 5
    //   353: invokeinterface 21 1 0
    //   358: goto +22 -> 380
    //   361: astore 13
    //   363: aload 6
    //   365: aload 13
    //   367: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   370: goto +10 -> 380
    //   373: aload 5
    //   375: invokeinterface 21 1 0
    //   380: aload_3
    //   381: ifnull +35 -> 416
    //   384: aload 4
    //   386: ifnull +24 -> 410
    //   389: aload_3
    //   390: invokeinterface 24 1 0
    //   395: goto +21 -> 416
    //   398: astore 13
    //   400: aload 4
    //   402: aload 13
    //   404: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   407: goto +9 -> 416
    //   410: aload_3
    //   411: invokeinterface 24 1 0
    //   416: aload 12
    //   418: areturn
    //   419: astore 9
    //   421: aload 9
    //   423: astore 8
    //   425: aload 9
    //   427: athrow
    //   428: astore 14
    //   430: aload 7
    //   432: ifnull +37 -> 469
    //   435: aload 8
    //   437: ifnull +25 -> 462
    //   440: aload 7
    //   442: invokeinterface 21 1 0
    //   447: goto +22 -> 469
    //   450: astore 15
    //   452: aload 8
    //   454: aload 15
    //   456: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   459: goto +10 -> 469
    //   462: aload 7
    //   464: invokeinterface 21 1 0
    //   469: aload 14
    //   471: athrow
    //   472: astore 7
    //   474: aload 7
    //   476: astore 6
    //   478: aload 7
    //   480: athrow
    //   481: astore 16
    //   483: aload 5
    //   485: ifnull +37 -> 522
    //   488: aload 6
    //   490: ifnull +25 -> 515
    //   493: aload 5
    //   495: invokeinterface 21 1 0
    //   500: goto +22 -> 522
    //   503: astore 17
    //   505: aload 6
    //   507: aload 17
    //   509: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   512: goto +10 -> 522
    //   515: aload 5
    //   517: invokeinterface 21 1 0
    //   522: aload 16
    //   524: athrow
    //   525: astore 5
    //   527: aload 5
    //   529: astore 4
    //   531: aload 5
    //   533: athrow
    //   534: astore 18
    //   536: aload_3
    //   537: ifnull +35 -> 572
    //   540: aload 4
    //   542: ifnull +24 -> 566
    //   545: aload_3
    //   546: invokeinterface 24 1 0
    //   551: goto +21 -> 572
    //   554: astore 19
    //   556: aload 4
    //   558: aload 19
    //   560: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   563: goto +9 -> 572
    //   566: aload_3
    //   567: invokeinterface 24 1 0
    //   572: aload 18
    //   574: athrow
    //   575: astore_3
    //   576: new 26	java/lang/RuntimeException
    //   579: dup
    //   580: aload_3
    //   581: invokevirtual 27	java/sql/SQLException:toString	()Ljava/lang/String;
    //   584: aload_3
    //   585: invokespecial 28	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   588: athrow
    // Line number table:
    //   Java source line #103	-> byte code offset #0
    //   Java source line #104	-> byte code offset #7
    //   Java source line #106	-> byte code offset #17
    //   Java source line #107	-> byte code offset #24
    //   Java source line #106	-> byte code offset #34
    //   Java source line #108	-> byte code offset #37
    //   Java source line #106	-> byte code offset #47
    //   Java source line #109	-> byte code offset #50
    //   Java source line #110	-> byte code offset #62
    //   Java source line #111	-> byte code offset #71
    //   Java source line #112	-> byte code offset #81
    //   Java source line #113	-> byte code offset #88
    //   Java source line #125	-> byte code offset #93
    //   Java source line #115	-> byte code offset #210
    //   Java source line #116	-> byte code offset #219
    //   Java source line #117	-> byte code offset #230
    //   Java source line #118	-> byte code offset #240
    //   Java source line #119	-> byte code offset #249
    //   Java source line #120	-> byte code offset #258
    //   Java source line #121	-> byte code offset #268
    //   Java source line #123	-> byte code offset #291
    //   Java source line #124	-> byte code offset #298
    //   Java source line #125	-> byte code offset #302
    //   Java source line #106	-> byte code offset #419
    //   Java source line #125	-> byte code offset #428
    //   Java source line #106	-> byte code offset #472
    //   Java source line #125	-> byte code offset #481
    //   Java source line #106	-> byte code offset #525
    //   Java source line #125	-> byte code offset #534
    //   Java source line #126	-> byte code offset #576
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	589	0	this	BlockchainImpl
    //   0	589	1	paramLong	Long
    //   0	589	2	paramInt	int
    //   20	547	3	localConnection	Connection
    //   575	10	3	localSQLException	SQLException
    //   22	535	4	localObject1	Object
    //   32	484	5	localPreparedStatement1	PreparedStatement
    //   525	7	5	localThrowable1	java.lang.Throwable
    //   35	471	6	localObject2	Object
    //   45	418	7	localPreparedStatement2	PreparedStatement
    //   472	7	7	localThrowable2	java.lang.Throwable
    //   48	405	8	localObject3	Object
    //   69	223	9	localResultSet	ResultSet
    //   419	7	9	localThrowable3	java.lang.Throwable
    //   91	208	10	localObject4	Object
    //   113	5	11	localThrowable4	java.lang.Throwable
    //   152	5	11	localThrowable5	java.lang.Throwable
    //   189	5	11	localThrowable6	java.lang.Throwable
    //   228	6	11	i	int
    //   300	117	12	localObject5	Object
    //   322	5	13	localThrowable7	java.lang.Throwable
    //   361	5	13	localThrowable8	java.lang.Throwable
    //   398	5	13	localThrowable9	java.lang.Throwable
    //   428	42	14	localObject6	Object
    //   450	5	15	localThrowable10	java.lang.Throwable
    //   481	42	16	localObject7	Object
    //   503	5	17	localThrowable11	java.lang.Throwable
    //   534	39	18	localObject8	Object
    //   554	5	19	localThrowable12	java.lang.Throwable
    // Exception table:
    //   from	to	target	type
    //   103	110	113	java/lang/Throwable
    //   142	149	152	java/lang/Throwable
    //   180	186	189	java/lang/Throwable
    //   312	319	322	java/lang/Throwable
    //   351	358	361	java/lang/Throwable
    //   389	395	398	java/lang/Throwable
    //   50	93	419	java/lang/Throwable
    //   210	302	419	java/lang/Throwable
    //   50	93	428	finally
    //   210	302	428	finally
    //   419	430	428	finally
    //   440	447	450	java/lang/Throwable
    //   37	132	472	java/lang/Throwable
    //   210	341	472	java/lang/Throwable
    //   419	472	472	java/lang/Throwable
    //   37	132	481	finally
    //   210	341	481	finally
    //   419	483	481	finally
    //   493	500	503	java/lang/Throwable
    //   24	171	525	java/lang/Throwable
    //   210	380	525	java/lang/Throwable
    //   419	525	525	java/lang/Throwable
    //   24	171	534	finally
    //   210	380	534	finally
    //   419	536	534	finally
    //   545	551	554	java/lang/Throwable
    //   17	207	575	java/sql/SQLException
    //   210	416	575	java/sql/SQLException
    //   419	575	575	java/sql/SQLException
  }
  
  /* Error */
  public java.util.List<BlockImpl> getBlocksAfter(Long paramLong, int paramInt)
  {
    // Byte code:
    //   0: iload_2
    //   1: sipush 1440
    //   4: if_icmple +13 -> 17
    //   7: new 43	java/lang/IllegalArgumentException
    //   10: dup
    //   11: ldc 44
    //   13: invokespecial 45	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   16: athrow
    //   17: invokestatic 15	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   20: astore_3
    //   21: aconst_null
    //   22: astore 4
    //   24: aload_3
    //   25: ldc 58
    //   27: invokeinterface 17 2 0
    //   32: astore 5
    //   34: aconst_null
    //   35: astore 6
    //   37: new 50	java/util/ArrayList
    //   40: dup
    //   41: invokespecial 51	java/util/ArrayList:<init>	()V
    //   44: astore 7
    //   46: aload 5
    //   48: iconst_1
    //   49: aload_1
    //   50: invokevirtual 39	java/lang/Long:longValue	()J
    //   53: invokeinterface 40 4 0
    //   58: aload 5
    //   60: iconst_2
    //   61: iload_2
    //   62: invokeinterface 37 3 0
    //   67: aload 5
    //   69: invokeinterface 18 1 0
    //   74: astore 8
    //   76: aload 8
    //   78: invokeinterface 19 1 0
    //   83: ifeq +20 -> 103
    //   86: aload 7
    //   88: aload_3
    //   89: aload 8
    //   91: invokestatic 59	nxt/BlockDb:findBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/BlockImpl;
    //   94: invokeinterface 57 2 0
    //   99: pop
    //   100: goto -24 -> 76
    //   103: aload 8
    //   105: invokeinterface 48 1 0
    //   110: aload 7
    //   112: astore 9
    //   114: aload 5
    //   116: ifnull +37 -> 153
    //   119: aload 6
    //   121: ifnull +25 -> 146
    //   124: aload 5
    //   126: invokeinterface 21 1 0
    //   131: goto +22 -> 153
    //   134: astore 10
    //   136: aload 6
    //   138: aload 10
    //   140: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   143: goto +10 -> 153
    //   146: aload 5
    //   148: invokeinterface 21 1 0
    //   153: aload_3
    //   154: ifnull +35 -> 189
    //   157: aload 4
    //   159: ifnull +24 -> 183
    //   162: aload_3
    //   163: invokeinterface 24 1 0
    //   168: goto +21 -> 189
    //   171: astore 10
    //   173: aload 4
    //   175: aload 10
    //   177: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   180: goto +9 -> 189
    //   183: aload_3
    //   184: invokeinterface 24 1 0
    //   189: aload 9
    //   191: areturn
    //   192: astore 7
    //   194: aload 7
    //   196: astore 6
    //   198: aload 7
    //   200: athrow
    //   201: astore 11
    //   203: aload 5
    //   205: ifnull +37 -> 242
    //   208: aload 6
    //   210: ifnull +25 -> 235
    //   213: aload 5
    //   215: invokeinterface 21 1 0
    //   220: goto +22 -> 242
    //   223: astore 12
    //   225: aload 6
    //   227: aload 12
    //   229: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   232: goto +10 -> 242
    //   235: aload 5
    //   237: invokeinterface 21 1 0
    //   242: aload 11
    //   244: athrow
    //   245: astore 5
    //   247: aload 5
    //   249: astore 4
    //   251: aload 5
    //   253: athrow
    //   254: astore 13
    //   256: aload_3
    //   257: ifnull +35 -> 292
    //   260: aload 4
    //   262: ifnull +24 -> 286
    //   265: aload_3
    //   266: invokeinterface 24 1 0
    //   271: goto +21 -> 292
    //   274: astore 14
    //   276: aload 4
    //   278: aload 14
    //   280: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   283: goto +9 -> 292
    //   286: aload_3
    //   287: invokeinterface 24 1 0
    //   292: aload 13
    //   294: athrow
    //   295: astore_3
    //   296: new 26	java/lang/RuntimeException
    //   299: dup
    //   300: aload_3
    //   301: invokevirtual 61	java/lang/Exception:toString	()Ljava/lang/String;
    //   304: aload_3
    //   305: invokespecial 28	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   308: athrow
    // Line number table:
    //   Java source line #132	-> byte code offset #0
    //   Java source line #133	-> byte code offset #7
    //   Java source line #135	-> byte code offset #17
    //   Java source line #136	-> byte code offset #24
    //   Java source line #135	-> byte code offset #34
    //   Java source line #137	-> byte code offset #37
    //   Java source line #138	-> byte code offset #46
    //   Java source line #139	-> byte code offset #58
    //   Java source line #140	-> byte code offset #67
    //   Java source line #141	-> byte code offset #76
    //   Java source line #142	-> byte code offset #86
    //   Java source line #144	-> byte code offset #103
    //   Java source line #145	-> byte code offset #110
    //   Java source line #146	-> byte code offset #114
    //   Java source line #135	-> byte code offset #192
    //   Java source line #146	-> byte code offset #201
    //   Java source line #135	-> byte code offset #245
    //   Java source line #146	-> byte code offset #254
    //   Java source line #147	-> byte code offset #296
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	309	0	this	BlockchainImpl
    //   0	309	1	paramLong	Long
    //   0	309	2	paramInt	int
    //   20	267	3	localConnection	Connection
    //   295	10	3	localValidationException	NxtException.ValidationException
    //   22	255	4	localObject1	Object
    //   32	204	5	localPreparedStatement	PreparedStatement
    //   245	7	5	localThrowable1	java.lang.Throwable
    //   35	191	6	localObject2	Object
    //   44	67	7	localArrayList1	java.util.ArrayList
    //   192	7	7	localThrowable2	java.lang.Throwable
    //   74	30	8	localResultSet	ResultSet
    //   134	5	10	localThrowable3	java.lang.Throwable
    //   171	5	10	localThrowable4	java.lang.Throwable
    //   201	42	11	localObject3	Object
    //   223	5	12	localThrowable5	java.lang.Throwable
    //   254	39	13	localObject4	Object
    //   274	5	14	localThrowable6	java.lang.Throwable
    // Exception table:
    //   from	to	target	type
    //   124	131	134	java/lang/Throwable
    //   162	168	171	java/lang/Throwable
    //   37	114	192	java/lang/Throwable
    //   37	114	201	finally
    //   192	203	201	finally
    //   213	220	223	java/lang/Throwable
    //   24	153	245	java/lang/Throwable
    //   192	245	245	java/lang/Throwable
    //   24	153	254	finally
    //   192	256	254	finally
    //   265	271	274	java/lang/Throwable
    //   17	189	295	nxt/NxtException$ValidationException
    //   17	189	295	java/sql/SQLException
    //   192	295	295	nxt/NxtException$ValidationException
    //   192	295	295	java/sql/SQLException
  }
  
  public long getBlockIdAtHeight(int paramInt)
  {
    Block localBlock = (Block)this.lastBlock.get();
    if (paramInt > localBlock.getHeight()) {
      throw new IllegalArgumentException("Invalid height " + paramInt + ", current blockchain is at " + localBlock.getHeight());
    }
    if (paramInt == localBlock.getHeight()) {
      return localBlock.getId().longValue();
    }
    return BlockDb.findBlockIdAtHeight(paramInt);
  }
  
  /* Error */
  public java.util.List<BlockImpl> getBlocksFromHeight(int paramInt)
  {
    // Byte code:
    //   0: iload_1
    //   1: iflt +24 -> 25
    //   4: aload_0
    //   5: getfield 5	nxt/BlockchainImpl:lastBlock	Ljava/util/concurrent/atomic/AtomicReference;
    //   8: invokevirtual 6	java/util/concurrent/atomic/AtomicReference:get	()Ljava/lang/Object;
    //   11: checkcast 7	nxt/BlockImpl
    //   14: invokevirtual 73	nxt/BlockImpl:getHeight	()I
    //   17: iload_1
    //   18: isub
    //   19: sipush 1440
    //   22: if_icmple +13 -> 35
    //   25: new 43	java/lang/IllegalArgumentException
    //   28: dup
    //   29: ldc 74
    //   31: invokespecial 45	java/lang/IllegalArgumentException:<init>	(Ljava/lang/String;)V
    //   34: athrow
    //   35: invokestatic 15	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   38: astore_2
    //   39: aconst_null
    //   40: astore_3
    //   41: aload_2
    //   42: ldc 75
    //   44: invokeinterface 17 2 0
    //   49: astore 4
    //   51: aconst_null
    //   52: astore 5
    //   54: aload 4
    //   56: iconst_1
    //   57: iload_1
    //   58: invokeinterface 37 3 0
    //   63: aload 4
    //   65: invokeinterface 18 1 0
    //   70: astore 6
    //   72: new 50	java/util/ArrayList
    //   75: dup
    //   76: invokespecial 51	java/util/ArrayList:<init>	()V
    //   79: astore 7
    //   81: aload 6
    //   83: invokeinterface 19 1 0
    //   88: ifeq +20 -> 108
    //   91: aload 7
    //   93: aload_2
    //   94: aload 6
    //   96: invokestatic 59	nxt/BlockDb:findBlock	(Ljava/sql/Connection;Ljava/sql/ResultSet;)Lnxt/BlockImpl;
    //   99: invokeinterface 57 2 0
    //   104: pop
    //   105: goto -24 -> 81
    //   108: aload 7
    //   110: astore 8
    //   112: aload 4
    //   114: ifnull +37 -> 151
    //   117: aload 5
    //   119: ifnull +25 -> 144
    //   122: aload 4
    //   124: invokeinterface 21 1 0
    //   129: goto +22 -> 151
    //   132: astore 9
    //   134: aload 5
    //   136: aload 9
    //   138: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   141: goto +10 -> 151
    //   144: aload 4
    //   146: invokeinterface 21 1 0
    //   151: aload_2
    //   152: ifnull +33 -> 185
    //   155: aload_3
    //   156: ifnull +23 -> 179
    //   159: aload_2
    //   160: invokeinterface 24 1 0
    //   165: goto +20 -> 185
    //   168: astore 9
    //   170: aload_3
    //   171: aload 9
    //   173: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   176: goto +9 -> 185
    //   179: aload_2
    //   180: invokeinterface 24 1 0
    //   185: aload 8
    //   187: areturn
    //   188: astore 6
    //   190: aload 6
    //   192: astore 5
    //   194: aload 6
    //   196: athrow
    //   197: astore 10
    //   199: aload 4
    //   201: ifnull +37 -> 238
    //   204: aload 5
    //   206: ifnull +25 -> 231
    //   209: aload 4
    //   211: invokeinterface 21 1 0
    //   216: goto +22 -> 238
    //   219: astore 11
    //   221: aload 5
    //   223: aload 11
    //   225: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   228: goto +10 -> 238
    //   231: aload 4
    //   233: invokeinterface 21 1 0
    //   238: aload 10
    //   240: athrow
    //   241: astore 4
    //   243: aload 4
    //   245: astore_3
    //   246: aload 4
    //   248: athrow
    //   249: astore 12
    //   251: aload_2
    //   252: ifnull +33 -> 285
    //   255: aload_3
    //   256: ifnull +23 -> 279
    //   259: aload_2
    //   260: invokeinterface 24 1 0
    //   265: goto +20 -> 285
    //   268: astore 13
    //   270: aload_3
    //   271: aload 13
    //   273: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   276: goto +9 -> 285
    //   279: aload_2
    //   280: invokeinterface 24 1 0
    //   285: aload 12
    //   287: athrow
    //   288: astore_2
    //   289: new 26	java/lang/RuntimeException
    //   292: dup
    //   293: aload_2
    //   294: invokevirtual 61	java/lang/Exception:toString	()Ljava/lang/String;
    //   297: aload_2
    //   298: invokespecial 28	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   301: athrow
    // Line number table:
    //   Java source line #165	-> byte code offset #0
    //   Java source line #166	-> byte code offset #25
    //   Java source line #168	-> byte code offset #35
    //   Java source line #169	-> byte code offset #41
    //   Java source line #168	-> byte code offset #51
    //   Java source line #170	-> byte code offset #54
    //   Java source line #171	-> byte code offset #63
    //   Java source line #172	-> byte code offset #72
    //   Java source line #173	-> byte code offset #81
    //   Java source line #174	-> byte code offset #91
    //   Java source line #176	-> byte code offset #108
    //   Java source line #177	-> byte code offset #112
    //   Java source line #168	-> byte code offset #188
    //   Java source line #177	-> byte code offset #197
    //   Java source line #168	-> byte code offset #241
    //   Java source line #177	-> byte code offset #249
    //   Java source line #178	-> byte code offset #289
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	302	0	this	BlockchainImpl
    //   0	302	1	paramInt	int
    //   38	242	2	localConnection	Connection
    //   288	10	2	localSQLException	SQLException
    //   40	231	3	localObject1	Object
    //   49	183	4	localPreparedStatement	PreparedStatement
    //   241	6	4	localThrowable1	java.lang.Throwable
    //   52	170	5	localObject2	Object
    //   70	25	6	localResultSet	ResultSet
    //   188	7	6	localThrowable2	java.lang.Throwable
    //   79	30	7	localArrayList1	java.util.ArrayList
    //   132	5	9	localThrowable3	java.lang.Throwable
    //   168	4	9	localThrowable4	java.lang.Throwable
    //   197	42	10	localObject3	Object
    //   219	5	11	localThrowable5	java.lang.Throwable
    //   249	37	12	localObject4	Object
    //   268	4	13	localThrowable6	java.lang.Throwable
    // Exception table:
    //   from	to	target	type
    //   122	129	132	java/lang/Throwable
    //   159	165	168	java/lang/Throwable
    //   54	112	188	java/lang/Throwable
    //   54	112	197	finally
    //   188	199	197	finally
    //   209	216	219	java/lang/Throwable
    //   41	151	241	java/lang/Throwable
    //   188	241	241	java/lang/Throwable
    //   41	151	249	finally
    //   188	251	249	finally
    //   259	265	268	java/lang/Throwable
    //   35	185	288	java/sql/SQLException
    //   35	185	288	nxt/NxtException$ValidationException
    //   188	288	288	java/sql/SQLException
    //   188	288	288	nxt/NxtException$ValidationException
  }
  
  public Transaction getTransaction(Long paramLong)
  {
    return TransactionDb.findTransaction(paramLong);
  }
  
  public boolean hasTransaction(Long paramLong)
  {
    return TransactionDb.hasTransaction(paramLong);
  }
  
  /* Error */
  public int getTransactionCount()
  {
    // Byte code:
    //   0: invokestatic 15	nxt/Db:getConnection	()Ljava/sql/Connection;
    //   3: astore_1
    //   4: aconst_null
    //   5: astore_2
    //   6: aload_1
    //   7: ldc 78
    //   9: invokeinterface 17 2 0
    //   14: astore_3
    //   15: aconst_null
    //   16: astore 4
    //   18: aload_3
    //   19: invokeinterface 18 1 0
    //   24: astore 5
    //   26: aload 5
    //   28: invokeinterface 19 1 0
    //   33: pop
    //   34: aload 5
    //   36: iconst_1
    //   37: invokeinterface 20 2 0
    //   42: istore 6
    //   44: aload_3
    //   45: ifnull +35 -> 80
    //   48: aload 4
    //   50: ifnull +24 -> 74
    //   53: aload_3
    //   54: invokeinterface 21 1 0
    //   59: goto +21 -> 80
    //   62: astore 7
    //   64: aload 4
    //   66: aload 7
    //   68: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   71: goto +9 -> 80
    //   74: aload_3
    //   75: invokeinterface 21 1 0
    //   80: aload_1
    //   81: ifnull +33 -> 114
    //   84: aload_2
    //   85: ifnull +23 -> 108
    //   88: aload_1
    //   89: invokeinterface 24 1 0
    //   94: goto +20 -> 114
    //   97: astore 7
    //   99: aload_2
    //   100: aload 7
    //   102: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   105: goto +9 -> 114
    //   108: aload_1
    //   109: invokeinterface 24 1 0
    //   114: iload 6
    //   116: ireturn
    //   117: astore 5
    //   119: aload 5
    //   121: astore 4
    //   123: aload 5
    //   125: athrow
    //   126: astore 8
    //   128: aload_3
    //   129: ifnull +35 -> 164
    //   132: aload 4
    //   134: ifnull +24 -> 158
    //   137: aload_3
    //   138: invokeinterface 21 1 0
    //   143: goto +21 -> 164
    //   146: astore 9
    //   148: aload 4
    //   150: aload 9
    //   152: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   155: goto +9 -> 164
    //   158: aload_3
    //   159: invokeinterface 21 1 0
    //   164: aload 8
    //   166: athrow
    //   167: astore_3
    //   168: aload_3
    //   169: astore_2
    //   170: aload_3
    //   171: athrow
    //   172: astore 10
    //   174: aload_1
    //   175: ifnull +33 -> 208
    //   178: aload_2
    //   179: ifnull +23 -> 202
    //   182: aload_1
    //   183: invokeinterface 24 1 0
    //   188: goto +20 -> 208
    //   191: astore 11
    //   193: aload_2
    //   194: aload 11
    //   196: invokevirtual 23	java/lang/Throwable:addSuppressed	(Ljava/lang/Throwable;)V
    //   199: goto +9 -> 208
    //   202: aload_1
    //   203: invokeinterface 24 1 0
    //   208: aload 10
    //   210: athrow
    //   211: astore_1
    //   212: new 26	java/lang/RuntimeException
    //   215: dup
    //   216: aload_1
    //   217: invokevirtual 27	java/sql/SQLException:toString	()Ljava/lang/String;
    //   220: aload_1
    //   221: invokespecial 28	java/lang/RuntimeException:<init>	(Ljava/lang/String;Ljava/lang/Throwable;)V
    //   224: athrow
    // Line number table:
    //   Java source line #194	-> byte code offset #0
    //   Java source line #195	-> byte code offset #18
    //   Java source line #196	-> byte code offset #26
    //   Java source line #197	-> byte code offset #34
    //   Java source line #198	-> byte code offset #44
    //   Java source line #194	-> byte code offset #117
    //   Java source line #198	-> byte code offset #126
    //   Java source line #194	-> byte code offset #167
    //   Java source line #198	-> byte code offset #172
    //   Java source line #199	-> byte code offset #212
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	225	0	this	BlockchainImpl
    //   3	200	1	localConnection	Connection
    //   211	10	1	localSQLException	SQLException
    //   5	189	2	localObject1	Object
    //   14	145	3	localPreparedStatement	PreparedStatement
    //   167	4	3	localThrowable1	java.lang.Throwable
    //   16	133	4	localObject2	Object
    //   24	11	5	localResultSet	ResultSet
    //   117	7	5	localThrowable2	java.lang.Throwable
    //   62	5	7	localThrowable3	java.lang.Throwable
    //   97	4	7	localThrowable4	java.lang.Throwable
    //   126	39	8	localObject3	Object
    //   146	5	9	localThrowable5	java.lang.Throwable
    //   172	37	10	localObject4	Object
    //   191	4	11	localThrowable6	java.lang.Throwable
    // Exception table:
    //   from	to	target	type
    //   53	59	62	java/lang/Throwable
    //   88	94	97	java/lang/Throwable
    //   18	44	117	java/lang/Throwable
    //   18	44	126	finally
    //   117	128	126	finally
    //   137	143	146	java/lang/Throwable
    //   6	80	167	java/lang/Throwable
    //   117	167	167	java/lang/Throwable
    //   6	80	172	finally
    //   117	174	172	finally
    //   182	188	191	java/lang/Throwable
    //   0	114	211	java/sql/SQLException
    //   117	211	211	java/sql/SQLException
  }
  
  public DbIterator<TransactionImpl> getAllTransactions()
  {
    Connection localConnection = null;
    try
    {
      localConnection = Db.getConnection();
      PreparedStatement localPreparedStatement = localConnection.prepareStatement("SELECT * FROM transaction ORDER BY db_id ASC");
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public TransactionImpl get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return TransactionDb.findTransaction(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
  
  public DbIterator<TransactionImpl> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt)
  {
    return getAllTransactions(paramAccount, paramByte1, paramByte2, paramInt, Boolean.TRUE);
  }
  
  public DbIterator<TransactionImpl> getAllTransactions(Account paramAccount, byte paramByte1, byte paramByte2, int paramInt, Boolean paramBoolean)
  {
    Connection localConnection = null;
    try
    {
      StringBuilder localStringBuilder = new StringBuilder();
      if (paramBoolean != null) {
        localStringBuilder.append("SELECT * FROM (");
      }
      localStringBuilder.append("SELECT * FROM transaction WHERE recipient_id = ? ");
      if (paramInt > 0) {
        localStringBuilder.append("AND timestamp >= ? ");
      }
      if (paramByte1 >= 0)
      {
        localStringBuilder.append("AND type = ? ");
        if (paramByte2 >= 0) {
          localStringBuilder.append("AND subtype = ? ");
        }
      }
      localStringBuilder.append("UNION SELECT * FROM transaction WHERE sender_id = ? ");
      if (paramInt > 0) {
        localStringBuilder.append("AND timestamp >= ? ");
      }
      if (paramByte1 >= 0)
      {
        localStringBuilder.append("AND type = ? ");
        if (paramByte2 >= 0) {
          localStringBuilder.append("AND subtype = ? ");
        }
      }
      if (Boolean.TRUE.equals(paramBoolean)) {
        localStringBuilder.append(") ORDER BY timestamp ASC");
      } else if (Boolean.FALSE.equals(paramBoolean)) {
        localStringBuilder.append(") ORDER BY timestamp DESC");
      }
      localConnection = Db.getConnection();
      
      int i = 0;
      PreparedStatement localPreparedStatement = localConnection.prepareStatement(localStringBuilder.toString());
      localPreparedStatement.setLong(++i, paramAccount.getId().longValue());
      if (paramInt > 0) {
        localPreparedStatement.setInt(++i, paramInt);
      }
      if (paramByte1 >= 0)
      {
        localPreparedStatement.setByte(++i, paramByte1);
        if (paramByte2 >= 0) {
          localPreparedStatement.setByte(++i, paramByte2);
        }
      }
      localPreparedStatement.setLong(++i, paramAccount.getId().longValue());
      if (paramInt > 0) {
        localPreparedStatement.setInt(++i, paramInt);
      }
      if (paramByte1 >= 0)
      {
        localPreparedStatement.setByte(++i, paramByte1);
        if (paramByte2 >= 0) {
          localPreparedStatement.setByte(++i, paramByte2);
        }
      }
      new DbIterator(localConnection, localPreparedStatement, new DbIterator.ResultSetReader()
      {
        public TransactionImpl get(Connection paramAnonymousConnection, ResultSet paramAnonymousResultSet)
          throws NxtException.ValidationException
        {
          return TransactionDb.findTransaction(paramAnonymousConnection, paramAnonymousResultSet);
        }
      });
    }
    catch (SQLException localSQLException)
    {
      DbUtils.close(new AutoCloseable[] { localConnection });
      throw new RuntimeException(localSQLException.toString(), localSQLException);
    }
  }
}