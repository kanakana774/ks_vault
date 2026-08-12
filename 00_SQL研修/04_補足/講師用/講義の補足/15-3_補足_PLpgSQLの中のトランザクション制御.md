# 補足：PL/pgSQL の中のトランザクション制御

## 〜プロシージャや関数の中で `COMMIT` は書けるのか〜

12〜13章で書いた関数・プロシージャの中で `COMMIT` や `ROLLBACK` が使えるかどうかは、**書く場所によって変わります**。ここは規則が細かく、エラーメッセージも分かりにくいので、実測を添えて整理しておきます。

> **この資料の前提**
> - PostgreSQL 17 で動作確認しています。**掲載しているエラーメッセージはすべて実測**です。
> - [[15-1_導入_トランザクションとACID|15-1]] の SAVEPOINT（§4）を読んでいることを前提にします。

---

## 1. 関数とプロシージャの違い

| | `FUNCTION`（関数） | `PROCEDURE`（プロシージャ）／`DO` ブロック |
| :--- | :--- | :--- |
| 呼び出し方 | `SELECT my_func();` | `CALL my_proc();` / `DO $$ ... $$;` |
| 立ち位置 | **SQL文の一部**として実行される | **独立して**呼び出される |
| `COMMIT` / `ROLLBACK` | **一切使えない** | **条件付きで使える** |

**関数が `COMMIT` できない理由**は単純です。関数は `SELECT my_func() FROM ...` のように**SQL文の一部**として動きます。その SQL 文自体が1つのトランザクションの中にいるので、**その途中で勝手に確定されると、呼び出し元のSQLが成立しなくなります。**

プロシージャは `CALL` で単独に呼ばれるため、**自分でトランザクションの区切りを決められます**。「1件処理するごとに確定したい」というバッチ処理は、だから**関数ではなくプロシージャで書きます**。

---

## 2. プロシージャで `COMMIT` が使える条件

`CALL` すれば必ず使えるわけではありません。**2つの条件**があります。

### 2-1. 条件①：呼び出し側がトランザクションを開始していないこと

```sql
-- ○ 単独で CALL する
CALL my_proc();

-- ✕ 外側でトランザクションが始まっている
BEGIN;
CALL my_proc();     -- 中の COMMIT / ROLLBACK は失敗する
COMMIT;
```

外側で `BEGIN;` してから呼ぶと、プロシージャ内の `ROLLBACK` はこうなります（実測）。

```
ERROR:  invalid transaction termination
CONTEXT:  PL/pgSQL function p_test2() line 8 at ROLLBACK
```

> ⚠️ **自動コミットが off のツールで `CALL` すると、暗黙に `BEGIN` されているのでこのエラーになります。**
> 「手元では動いたのに、アプリから呼んだら `invalid transaction termination` が出る」という現象の正体はこれです。→ [[15-1_導入_トランザクションとACID|15-1]] §2-2

### 2-2. 条件②：`EXCEPTION` 句を持つブロックの「本体」ではないこと

ここが一番の落とし穴です。**`BEGIN ... EXCEPTION ... END` と書いた瞬間、PostgreSQL はそのブロックのためにセーブポイントを1つ作ります。**

**セーブポイントが生きている間は、その外側のトランザクションを確定できません。**

```sql
CREATE OR REPLACE PROCEDURE p_test4()
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO log_t VALUES ('b');
    COMMIT;                      -- ★ EXCEPTION 句を持つブロックの本体での COMMIT
    RAISE INFO 'COMMIT を通過した';
EXCEPTION
    WHEN OTHERS THEN
        RAISE INFO 'ハンドラ: %', SQLERRM;
END;
$$;

CALL p_test4();
```

実測：

```
INFO:  ハンドラ: cannot commit while a subtransaction is active
```

**`cannot commit while a subtransaction is active`（サブトランザクションが動いている間はコミットできません）。** `COMMIT` 自体がエラーになり、自分の `EXCEPTION` ハンドラに捕まっています。

---

## 3. 【要注意】`EXCEPTION` ハンドラの「中」は例外的に通ってしまう

**ここは誤解しやすいところなので、実測で確認しておきます。**

「`EXCEPTION` ブロックの中では `COMMIT` できない」と説明されることがありますが、**正確ではありません。** 禁止されているのは §2-2 のとおり**ブロックの本体**であって、**`WHEN OTHERS THEN` 以降のハンドラの中は通ります。**

```sql
CREATE OR REPLACE PROCEDURE p_test2()
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO log_t VALUES ('手続きの中で入れた行');
    RAISE EXCEPTION 'わざとエラー';
EXCEPTION
    WHEN OTHERS THEN
        RAISE INFO 'ハンドラ: %', SQLERRM;
        ROLLBACK;                       -- ★ ハンドラの中
        RAISE INFO 'ROLLBACK を通過した';
END;
$$;

CALL p_test2();
```

実測：

```
INFO:  ハンドラ: わざとエラー
INFO:  ROLLBACK を通過した
CALL
```

**エラーになりません。** ハンドラに入った時点で、そのブロックのセーブポイントは**すでに巻き戻されて解放されている**ためです。

### 3-1. しかし、書くべきではない

通るからといって書いてよいわけではありません。**ハンドラ内の `ROLLBACK` には2つの問題があります。**

**問題① そもそも不要です。** ハンドラに入った時点で、そのブロック内の変更は**すでに巻き戻されています**。`ROLLBACK` を書いても書かなくても結果は同じです（実測）。

```sql
-- ROLLBACK を書かない版
CREATE OR REPLACE PROCEDURE p_test6()
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO log_t VALUES ('ROLLBACKなし版');
    RAISE EXCEPTION 'わざとエラー';
EXCEPTION
    WHEN OTHERS THEN
        RAISE INFO 'ハンドラ（ROLLBACKは書いていない）: %', SQLERRM;
END;
$$;

CALL p_test6();
SELECT count(*) FROM log_t;
```

```
 count
-------
     0        ← ROLLBACK を書かなくても、INSERT は消えている
```

**問題② 呼び出し方によっては壊れます。** §2-1 のとおり、外側でトランザクションが開いていると `invalid transaction termination` で落ちます。**「単独で `CALL` したときだけ動く」プロシージャ**になってしまい、アプリから呼んだ瞬間に壊れます。

> **結論：`EXCEPTION` ハンドラの中に `COMMIT` / `ROLLBACK` を書かないでください。** 何も書かないのが正解です。

---

## 4. 「勝手に作られるトランザクション」の正体

PL/pgSQL の中で意識すべき暗黙のトランザクション制御は2つです。

### 4-1. 暗黙のメイントランザクション

プロシージャや `DO` ブロックを開始すると、自動的に1つのトランザクションが始まっています。**`COMMIT` を書くと、そこまでを確定し、直後に新しいトランザクションを自動で開始します。**

つまりプロシージャ内の `COMMIT` は「終わり」ではなく「区切り」です。`COMMIT` の後も処理は続けられます。

### 4-2. `EXCEPTION` による暗黙のサブトランザクション

`BEGIN ... EXCEPTION` を書くと、内部的にセーブポイントが作られます。

1. `BEGIN`（`EXCEPTION` 付き）に入る → **`SAVEPOINT` を自動作成**
2. 内部のSQLを実行
3. エラー発生 → **そのセーブポイントまで自動ロールバック**し、`EXCEPTION` 節へ
4. 正常終了 → **セーブポイントを自動解放（`RELEASE`）**

> ⚠️ **ループの中に `EXCEPTION` ブロックを置くと、回るたびにセーブポイントが作られます。**
> [[15-1_導入_トランザクションとACID|15-1]] §4-3 のとおり、**1トランザクション内で64個を超えると性能が落ちます**。ループ内で `EXCEPTION` を使うなら、**各回で `COMMIT` してトランザクションを切る**設計にしてください（次のテンプレートがまさにそれです）。

---

## 5. バッチ処理のテンプレート

以上を踏まえると、「1件ずつ処理して、失敗しても止まらず、成功分は残す」バッチは次の形になります。

```sql
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT id FROM my_table WHERE processed = false LOOP

        -- ① エラーを閉じ込める境界を、内側のブロックで作る
        BEGIN
            UPDATE my_table SET processed = true WHERE id = r.id;
            -- ここでエラーが起きても、この内側 BEGIN の開始までしか戻らない

        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'ID % でエラー: %', r.id, SQLERRM;
            -- ここに ROLLBACK は書かない（§3-1）
        END;

        -- ② 例外処理ブロックの「外」で COMMIT する
        --    ここなら §2-2 の制約に引っかからない
        COMMIT;

    END LOOP;
END $$;
```

**構造のポイントは2つです。**

1. **`EXCEPTION` は内側のブロックに閉じ込める。** 1件の失敗が全体を止めないようにするため
2. **`COMMIT` は内側ブロックの外に置く。** 内側の本体に書くと `cannot commit while a subtransaction is active` になるため（§2-2）

**この形にすると、内側ブロックのセーブポイントは毎回 `COMMIT` で解放される**ので、§4-2 のサブトランザクション増加も起きません。

---

## まとめ

| 書く場所 | `COMMIT` / `ROLLBACK` | 
| :--- | :--- |
| `FUNCTION` の中 | **使えない**（常にアトミック） |
| `PROCEDURE` / `DO` の中、外側でトランザクション未開始 | **使える** |
| `PROCEDURE` / `DO` の中、外側で `BEGIN;` 済み | `invalid transaction termination` |
| `EXCEPTION` 句を持つブロックの**本体** | `cannot commit while a subtransaction is active` |
| `EXCEPTION` **ハンドラの中** | 通ってしまうが、**不要かつ危険。書かない** |

**覚え方**：PL/pgSQL は `EXCEPTION` を使い始めると、その区間を**「ひとまとまりの安全圏（サブトランザクション）」**として扱います。**安全圏の中にいる間は、DB全体の確定はさせてもらえません。**

---

### 関連する資料

- [[15-1_導入_トランザクションとACID|15-1 導入編]] … SAVEPOINT、自動コミット、トランザクションの中断状態
- [[15-2_実践_ロックとMVCC|15-2 実践編]] … ロックとMVCC。特に **§6 トランザクションの境界**（どこで区切るか）
- [[15-4_実践_ロック待ちの調査と予防|15-4 実践編]] … `COMMIT` 漏れが `idle in transaction` としてどう見えるか
