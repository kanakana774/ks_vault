-- テーブル構成の作成

```SQL
-- 顧客マスタ (customers_mst)
CREATE TABLE customers_mst (
customer_id SERIAL PRIMARY KEY,
customer_name VARCHAR(255) NOT NULL,
email VARCHAR(255) UNIQUE NOT NULL,
created_date DATE NOT NULL,
deleted_at TIMESTAMPTZ
);

-- 商品マスタ (products_mst)
CREATE TABLE products_mst (
product_id SERIAL PRIMARY KEY,
category VARCHAR(100) NOT NULL,
product_name VARCHAR(255) NOT NULL,
price NUMERIC(10, 2) NOT NULL,
stock_quantity INTEGER NOT NULL,
memo TEXT,
deleted_at TIMESTAMPTZ
);

-- 注文トランザクション (orders_trn)
CREATE TABLE orders_trn (
order_id SERIAL PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_date DATE NOT NULL,
deleted_at TIMESTAMPTZ,
FOREIGN KEY (customer_id) REFERENCES customers_mst(customer_id)
);

-- 注文明細トランザクション (order_details_trn)
CREATE TABLE order_details_trn (
order_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
quantity INTEGER NOT NULL,
deleted_at TIMESTAMPTZ,
PRIMARY KEY (order_id, product_id),
FOREIGN KEY (order_id) REFERENCES orders_trn(order_id),
FOREIGN KEY (product_id) REFERENCES products_mst(product_id)
);

-- サンプルデータの挿入

-- customers_mst にデータ挿入
INSERT INTO customers_mst (customer_name, email, created_date) VALUES
('佐藤 太郎', 'sato.taro@example.com', '2023-01-15'),
('鈴木 花子', 'suzuki.hanako@example.com', '2023-02-20'),
('田中 健太', 'tanaka.kenta@example.com', '2023-03-01'),
('山田 恵美', 'yamada.emi@example.com', '2023-04-10'),
('渡辺 剛', 'watanabe.takeshi@example.com', '2023-05-05'),
('高橋 明', 'takahashi.akira@example.com', '2023-06-12'),
('中村 友子', 'nakamura.tomoko@example.com', '2023-07-25'),
('小林 大輔', 'kobayashi.daisuke@example.com', '2023-08-01');

-- products_mst にデータ挿入
INSERT INTO products_mst (category, product_name, price, stock_quantity, memo) VALUES
('Electronics', 'ワイヤレスイヤホン', 12800.00, 150, '高音質でノイズキャンセリング機能付き'),
('Books', 'SQL 入門', 2500.00, 200, NULL), -- NULL データ
('Home & Kitchen', '電気ケトル', 4500.00, 80, '1L 容量、自動電源オフ機能'),
('Electronics', 'スマートウォッチ', 29800.00, 100, '心拍数モニタリング、GPS 搭載'),
('Books', 'Python プログラミング', 3200.00, 120, '初心者向けの解説書'),
('Food', 'オーガニックコーヒー豆', 1800.00, 300, NULL), -- NULL データ
('Home & Kitchen', '高性能ブレンダー', 9800.00, 50, 'スムージー作りに最適'),
('Electronics', 'USB 充電器', 1500.00, 500, 'PD 対応、急速充電可能'),
('Books', 'データ分析の基礎', 3800.00, 90, '統計学の基本から学習'),
('Food', '高級チョコレートアソート', 2200.00, 180, 'ギフトにも最適'),
('Electronics', 'ゲーミングマウス', 7800.00, 70, NULL), -- NULL データ
('Home & Kitchen', 'セラミックフライパン', 5500.00, 110, '焦げ付きにくい加工'),
('Books', '自己啓発の法則', 1800.00, 250, '成功へのヒント'),
('Electronics', 'ポータブルバッテリー', 3980.00, 220, '大容量、軽量設計'),
('Food', '国産はちみつ', 1200.00, 400, '100%純粋なはちみつ');

-- orders_trn にデータ挿入
INSERT INTO orders_trn (customer_id, order_date) VALUES
(1, '2023-08-01'), -- 佐藤太郎
(2, '2023-08-05'), -- 鈴木花子
(1, '2023-08-10'), -- 佐藤太郎 (2 回目)
(3, '2023-08-12'), -- 田中健太
(4, '2023-08-15'), -- 山田恵美
(2, '2023-08-20'), -- 鈴木花子 (2 回目)
(5, '2023-08-22'), -- 渡辺剛
(1, '2023-08-25'), -- 佐藤太郎 (3 回目)
(6, '2023-09-01'), -- 高橋明
(7, '2023-09-05'), -- 中村友子
(8, '2023-09-10'); -- 小林大輔

-- order_details_trn にデータ挿入
INSERT INTO order_details_trn (order_id, product_id, quantity) VALUES
(1, 1, 1), -- 佐藤: ワイヤレスイヤホン
(1, 6, 2), -- 佐藤: オーガニックコーヒー豆
(2, 2, 1), -- 鈴木: SQL 入門
(2, 3, 1), -- 鈴木: 電気ケトル
(3, 4, 1), -- 佐藤: スマートウォッチ
(3, 8, 3), -- 佐藤: USB 充電器
(4, 5, 1), -- 田中: Python プログラミング
(4, 7, 1), -- 田中: 高性能ブレンダー
(5, 1, 1), -- 山田: ワイヤレスイヤホン
(5, 9, 1), -- 山田: データ分析の基礎
(6, 10, 2), -- 鈴木: 高級チョコレートアソート
(7, 11, 1), -- 渡辺: ゲーミングマウス
(7, 14, 1), -- 渡辺: ポータブルバッテリー
(8, 2, 1), -- 佐藤: SQL 入門
(8, 12, 1), -- 佐藤: セラミックフライパン
(9, 13, 1), -- 高橋: 自己啓発の法則
(10, 15, 5), -- 中村: 国産はちみつ
(11, 1, 1); -- 小林: ワイヤレスイヤホン
```

# EC サイトデータ SELECT 問題集

## 問題 1: 全ての顧客情報を取得する

### 重要度: ★★★

### 問題目的: テーブルから全ての行と列を取得する SELECT \*の基本的な使い方を理解する。

### 問題: customers_mst テーブルに登録されている全ての顧客の情報を取得してください。

### 答え:

```SQL
SELECT *
FROM customers_mst;
```

## 問題 2: 特定の商品情報のみを取得する

### 重要度: ★★★

### 問題目的: 特定の列だけを選択して取得する SELECT 列名の基本的な使い方を理解する。

### 問題: products_mst テーブルから、商品の名前(product_name)と価格(price)だけを取得してください。

### 答え:

```SQL
SELECT product_name, price
FROM products_mst;
```

## 問題 3: 高価格な商品を絞り込む

### 重要度: ★★★★

### 問題目的: WHERE 句と比較演算子(>)を使用して、条件に合致するデータを絞り込む方法を理解する。

### 問題: products_mst テーブルから、価格が 10000 円より高い商品の全ての情報を取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE price > 10000;
```

## 問題 4: 特定のカテゴリの商品を検索する

### 重要度: ★★★★

### 問題目的: WHERE 句と等号演算子(=)を使用して、特定の文字列に一致するデータを絞り込む方法を理解する。

### 問題: products_mst テーブルから、カテゴリ(category)が 'Electronics' の商品の、商品名と価格を取得してください。

### 答え:

```SQL
SELECT product_name, price
FROM products_mst
WHERE category = 'Electronics';
```

## 問題 5: 商品を価格が高い順に並べる

### 重要度: ★★★★

### 問題目的: ORDER BY 句と DESC を使用して、結果を降順に並び替える方法を理解する。

### 問題: products_mst テーブルから、全ての商品の情報を取得し、価格が高い順に並び替えてください。

### 答え:

```SQL
SELECT *
FROM products_mst
ORDER BY price DESC;
```

## 問題 6: 在庫数が少ない商品を特定する

### 重要度: ★★★★

### 問題目的: WHERE 句と比較演算子(<)、および ORDER BY 句を組み合わせて、実用的な絞り込みと並び替えを行う。

### 問題: products_mst テーブルから、在庫数(stock_quantity)が 100 個未満の商品を、在庫数が少ない順に（昇順で）表示してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE stock_quantity < 100
ORDER BY stock_quantity ASC; -- ASC はデフォルトなので省略しても OK
```

## 問題 7: 特定の範囲内の価格を持つ商品を検索する

### 重要度: ★★★★★

### 問題目的: WHERE 句と BETWEEN 演算子を使用して、数値範囲内のデータを効率的に絞り込む方法を理解する。

### 問題: products_mst テーブルから、価格が 5000 円以上 10000 円以下の商品の情報を取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE price BETWEEN 5000 AND 10000;
```

### バリエーション: AND 演算子と>=, <=を使った場合

```SQL
SELECT *
FROM products_mst
WHERE price >= 5000 AND price <= 10000;
```

## 問題 8: 複数のカテゴリの商品を検索する

### 重要度: ★★★★★

### 問題目的: WHERE 句と IN 演算子を使用して、複数の値のいずれかに一致するデータを絞り込む方法を理解する。

### 問題: products_mst テーブルから、カテゴリが'Books'または'Food'の商品の情報を取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE category IN ('Books', 'Food');
```

### バリエーション: OR 演算子を使った場合

```SQL
SELECT *
FROM products_mst
WHERE category = 'Books' OR category = 'Food';
```

## 問題 9: 特定の文字列を含む商品名を検索する

### 重要度: ★★★★★

### 問題目的: WHERE 句と LIKE 演算子（%ワイルドカード）を使用して、部分一致で文字列を検索する方法を理解する。

### 問題: products_mst テーブルから、商品名(product_name)に**「ワイヤレス」という文字列が含まれる**商品を全て取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE product_name LIKE '%ワイヤレス%';
```

## 問題 10: 特定の条件を満たす顧客と注文を組み合わせる

### 重要度: ★★★★★

### 問題目的: 複数の WHERE 条件を AND や OR で組み合わせ、結果を ORDER BY で並び替える複合的なクエリを作成する。

### 問題: customers_mst テーブルから、2023 年 3 月 1 日以降に登録された顧客の情報のうち、メールアドレスに「example.com」が含まれる顧客を、登録日が新しい順に取得してください。

### 答え:

```SQL
SELECT customer_name, email, created_date
FROM customers_mst
WHERE created_date >= '2023-03-01' AND email LIKE '%example.com%'
ORDER BY created_date DESC;
```

## 問題 11: 特定の件数のみ取得する (LIMIT)

### 重要度: ★★★★

### 問題目的: LIMIT 句を使用して、取得する行の数を制限する方法を理解する。

### 問題: products_mst テーブルから、価格が高い順に上位 3 件の商品名と価格を取得してください。

### 答え:

```SQL
SELECT product_name, price
FROM products_mst
ORDER BY price DESC
LIMIT 3;
```

## 問題 12: 特定の開始位置からデータを取得する (OFFSET)

### 重要度: ★★★★

### 問題目的: OFFSET 句と LIMIT 句を組み合わせて、特定の開始位置からデータを取得する方法（ページネーションの基礎）を理解する。

### 問題: products_mst テーブルから、価格が高い順に並べた際に、4 番目から 2 件の商品名と価格を取得してください。

### 答え:

```SQL
SELECT product_name, price
FROM products_mst
ORDER BY price DESC
LIMIT 2 OFFSET 3; -- OFFSET 3 は 0 から数えて 3 番目（つまり 4 つ目）のレコードから開始する
```

## 問題 13: メモが登録されていない商品を見つける (IS NULL)

### 重要度: ★★★★★

### 問題目的: NULL 値の特殊な扱いや IS NULL 演算子の重要性を理解する。

### 問題: products_mst テーブルから、メモ(memo)が登録されていない（NULL である）商品の商品名とカテゴリを取得してください。

### 答え:

```SQL
SELECT product_name, category
FROM products_mst
WHERE memo IS NULL;
```

## 問題 14: メモが登録されている商品を見つける (IS NOT NULL)

### 重要度: ★★★★★

### 問題目的: IS NOT NULL 演算子の使い方を理解する。

### 問題: products_mst テーブルから、メモ(memo)が登録されている（NULL ではない）商品の商品名とメモを取得してください。

### 答え:

```SQL
SELECT product_name, memo
FROM products_mst
WHERE memo IS NOT NULL;
```

## 問題 15: 複合条件と複数列での並び替え

### 重要度: ★★★★★

### 問題目的: 複数の論理演算子(AND, OR)と複数列での並び替えを組み合わせた複雑なクエリの作成能力を養う。

### 問題: products_mst テーブルから、カテゴリが'Electronics'で在庫数が 100 個以上の商品、またはカテゴリが'Home & Kitchen'で価格が 5000 円以下の商品を、カテゴリ名で昇順、次に価格で降順に並び替えて取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE (category = 'Electronics' AND stock_quantity >= 100)
OR (category = 'Home & Kitchen' AND price <= 5000)
ORDER BY category ASC, price DESC;
```

## 問題 16: LIKE 演算子と\*ワイルドカードの応用

### 重要度: ★★★★

### 問題目的: \*ワイルドカードを含めた LIKE 演算子のパターンマッチング能力を深める。

### 問題: products\*mst テーブルから、商品名の 2 文字目が「ー」である商品を全て取得してください。

### 答え:

```SQL
SELECT *
FROM products_mst
WHERE product_name LIKE '*ー%';
```

## 問題 17: 特定期間の顧客の登録情報検索

### 重要度: ★★★★

### 問題目的: 日付型データの範囲検索と並び替えを正確に行う。

### 問題: customers_mst テーブルから、2023 年 3 月 1 日から 2023 年 7 月 31 日までの間に登録された顧客の、顧客名と登録日を、登録日が古い順に取得してください。

### 答え:

```SQL
SELECT customer_name, created_date
FROM customers_mst
WHERE created_date BETWEEN '2023-03-01' AND '2023-07-31'
ORDER BY created_date ASC;
```

---

EC サイトデータ INSERT 問題集

## 問題 1: 全ての情報を指定して新しい顧客を登録する

### 重要度: ★★★★

### 問題目的: INSERT INTO テーブル名 VALUES (...) を使用して、テーブルの全ての列に値を指定して新しい行を追加する方法を理解する。SERIAL 型（customer_id）は自動採番されるため、INSERT 文で指定する必要がないことを確認する。

### 問題: customers_mst テーブルに、以下の情報を全て指定して新しい顧客「杉山 陽子」（メールアドレス: sugiyama.yoko@example.com、登録日: 2024-01-10）を登録してください。

### 答え:

```SQL
INSERT INTO customers_mst (customer_name, email, created_date)
VALUES ('杉山 陽子', 'sugiyama.yoko@example.com', '2024-01-10');
```

補足: customer_id は SERIAL 型なので自動採番されます。

## 問題 2: 必須項目のみ指定して新しい商品を登録する

### 重要度: ★★★★★

### 問題目的: INSERT INTO テーブル名 (列名 1, 列名 2, ...) VALUES (値 1, 値 2, ...) を使用して、特定の列にのみ値を指定して新しい行を追加する方法を理解する。DEFAULT 値が設定される列（stock_quantity）や、NULL が許可されている列（memo）の挙動を確認する。

### 問題: products_mst テーブルに、以下の情報を指定して新しい商品「ワイヤレス充電器」（カテゴリ: Electronics、価格: 3500.00）を登録してください。在庫数とメモは指定しないでください。

### 答え:

```SQL
INSERT INTO products_mst (category, product_name, price)
VALUES ('Electronics', 'ワイヤレス充電器', 3500.00);
```

補足: stock_quantity にはデフォルト値の 0 が、memo には NULL が自動的に設定されます。

## 問題 3: 特定の注文に新しい注文明細を追加する

### 重要度: ★★★★★

### 問題目的: 複合主キーを持つテーブルへの INSERT 方法を理解し、既存の order_id と product_id の組み合わせが重複しないように注意する。

### 問題: 既存の orders_trn の order_id が 1 の注文に、新しい注文明細として product_id が 15 の商品を 3 個追加してください。

### 答え:

```SQL
INSERT INTO order_details_trn (order_id, product_id, quantity)
VALUES (1, 15, 3);
```

補足: この操作により、佐藤 太郎氏の 1 回目の注文（order_id=1）に、「国産はちみつ」が追加されます。もし order_id=1 と product_id=15 の組み合わせが既に存在する場合、主キー制約違反のエラーが発生します。

## 問題 4: 最小限の顧客情報で登録する

### 重要度: ★★★★

### 問題目的: 必須の NOT NULL 制約を持つ列にのみ値を指定し、それ以外の列（自動採番やデフォルト値を持つ列）を省略して INSERT できることを確認する。

### 問題: customers_mst テーブルに、顧客名「加藤 浩」（メールアドレス: kato.hiroshi@example.com）を登録してください。登録日は指定しません。

### 答え:

```SQL
INSERT INTO customers_mst (customer_name, email)
VALUES ('加藤 浩', 'kato.hiroshi@example.com');
```

補足: created_date は NOT NULL ですが、この問題では created_date を DEFAULT CURRENT_DATE としていないため、エラーになる可能性があります。もしテーブル定義で created_date DATE NOT NULL DEFAULT CURRENT_DATE と定義されていれば、このクエリは成功します。現在のテーブル定義ではエラーになりますので、研修ではその点を議論することもできます。
（※今回のテーブル定義では created_date DATE NOT NULL なので、このクエリはエラーになります。もし成功させる場合は、テーブル定義の created_date に DEFAULT CURRENT_DATE を追加するか、created_date も明示的に指定する必要があります。）

## 問題 5: 注文と注文明細を同時に登録するシナリオを考える

### 重要度: ★★★★★

### 問題目的: 関連する複数のテーブルにデータを挿入する際の順序と外部キー制約の理解を促す。実際には INSERT 文は複数になることを体験する。

### 問題: customer_id が 2 の顧客（鈴木 花子）が、2024-02-01 に「電気ケトル」（product_id=3）を 1 個、および「高級チョコレートアソート」（product_id=10）を 2 個購入する注文を登録してください。

ヒント: まず orders_trn に注文を登録し、その order_id を使用して order_details_trn に明細を登録する必要があります。PostgreSQL では RETURNING 句を使って新しく生成された order_id を取得できます。

### 答え:

```SQL
-- 1. まず orders_trn に新しい注文を登録し、生成された order_id を取得
INSERT INTO orders_trn (customer_id, order_date)
VALUES (2, '2024-02-01')
RETURNING order_id; -- この実行で order_id が返されます。例: 12

-- 2. 取得した order_id (例: 12) を使用して order_details_trn に明細を登録
-- ここでは便宜的に order_id を 12 と仮定します。実際には上のクエリ実行結果を元にしてください。
INSERT INTO order_details_trn (order_id, product_id, quantity)
VALUES (12, 3, 1); -- 電気ケトルを 1 個

INSERT INTO order_details_trn (order_id, product_id, quantity)
VALUES (12, 10, 2); -- 高級チョコレートアソートを 2 個
```

補足: RETURNING order_id は PostgreSQL 固有の便利な機能です。他の RDBMS では、シーケンスを使った値の取得や、SCOPE_IDENTITY()（SQL Server）、LAST_INSERT_ID()（MySQL）など、異なる方法で新しく生成された ID を取得します。実務ではアプリケーション側でこれらの ID を連携して次の INSERT を行うことになります。

---

EC サイトデータ UPDATE 問題集

## 問題 1: 特定商品の価格を変更する

### 重要度: ★★★★

### 問題目的: UPDATE 文と WHERE 句を用いて、特定の 1 つの行の特定の列の値を更新する基本的な方法を理解する。

### 問題: products_mst テーブルの**「ワイヤレスイヤホン」**の価格を、現在の 12800.00 円から 11980.00 円に更新してください。

### 答え:

```SQL
UPDATE products_mst
SET price = 11980.00
WHERE product_name = 'ワイヤレスイヤホン';
```

## 問題 2: 複数の情報を同時に更新する

### 重要度: ★★★★★

### 問題目的: 1 つの UPDATE 文で複数の列の値を同時に更新する方法を理解する。

### 問題: products_mst テーブルの**「SQL 入門」**について、以下の 2 つの情報を更新してください。

価格を 2200.00 円に変更する。
在庫数を 180 個に変更する。

### 答え:

```SQL
UPDATE products_mst
SET price = 2200.00, stock_quantity = 180
WHERE product_name = 'SQL 入門';
```

## 問題 3: 在庫数に基づいて商品の価格を割引する

### 重要度: ★★★★★

### 問題目的: 既存の列の値を参照して計算し、その結果で列を更新する方法を理解する。

### 問題: products_mst テーブルで、在庫数(stock_quantity)が 200 個以上ある商品の価格を 10%割引してください。

### 答え:

```SQL
UPDATE products_mst
SET price = price \* 0.9
WHERE stock_quantity >= 200;
```

### バリエーション:

```SQL
price _ 0.90 や price - (price _ 0.10) など、計算方法は複数考えられます。
```

## 問題 4: 特定カテゴリの商品のメモを更新する

### 重要度: ★★★★

### 問題目的: WHERE 句でカテゴリを指定し、複数の行を一括で更新する方法を理解する。また、TEXT 型の列の更新も行う。

### 問題: products_mst テーブルで、カテゴリ(category)が'Books'の全ての商品のメモ欄を'人気書籍'に更新してください。

### 答え:

```SQL
UPDATE products_mst
SET memo = '人気書籍'
WHERE category = 'Books';
```

## 問題 5: 登録日が古い顧客のメールアドレスを更新する（条件の組み合わせ）

### 重要度: ★★★★★

### 問題目的: WHERE 句で複数の条件（日付比較と論理演算子）を組み合わせて、更新対象を絞り込む方法を理解する。

### 問題: customers_mst テーブルで、2023 年 4 月 1 日より前に登録された顧客のメールアドレスのドメインを@example.com から@newcompany.com に変更してください。（例: sato.taro@example.com → sato.taro@newcompany.com）

### 答え:

```SQL
UPDATE customers_mst
SET email = REPLACE(email, '@example.com', '@newcompany.com')
WHERE created_date < '2023-04-01';
```

補足: REPLACE 関数は PostgreSQL の文字列操作関数です。email 列の値から特定の文字列を検索し、別の文字列に置き換えます。

## 問題 6: 特定の顧客の最近の注文日を更新する

### 重要度: ★★★★★

### 問題目的: 特定の顧客 ID に紐づく注文の中で、最新の注文日を更新する（ただし、この段階ではサブクエリは使わない）。

### 問題: orders_trn テーブルで、customer_id が 1 の顧客（佐藤 太郎）の最も新しい注文日を 2023-08-30 に更新してください。

ヒント: まず customer_id=1 の最新の order_date を持つ order_id を特定し、その order_id を使って更新します。

### 答え:

```SQL
-- 1. まず、customer_id=1 の最も新しい注文日を持つ order_id を確認する (SELECT 文で確認)
SELECT order_id, order_date
FROM orders_trn
WHERE customer_id = 1
ORDER BY order_date DESC
LIMIT 1;

-- 例えば、上記で order_id が 8 と判明した場合の更新クエリ
UPDATE orders_trn
SET order_date = '2023-08-30'
WHERE order_id = 8;
```

補足: この問題は、実務でよく遭遇する「特定の条件に合致する 1 件（または数件）のレコードを特定し、そのレコードを更新する」というシナリオをシミュレートしています。SELECT で確認し、その結果を使って UPDATE をかけるという流れです。

## 問題 7: メモが NULL の商品にデフォルト値を設定する

### 重要度: ★★★★

### 問題目的: IS NULL 演算子を使って、NULL 値を持つ行を対象に更新を行う方法を理解する。

### 問題: products_mst テーブルで、メモ(memo)が NULL である商品のメモ欄を、'詳細未設定'という文字列に更新してください。

### 答え:

```SQL
UPDATE products_mst
SET memo = '詳細未設定'
WHERE memo IS NULL;
```

## 問題 8: 特定のカテゴリに属さない商品の在庫数を調整する

### 重要度: ★★★★★

### 問題目的: NOT IN 演算子を使用して、指定した複数の条件に合致しない行を更新対象とする方法を理解する。

### 問題: products_mst テーブルで、カテゴリが'Electronics'と'Books'ではない商品の在庫数(stock_quantity)を現在の値から 10 個増加させてください。

### 答え:

```SQL
UPDATE products_mst
SET stock_quantity = stock_quantity + 10
WHERE category NOT IN ('Electronics', 'Books');
```

## 問題 9: 売れ残りの可能性のある商品の在庫をゼロにする

### 重要度: ★★★★★

### 問題目的: 複数の WHERE 条件を AND で組み合わせ、特定のシナリオ（価格が安く、かつ在庫が残っている商品）で一括更新を行う。

### 問題: products_mst テーブルで、価格が 2000 円未満で、かつ在庫数が 0 より大きい（売れ残っている）商品の在庫数(stock_quantity)を 0 に設定してください。

### 答え:

```SQL
UPDATE products_mst
SET stock_quantity = 0
WHERE price < 2000 AND stock_quantity > 0;
```

## 問題 10: 特定の顧客グループの登録日を今日の最新日付に更新する

### 重要度: ★★★★★

### 問題目的: 特定の ID リストを IN 句で指定し、日付型カラムをシステム日付関数（CURRENT_DATE）で更新する。

### 問題: customers_mst テーブルで、customer_id が 2, 5, 7 の顧客の登録日(created_date)を、今日の最新日付に更新してください。

### 答え:

```SQL
UPDATE customers_mst
SET created_date = CURRENT_DATE
WHERE customer_id IN (2, 5, 7);
```

## 問題 11: 特定のキーワードを含む商品の価格を再設定する

### 重要度: ★★★★★

### 問題目的: LIKE 演算子を WHERE 句で使用して、文字列の部分一致で更新対象を絞り込む。

### 問題: products_mst テーブルで、商品名に**「コーヒー」または「はちみつ」という文字列が含まれる商品の価格を 1500.00 円**に統一して更新してください。

### 答え:

```SQL
UPDATE products_mst
SET price = 1500.00
WHERE product_name LIKE '%コーヒー%' OR product_name LIKE '%はちみつ%';
```

## 問題 12: 【危険！】 全ての商品価格をゼロにする

### 重要度: ★★★★★

### 問題目的: WHERE 句を付けずに UPDATE 文を実行した場合、テーブル全ての行が更新されてしまうことを理解し、その危険性を認識する。

### 問題: products_mst テーブルの全ての商品の価格を 0.00 円に更新してください。

警告: この操作はテーブルの全てのデータに影響を与えます。実務環境では絶対に行わないでください。この問題は WHERE 句の重要性を理解するためのものです。

### 答え:

```SQL
UPDATE products_mst
SET price = 0.00; -- WHERE 句がないため、全ての行が更新される
```

補足: この問題は、WHERE 句の指定を忘れた場合に何が起こるかを示すためのものです。本番環境での UPDATE 文は、常に WHERE 句で更新対象を絞り込むことを徹底する必要があります。
これらの追加問題は、UPDATE 文の柔軟性と、WHERE 句の条件指定の重要性をより深く学べるように設計しました。特に最後の問題は、実務での UPDATE 操作がいかに慎重に行われるべきかを学ぶ上で非常に重要です。
次にどのようなテーマの問題を作成しましょうか？DELETE や ALTER TABLE、または結合（JOIN）など、ご希望のテーマがあれば教えてください！

---

EC サイトデータ DELETE 問題集

## 問題 1: 特定の商品を削除する

### 重要度: ★★★★

### 問題目的: DELETE 文と WHERE 句を用いて、特定の 1 つの行をテーブルから削除する基本的な方法を理解する。

### 問題: products_mst テーブルから、**「セラミックフライパン」**の情報を削除してください。

### 答え:

```SQL
DELETE FROM products_mst
WHERE product_name = 'セラミックフライパン';
```

## 問題 2: 在庫がゼロの商品を全て削除する

### 重要度: ★★★★★

### 問題目的: WHERE 句と比較演算子を用いて、条件に合致する複数の行を一括で削除する方法を理解する。

### 問題: products_mst テーブルから、在庫数(stock_quantity)が 0 個の商品を全て削除してください。

### 答え:

```SQL
DELETE FROM products_mst
WHERE stock_quantity = 0;
```

## 問題 3: 特定のカテゴリに属する商品をまとめて削除する

### 重要度: ★★★★

### 問題目的: WHERE 句でカテゴリを指定し、複数の関連する行をまとめて削除する方法を理解する。

### 問題: products_mst テーブルから、カテゴリ(category)が'Food'の全ての商品を削除してください。

### 答え:

```SQL
DELETE FROM products_mst
WHERE category = 'Food';
```

## 問題 4: 長期間利用がない顧客を削除する

### 重要度: ★★★★★

### 問題目的: 日付型の列と日付比較を用いて、古いデータを削除する方法を理解する。

### 問題: customers_mst テーブルから、2023 年 3 月 1 日より前に登録された顧客の情報を削除してください。

### 答え:

```SQL
DELETE FROM customers_mst
WHERE created_date < '2023-03-01';
```

## 問題 5: 特定の商品名パターンに合致する商品を削除する

### 重要度: ★★★★★

### 問題目的: LIKE 演算子を WHERE 句で使用して、文字列の部分一致で削除対象を絞り込む。

### 問題: products_mst テーブルから、商品名に**「充電器」という文字列が含まれる**商品を全て削除してください。

### 答え:

```SQL
DELETE FROM products_mst
WHERE product_name LIKE '%充電器%';
```

## 問題 6: メモが設定されていない商品を削除する

### 重要度: ★★★★

### 問題目的: IS NULL 演算子を使って、NULL 値を持つ行を対象に削除を行う方法を理解する。

### 問題: products_mst テーブルから、メモ(memo)が NULL である商品を全て削除してください。

### 答え:

```SQL
DELETE FROM products_mst
WHERE memo IS NULL;
```

## 問題 7: 特定の顧客 ID に関連する注文明細と注文を削除する

### 重要度: ★★★★★

### 問題目的: 外部キー制約のあるテーブルでデータを削除する際の順序を理解する。親テーブルのレコードを削除する前に子テーブルのレコードを削除する必要があることを学ぶ。

### 問題: customer_id が 4 の顧客（山田 恵美）が、これまでに購入した全ての注文明細と、その顧客に関連する全ての注文情報をデータベースから削除してください。

ヒント: 外部キー制約により、orders_trn のレコードを削除する前に、まずそれに関連する order_details_trn のレコードを削除する必要があります。

### 答え:

```SQL
-- 1. まず、削除対象の顧客に関連する注文の order_id を取得する（確認用）
SELECT order_id
FROM orders_trn
WHERE customer_id = 4;
-- 例: order_id = 5 が該当すると仮定

-- 2. 次に、取得した order_id に紐づく注文明細（order_details_trn）を削除
DELETE FROM order_details_trn
WHERE order_id IN (SELECT order_id FROM orders_trn WHERE customer_id = 4);
-- または、もし削除対象の order_id が特定できていれば
-- DELETE FROM order_details_trn WHERE order_id = 5;

-- 3. 最後に、対象の顧客の注文（orders_trn）を削除
DELETE FROM orders_trn
WHERE customer_id = 4;
```

補足: この問題は、外部キー制約が設定されているテーブルで削除操作を行う際の重要な考慮点を示しています。実務では、ON DELETE CASCADE のような外部キーオプションを設定することで、親レコードの削除時に子レコードも自動的に削除するよう設定できますが、意図しないデータ削除を防ぐためには、手動での削除順序を理解することも重要です。

## 問題 8: 【極めて危険！】 全ての注文データを削除する

### 重要度: ★★★★★

### 問題目的: WHERE 句を付けずに DELETE 文を実行した場合、テーブル全ての行が削除されてしまうことを理解し、その極めて高い危険性を認識する。

### 問題: orders_trn テーブルの全ての注文情報を削除してください。

警告: この操作はテーブルの全てのデータに影響を与えます。実務環境では絶対に行わないでください。この問題は WHERE 句の重要性と、DELETE の破壊的な側面を理解するためのものです。

### 答え:

```SQL
DELETE FROM orders_trn; -- WHERE 句がないため、全ての行が削除される
```

補足: この問題は、UPDATE と同様に WHERE 句の指定を忘れた場合に何が起こるかを示すためのものです。本番環境での DELETE 文は、常に WHERE 句で削除対象を絞り込むことを徹底し、実行前には必ず内容を確認することが必要です。
これらの DELETE 問題は、データの削除における基礎から応用、そして最も重要な「危険な操作」の認識までを網羅しています。特に外部キー制約が絡む問題や、WHERE 句なしの DELETE は実務で非常に重要な学習ポイントとなるでしょう。
次に、ALTER TABLE や JOIN など、さらに高度な SQL 操作の問題に進みますか？ご希望のテーマをお知らせください！
