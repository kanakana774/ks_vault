# **SQL 基礎：データベースとテーブル**

## **導入：データベースと RDBMS**

この講義では、リレーショナルデータベース（RDB）の基本的な概念と、それらを操作するための SQL（Structured Query Language）の基礎を学びます。特に、PostgreSQL を例に具体的な操作を見ていきますが、ここで学ぶ知識は他の主要な RDBMS（MySQL, Oracle Database, SQL Server など）にも広く応用できます。

### **データベースとは？**

**データベース**とは、整理され、構造化された情報の集合体のことです。データを効率的に保存、管理、検索するために使用されます。

### **RDBMS（リレーショナルデータベース管理システム）とは？**

**RDBMS**は、リレーショナルデータベースを管理するためのソフトウェアです。データを**テーブル**という形式で管理し、テーブル間の関係性（リレーション）を定義できるのが特徴です。

### **テーブルとは？**

**テーブル**は、データベース内でデータを格納する基本的な構造です。Excel のスプレッドシートのように、行と列で構成されます。

- **列（カラム、フィールド）**: 特定の種類のデータを格納します（例: ユーザー名、商品の価格）。
- **行（レコード、タプル）**: 1 つのエンティティ（例: 1 人のユーザー、1 つの商品）に関するすべての情報を格納します。

## **データベースの作成：CREATE DATABASE**

新しいデータベースを作成する際に使用するコマンドです。

### **基本構文**

```SQL
CREATE DATABASE データベース名;
```

### **例**

my_company_db という名前のデータベースを作成します。

```SQL
CREATE DATABASE my_company_db;
```

### **考慮事項**

- **データベース名**: ユニークでなければなりません。
- **権限**: PostgreSQL では、CREATE DATABASE を実行するには**スーパーユーザー権限**または CREATEDB ロールが必要です。実務では権限管理が重要になるため、適切な権限を持つユーザーで実行しましょう。
- **文字エンコーディング**: 日本語を扱う場合は、UTF8 を指定することが多いです。（例: CREATE DATABASE my_db ENCODING 'UTF8';）
- **所有者（Owner）**: データベースの所有者を指定できます。（例: CREATE DATABASE my_db OWNER user_name;）
- **データベースとスキーマの関係**: RDBMS によっては、「データベース」と「スキーマ」の用語が異なる意味合いで使われることがあります。例えば、Oracle では「スキーマ」がユーザー（アカウント）に紐づく論理的な領域を指し、PostgreSQL では「データベース」の内部に複数の「スキーマ」を持つことができます。製品によって用語の定義や階層が異なるため、混同しないよう注意が必要です。

## **テーブルの作成：CREATE TABLE**

データベース内に新しいテーブルを作成するコマンドです。テーブル名、列名、データ型、および制約を定義します。

### **基本構文**

```SQL
CREATE TABLE テーブル名 (
 列名 1 データ型 [制約],
 列名 2 データ型 [制約],
 ...
 [テーブル制約]
);
```

### **命名規則の注意点**

テーブル名や列名には、以下の点に注意して命名しましょう。

- **半角英数字とアンダースコア（\_）**: これらを使用することが一般的です。
- **スネークケース**: user_id, product_name のように、単語間をアンダースコアで繋ぐ**スネークケース**が推奨されます。
- **予約語の回避**: SQL のキーワード（例: SELECT, FROM, WHERE など）はテーブル名や列名として使用できません。もしどうしても使用したい場合は、ダブルクォーテーションで囲む必要がありますが、これは非推奨です。
- **読みやすさ**: テーブルや列の役割がわかるような、意味のある名前をつけましょう。


### **列制約（Column Constraints）**

列に特定の条件を課すことで、データの整合性を保ちます。

#### **NOT NULL**

その列に NULL（値がない状態）を許可しません。

```SQL
CREATE TABLE users (
 user_id INTEGER PRIMARY KEY,
 name VARCHAR(100) NOT NULL -- nameはnullを許さない
);
```

#### **UNIQUE**

その列の全ての値が一意であることを保証します。NULL は複数存在できます。

```SQL
CREATE TABLE employees (
 employee_id INTEGER PRIMARY KEY,
 email VARCHAR(255) UNIQUE -- email は重複を許さない
);
```

| employee_id | email           |
| ----------- | --------------- |
| 1           | AAA@gmail.co.jp |
| 2           | BBB@gmail.co.jp |
| 3           | null            |
| 4           | null            |
⇒nullは被ってもok

#### **PRIMARY KEY (主キー)**

テーブルの各行を一意に識別するための**列**または**列の組み合わせ**です。

- NOT NULL と UNIQUE の両方の特性を自動的に持ちます。
- テーブルごとに 1 つだけ設定できます。

```SQL
CREATE TABLE products (
 product_id INTEGER PRIMARY KEY, -- product_id が主キー
 product_name VARCHAR(255) NOT NULL
);
```

#### **複合主キー (Composite Primary Key)**

主キーは、単一の列だけでなく、**複数の列の組み合わせ**で構成することも可能です。これを**複合主キー**と呼びます。複合主キーの場合、その**組み合わせた値がテーブル全体で一意かつ NULL でない**ことを保証します。テーブル全体の主キーは**論理的に 1 つ**とみなされます。

**例:** ある学生が複数の科目を受講している場合に、学生 ID と科目 ID の組み合わせで一意に成績を特定するテーブルを考えます。

```SQL
CREATE TABLE grades (
 student_id INTEGER,
 course_id INTEGER,
 grade VARCHAR(2),
 PRIMARY KEY (student_id, course_id) -- student_id と course_id の組み合わせが主キー
);
```

この例では、

- student_id が 1 で course_id が 101 の組み合わせは 1 つしか存在できません。
- student_id が 1 で course_id が 102 の組み合わせは別の行として存在できます。  
  このように、個々の列は重複しても、組み合わせとして重複しないことで一意性を保証します。

#### **主キーの自動採番**

主キーには、新しい行が追加されるたびに自動的に一意の値を生成する機能を持たせることがよくあります。

- **PostgreSQL**: SERIAL や BIGSERIAL 型がよく使われます。最近では SQL 標準に準拠した GENERATED ALWAYS AS IDENTITY 句の使用が推奨されています。

```SQL
  -- SERIAL 型の例
  CREATE TABLE example_serial (
   id SERIAL PRIMARY KEY,
   data TEXT
  );

  -- GENERATED ALWAYS AS IDENTITY の例 (PostgreSQL 10 以降推奨)
  CREATE TABLE example_identity (
   id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   data TEXT
  );
```

- **MySQL**: AUTO_INCREMENT を使用します。

```SQL
  -- MySQL の例
  CREATE TABLE example_auto_increment (
   id INT AUTO_INCREMENT PRIMARY KEY,
   data VARCHAR(255)
  );
```

- **Oracle**: SEQUENCE オブジェクトとトリガーを組み合わせて使用するか、IDENTITY 列を定義します。

```SQL
  -- Oracle の例 (IDENTITY 列)
  CREATE TABLE example_identity_oracle (
   id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
   data VARCHAR2(255)
  );
```

このように、**自動採番の仕組みは RDBMS ごとに大きな違いがある**ため、使用する RDBMS のドキュメントを確認することが重要です。

#### **補足：PRIMARY KEY と UNIQUE の違い**

どちらも値の一意性を保証しますが、重要な違いがあります。

| 特徴                   | PRIMARY KEY                                                          | UNIQUE                                                             |
| :--------------------- | :------------------------------------------------------------------- | :----------------------------------------------------------------- |
| **NULL の許容**        | **不可** (NOT NULL を自動的に持つ)                                   | **可** (複数 NULL が存在可能)                                      |
| **テーブルあたりの数** | 1 つのみ (単一列または複合列として)                                  | 複数設定可能                                                       |
| **目的**               | 行を一意に識別するための「主たるキー」                               | 指定された列（または列の組み合わせ）の値が重複しないことを保証する |
| **インデックス**       | 自動的にクラスタードインデックスが作成されることが多い（RDBMS 依存） | 自動的に非クラスタードインデックスが作成されることが多い           |

#### **DEFAULT**

値を指定しなかった場合に、自動的に設定されるデフォルト値を定義します。

```SQL
CREATE TABLE orders (
 order_id INTEGER PRIMARY KEY,
 order_date DATE DEFAULT CURRENT_DATE -- デフォルトで現在の日付が設定される
);
```

#### **CHECK**

その列に挿入される値が、指定された条件を満たしていることを強制します。

```SQL
CREATE TABLE students (
 student_id INTEGER PRIMARY KEY,
 age INTEGER CHECK (age >= 0 AND age <= 150) -- 年齢は 0 から 150 の範囲
);
```

#### **FOREIGN KEY (外部キー)**

他のテーブルの PRIMARY KEY または UNIQUE 列を参照する列です。テーブル間の関連性を定義し、参照整合性を維持します。

例: orders テーブルの customer_id が、customers テーブルの customer_id を参照するようにします。

```SQL
CREATE TABLE customers (
 customer_id INTEGER PRIMARY KEY,
 customer_name VARCHAR(255) NOT NULL
);

CREATE TABLE orders (
 order_id INTEGER PRIMARY KEY,
 customer_id INTEGER,
 order_date DATE,
 FOREIGN KEY (customer_id) REFERENCES customers (customer_id) -- customers テーブルの customer_id を参照
);
```

customers：

| <font color="#ff0000">customer_id</font> | customer_name |
| ---------------------------------------- | ------------- |
| 1                                        | bob           |
| 2                                        | jon           |

orders：

| order_id | <font color="#ff0000">customer_id</font> | order_date |
| -------- | ---------------------------------------- | ---------- |
| 1        | 1                                        | 2025/11/1  |
| 2        | 1                                        | 2025/11/2  |
| 3        | 2                                        | 2025/11/2  |
| 4        | **3 ✖**                                  | 2025/11/3  |

⇒被参照側（customers）にない>customer_idは入れられない

#### **外部キーの参照動作 (ON DELETE, ON UPDATE)**

外部キー制約には、参照先の親テーブルの行が削除されたり、主キーが更新されたりした場合に、子テーブルの行をどのように扱うかを定義するオプションがあります。これは実務で非常に重要です。

| オプション  | 説明                                                                                                                                    |
| :---------- | :-------------------------------------------------------------------------------------------------------------------------------------- |
| NO ACTION   | **デフォルト動作**。親テーブルの行が削除/更新されようとした場合、参照している子テーブルの行が存在すればエラーとなり、操作を拒否します。 |
| RESTRICT    | NO ACTION とほぼ同じです。操作を拒否します。                                                                                            |
| CASCADE     | 親テーブルの行が削除/更新された場合、関連する子テーブルの行も**自動的に削除/更新**されます。                                            |
| SET NULL    | 親テーブルの行が削除/更新された場合、子テーブルの外部キー列の値を NULL に設定します。                                                   |
| SET DEFAULT | 親テーブルの行が削除/更新された場合、子テーブルの外部キー列の値をデフォルト値に設定します。                                             |

**例:**

```SQL
CREATE TABLE orders (
 order_id INTEGER PRIMARY KEY,
 customer_id INTEGER,
 order_date DATE,
 FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
 ON DELETE CASCADE -- 親テーブル(customers)の顧客が削除されたら、その顧客の注文も削除する
 ON UPDATE RESTRICT -- 親テーブル(customers)の顧客 ID が更新されたら、参照している注文があれば更新を拒否する
);
```

NO ACTION`（または`RESTRICT`）がデフォルトであり、親側の削除がエラーになることが多いことを理解しておくことが重要です。

---


## コラム：テーブルの種類（マスタとトラン）

### マスタ（マスターテーブル）

#### 定義

業務で使用する「基礎情報」や「参照情報」を保持するテーブル。

#### 特徴

- 変更頻度が低い
- データの整合性が重要
- 他のテーブルから参照されることが多い

#### 例

- 顧客マスタ（顧客 ID、氏名、住所など）
- 商品マスタ（商品コード、商品名、価格など）
- 社員マスタ（社員 ID、部署、役職など）

### トラン（トランザクションテーブル）

#### 定義

日々の業務で発生する「取引情報」や「履歴情報」を記録するテーブル。

#### 特徴

- 変更・追加が頻繁にある
- 時系列で蓄積される
- マスタ情報を参照して意味を持つ

#### 例

- 売上トラン（売上 ID、顧客 ID、商品 ID、数量、売上日など）
- 注文トラン（注文 ID、注文日、商品 ID、数量など）
- 勤怠トラン（社員 ID、出勤日、勤務時間など）

---
## **テーブル構造の変更と削除**

### **テーブルの削除：DROP TABLE**

既存のテーブルをデータベースから完全に削除します。

#### **基本構文**

```SQL
DROP TABLE テーブル名;
```

#### **例**

users テーブルを削除します。

```SQL
DROP TABLE users;
```

#### **重要なオプション**

- IF EXISTS: テーブルが存在しない場合でもエラーを出さずに実行します。スクリプトの実行時に便利です。

```SQL
  DROP TABLE IF EXISTS old_table;
```

- CASCADE: 削除しようとしているテーブルを参照している他のオブジェクト（例: 外部キー制約を持つテーブル）も一緒に削除します。**注意して使用してください！**

```SQL
  DROP TABLE customers CASCADE; -- customers テーブルを削除し、それに関連する外部キーも削除される
```

#### **実務での注意点**

DROP TABLE はテーブルとデータを完全に削除するため、**本番環境で安易に実行することはほとんどありません**。データ削除が必要な場合は、代わりに以下の方法を検討します。

- **TRUNCATE TABLE**: テーブル構造は残し、全てのデータを高速に削除します。トランザクションログは少なく、ロールバックはできません。
- **DELETE FROM**: WHERE 句で条件を指定して一部のデータを削除したり、全データを削除したりできます。トランザクションログに記録され、ロールバック可能です。
- **論理削除**: テーブルからデータを物理的に削除するのではなく、削除フラグ（例: is_deleted BOOLEAN DEFAULT FALSE）を立てて、そのデータが「削除された」状態であることを示す方法です。データ復旧が容易ですが、クエリが複雑になることがあります。

### **テーブル構造の変更：ALTER TABLE**

既存のテーブルの構造を変更する際に使用します。

#### **列の追加：ADD COLUMN**

新しい列をテーブルに追加します。

```SQL
ALTER TABLE テーブル名 ADD COLUMN 新しい列名 データ型 [制約];
```

例: products テーブルに price 列を追加します。

```SQL
ALTER TABLE products ADD COLUMN price NUMERIC(10, 2) DEFAULT 0.00;
```

#### **列の削除：DROP COLUMN**

既存の列をテーブルから削除します。

```SQL
ALTER TABLE テーブル名 DROP COLUMN 列名;
```

例: employees テーブルから email 列を削除します。

```SQL
ALTER TABLE employees DROP COLUMN email;
```

#### **DROP COLUMN の RDBMS 依存性**

ALTER TABLE DROP COLUMN は多くの RDBMS でサポートされていますが、Oracle の古いバージョンなど、一部の RDBMS やバージョンでは直接サポートされていなかったり、特定の制約があったりする場合があります。実務で実行する際は、使用している RDBMS のバージョンを確認しましょう。

#### **列のデータ型変更：ALTER COLUMN TYPE**

既存の列のデータ型を変更します。データの互換性に注意が必要です。

```SQL
ALTER TABLE テーブル名 ALTER COLUMN 列名 TYPE 新しいデータ型;
```

例: products テーブルの product_name 列の長さを変更します。

```SQL
ALTER TABLE products ALTER COLUMN product_name TYPE VARCHAR(500);
```

#### **制約の追加・削除**

制約の追加:
ADD CONSTRAINT 句を使用します。

```SQL
ALTER TABLE customers ADD CONSTRAINT unique_email UNIQUE (email);
```

制約の削除:
DROP CONSTRAINT 句を使用します。

```SQL
ALTER TABLE orders DROP CONSTRAINT orders_customer_id_fkey; -- 外部キー制約の削除
ALTER TABLE employees DROP CONSTRAINT employees_email_key; -- UNIQUE 制約の削除
```

※制約名は RDBMS によって自動生成される場合があるため、確認が必要です。
