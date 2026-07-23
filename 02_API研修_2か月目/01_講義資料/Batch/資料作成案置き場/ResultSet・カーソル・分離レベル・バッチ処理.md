
> [!important]
> **要約：**
> cursorはDBに結果セットを保持するわけではなく、selectした結果のCTIDを貯めておいてくれるだけ
> ⇒分離レベルはread commitedなので、fetch時に他トランザクションの更新内容が読めてしまいで、起動時と結果が異なってしまうことも。
> ⇒そのため、バッチ実行中は冪等が保証されるクエリやらロックやらにしないと安全とは言えない。
> また、リスタート時にはselectが再度やり直されるので、ここでも冪等なクエリなり条件になってないと安全とは言えない。


# PostgreSQL × JDBC × Spring Batch

# ## ResultSet・カーソル・分離レベル・バッチ処理に関するまとめ

---

# 1. ResultSet とカーソルの本質

## ✔ ResultSet は「DB 側のカーソル」を読む仕組み

- JDBC の ResultSet は **Java メモリに全件を保持しているわけではない**
- 多くの DB（PostgreSQL / MySQL / Oracle / SQLServer）は  
    **サーバーサイドカーソル** を使い、必要な行だけをストリーミングする
- Java が保持するのは **現在の行 + ドライバのバッファ** 程度

[JdbcCursorItemReader](https://spring.pleiades.io/spring-batch/reference/readers-and-writers/database.html#JdbcCursorItemReader)

---

## ✔ DB は「結果セットをメモリに固定」していない

よくある誤解：

> SELECT の結果を DB がメモリに保持して、そこをカーソルで読む

これは **間違い**。

実際には：

- DB は **行の位置（ポインタ）だけ保持**
- 行データそのものは **MVCC の最新バージョンを都度読み取る**
- よって **途中で UPDATE があれば値が変わる可能性がある**

---

# 2. pgAdmin の SELECT が変わらない理由

## ✔ pgAdmin は「クライアント側で全件キャッシュ」しているだけ

- pgAdmin や psql の結果ウィンドウは  
    **SELECT 結果をクライアント側に全件ロードして保持**
- UPDATE しても **再実行しない限り画面は変わらない**
- これは **DB のカーソル挙動とは無関係**

---

# 3. 分離レベルとカーソルの関係

## ✔ Read Committed のカーソルは「ライブビュー」

Read Committed の定義：

> 各ステートメント実行時に最新のコミット済みデータを読む

カーソルの FETCH は **独立したステートメント**。

つまり：

- SELECT は一度だけ
- しかし FETCH は複数回
- そのたびに **最新の行バージョンを MVCC から読む**

結果：

- **途中で UPDATE があれば ResultSet の値が変わる可能性がある**

---

## ✔ Repeatable Read（PostgreSQL）は「トランザクション開始時スナップショット」

- PostgreSQL の Repeatable Read は  
    **トランザクション開始時点のスナップショットを固定**
- FETCH のたびに同じバージョンを読む
- UPDATE は見えない

---

## ✔ Serializable（PostgreSQL）は「SSI（Serializable Snapshot Isolation）」

重要ポイント：

- **ロックしない**
- **他トランザクションを待たせない**
- **スナップショット固定**
- **競合が起きたら後続トランザクションをエラーにする**

つまり：

> Serializable は「ロック地獄」ではなく「再実行地獄」になりうる

---

# 4. Spring Batch とカーソルの問題点

## ✔ CursorItemReader は「長時間カーソルを開きっぱなし」

- 1時間のバッチでカーソルを開くと  
    → その間に UPDATE が入る  
    → Read Committed なら値が変わる  
    → Serializable なら最後に **Serialization Failure** でロールバック

つまり：

> **CursorItemReader × 長時間バッチ × Serializable は危険**

---

## ✔ PagingItemReader の方が再現性が高い

- 毎回 SELECT を実行するため  
    → スナップショットが安定  
    → カーソルのような“途中で変わる”問題が起きにくい

---

# 5. 冪等性の重要性

## ✔ 冪等性が最強の戦略

- 冪等性があれば  
    → Read Committed でも安全  
    → カーソルでもページングでも OK  
    → 再実行しても壊れない

例：

- 「status = 'READY' の行だけ処理し、処理後に 'DONE' に更新」
- 「昨日のデータだけ処理（ただし updated_at で固定）」
- 「処理対象を別テーブルにスナップショットしてから処理」

---

## ✔ 「昨日のデータだけ処理」は弱点あり

- 昨日のデータが今日 UPDATE されたらどうする？
- よくある対策：

```
WHERE created_at < 今日の0時
  AND updated_at < 今日の0時
```

または：

- 処理対象を別テーブルにコピーして固定
- キューに積む
- status フラグで制御

---

# 6. どの戦略を選ぶべきか？

|戦略|長所|短所|バッチ適性|
|---|---|---|---|
|**Serializable + Cursor**|スナップショット固定|長時間で再実行地獄|❌|
|**Read Committed + Cursor**|シンプル|途中でデータが変わる|⚠️ 冪等性があれば可|
|**PagingItemReader**|再現性高い|ページングキー必要|◎|
|**冪等性の確保**|最強|設計が必要|◎◎|
|**昨日のデータだけ処理**|簡単|更新に弱い|△|

---

# 7. 最終結論（あなたの状況に最適な判断）

- **Serializable はロックしないが、長時間バッチには向かない（再実行地獄）**
- **Read Committed × Cursor は途中でデータが変わる可能性がある**
- **冪等性を確保できるなら、それが最強で現実的**
- **Spring Batch では PagingItemReader が最も安全で推奨**

---

# 8. さらに深掘りしたい場合

必要なら以下もまとめられる：

- PostgreSQL の MVCC の内部動作（行バージョン管理）
- CursorItemReader が危険になる具体例
- 冪等性の実装パターン（実コード例）
- 大量データバッチの設計パターン（スナップショット方式、キュー方式など）

---

必要なら、このまとめを **図解** にしたり、  
**あなたのバッチ要件に合わせた最適な設計案** を作ることもできるよ。