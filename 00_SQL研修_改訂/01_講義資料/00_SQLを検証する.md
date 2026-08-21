# 書いたSQLを自分で検証する

| 印 | 意味 |
| :--- | :--- |
| （無印） | **原理**。なぜそうなるかを理解する。手順はここから導ける |
| 🔑 | **覚えるところ**。理屈から出てこないので、覚えるか手元に置く |
| 🐘 | **PostgreSQL だけの話**。他のDBでは違う。名前は忘れてよい |

---

## この章のねらい

`SELECT` は間違っても誰も困りません。結果を見て、書き直せばいい。

`UPDATE` と `DELETE` は違います。**`COMMIT` した後は戻せません。**

だから実行する**前に**確かめるしかない。そのやり方を身につけるのがこの章です。
現場では、これができる人は「分かっているエンジニア」として扱われます。

### 原理はひとつだけ

> **変える前に、変わる範囲を見る。**

このあと手順が5つ出てきますが、**全部この1文から導けます。** 暗記する必要はありません。
「変わる範囲ってどこだ」「見るってどうやるんだ」を順番に詰めていくだけです。

---

## 1. 同じ `WHERE` で `SELECT` する

「変わる範囲」を決めているのは `WHERE` です。
ということは、**`UPDATE` の `WHERE` をそのまま `SELECT` に付ければ、変わる行が見える。**

たとえば「東京の顧客のランクを1つ上げたい」とき。書きたいのはこれです。

```sql
UPDATE customers SET membership_id = membership_id + 1
WHERE city = 'Tokyo';
```

先に、`WHERE` だけ同じにして `SELECT` します。

```sql
SELECT customer_id, name, city, membership_id
FROM customers
WHERE city = 'Tokyo'
ORDER BY customer_id;
```

```
 customer_id | name  | city  | membership_id
-------------+-------+-------+---------------
           1 | Alice | Tokyo |             2
           4 | Diana | Tokyo |             3
           5 | Ellen | Tokyo |             2
           7 | Hank  | Tokyo |             1
          10 | Kevin | Tokyo |             3
(5 rows)
```

**5件。** これが「変わる範囲」です。ここで顔ぶれを見ておくと、「あれ、この人も入るのか」に気づけます。

> ⚠️ **`SET` の中身は変えない。`WHERE` だけを写す。**
> `SET` を `SELECT` に混ぜようとすると別のクエリになってしまいます。見たいのは**どの行が対象か**です。

---

## 2. 件数を先に見積もる

`SELECT` で 5件だと分かったので、`UPDATE` は **5件のはず**です。

実行するとこう返ります。

```
UPDATE 5
```

**この数字と、さっきの件数を照合します。** 合っていれば狙いどおり。

- **5件のはずが 0件** → `WHERE` が誰にも当たっていない。条件のスペルミスか、値が違う
- **5件のはずが 11件** → `WHERE` を書き忘れた可能性がある。**全件更新は事故のいちばん多い形**

> ⚠️ **`UPDATE 0` はエラーではありません。**
> エラーが出ないので「成功した」と思ってしまいがちですが、**1件も変わっていない**という意味です。
> **件数を見ないコードは、検証していないのと同じ**です。

---

## 3. `BEGIN` で囲む

1と2をやっても、**実行してから間違いに気づくこと**はあります。そのときに戻せるようにしておきます。

```sql
BEGIN;

UPDATE customers SET membership_id = membership_id + 1
WHERE city = 'Tokyo';
-- UPDATE 5  ← ここで件数を確認

SELECT customer_id, name, membership_id
FROM customers WHERE city = 'Tokyo' ORDER BY customer_id;
-- 中身も確認

COMMIT;   -- 良ければ確定
-- ROLLBACK;  -- 違っていたら戻す
```

`ROLLBACK` した場合の実測です。

```
BEGIN
UPDATE 5
 customer_id | name  | membership_id      ← 更新後（1つ増えている）
-------------+-------+---------------
           1 | Alice |             3
           4 | Diana |             4
           5 | Ellen |             3
           7 | Hank  |             2
          10 | Kevin |             4
(5 rows)

ROLLBACK
 customer_id | name  | membership_id      ← 元に戻っている
-------------+-------+---------------
           1 | Alice |             2
           4 | Diana |             3
           5 | Ellen |             2
           7 | Hank  |             1
          10 | Kevin |             3
(5 rows)
```

**`BEGIN` の中なら、実際に更新した結果を見てから決められます。** これが一番強い検証です。

トランザクションそのものの話は [[15-1_導入_トランザクションとACID|15-1 トランザクションとACID]] で扱います。ここでは「囲むと戻せる」だけ押さえてください。

> ⚠️ **`BEGIN` と打ったら、必ず `COMMIT` か `ROLLBACK` で閉じる。**
> 開いたまま放置すると、その行を他の人が更新できずに待たされます（[[15-2_実践_ロックとMVCC|15-2 ロックとMVCC]]）。
