# トランザクションと排他制御（実践編）
## 〜2つの窓を並べて、同時実行を目で見る〜

[[15-1_導入_トランザクションとACID|15-1]] は**自分1人**の話でした。ここからは**他の人が同時に同じデータを触る**とどうなるかを扱います。

同時実行の話は、**文章で読んでも頭に入りません。** 2つの窓を並べて、片方を止めて、もう片方が待たされるのを実際に見るのが一番早いです。この資料はそのための手順書として書いてあります。

> **この資料の前提**
> - PostgreSQL 17 で動作確認しています。**掲載している待ち時間・エラーメッセージはすべて実測値**です（待ち時間はマシンによって変わりますが、「待つ／待たない」の結果は変わりません）。
> - 使用するテーブルは [[08_結合から使用するテーブル|08_結合から使用するテーブル]] のものです。
> - シナリオを何度も試すので、**値が分からなくなったら §0 のリセットSQL**で戻してください。

---

## 0. 準備

### 0-1. 窓を2つ開く

**pgAdmin の場合**：同じデータベースを右クリック →「クエリツール」を2回開きます。タブが2つできるので、片方を**窓A**、もう片方を**窓B**とします。

**psql の場合**：ターミナルを2つ開き、それぞれで `psql -U postgres -d <DB名>` を実行します。

> ⚠️ **同じタブで交互に打っても意味がありません。** 同時実行の話は「接続が2本ある」ことが前提です。必ず窓を2つ用意してください。

### 0-2. どちらの窓にいるか分からなくなったら

各窓で次を実行すると、その接続の番号（プロセスID）が出ます。付箋代わりに使ってください。

```sql
SELECT pg_backend_pid();
```

### 0-3. リセットSQL

シナリオを試すと値が変わっていきます。**次のSQLを窓Aで実行すれば初期状態に戻ります**（トランザクションが開いたままなら、先に `ROLLBACK;` してください）。

```sql
ROLLBACK;   -- 開きっぱなしのトランザクションがあれば閉じる

UPDATE products  SET price = 1200.00 WHERE product_id = 1;
UPDATE products  SET price =  800.00 WHERE product_id = 2;
UPDATE inventory SET stock_quantity = 50 WHERE product_id = 1 AND warehouse_id = 1;
```

---

## 1. 書き込み同士はぶつかる

### 1-1. 【2窓】`UPDATE` は待たされる

**窓A** で次を実行します。**`COMMIT` はまだ打ちません。**

```sql
-- 窓A
BEGIN;
UPDATE products SET price = price + 100 WHERE product_id = 1;
```

```
BEGIN
UPDATE 1
```

すぐ返ってきます。次に**窓B**で、**同じ行**を更新してみます。

```sql
-- 窓B
UPDATE products SET price = price + 50 WHERE product_id = 1;
```

**窓Bは返ってきません。** pgAdmin なら実行ボタンが「実行中」のまま、psql ならプロンプトが戻りません。

この状態で**窓A**を確定します。

```sql
-- 窓A
COMMIT;
```

**その瞬間、窓Bが完了します。** 実測では、窓Aが約6秒待ってから `COMMIT` したとき、窓Bの `UPDATE` は **5,958ミリ秒**かかりました。**窓Aが握っていた時間ぶん、まるごと待たされています。**

**なぜ待つのか。** `UPDATE` は対象の行に**行ロック**を自動的に取ります。ロックは**トランザクションが終わる（`COMMIT` か `ROLLBACK`）まで**保持されます。窓Bは「窓Aのトランザクションが終わるのを待つ」しかありません。

> **ここが「`COMMIT` を忘れる」ことの本当の怖さです。**
> 自動コミットが off のツールで `UPDATE` を打ったまま放置すると、**その行を触ろうとする全員が止まります。** 昼休みに戻ってきたら障害になっていた、という事故はこれです。

### 1-2. 待たされた側は、どの値を読み直すのか

`COMMIT` 後、価格を確認します。

```sql
SELECT price FROM products WHERE product_id = 1;
```

```
  price
---------
 1350.00
```

**1200 → +100（窓A）→ +50（窓B）＝ 1350 です。** 窓Bが「自分が待ち始めた時点の 1200」に 50 を足して 1250 になったりはしません。

これが既定の分離レベル **READ COMMITTED** の重要な性質です。**待たされた `UPDATE` は、ロックが外れた後に対象行を読み直してから実行されます。**

> ⚠️ **これは `price = price + 50` のように「今の値を元に計算する」書き方だから正しく動いています。**
> アプリ側で `SELECT` して値を受け取り、計算した結果を `UPDATE price = 1250` のように**絶対値で書き戻す**と、この保護は効きません。そこで起きるのが次の §4 の**ロストアップデート**です。

### 1-3. 誰が誰を待っているのかを調べる

「処理が固まった」ときに原因を特定するためのクエリです。**実務で最も使う1本**なので、書き写しておいてください。

```sql
SELECT pid,
       pg_blocking_pids(pid) AS 待たせている相手,
       wait_event_type       AS 待ちの種類,
       left(query, 50)       AS 実行中のSQL
FROM pg_stat_activity
WHERE cardinality(pg_blocking_pids(pid)) > 0;
```

実測の出力：

```
  pid  | 待たせている相手 | 待ちの種類 |                 実行中のSQL
-------+------------------+------------+---------------------------------------------
 23380 | {9452}           | Lock       | UPDATE products SET price = price + 1 WHERE
```

**「PID 23380 は、PID 9452 に待たされている」**と読みます。犯人が分かったら、その接続が何をしているかを確認します。

```sql
SELECT pid, state, xact_start, left(query, 80)
FROM pg_stat_activity WHERE pid = 9452;
```

どうしても止められないときは、最終手段として接続を切ります。

```sql
SELECT pg_cancel_backend(9452);     -- 実行中のクエリだけキャンセル（穏当）
SELECT pg_terminate_backend(9452);  -- 接続ごと切断（トランザクションはロールバックされる）
```

> ⚠️ `pg_terminate_backend` は**他人の処理を強制終了させるコマンド**です。本番で実行する前に、必ずその接続が何をしているか確認し、可能なら持ち主に連絡してください。

---

## 2. 読み取りは待たされない：MVCC

### 2-1. 【2窓】`UPDATE` 中でも `SELECT` は即座に返る

さきほどと同じように、**窓A**で更新して止めておきます。

```sql
-- 窓A
BEGIN;
UPDATE products SET price = 120 WHERE product_id = 1;
```

この状態で**窓B**から、**ロックを付けずに**読んでみます。

```sql
-- 窓B
SELECT price FROM products WHERE product_id = 1;
```

```
  price
---------
 1200.00
```

**待たされません。実測 0.917ミリ秒で返ってきます。** そして返ってくるのは、**窓Aが書き換える前の 1200.00** です。

窓Aが `COMMIT` した後にもう一度読むと、今度は 120 が返ります。

**これが MVCC（Multi-Version Concurrency Control、多版型同時実行制御）です。**

### 2-2. なぜ古い値が読めるのか

PostgreSQL は `UPDATE` のとき、**元の行を上書きしません。** 新しい内容の行を**別に追加**し、「古い行はいつまで有効か」「新しい行はいつから有効か」という情報を各行に持たせています。

つまりディスク上には、**同じデータの複数のバージョンが並んで存在している**わけです。

各トランザクションは開始時に**スナップショット**（「自分にはどのバージョンが見えるか」の判定基準）を持ち、それに従って適切なバージョンを読みます。**読む側は誰ともぶつからないので、待つ必要がありません。**

| | ブロックするか |
| :--- | :--- |
| 読み取り vs 読み取り | しない |
| **読み取り vs 書き込み** | **しない**（MVCC のおかげ） |
| 書き込み vs 書き込み（同じ行） | **する** |

**「読み取りと書き込みが互いにブロックしない」——これが PostgreSQL の同時実行性能の土台**です。

### 2-3. MVCC の代償

いいことばかりではありません。**古いバージョンの行がディスクに溜まり続けます。**

これを回収するのが **`VACUUM`** です。回収が追いつかないとテーブルが膨らみ（テーブル肥大化）、読むページ数が増えて全体が遅くなります。

> `VACUUM` とテーブル肥大化については [[17-3_運用_チューニングの進め方|17-3 実務編]] §3 で扱います。**「MVCC だからVACUUMが要る」**という繋がりだけ覚えておいてください。

---

## 3. 分離レベル

### 3-1. 同時実行で起きうる3つの現象

複数のトランザクションが同時に動くと、次のような「読み取りのおかしさ」が起こりえます。

| 現象 | 何が起きるか |
| :--- | :--- |
| **ダーティリード** | 他のトランザクションの**まだ確定していない**変更が見えてしまう |
| **ノンリピータブルリード** | 同じ行を2回読んだら、**値が変わっていた**（他が `UPDATE` して `COMMIT` した） |
| **ファントムリード** | 同じ条件で2回読んだら、**行が増えていた**（他が `INSERT` して `COMMIT` した） |

どこまで許容するかを決めるのが**分離レベル**です。

### 3-2. PostgreSQL の分離レベルは実質3つ

SQL標準では4段階が定義されていますが、**PostgreSQL の実際の挙動は標準の表とは違います。** ここは誤解が多いので、実測で確かめます。

まず `READ UNCOMMITTED` を指定してみます。**窓A**で更新して止め、

```sql
-- 窓A
BEGIN;
UPDATE products SET price = 9999 WHERE product_id = 1;
```

**窓B**で、いちばん緩い分離レベルを明示して読みます。

```sql
-- 窓B
BEGIN TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SHOW transaction_isolation;
SELECT price FROM products WHERE product_id = 1;
COMMIT;
```

```
 transaction_isolation
-----------------------
 read uncommitted

  price
---------
 1200.00
```

**指定は受け付けられている（`read uncommitted` と表示される）のに、未確定の 9999 は見えません。**

**PostgreSQL には READ UNCOMMITTED が実装されていません。** 指定してもエラーにはならず、**READ COMMITTED として動きます**。つまり **PostgreSQL でダーティリードは起こせません。**

### 3-3. 【2窓】READ COMMITTED と REPEATABLE READ を比べる

残り3つのうち、実務で出会う2つを比べます。**窓A**を長めのトランザクションにして、その途中で**窓B**が更新と挿入を行います。

```sql
-- 窓A（分離レベルを変えて2回試す）
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;   -- 2回目は REPEATABLE READ に変える
SELECT price FROM products WHERE product_id = 1;           -- ① 1回目の読み取り
-- ここで窓Bを実行する
SELECT price FROM products WHERE product_id = 1;           -- ② 2回目の読み取り
SELECT count(*) FROM products WHERE category_id = 1;       -- ③ 件数
COMMIT;
```

```sql
-- 窓B（窓Aの①と②の間に実行する）
UPDATE products SET price = 1300 WHERE product_id = 1;
INSERT INTO products (product_id, product_name, category_id, price)
VALUES (900, 'Ghost', 1, 1);
```

実測結果：

| 窓Aの分離レベル | ① 1回目 | ② 2回目 | ③ 件数 |
| :--- | ---: | ---: | ---: |
| **READ COMMITTED**（既定） | 1200.00 | **1300.00** | **5** |
| **REPEATABLE READ** | 1200.00 | **1200.00** | **4** |

- **READ COMMITTED** は、②で値が変わり（ノンリピータブルリード）、③で行が増えています（ファントムリード）。**他がコミットするたびに、見える世界が更新される**からです。
- **REPEATABLE READ** は、トランザクション開始時のスナップショットを最後まで使うので、**②も③も最初と同じ**です。

### 3-4. 標準の表をそのまま覚えないこと

よく載っている「SQL標準の分離レベル表」と、PostgreSQL の実際は次のように違います。

| 分離レベル | ダーティリード | ノンリピータブルリード | ファントムリード | PostgreSQL での実際 |
| :--- | :---: | :---: | :---: | :--- |
| Read Uncommitted | 標準では発生 | 標準では発生 | 標準では発生 | **実装なし。Read Committed として動く** |
| **Read Committed（既定）** | 防ぐ | 発生する | 発生する | 標準どおり |
| Repeatable Read | 防ぐ | 防ぐ | 標準では発生 | **ファントムも防ぐ**（標準より強い） |
| Serializable | 防ぐ | 防ぐ | 防ぐ | 標準どおり＋直列化異常も防ぐ |

**覚えるべきは2点です。**

1. **PostgreSQL でダーティリードは起きない**（Read Uncommitted が無い）
2. **PostgreSQL の Repeatable Read はファントムも防ぐ**（スナップショットを固定するため）

### 3-5. REPEATABLE READ 以上は「失敗する」ことがある

強い分離レベルはタダではありません。**REPEATABLE READ 以上では、他と衝突したトランザクションがエラーで落とされます。**

**窓A**で REPEATABLE READ のまま同じ行を更新しようとすると：

```
ERROR:  could not serialize access due to concurrent update
ROLLBACK
```

**待つのではなく、失敗します。** つまり **REPEATABLE READ / SERIALIZABLE を使うなら、アプリ側に「失敗したら最初からやり直す」リトライ処理が必須**です。これを書かずに分離レベルだけ上げると、繁忙時にエラーが多発します。

### 3-6. どれを使えばいいか

**既定の READ COMMITTED のままでよい**、が答えです。理由は3つあります。

1. 多くの処理は、同じ行を1トランザクション内で2回読みません。ノンリピータブルリードが問題にならない
2. 同時実行性能が最も高い
3. リトライ処理を書かなくてよい

**「同じデータを2回読んで、その間で変わっていると困る」処理**（月次の集計、複数テーブルを跨いだ整合性チェックなど）でだけ REPEATABLE READ を検討します。そして**その場合はリトライをセットで実装します。**

**「特定の行だけを他人に触らせたくない」だけなら、分離レベルを上げるより次の行ロックのほうが適切です。**

---

## 4. 明示的な行ロック

### 4-1. 【2窓】ロックを取らないと在庫が壊れる（ロストアップデート）

「在庫を確認してから引く」という、どのシステムにもある処理を考えます。

```
① 在庫を SELECT で読む
② アプリ側で「読んだ値 − 注文数」を計算する
③ 計算結果を UPDATE で書き戻す
```

在庫50個の商品に、**窓Aが5個・窓Bが3個**の注文を同時に処理します。正しい結果は **50 − 5 − 3 = 42** のはずです。

```sql
-- 窓A
BEGIN;
SELECT stock_quantity FROM inventory WHERE product_id=1 AND warehouse_id=1;   -- 50 を読む
-- （アプリが 50-5=45 を計算）
UPDATE inventory SET stock_quantity = 45 WHERE product_id=1 AND warehouse_id=1;
COMMIT;
```

```sql
-- 窓B（窓Aと同じタイミングで開始する）
BEGIN;
SELECT stock_quantity FROM inventory WHERE product_id=1 AND warehouse_id=1;   -- 50 を読む
-- （アプリが 50-3=47 を計算）
UPDATE inventory SET stock_quantity = 47 WHERE product_id=1 AND warehouse_id=1;
COMMIT;
```

実測結果：

```
窓Aが読んだ値: 50
窓Bが読んだ値: 50
最終的な在庫  : 47      ← 42 になるはずが 47
```

**窓Aの「5個引く」が完全に消えました。** これが**ロストアップデート（更新の喪失）**です。

**§2 で見た MVCC が、ここでは裏目に出ています。** 「読み取りは待たされない」ので、**2つの窓が同じ 50 を読めてしまう**のです。`UPDATE` 同士はぶつかって片方が待ちますが、**待った側は「自分が読んだ 50」を元に計算済みの 47 を、そのまま書き込みます。**

> **これは実際に在庫がマイナスになったり、二重に商品が売れたりする原因です。** テストでは同時に叩かないので発覚せず、本番の繁忙時にだけ起きる——という一番厄介な種類のバグになります。

### 4-2. `SELECT ... FOR UPDATE` で防ぐ

解決策は、**読む時点でロックを取ってしまう**ことです。

```sql
-- 窓A
BEGIN;
SELECT stock_quantity FROM inventory
 WHERE product_id=1 AND warehouse_id=1
 FOR UPDATE;                                   -- ★ この行に排他ロックを取る
-- （アプリが 50-5=45 を計算）
UPDATE inventory SET stock_quantity = 45 WHERE product_id=1 AND warehouse_id=1;
COMMIT;
```

窓Bも同じく `FOR UPDATE` を付けます。すると**窓Bは `UPDATE` ではなく `SELECT` の時点で待たされます。**

実測結果：

```
窓Aが読んだ値: 50
窓Bの SELECT ... FOR UPDATE : 3,229ms 待たされた
窓Bが読んだ値: 45      ← 窓Aの COMMIT 後の値
```

窓Bは 45 を読むので、計算結果は 45 − 3 = **42**。**正しくなりました。**

**ポイントは「待つ位置が `UPDATE` から `SELECT` に前倒しされた」ことです。** §4-1 では読み取りが待たされなかったせいで、両方が古い 50 を掴んでいました。`FOR UPDATE` は**読み取りの時点で他を締め出す**ので、そもそも古い値を掴めなくなります。

**`FOR UPDATE` は「これから更新するので、読んだ瞬間から押さえておく」という意思表示です。**

> 💡 **`UPDATE inventory SET stock_quantity = stock_quantity - 5` と1文で書けば `FOR UPDATE` は要りません**（§1-2 で見たとおり、`UPDATE` は行を読み直すため）。
> **`FOR UPDATE` が必要になるのは、「読んだ値を使ってアプリ側で判断・計算してから書き戻す」場合**です。在庫チェック（足りなければ注文を断る）はまさにこれに当たります。

### 4-3. 待ちたくないときの選択肢

`FOR UPDATE` は既定では「取れるまで待つ」動きです。待ちたくない場合に2つの書き方があります。

```sql
-- ① 取れなければ即エラー
SELECT * FROM products WHERE product_id = 1 FOR UPDATE NOWAIT;
```

```
ERROR:  could not obtain lock on row in relation "products"
```

```sql
-- ② ロックされている行は飛ばして、取れた行だけ返す
SELECT product_id FROM products WHERE product_id IN (1,2,3) ORDER BY product_id
FOR UPDATE SKIP LOCKED;
```

商品1と2が他セッションにロックされている状態での実測：

```
 product_id
------------
          3
```

- **`NOWAIT`** … 「待つくらいなら失敗させて、ユーザーに『混み合っています』と返したい」場面で使います
- **`SKIP LOCKED`** … **ジョブキューの定番**です。複数のワーカーが同じテーブルから未処理タスクを取り合うとき、これを付けると**互いに違う行を掴む**ので、待ちも重複処理も起きません

### 4-4. ロックの種類：`FOR SHARE`

`FOR UPDATE` のほかに、**`FOR SHARE`（共有ロック）**があります。

- **`FOR UPDATE`（排他）** … 「この行は自分が更新する。**他の誰にも触らせない**」
- **`FOR SHARE`（共有）** … 「この行を参照するので、**変更はさせない**。でも**同じように参照したいだけの人となら共存してよい**」

実測で確かめると：

| 窓Aが `FOR SHARE` 中に、窓Bが… | 結果 |
| :--- | :--- |
| 同じ行を `FOR SHARE` | **1.864ms で成功**（待たない） |
| 同じ行を `UPDATE` | **5,880ms 待たされた** |

**「変更はブロックするが、参照同士は共存できる」**——これが共有ロックです。

### 4-5. 【2窓】外部キーは、黙ってロックを取っている

**`FOR SHARE` を自分で書く場面は、実はあまりありません。** なぜなら**PostgreSQL が必要な場面では自動で取っているから**です。

代表例が**外部キー**です。子テーブルに行を挿入すると、**参照先の親行に自動的にロックが取られます。**

**窓A**で、顧客6番を参照する注文を挿入します（`COMMIT` しません）。

```sql
-- 窓A
BEGIN;
INSERT INTO orders (order_id, customer_id, order_date, status)
VALUES (999, 6, CURRENT_DATE, 'Pending');
```

この状態で**窓B**から、その顧客行を触ってみます。

```sql
-- 窓B（a）親行の「キーではない列」を更新
UPDATE customers SET city = 'Kyoto' WHERE customer_id = 6;
```

```sql
-- 窓B（b）親行を削除
DELETE FROM customers WHERE customer_id = 6;
```

実測結果：

| 窓Bの操作 | 結果 |
| :--- | :--- |
| (a) 非キー列の `UPDATE` | **2.512ms で成功**（待たない） |
| (b) `DELETE` | **6,861ms 待たされた** |

**自動で取られているのは `FOR KEY SHARE` という種類のロックです。** これは「**参照している主キーの値が消えたり変わったりしなければいい**」という、必要最小限のロックです。だから住所の変更は通り、削除だけが止まります。

> **ここが実務で効いてくる知識です。**
> 「注文を登録しただけなのに、顧客マスタの更新処理が固まった」という現象は、**FK の自動ロックが原因**であることがよくあります。自分で `LOCK` も `FOR UPDATE` も書いていないのに待たされるのは、こういう仕組みが裏で動いているからです。
>
> 逆に言えば、**FK 参照のために自分で `FOR SHARE` を書く必要はありません。** PostgreSQL がもっと弱い（＝邪魔しない）ロックで、すでに面倒を見ています。

### 4-6. 4つのロックモード（参考）

行ロックには強さの違う4段階があります。**暗記は不要ですが、`pg_locks` を見たときに読めるように**一覧にしておきます。強い順に：

| モード | 取り方 | 何をブロックするか |
| :--- | :--- | :--- |
| `FOR UPDATE` | 明示的に書く／`DELETE` と**キー列を変える** `UPDATE` が自動で取る | 他のすべての行ロックと更新 |
| `FOR NO KEY UPDATE` | **キー列を変えない** `UPDATE` が自動で取る | `FOR UPDATE` と `FOR SHARE` |
| `FOR SHARE` | 明示的に書くときだけ | 更新・削除 |
| `FOR KEY SHARE` | **FK 参照時に自動で取られる** | 削除とキー列の変更のみ |

ここでいう「キー列」とは、**主キーやユニーク制約に使われている列**のことです。§4-5 で住所の `UPDATE` が通ったのは、`city` がキー列ではないので `FOR NO KEY UPDATE` で済み、`FOR KEY SHARE` と衝突しなかったからです。

**下に行くほど「邪魔しない」ロックです。** PostgreSQL が自動で取るときは、常に**必要最小限の強さ**を選んでいます。

---

## 5. デッドロック

### 5-1. 【2窓】起こしてみる

**2つのトランザクションが、お互いの持っているロックを待ち合う**と、永久に進めなくなります。これが**デッドロック**です。

意図的に起こすのは簡単で、**ロックを取る順番を逆にする**だけです。

```sql
-- 窓A：商品1 → 商品2 の順
BEGIN;
UPDATE products SET price = price + 1 WHERE product_id = 1;   -- ①
-- （ここで窓Bの②まで進める）
UPDATE products SET price = price + 1 WHERE product_id = 2;   -- ③ 窓Bを待つ
COMMIT;
```

```sql
-- 窓B：商品2 → 商品1 の順
BEGIN;
UPDATE products SET price = price + 1 WHERE product_id = 2;   -- ②
UPDATE products SET price = price + 1 WHERE product_id = 1;   -- ④ 窓Aを待つ
COMMIT;
```

③で窓Aは「商品2を持っている窓B」を待ち、④で窓Bは「商品1を持っている窓A」を待ちます。**互いに相手の終了を待つので、放っておけば永久に進みません。**

**約1秒後、PostgreSQL が検知して片方を殺します。** 実測での窓B側の出力：

```
ERROR:  deadlock detected
DETAIL:  Process 25544 waits for ShareLock on transaction 18430; blocked by process 14048.
Process 14048 waits for ShareLock on transaction 18431; blocked by process 25544.
HINT:  See server log for query details.
CONTEXT:  while updating tuple (0,21) in relation "products"
ROLLBACK
```

**窓Bは `ROLLBACK` され、窓Aは何事もなく完了します。** どちらが犠牲になるかは選べません。

### 5-2. 検知の仕組み

PostgreSQL は、ロック待ちが **`deadlock_timeout`（既定 1秒）**を超えたときだけ、デッドロックの有無を調べます。

```sql
SHOW deadlock_timeout;
```

```
 deadlock_timeout
------------------
 1s
```

**毎回調べているわけではありません**（調べるコスト自体が高いため）。だから「1秒くらいは普通に待つ」のが正常な動作です。

### 5-3. 対策：ロックを取る順番を揃える

**デッドロックは、複数の行を「違う順番で」ロックすることで起きます。** 逆に言えば、**全員が同じ順番でロックを取れば起きません。**

```sql
-- ✕ 注文に入っていた順に処理する（順番がバラバラになる）
UPDATE products SET ... WHERE product_id = 5;
UPDATE products SET ... WHERE product_id = 2;

-- ○ 常に ID の昇順で処理する
UPDATE products SET ... WHERE product_id = 2;
UPDATE products SET ... WHERE product_id = 5;
```

複数行をまとめてロックする場合も、**`ORDER BY` を付ける**だけで順序が揃います。

```sql
SELECT * FROM products WHERE product_id IN (5, 2, 9) ORDER BY product_id FOR UPDATE;
```

対策の優先順位は次のとおりです。

1. **ロック順序を統一する**（最も効果的。設計時のルールにする）
2. **トランザクションを短くする**（待ち合う時間そのものを減らす）
3. **デッドロックエラーを受けたらリトライする**（確率は下げられても0にはできないため、最後の保険）

> **デッドロックは「起きたら設計を見直す」サインです。** リトライで握りつぶすだけにすると、負荷が上がるほど発生率が上がり、いずれ回らなくなります。

---

## この章のまとめ

| | 覚えること |
| :--- | :--- |
| **書き込み同士** | 同じ行なら待つ。ロックは**トランザクションが終わるまで**外れない |
| **読み取り** | MVCC のおかげで**待たされない**。未確定の変更は見えず、古い値が読める |
| **分離レベル** | **既定の READ COMMITTED のままでよい。** PostgreSQL にダーティリードは無い |
| **ロストアップデート** | 「読んで、計算して、書き戻す」は**壊れる**。`SELECT ... FOR UPDATE` で防ぐ |
| **`FOR SHARE`** | 自分で書く場面は少ない。**FK は勝手に `FOR KEY SHARE` を取っている** |
| **デッドロック** | **ロック順序を揃える**。約1秒後に片方が `ROLLBACK` される |
| **困ったとき** | `pg_blocking_pids()` で「誰が誰を待っているか」を見る |

---

### 関連する資料

- [[15-1_導入_トランザクションとACID|15-1 導入編]] … トランザクションの基本、ACID、SAVEPOINT
- [[15-3_補足_PLpgSQLの中のトランザクション制御|15-3 補足]] … プロシージャ・関数の中で `COMMIT` は書けるのか
- [[17-3_運用_チューニングの進め方|17-3 実務編]] §3 … MVCC の後始末（`VACUUM`）とテーブル肥大化
