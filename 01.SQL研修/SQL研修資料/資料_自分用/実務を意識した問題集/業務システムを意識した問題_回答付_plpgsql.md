### 演習用のテーブルとデータ

**1. 追加テーブル定義 (DDL)**

```sql
-- ==================================================
-- 演習用テーブル定義 (DDL)
-- ==================================================

-- 商品マスタ
CREATE TABLE products (
    product_id INTEGER PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price INTEGER NOT NULL
);

-- 店舗マスタ
CREATE TABLE stores (
    store_id INTEGER PRIMARY KEY,
    store_name VARCHAR(50) NOT NULL,
    prefecture VARCHAR(10) NOT NULL
);

-- 顧客マスタ
CREATE TABLE customers (
    customer_id INTEGER PRIMARY KEY,
    customer_name VARCHAR(50) NOT NULL,
    registration_date DATE NOT NULL,
    address VARCHAR(255),
    status VARCHAR(20) DEFAULT '有効'
);

-- システム設定マスタ
CREATE TABLE system_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL,
    description TEXT
);

-- 売上実績
CREATE TABLE sales (
    sale_id BIGSERIAL PRIMARY KEY,
    sale_timestamp TIMESTAMP NOT NULL,
    customer_id INTEGER REFERENCES customers(customer_id),
    store_id INTEGER REFERENCES stores(store_id),
    product_id INTEGER REFERENCES products(product_id),
    quantity INTEGER NOT NULL
);

-- 在庫テーブル
CREATE TABLE inventory (
    store_id INTEGER NOT NULL REFERENCES stores(store_id),
    product_id INTEGER NOT NULL REFERENCES products(product_id),
    stock_quantity INTEGER NOT NULL,
    PRIMARY KEY (store_id, product_id)
);

-- 出荷指示テーブル
CREATE TABLE shipping_instructions (
    shipping_id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales(sale_id),
    customer_id INTEGER NOT NULL,
    shipping_address VARCHAR(255) NOT NULL,
    product_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT '未出荷', -- (未出荷, 出荷済, キャンセル)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 店舗別月次売上サマリ
CREATE TABLE store_monthly_summaries (
    store_id INTEGER NOT NULL,
    target_month DATE NOT NULL,
    total_sales BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (store_id, target_month)
);

-- データ品質ログ
CREATE TABLE data_quality_log (
    log_id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    target_record_id VARCHAR(50) NOT NULL,
    issue_description TEXT NOT NULL,
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

```

**2. 追加・変更サンプルデータ (DML)**

```sql
-- ==================================================
-- 演習用サンプルデータ (DML)
-- ==================================================

-- 商品マスタ
INSERT INTO products (product_id, product_name, price) VALUES
(1, '高性能ノートPC', 150000),
(2, 'ワイヤレスマウス', 3000),
(3, '27インチ4Kモニター', 45000),
(4, 'メカニカルキーボード', 12000),
(5, 'USB-Cハブ', 4000);

-- 店舗マスタ
INSERT INTO stores (store_id, store_name, prefecture) VALUES
(1, '東京本店', '東京都'),
(2, '大阪支店', '大阪府'),
(3, '福岡営業所', '福岡県');

-- 顧客マスタ
INSERT INTO customers (customer_id, customer_name, registration_date, address, status) VALUES
(101, '田中 太郎', '2024-01-15', '東京都千代田区1-1-1', '有効'),
(102, '鈴木 一郎', '2024-02-20', '大阪府大阪市北区2-2-2', '有効'),
(103, '佐藤 花子', '2024-03-10', '福岡県福岡市博多区3-3-3', '有効'),
(104, '高橋 次郎', '2025-09-01', '東京都渋谷区4-4-4', '有効'),
(105, '伊藤 三郎', '2025-10-05', '大阪府堺市5-5-5', '有効'),
-- 問題5用の不正データ
(901, '未来 祐子', '2099-12-31', '未来市1-2-3', '有効'),
(902, '㈱テスト', '2024-05-15', 'テスト県テスト市4-5-6', '有効');

-- システム設定マスタ
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('consumption_tax_rate', '0.10', '消費税率 (例: 10%なら0.10)');

-- 売上実績
INSERT INTO sales (sale_timestamp, customer_id, store_id, product_id, quantity) VALUES
('2025-09-10 13:30:00', 101, 1, 1, 1),
('2025-09-10 13:30:00', 101, 1, 2, 1),
('2025-09-15 18:00:00', 102, 2, 3, 2),
('2025-10-01 11:00:00', 103, 1, 4, 1),
('2025-10-05 15:45:00', 105, 2, 1, 1),
('2025-10-20 10:10:00', 101, 1, 5, 1),
('2025-10-25 19:20:00', 104, 3, 2, 2),
('2025-11-01 12:00:00', 102, 2, 3, 1);

-- 在庫テーブル
INSERT INTO inventory (store_id, product_id, stock_quantity) VALUES
(1, 1, 10), (1, 2, 50), (1, 3, 5), (1, 4, 20), (1, 5, 30),
(2, 1, 5), (2, 2, 100), (2, 3, 10), (2, 4, 0), (2, 5, 10),
(3, 1, 2), (3, 2, 30), (3, 3, 0), (3, 4, 5), (3, 5, 5);
```

---

### PL/pgSQL 演習問題 (5問) - 基本編

### 問題1：特定店舗の在庫一覧取得関数

**【想定タスク】**
店舗のスタッフが、自分が所属する店舗の現在の在庫状況をシステムで確認する場面を想定します。商品IDだけでは分かりにくいため、商品名も一覧で表示する必要があります。

**【問題】**
店舗ID (`p_store_id` INTEGER) を引数として受け取り、その店舗にある商品の「商品名」「在庫数」を一覧で返す関数 `get_store_inventory` を作成してください。在庫数が0の商品も表示してください。

**【解答例】**
```sql
CREATE OR REPLACE FUNCTION get_store_inventory(p_store_id INTEGER)
RETURNS TABLE(product_name VARCHAR, stock_quantity INTEGER) AS $$
BEGIN
    RETURN QUERY
    SELECT
        p.product_name,
        i.stock_quantity
    FROM
        inventory AS i
    JOIN
        products AS p ON i.product_id = p.product_id
    WHERE
        i.store_id = p_store_id
    ORDER BY
        p.product_id;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- SELECT * FROM get_store_inventory(1); -- 東京本店の在庫一覧
```

**【出題意図】**
*   **SQLの基本:** `SELECT`, `JOIN`, `WHERE` という最も基本的なSQL構文を正しく使えるかを確認します。
*   **関数の基本:** `RETURNS TABLE` を使った、複数行の結果を返す関数の基本的な書き方を習得させます。
*   **データの意味を考える:**
    *   なぜ `inventory` テーブルだけでなく `products` テーブルを `JOIN` する必要があるのか？ → システムが扱う「ID」と、人間（ユーザー）が理解する「名前」を結びつける必要性を理解させます。これは業務システムの基本です。
    *   この関数が返すデータが「誰に、何のために」使われるのか（店舗スタッフが、顧客への案内や棚卸しに使う）を想像させ、SQLが実際の業務シーンとどう繋がっているかを意識させます。

---

### 問題2：商品価格のランク判定関数

**【想定タスク】**
商品の一覧画面で、価格帯によって商品のアイコンや表示色を変える要件が出ました。そのロジックをデータベース側の共通関数として用意します。

**【問題】**
商品ID (`p_product_id` INTEGER) を引数として受け取り、その商品の価格 (`price`) に応じて価格ランクを文字列で返す関数 `get_product_price_rank` を作成してください。ランクの判定基準は以下の通りです。

*   50,000円以上: '高価格帯'
*   10,000円以上 50,000円未満: '中価格帯'
*   10,000円未満: '低価格帯'

**【解答例】**
```sql
CREATE OR REPLACE FUNCTION get_product_price_rank(p_product_id INTEGER)
RETURNS VARCHAR AS $$
DECLARE
    v_price INTEGER;
    v_rank VARCHAR;
BEGIN
    -- 商品マスタから価格を取得
    SELECT price INTO v_price
    FROM products
    WHERE product_id = p_product_id;

    -- 価格に応じてランクを判定
    IF v_price >= 50000 THEN
        v_rank := '高価格帯';
    ELSIF v_price >= 10000 THEN
        v_rank := '中価格帯';
    ELSE
        v_rank := '低価格帯';
    END IF;

    RETURN v_rank;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- SELECT product_name, price, get_product_price_rank(product_id) FROM products;
```

**【出題意図】**
*   **PL/pgSQLの基本構文:** `DECLARE` による変数宣言、`SELECT INTO` での単一レコードの取得、`IF-ELSIF-ELSE` による条件分岐という、PL/pgSQLの基本的な流れを学びます。
*   **ロジックのカプセル化:** 「価格帯を判定する」というビジネスロジックを関数として一つにまとめることのメリット（再利用性、仕様変更時の修正箇所の限定）を理解させます。
*   **データの意味を考える:**
    *   この「価格ランク」というデータは、元のテーブルには存在しない「導出データ」です。既存のデータから、ビジネス上の意味を持つ新しいデータを生成する、というSQLの役割を理解させます。
    *   もし将来、ランクの基準が変更になった場合（例：10万円以上を「超高価格帯」とする）、どこを修正すれば良いかを考えさせ、ロジックを一元管理する重要性を学ばせます。

---

### 問題3：特定商品の価格改定プロシージャ

**【想定タスク】**
仕入れ価格の変動に伴い、特定商品の販売価格を改定する必要がでてきました。安全に価格を更新するための手順（プロシージャ）を準備します。

**【問題】**
商品ID (`p_product_id` INTEGER) と、新しい価格 (`p_new_price` INTEGER) を引数として受け取り、`products` テーブルの価格を更新するプロシージャ `update_product_price` を作成してください。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE update_product_price(p_product_id INTEGER, p_new_price INTEGER) AS $$
BEGIN
    -- productsテーブルの価格を更新
    UPDATE products
    SET price = p_new_price
    WHERE product_id = p_product_id;

    RAISE NOTICE '商品ID: % の価格を % 円に更新しました。', p_product_id, p_new_price;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- CALL update_product_price(5, 4500); -- USB-Cハブを4500円に価格改定
```

**【出題意図】**
*   **データ更新処理の基本:** `UPDATE`文をプロシージャ内で実行する基本的な方法を学びます。引数を使って、更新対象のレコードと値を動的に変更できることを理解します。
*   **プロシージャの役割:** なぜ直接`UPDATE`文を実行するのではなく、プロシージャを使うのか？ → 処理を定型化し、誰が実行しても同じ手順で行えるようにするため（操作ミスの防止）という、プロシージャの役割の一つを学ばせます。
*   **データの意味を考える:**
    *   「価格」というデータは、売上や利益に直結する非常に重要なデータです。このデータを変更する処理には、なぜ慎重さが求められるのかを考えさせます。
    *   `RAISE NOTICE` で処理結果のメッセージを出すことの重要性（実行した内容が正しかったかを確認できる、ログとして残せる）を理解させ、単に処理を実行するだけでなく、実行した証跡を残す意識を養います。

---

### 問題4：売上データの簡易登録プロシージャ

**【想定タスク】**
電話注文など、システムを介さずに受けた注文を手動で売上データとして登録する場面を想定します。入力ミスを減らすため、定型的な登録処理をプロシージャとして用意します。

**【問題】**
顧客ID (`p_customer_id` INTEGER)、店舗ID (`p_store_id` INTEGER)、商品ID (`p_product_id` INTEGER)、数量 (`p_quantity` INTEGER) を引数として受け取り、`sales` テーブルに売上データを1件追加するプロシージャ `add_sales_record` を作成してください。売上日時 (`sale_timestamp`) には、プロシージャが実行された現在の時刻を記録してください。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE add_sales_record(
    p_customer_id INTEGER,
    p_store_id INTEGER,
    p_product_id INTEGER,
    p_quantity INTEGER
) AS $$
BEGIN
    -- salesテーブルに新しいレコードを挿入
    INSERT INTO sales (sale_timestamp, customer_id, store_id, product_id, quantity)
    VALUES (NOW(), p_customer_id, p_store_id, p_product_id, p_quantity);
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- CALL add_sales_record(101, 1, 3, 1); -- 田中さんが東京本店で27インチ4Kモニターを1つ購入
```

**【出題意図】**
*   **データ挿入処理の基本:** `INSERT`文をプロシージャ内で実行し、引数で渡された値を使って新しいレコードを作成する基本的な方法を学びます。`NOW()`のような、データベースが提供する便利な関数の使い方を知ります。
*   **インターフェースとしての役割:** このプロシージャは、アプリケーションや他のバッチ処理から呼び出される「部品（インターフェース）」として機能します。データ登録のロジックをDB側に集約するメリットを理解させます。
*   **データの意味を考える:**
    *   引数として渡されるID（顧客ID, 店舗ID, 商品ID）が、それぞれどのマスタテーブルのどのレコードを指しているのかを意識させます。これらのIDが間違っていると、データの関連性が崩れ、意味のないデータになってしまうことを理解させます（参照整合性）。
    *   なぜ `sale_id` は引数で渡さず、`sale_timestamp` は `NOW()` で自動的に入れるのか？ → 主キー（`BIGSERIAL`）やタイムスタンプのように、データベース側で自動的に採番・設定すべきデータの存在を理解させます。

---

### 問題5：顧客別購入回数の集計プロシージャ

**【想定タスク】**
営業担当者が、各顧客がこれまでに何回購入してくれたか（来店回数や注文回数に相当）を簡単に把握したい、という要望がありました。顧客ごとの購入回数を集計する簡単な処理を作成します。

**【問題】**
顧客ID (`p_customer_id` INTEGER) を引数として受け取り、その顧客の`sales`テーブル上のレコード数（=購入回数）を数え、`RAISE NOTICE` を使って「顧客名：〇〇様、購入回数：〇回」という形式で結果メッセージを表示するプロシージャ `show_customer_purchase_count` を作成してください。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE show_customer_purchase_count(p_customer_id INTEGER) AS $$
DECLARE
    v_customer_name VARCHAR;
    v_purchase_count INTEGER;
BEGIN
    -- 顧客名を取得
    SELECT customer_name INTO v_customer_name
    FROM customers
    WHERE customer_id = p_customer_id;

    -- 購入回数を集計
    SELECT COUNT(*) INTO v_purchase_count
    FROM sales
    WHERE customer_id = p_customer_id;

    -- 結果をメッセージで表示
    RAISE NOTICE '顧客名： %様、購入回数： %回', v_customer_name, v_purchase_count;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- CALL show_customer_purchase_count(101);
```

**【出題意図】**
*   **集計関数の基本:** `COUNT()` を使って特定の条件に合致するレコード数を数える、という基本的な集計処理を学びます。
*   **複数テーブルからの情報取得:** 顧客名（`customers`テーブル）と購入回数（`sales`テーブル）という、異なるテーブルにある情報を組み合わせて一つの意味のある結果（誰が何回買ったか）を出力する流れを体験します。
*   **データの意味を考える:**
    *   「購入回数」というデータは、元のテーブルには直接存在しない、集計によって初めて生まれる「指標」です。このような指標が、顧客のロイヤリティを測るなど、ビジネス上の判断材料になることを理解させます。
    *   このプロシージャでは単純に`COUNT(*)`で回数を数えていますが、「1回の会計で複数商品を買っても1回と数えるべきでは？」といった、より業務に即した「購入回数」の定義について考えるきっかけを与えます。これにより、単純な集計の裏にある業務ルールの重要性を意識させます。

---

### PL/pgSQL 演習問題 (5問) - 中級編

### 問題1：日次在庫締め処理プロシージャ

**【想定タスク】**
ECサイトの裏側では、日中のオンライン処理の負荷を避けるため、1日の売上をまとめて在庫に反映する夜間バッチが動いています。本日分の売上実績（`sales`テーブル）を集計し、`inventory`テーブルの在庫数を更新する日次締め処理を実装します。

**【問題】**
引数として対象日 (`target_date` DATE) を受け取り、その日の商品ごとの売上数量を合計し、店舗ごとの在庫（`inventory`テーブル）から差し引くプロシージャ `execute_daily_stock_closing` を作成してください。もし、この処理によって在庫がマイナスになってしまう商品があった場合は、その商品の更新は行わず、エラーメッセージを出力してください（処理全体は停止させない）。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE execute_daily_stock_closing(target_date DATE) AS $$
DECLARE
    sales_rec RECORD;
    current_stock INTEGER;
BEGIN
    -- 対象日の店舗別・商品別売上数量を集計するループ
    FOR sales_rec IN
        SELECT
            s.store_id,
            s.product_id,
            p.product_name,
            SUM(s.quantity) AS total_quantity
        FROM
            sales AS s
        JOIN
            products AS p ON s.product_id = p.product_id
        WHERE
            s.sale_timestamp::date = target_date
        GROUP BY
            s.store_id, s.product_id, p.product_name
    LOOP
        -- 現在庫を悲観ロックで取得
        SELECT stock_quantity INTO current_stock
        FROM inventory
        WHERE store_id = sales_rec.store_id AND product_id = sales_rec.product_id
        FOR UPDATE;

        -- 在庫が足りるかチェック
        IF current_stock >= sales_rec.total_quantity THEN
            -- 在庫を更新
            UPDATE inventory
            SET stock_quantity = stock_quantity - sales_rec.total_quantity
            WHERE store_id = sales_rec.store_id AND product_id = sales_rec.product_id;
            RAISE NOTICE '在庫更新完了: 店舗ID=%, 商品=%s, 更新数量=%', sales_rec.store_id, sales_rec.product_name, sales_rec.total_quantity;
        ELSE
            -- 在庫不足の場合はエラーメッセージを出力
            RAISE WARNING '在庫不足エラー: 店舗ID=%, 商品=%s, 現在庫=%, 要求数=%',
                sales_rec.store_id, sales_rec.product_name, current_stock, sales_rec.total_quantity;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 実行例（2025-09-10の売上を在庫に反映）
-- CALL execute_daily_stock_closing('2025-09-10');
```

**【出題意図】**
*   **バッチ処理の実装:** 大量データを一括で処理する典型的なバッチ処理のロジックを実装するスキルを養います。カーソルやFORループを使った行単位の処理を学びます。
*   **データ整合性の担保:** 在庫がマイナスになるという「ありえないデータ」の発生を防ぐためのチェックロジックの重要性を理解させます。`RAISE WARNING` を使い、エラーがありつつも処理を継続させる方法を学びます。
*   **データの意味を考える:**
    *   なぜオンライン（売上が発生した都度）で在庫を更新せず、夜間バッチで一括処理するのか？ → 日中の高負荷なトランザクションを避け、システム全体のパフォーマンスを安定させるため、という設計思想を理解させます。
    *   「在庫数」というデータが、単なる数値ではなく、企業の資産であり、販売機会に直結する重要な指標であることを意識させます。データの不整合がビジネス上の損失（欠品、機会損失）に繋がることを考えさせます。

---

### 問題2：物流システム連携用データ作成プロシージャ

**【想定タスク】**
基幹システムから物流システムへ、出荷すべき商品の情報を連携する必要があります。毎日定時に、その日に登録された売上データの中から出荷対象の情報を抽出し、物流システムが読み込める中間テーブル（`shipping_instructions`）にデータを作成するバッチ処理を実装します。

**【問題】**
引数として対象日 (`target_date` DATE) を受け取り、その日に`sales`テーブルに登録されたデータをもとに、`shipping_instructions`テーブルに出荷指示データを作成するプロシージャ `create_shipping_data_for_logistics` を作成してください。`shipping_address`には、`customers`テーブルの住所を入れてください。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE create_shipping_data_for_logistics(target_date DATE) AS $$
BEGIN
    INSERT INTO shipping_instructions (sale_id, customer_id, shipping_address, product_id, quantity)
    SELECT
        s.sale_id,
        s.customer_id,
        c.address AS shipping_address,
        s.product_id,
        s.quantity
    FROM
        sales AS s
    JOIN
        customers AS c ON s.customer_id = c.customer_id
    WHERE
        s.sale_timestamp::date = target_date
        AND NOT EXISTS ( -- 既に登録済みのデータは除外（冪等性の確保）
            SELECT 1
            FROM shipping_instructions si
            WHERE si.sale_id = s.sale_id
        );
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- CALL create_shipping_data_for_logistics('2025-10-05');
```

**【出題意図】**
*   **システム間連携:** 異なるシステム間でデータを連携する際の基本的な考え方（必要なデータを抽出し、相手のフォーマットに合わせて変換・作成する）を学びます。
*   **バッチの冪等性:** `NOT EXISTS`句などを用いて、バッチ処理を何度実行しても結果が同じになる「冪等性」を担保する実装方法を習得させます。これは、障害発生時の再実行などを考慮した、堅牢なバッチ処理に不可欠な考え方です。（「冪等性（べきとうせい）」とは、**ある操作を1回実行しても、複数回実行しても、結果が同じになる性質**）
*   **データの意味を考える:**
    *   なぜ`sales`テーブルを直接、物流システムに見せないのか？ → システム間の依存度を下げ（疎結合）、お互いのテーブル構造の変更が影響しないようにするため、というシステム設計の基本原則を理解させます。
    *   物流システムにとって必要な情報は何か？（例：顧客名より届け先住所、商品価格より商品コードと数量が重要）を考えさせ、データの使われる「文脈」によって、その価値や見せ方が変わることを学ばせます。

---

### 問題3：トリガーによる出荷指示の自動登録

**【想定タスク】**
リアルタイム性が求められる一部の注文について、売上データが登録された瞬間に、即座に出荷指示データを作成する仕組みを導入することになりました。これにより、注文から発送までのリードタイム短縮を狙います。

**【問題】**
`sales`テーブルに新しいレコードが`INSERT`された後（`AFTER INSERT`）、その売上情報を元に`shipping_instructions`テーブルに対応するレコードを自動で作成するトリガー関数 `trg_create_shipping_instruction` と、そのトリガーを作成してください。

**【解答例】**
```sql
-- トリガー関数
CREATE OR REPLACE FUNCTION trg_create_shipping_instruction()
RETURNS TRIGGER AS $$
DECLARE
    v_address VARCHAR;
BEGIN
    -- 顧客マスタから住所を取得
    SELECT address INTO v_address FROM customers WHERE customer_id = NEW.customer_id;

    -- 出荷指示テーブルにデータを挿入
    INSERT INTO shipping_instructions (sale_id, customer_id, shipping_address, product_id, quantity)
    VALUES (NEW.sale_id, NEW.customer_id, v_address, NEW.product_id, NEW.quantity);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- トリガーの作成
CREATE TRIGGER sales_after_insert_trigger
AFTER INSERT ON sales
FOR EACH ROW
EXECUTE FUNCTION trg_create_shipping_instruction();

-- 実行例（このINSERT文を実行すると、自動でshipping_instructionsにもデータが作られる）
-- INSERT INTO sales (sale_timestamp, customer_id, store_id, product_id, quantity)
-- VALUES ('2025-11-09 18:00:00', 103, 1, 5, 2);
```

**【出題意図】**
*   **トリガーの実装:** データベースのイベント（`INSERT`）を起点に、自動で別の処理を実行するトリガーの仕組みを学びます。`NEW`という特殊な変数を使って、追加された行のデータにアクセスする方法を習得します。
*   **ビジネスロジックのDB実装:** アプリケーション側の実装漏れを防ぎ、データ整合性をDBレベルで強制的に担保する方法の一つとしてトリガーを理解させます。
*   **データの意味を考える:**
    *   この処理を「バッチ」ではなく「トリガー」で実装するビジネス上の理由は何か？ → リアルタイム性、即時性という要件があることを理解させ、技術選定がビジネス要件に依存することを学ばせます。
    *   トリガーの便利さと危険性（処理がブラックボックス化しやすい、多用するとパフォーマンスに影響が出る、デバッグが難しいなど）の両面を考えさせ、適切な利用シーンを見極める重要性を意識させます。

---

### 問題4：税額計算の共通関数化

**【想定タスク】**
システム内の様々なSQL（帳票出力、APIレスポンス生成など）で、税抜価格から税込価格を計算する処理が何度も登場します。将来の税率変更に備え、計算ロジックを一元管理するため、この処理を共通関数として作成します。

**【問題】**
税抜価格 (`price` INTEGER) を引数として受け取り、`system_settings`テーブルに設定された消費税率（`consumption_tax_rate`）を元に税込価格を計算して返す関数 `calculate_tax_included_price` を作成してください。戻り値は整数とし、小数点以下は切り捨ててください。

**【解答例】**
```sql
CREATE OR REPLACE FUNCTION calculate_tax_included_price(price INTEGER)
RETURNS INTEGER AS $$
DECLARE
    tax_rate NUMERIC;
    tax_included_price INTEGER;
BEGIN
    -- 設定テーブルから税率を取得
    SELECT setting_value::NUMERIC INTO tax_rate
    FROM system_settings
    WHERE setting_key = 'consumption_tax_rate';

    -- 税込価格を計算（小数点以下切り捨て）
    tax_included_price := FLOOR(price * (1 + tax_rate));

    RETURN tax_included_price;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- SELECT product_name, price, calculate_tax_included_price(price) AS price_with_tax FROM products;
```

**【出題意図】**
*   **ロジックの共通化と再利用:** 同じロジックを何度も書くことの非効率さとリスク（修正漏れなど）を理解し、関数化によるメンテナンス性向上のメリットを学ばせます。
*   **設定値の外部化:** 税率のような将来変更される可能性のある値を、ロジック内にハードコーディングするのではなく、マスタテーブルなどから動的に取得する設計の重要性を理解させます。
*   **データの意味を考える:**
    *   なぜこの関数が必要なのか？ → 「もし関数化されていなかったら、税率が変更された時に何が起きるか？」を想像させます。多数のSQLを一つ一つ手で修正する必要があり、修正漏れやミスによる金額の不整合といった、重大な障害に繋がるリスクがあることを理解させます。
    *   この関数が返す「税込価格」というデータが、顧客への請求や会計処理の基礎となる、極めて重要なデータであることを意識させ、正確性への責任感を養います。

---

### 問題5：顧客データの品質チェックプロシージャ

**【想定タスク】**
外部システムから連携されたり、手入力されたりする顧客データには、時に不正なデータ（例：登録日が未来になっている）が含まれることがあります。データの品質を維持するため、定期的に顧客マスタをチェックし、問題のあるデータを検出してログに残すバッチ処理を実装します。

**【問題】**
`customers`テーブルの全件をスキャンし、以下の不正条件に合致するレコードを検出するプロシージャ `validate_customer_data` を作成してください。
1.  登録日 (`registration_date`) が未来の日付になっている。
2.  顧客名 (`customer_name`) にJIS第1・第2水準漢字以外で使われがちな、いわゆる機種依存文字（例：㈱）が含まれている。

不正が検出されたレコードについては、`data_quality_log`テーブルに問題の内容を記録し、`customers`テーブルの`status`を「要確認」に更新してください。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE validate_customer_data() AS $$
DECLARE
    cust_rec RECORD;
BEGIN
    FOR cust_rec IN SELECT * FROM customers WHERE status = '有効'
    LOOP
        -- 条件1: 登録日が未来かチェック
        IF cust_rec.registration_date > CURRENT_DATE THEN
            INSERT INTO data_quality_log (table_name, target_record_id, issue_description)
            VALUES ('customers', cust_rec.customer_id::TEXT, '登録日が未来の日付です: ' || cust_rec.registration_date);

            UPDATE customers SET status = '要確認' WHERE customer_id = cust_rec.customer_id;
        END IF;

        -- 条件2: 機種依存文字(㈱)が含まれているかチェック
        IF cust_rec.customer_name LIKE '%㈱%' THEN
            INSERT INTO data_quality_log (table_name, target_record_id, issue_description)
            VALUES ('customers', cust_rec.customer_id::TEXT, '顧客名に機種依存文字が含まれています: ' || cust_rec.customer_name);

            -- 同一レコードで複数エラーがあってもUPDATEは1回で済むように
            UPDATE customers SET status = '要確認' WHERE customer_id = cust_rec.customer_id;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- CALL validate_customer_data();
```

**【出題意図】**
*   **データクレンジング:** システムの安定稼働の裏側で行われている、地道だが重要なデータ品質維持活動（データクレンジング）を体験させます。
*   **防御的プログラミング:** 「データは常に正しいとは限らない」という前提に立ち、不正なデータが後続の処理に影響を与えないように、事前に検知・修正する考え方を学びます。
*   **データの意味を考える:**
    *   なぜこのようなデータチェックが必要なのか？ → 例えば、未来の日付は集計処理の不具合を、機種依存文字は別システムとの連携時の文字化けやエラーを引き起こす可能性があることを理解させます。
    *   一見正常に見えるデータでも、ビジネスルール上「不正」である場合があることを学ばせます。データの正しさは、形式的な正しさ（例：日付型であること）だけでなく、意味的な正しさ（例：未来日でないこと）も担保する必要があることを意識させ、より深いレベルでデータと向き合う姿勢を養います。

---


### エラーハンドリング強化 演習問題 (5問)

#### 演習のためのテーブル定義変更

今回の問題に取り組むにあたり、既存のテーブルに以下の変更を加えます。

```sql
-- customersテーブルにemailカラムを追加（一意制約付き）
ALTER TABLE customers ADD COLUMN email VARCHAR(255);
ALTER TABLE customers ADD CONSTRAINT customers_email_key UNIQUE (email);

-- salesテーブルにstatusカラムを追加
ALTER TABLE sales ADD COLUMN status VARCHAR(20) DEFAULT '注文受付'; -- (注文受付, 出荷済, キャンセル)

-- サンプルデータ更新
UPDATE customers SET email = 'taro.tanaka@example.com' WHERE customer_id = 101;
UPDATE customers SET email = 'ichiro.suzuki@example.com' WHERE customer_id = 102;
UPDATE customers SET email = 'hanako.sato@example.com' WHERE customer_id = 103;
UPDATE customers SET email = 'jiro.takahashi@example.com' WHERE customer_id = 104;
UPDATE customers SET email = 'saburo.ito@example.com' WHERE customer_id = 105;

-- 出荷済みデータを1件作成
UPDATE sales SET status = '出荷済' WHERE sale_id = 1;
```


### 問題1：安全な顧客情報更新プロシージャ

**【想定タスク】**
顧客情報（氏名、メールアドレス）を変更するAPIのバックエンドで呼ばれるプロシージャを実装します。このとき、存在しない顧客を指定されたり、他の人が使っているメールアドレスに変更しようとしたりする不正なリクエストを考慮し、データベースレベルでデータの整合性を保護する必要があります。

**【問題】**
顧客ID (`p_customer_id`)、新しい氏名 (`p_customer_name`)、新しいメールアドレス (`p_email`) を引数として受け取り、顧客情報を更新するプロシージャ `update_customer_safely` を作成してください。
以下のエラーケースを考慮し、それぞれ適切な例外処理を実装してください。

1.  指定された `p_customer_id` の顧客が存在しない場合、「指定された顧客IDは存在しません。」というエラーを発生させる。
2.  指定された `p_email` が既に他の顧客によって使用されている場合、「そのメールアドレスは既に使用されています。」というエラーを発生させる。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE update_customer_safely(
    p_customer_id INTEGER,
    p_customer_name VARCHAR,
    p_email VARCHAR
) AS $$
DECLARE
    customer_exists BOOLEAN;
BEGIN
    -- 1. 顧客の存在チェック
    SELECT EXISTS(SELECT 1 FROM customers WHERE customer_id = p_customer_id) INTO customer_exists;
    IF NOT customer_exists THEN
        RAISE EXCEPTION '指定された顧客IDは存在しません。(ID: %)', p_customer_id;
    END IF;

    -- 2. 顧客情報の更新
    UPDATE customers
    SET
        customer_name = p_customer_name,
        email = p_email
    WHERE
        customer_id = p_customer_id;

    RAISE NOTICE '顧客情報を更新しました。(ID: %)', p_customer_id;

EXCEPTION
    -- 一意制約違反のエラーを捕捉
    WHEN unique_violation THEN
        RAISE EXCEPTION 'そのメールアドレスは既に使用されています。(Email: %)', p_email;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- 成功ケース
-- CALL update_customer_safely(101, '田中 太郎(更新)', 'new.taro.tanaka@example.com');

-- 失敗ケース1: 存在しない顧客ID
-- CALL update_customer_safely(999, '存在しない顧客', 'dummy@example.com');

-- 失敗ケース2: メールアドレスの重複
-- CALL update_customer_safely(102, '鈴木 一郎', 'taro.tanaka@example.com');
```

**【出題意図】**
*   **事前チェックの重要性:** `UPDATE`文を実行する前に、対象レコードの存在を確認する、という防御的なプログラミングの考え方を学ばせます。
*   **例外ブロックの活用:** `EXCEPTION`ブロックを使い、データベースの制約違反（`unique_violation`）という予期せぬエラーを捕捉し、利用者に分かりやすい業務的なメッセージに変換するスキルを養います。
*   **データの意味を考える:**
    *   なぜ無愛想なDBエラーメッセージ（例: `duplicate key value violates unique constraint "customers_email_key"`）をそのまま返してはいけないのか？ → 利用者（またはフロントエンド開発者）が混乱するため、エラーの原因を特定しやすい「意味のある」メッセージに翻訳する必要があることを理解させます。
    *   メールアドレスの一意性がビジネス上なぜ重要なのか（ログイン認証、本人確認など）を考えさせ、データ制約がビジネスルールを担保していることを意識させます。

---

### 問題2：注文キャンセル処理と在庫の戻し

**【想定タスク】**
顧客からの注文キャンセル依頼に対応する処理を実装します。キャンセル処理は、単に売上データの状態を変えるだけでなく、「引き当てた在庫を元に戻す」という関連処理も伴います。これらの処理は一体不可分（アトミック）であるべきです。

**【問題】**
売上ID (`p_sale_id`) を引数として受け取り、注文をキャンセルするプロシージャ `cancel_order` を作成してください。
処理内容は以下の通りです。

1.  `sales`テーブルの`status`を「キャンセル」に更新する。
2.  その売上に対応する商品の在庫を、`inventory`テーブルに戻す（販売数量分だけ増やす）。
3.  これら2つの処理は、必ず両方成功するか、両方失敗する（元の状態に戻る）ようにしてください。

さらに、以下の業務ルール上のエラーチェックを実装してください。
*   指定された`sale_id`が存在しない場合はエラーとする。
*   既に`status`が「出荷済」の注文はキャンセルできないため、エラーとする。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE cancel_order(p_sale_id BIGINT) AS $$
DECLARE
    sale_rec RECORD;
BEGIN
    -- 対象の売上データを取得（行をロック）
    SELECT * INTO sale_rec FROM sales WHERE sale_id = p_sale_id FOR UPDATE;

    -- 存在チェック
    IF NOT FOUND THEN
        RAISE EXCEPTION '指定された売上IDは存在しません。(ID: %)', p_sale_id;
    END IF;

    -- ステータスチェック
    IF sale_rec.status = '出荷済' THEN
        RAISE EXCEPTION '出荷済みの注文はキャンセルできません。(ID: %)', p_sale_id;
    END IF;

    IF sale_rec.status = 'キャンセル' THEN
        RAISE WARNING 'この注文は既にキャンセルされています。(ID: %)', p_sale_id;
        RETURN;
    END IF;

    -- 1. 売上ステータスを更新
    UPDATE sales SET status = 'キャンセル' WHERE sale_id = p_sale_id;

    -- 2. 在庫を戻す
    UPDATE inventory
    SET stock_quantity = stock_quantity + sale_rec.quantity
    WHERE store_id = sale_rec.store_id AND product_id = sale_rec.product_id;

    RAISE NOTICE '注文をキャンセルし、在庫を戻しました。(売上ID: %)', p_sale_id;

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'キャンセル処理中に予期せぬエラーが発生しました。処理はロールバックされます。';
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- 成功ケース (sale_id=4は未出荷)
-- CALL cancel_order(4);

-- 失敗ケース1: 出荷済み
-- CALL cancel_order(1);

-- 失敗ケース2: 存在しないID
-- CALL cancel_order(999);
```

**【出題意図】**
*   **トランザクションの原子性:** 複数のテーブル更新を伴う処理で、途中でエラーが起きた場合にデータが中途半端な状態（売上はキャンセルなのに在庫が戻っていない等）になることの危険性を理解させます。PL/pgSQLのブロック全体がデフォルトでトランザクションとして扱われることを体感させます。
*   **業務ルールの実装:** 「出荷済みはキャンセル不可」といった、システムの制約ではなくビジネス上のルールを、`IF`文と`RAISE EXCEPTION`で実装する方法を学びます。
*   **データの意味を考える:**
    *   この処理の「原子性」がなぜ重要なのか？ → もし原子性が担保されないと、幽霊在庫（データ上はあるが現物はない）や逆のパターンが発生し、棚卸で差異が発覚するなど、後工程の業務に大きな混乱をもたらすことを想像させます。
    *   `sales`テーブルの`status`というデータが、後続の処理（在庫を戻すか、出荷指示を出すか等）の流れを制御する重要な「フラグ」としての意味を持つことを理解させます。

---

### 問題3：ゼロ除算を回避する平均単価計算関数

**【想定タスク】**
店舗ごとのパフォーマンスを分析するため、「顧客一人当たりの平均購入金額」を計算する帳票を作成します。しかし、中にはその月の売上が0件だった店舗もあり、単純に割り算をするとゼロ除算エラーでバッチ全体が停止してしまいます。

**【問題】**
店舗ID (`p_store_id`) と対象年月 (`p_target_month`、`YYYY-MM`形式のTEXT) を引数として受け取り、その店舗の「顧客一人当たりの平均購入金額」を返す関数 `get_average_sales_per_customer` を作成してください。
計算式は `対象月の総売上 ÷ 対象月の購入顧客数（ユニーク）` とします。

以下のエラーハンドリングを実装してください。
*   対象月の購入顧客数が0人の場合、ゼロ除算エラーを発生させずに `0` を返す。

**【解答例】**
```sql
CREATE OR REPLACE FUNCTION get_average_sales_per_customer(p_store_id INTEGER, p_target_month TEXT)
RETURNS NUMERIC AS $$
DECLARE
    total_sales BIGINT;
    unique_customer_count INTEGER;
    avg_sales NUMERIC;
BEGIN
    -- 対象年月の開始日と終了日を計算
    DECLARE
        start_date DATE := (p_target_month || '-01')::date;
        end_date DATE := (start_date + INTERVAL '1 month - 1 day');
    BEGIN
        -- 総売上とユニーク顧客数を計算
        SELECT
            COALESCE(SUM(s.quantity * p.price), 0),
            COUNT(DISTINCT s.customer_id)
        INTO
            total_sales,
            unique_customer_count
        FROM
            sales s
        JOIN
            products p ON s.product_id = p.product_id
        WHERE
            s.store_id = p_store_id
            AND s.sale_timestamp BETWEEN start_date AND end_date;
    END;

    -- 顧客数が0なら0を返す
    IF unique_customer_count = 0 THEN
        RETURN 0;
    END IF;

    -- 平均を計算
    avg_sales := total_sales::NUMERIC / unique_customer_count;

    RETURN TRUNC(avg_sales, 2); -- 小数点第3位以下を切り捨て

EXCEPTION
    WHEN division_by_zero THEN
        -- このロジックではIF文で回避しているが、万が一のための例外捕捉
        RAISE WARNING 'ゼロ除算が発生しました。0を返します。';
        RETURN 0;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- 売上があるケース (大阪支店 2025-09)
-- SELECT get_average_sales_per_customer(2, '2025-09');

-- 売上がないケース (福岡営業所 2025-09)
-- SELECT get_average_sales_per_customer(3, '2025-09');
```

**【出題意図】**
*   **ゼロ除算エラーの回避:** `division_by_zero`はバッチ処理を停止させる典型的なエラーであり、これを`IF`文による事前チェックや`NULLIF`関数で回避するテクニックを学ばせます。
*   **特定の例外の捕捉:** `EXCEPTION WHEN division_by_zero THEN ...` のように、特定のエラー条件を捕捉する方法を学びます。これにより、エラーの種類に応じた柔軟な対応が可能になります。
*   **データの意味を考える:**
    *   なぜエラーで処理を止めるのではなく、`0`を返すべきなのか？ → 1店舗のデータがないだけで帳票全体の出力が止まってしまうと、他の正常な店舗の分析もできなくなり、業務影響が大きいためです。異常ケースを想定し、システム全体を止めない「しなやかな」設計の重要性を理解させます。
    *   計算結果の`0`というデータが、「売上がなかった」というビジネス上の「意味」を表していることを意識させます。

---

### 問題4：複数件ヒットの危険性を考慮したデータ取得関数

**【想定タスク】**
ある顧客の最新の売上IDを取得する内部関数を実装します。この関数は、システム内の様々な場所から「ある顧客の最後の取引」という前提で利用されます。もし、データ不整合など何らかの理由で、本来1件しか返らないはずの問い合わせが複数件返ってしまうと、深刻なバグに繋がります。

**【問題】**
顧客ID (`p_customer_id`) を引数として受け取り、その顧客の最も新しい（`sale_timestamp`が最新の）`sale_id`を1件だけ返す関数 `get_latest_sale_id` を作成してください。
以下のエラーケースを考慮し、例外処理を実装してください。

1.  対象の顧客に一度も購入履歴がない場合、`NULL`を返す。
2.  （本来ありえないが）`sale_timestamp`が全く同じ最新の売上が複数存在した場合、「最新の売上が複数件存在し、一意に特定できません。」というエラーを発生させる。

**【解答例】**
```sql
CREATE OR REPLACE FUNCTION get_latest_sale_id(p_customer_id INTEGER)
RETURNS BIGINT AS $$
DECLARE
    latest_sale_id BIGINT;
BEGIN
    SELECT sale_id INTO latest_sale_id
    FROM sales
    WHERE customer_id = p_customer_id
    ORDER BY sale_timestamp DESC, sale_id DESC -- 同一タイムスタンプの場合はIDで順序付け
    LIMIT 1;

    RETURN latest_sale_id;

EXCEPTION
    -- SELECT INTO で結果が0件だった場合
    WHEN no_data_found THEN
        RETURN NULL;
    -- SELECT INTO で結果が2件以上だった場合（このロジックではLIMIT 1で回避しているが学習のため）
    WHEN too_many_rows THEN
        RAISE EXCEPTION '最新の売上が複数件存在し、一意に特定できません。(顧客ID: %)', p_customer_id;
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- 成功ケース
-- SELECT get_latest_sale_id(101);

-- 購入履歴がない顧客
-- INSERT INTO customers (customer_id, customer_name, registration_date) VALUES (106, '新規 顧客', '2025-11-01');
-- SELECT get_latest_sale_id(106);
```
**【出題意図】**
*   **`SELECT INTO`の例外:** `SELECT INTO`構文が、結果が0件の場合に`no_data_found`、2件以上の場合に`too_many_rows`という例外を発生させる特性を学ばせます。これは非常に頻出するエラーパターンです。
*   **データへの疑い:** 「データは常に期待通りとは限らない」という視点を養います。`LIMIT 1`などで対処する方法と、例外を検知して能動的にエラーにする方法の両面を理解させます。
*   **データの意味を考える:**
    *   なぜ「最新の売上が複数ある」ことが問題なのか？ → この関数の呼び出し元が「最新取引は1件」と想定して後続処理を組んでいる場合、予期せぬ動作（例：二重でポイントを付与する）を引き起こす可能性があることを想像させます。
    *   `NULL`という戻り値が、エラーではなく「購入履歴がない」という正常な状態を表す「意味」を持つことを理解させ、エラーと正常系の境界線を意識させます。

---

### 問題5：一括登録バッチのトランザクション制御

**【想定タスク】**
外部システムから連携された商品リスト（複数件）を、`products`テーブルに一括で登録するバッチ処理を実装します。連携データには、既に存在する商品IDや、価格がマイナスになっている不正なデータが含まれている可能性があります。

**【問題】**
商品情報の配列（`TEXT`型配列、各要素は `商品ID,商品名,価格` のCSV形式）を引数として受け取り、`products`テーブルに一括登録するプロシージャ `bulk_insert_products` を作成してください。
以下のルールに従って処理を実装してください。

1.  受け取った配列のすべての商品を、1つのトランザクション内で処理する。
2.  ループ処理中、価格がマイナスのデータが見つかった場合は、業務エラーとして処理を中断させる。
3.  1件でも登録に失敗した場合（主キー重複、業務エラーなど）、それまでに登録した商品もすべて取り消し、1件も登録されていない状態に戻す。

**【解答例】**
```sql
CREATE OR REPLACE PROCEDURE bulk_insert_products(p_products_csv_array TEXT[]) AS $$
DECLARE
    product_csv TEXT;
    product_data TEXT[];
    p_id INTEGER;
    p_name VARCHAR;
    p_price INTEGER;
BEGIN
    -- このプロシージャ全体が1つのトランザクションになる

    FOREACH product_csv IN ARRAY p_products_csv_array
    LOOP
        -- CSVをパース
        product_data := string_to_array(product_csv, ',');
        p_id := product_data[1]::INTEGER;
        p_name := product_data[2];
        p_price := product_data[3]::INTEGER;

        -- 業務ルールチェック：価格がマイナスでないか
        IF p_price < 0 THEN
            RAISE EXCEPTION '価格がマイナスの商品は登録できません。(商品ID: %, 価格: %)', p_id, p_price;
        END IF;

        -- データを挿入（主キー重複があればここで unique_violation が発生する）
        INSERT INTO products (product_id, product_name, price)
        VALUES (p_id, p_name, p_price);

        RAISE NOTICE '商品ID: % を登録しました。', p_id;
    END LOOP;

EXCEPTION
    WHEN OTHERS THEN
        -- unique_violationやその他のエラーをすべて捕捉
        RAISE INFO 'エラーが発生したため、すべての処理をロールバックします。';
        RAISE; -- 元のエラーを再スローして呼び出し元に伝える
END;
$$ LANGUAGE plpgsql;

-- 実行例
-- 成功ケース
-- CALL bulk_insert_products(ARRAY['10,新商品A,5000', '11,新商品B,12000']);

-- 失敗ケース1：途中に主キー重複
-- CALL bulk_insert_products(ARRAY['12,新商品C,8000', '1,高性能ノートPC,150000', '13,新商品D,3000']);
-- (この場合、12も登録されない)

-- 失敗ケース2：途中に業務エラー（価格マイナス）
-- CALL bulk_insert_products(ARRAY['14,新商品E,1000', '15,欠陥商品F,-500', '16,新商品G,2000']);
-- (この場合、14も登録されない)
```

**【出題意図】**
*   **ALL or NOTHINGの実現:** バッチ処理や一括更新において、「全部成功か、全部失敗か」という原子性を保証することがいかに重要かを学ばせます。プロシージャ全体が単一トランザクション内で実行されることを利用し、エラー発生時に自動的にロールバックされる挙動を体感させます。
*   **カスタム配列型の活用:** 配列を引数に取り、ループで処理するという、より実践的なデータ構造の扱い方を習得します。
*   **データの意味を考える:**
    *   なぜ「中途半端な成功」が許されないのか？ → データ連携の途中で処理が止まった場合、連携元と連携先でデータの件数が合わなくなり、どちらが正しい状態か分からなくなってしまうためです。「成功した分だけ再実行」といったリカバリ処理が非常に複雑になることを想像させ、ALL or NOTHING設計の合理性を理解させます。
    *   このプロシージャが失敗したとき、呼び出し元のプログラムは「1件も登録されていない」ことを保証されるため、リトライなどの後続処理をシンプルに実装できる、というシステム全体設計の視点を養います。