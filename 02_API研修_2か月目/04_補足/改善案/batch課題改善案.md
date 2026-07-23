t_scoreテーブルにupdate系は必要ない気がします。  
CustomItemProcesserクラスで今日と同じ日でないならnullにするという処理について、登録日の方なのか更新日の方なのか分からなくなるため。

TGameHistoryEntityクラスのフィールドに  
『　/** ゲーム履歴ID _/  
　　private int historyId;　』  
があり、本来あるべき  
『　/_* 処理日 */  
　　private LocalDate processedDay;　』  
がありませんでした。

TGameHistoryMapperインターフェースで  
『　// ゲームの履歴を登録する用の抽象メソッドを作る。  
　　public void insertTGameHistory(TGameHistoryEntity tGameHistory);　』  
となっていて、コメントの内容(「ゲームの履歴」の部分)が間違っていると思われます。
⇒これは、どんな名前がいいだろう？

AggregateTaskletクラスのRepeatStatusメソッド内  
『　// tGameHistoryMapper.updateTotalPlayNumbers(totalPlayNumbers);　』  
になっていました。  
また、TGameHistoryMapperインターフェースにupdateTotalPlayNumbersメソッドがなく、TGameHistoryMapper.xmlにもありませんでした。