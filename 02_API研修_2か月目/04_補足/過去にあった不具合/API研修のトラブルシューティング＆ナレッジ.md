# Spring Boot & MyBatis トラブルシューティング・ナレッジベース

## 1. リクエスト制御 (Controller / Form) 編
ユーザーからの入力をどう受け取り、どうバリデーションするかに関する知見です。

### 1-1. クエリパラメータの型選択：`String` vs `Long/int`
**【事象】**
IDなどの数値を `String` で受け取って内部でパースすると、数値以外の文字列が来た場合に `500 Internal Server Error` になりやすい。

**【仕組みと解決策】**
*   **Springの自動パース機能を利用する：**
    引数を `Long` や `Integer` で定義すると、Spring Boot（MethodArgumentResolver）がパースを試みます。パースに失敗（文字列が来た場合など）すると、Springが自動で **`400 Bad Request`** を返してくれます。
*   **Stringで受け取った場合：**
    自分で `Integer.parseInt()` などを行う必要があり、例外処理を忘れると `500` エラーになります。
*   **結論：**
    数値として期待する値は、最初から数値型で受けるのがベストプラクティスです。

### 1-2. プリミティブ型 (`int`) と ラッパー型 (`Integer`) の罠
**【事象】**
Formクラスで `private int version;` と定義し `@NotNull` を付けても、リクエストに値がない場合にバリデーションをすり抜けて `0` が入ってしまう。

**【理由：デフォルト値の存在】**
*   Javaのプリミティブ型（`int`, `long`等）は、インスタンス化された時点でデフォルト値（`0`）を持ちます。そのため、リクエストが空でも **「nullではない（0である）」** と判定され、`@NotNull` を通過します。
*   **解決策：**
    未入力を検知したい場合は、ラッパー型（`Integer`, `Long`等）を使用します。ラッパー型は初期値が `null` なので、`@NotNull` や `@NotEmpty` が正しく機能します。

---

## 2. MyBatis 仕組みと挙動 編
XMLとインターフェースの優先順位や、内部の処理フローに関する深い知見です。

### 2-1. Mapperインターフェース vs XML の優先順位
**【結論：XMLが「真実の定義」である】**
MyBatis内部では、起動時にXMLの内容を読み取って `MappedStatement` オブジェクトを作成します。

*   **優先順位：** 実行時に使われる設定（`parameterType`, `resultType` 等）は **XML側の定義が優先** されます。
*   **インターフェースの役割：** Java側のシグネチャは、あくまでプロキシ（代理）としてのヒントです。
*   **不整合の末路：** 
    XMLで `resultType="User"` と定義しているのに、インターフェースの戻り値を `int` にすると、実行時に型の不整合（`ClassCastException` 等）が発生し、ランタイムエラーとなります。コンパイルエラーにならないため注意が必要です。

### 2-2. INSERT/UPDATE/DELETE に `resultType` は不要
**【仕組み：SqlCommandTypeによる固定挙動】**
*   **内部挙動：** MyBatisはXMLパース時（`XMLStatementBuilder#parseStatementNode()`）にタグ名を見て `SqlCommandType` を決定します。
*   **INSERT等の戻り値：** `INSERT/UPDATE/DELETE` の場合、MyBatisは **「更新件数 (int)」を返す** ように内部で固定されています。
*   **注意：** XMLに `resultType` を書いても無視されます。「登録したオブジェクトそのもの」を戻り値で受け取ることはできません。

### 2-3. SELECT時のEntity生成とデフォルトコンストラクタ
**【仕組み：リフレクションによる生成】**
MyBatisが `SELECT` 結果をEntityにマッピングする際、内部でそのクラスを `new` します。
*   このとき、**「引数なしのデフォルトコンストラクタ」** が必要です。Lombokなどで全引数コンストラクタのみを作っていると、MyBatisがインスタンス化に失敗してエラーになります。後述のLombok編を併せて参照してください。

---

## 3. MyBatis 動的SQL & 構文 編
XML記述におけるミスや、便利なタグの正しい使い分けです。

### 3-1. `<where>` タグの真価と使いどころ
**【事象】**
条件が一つしかない場合に `<where>` を使うのは冗長に見える。

**【正しい使い分け】**
*   **`<where>` タグの役割：**
    1. 内部のコンテンツ（`if`等）が一つでも結果を返せば、自動で `WHERE` 句を挿入する。
    2. 最初の条件が `AND` や `OR` で始まっていた場合、それを**自動で削除**する。
*   **アドバイス：**
    条件が複数ある（かつ、どれが最初に来るか不定な）検索画面などでは `<where>` は必須です。しかし、条件が一つ固定であれば、指摘の通り直書きしたほうがシンプルになります。⇒まあこれはどちらもいい。

### 3-2. `<if>` テスト式での致命的なミス（`==` vs `=`）
**【事象】**
`<if test="limit = ''">` と書いてしまい、比較ではなく **「空文字の代入」** が行われ、意図せず条件が `true` になったり、パラメータが書き換わったりする。

**【解決策】**
*   **比較演算子を使う：** 必ず `==`（または `!=`）を使用します。
*   XMLパースの際、`test` 属性内の式は OGNL (Object-Graph Navigation Language) として解釈されます。ここでの `=` は代入を意味するため、非常に発見しにくいバグを生みます。

### 3-3. XMLパースエラー (`SaxParseException`) のチェックリスト
XMLが読み込めないとき、まず疑うべき「初歩的だが多い」原因です。
*   **属性名のキャメルケース：** `parameterType` を `parametertype` と書いていないか。
*   **全角スペースの混入：** コピー＆ペースト時にタグの間に全角スペースが混じると、XMLとして不正になりパースに失敗します。
*   **閉じタグの忘れ：** `<if>` を開いて `</if>` を忘れる等。

### 3-4.resultMapで空のtagsを返したいとき
todo課題2で一件get時に下記のように紐づくtagがなければ空で返したい場合について
```json
{"todoId":4,"title":"got4","content":"content","version":0,"tags":[]}
``` 

```XML
    <resultMap type="jp.aevic.todo.entity.todo.TodoEntity" id="todoWithTag">
        <id property="todoId" column="todo_id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="version" column="version"/> ←versionが被り
        <collection property="tags" ofType="jp.aevic.todo.entity.tag.TagEntity">
            <id property="tagId" column="tag_id"/>
            <result property="name" column="name"/>
            <result property="version" column="version"/> ←versionが被り
        </collection>
    </resultMap>
```

```SQL
        SELECT
            td.todo_id,
            td.title,
            td.content,
            td.version, -- ←versionが被り
            tg.tag_id,
            tg.name,
            tg.version -- ←versionが被り
```

 結果の列名（version）が被ってると、tagEntity生成（null,null,null）⇒setterで値セット（null,null,0←todoのversion）の際にtodoの方のversionをmybatisがセットしてしまう現象

```XML
    <resultMap type="jp.aevic.todo.entity.todo.TodoEntity" id="todoWithTag">
        <id property="todoId" column="todo_id"/>
        <result property="title" column="title"/>
        <result property="content" column="content"/>
        <result property="version" column="version"/>
        <collection property="tags" ofType="jp.aevic.todo.entity.tag.TagEntity">
            <id property="tagId" column="tag_id"/>
            <result property="name" column="name"/>
            <result property="version" column="tag_version"/>
        </collection>
    </resultMap>
```

```SQL
        SELECT
            td.todo_id,
            td.title,
            td.content,
            td.version,
            tg.tag_id,
            tg.name,
            tg.version as tag_version
```
このように別名に変えてあげれば問題ないみたい
また、notNullColumnを使うのも可


---

## 4. Lombok 編
EntityやFormの定義を簡略化する際の、MyBatisやSpringとの親和性についてです。

### 4-1. コンストラクタ注釈の使い分け
Entityクラスには以下の3点セットを付けるのが「実務での安定構成」です。

1.  **`@NoArgsConstructor` (必須)：**
    MyBatisが検索結果をマッピングする際に使用する「引数なしコンストラクタ」を生成します。
2.  **`@AllArgsConstructor`：**
    テストコードや、Controllerでの詰め替え時に全フィールドを一気にセットするために重宝します。
3.  **`@RequiredArgsConstructor` / `@Data`：**
    `final` フィールド（DI対象のServiceなど）を扱うクラス（Service層など）で主に使用します。

**【ニュアンスの補足】**
MyBatisを使う場合、`@Builder` を使う際も `@NoArgsConstructor` がないとエラーになるケースがあります。**「フレームワーク（MyBatis）が裏側で `new` するための口を用意しておく」** という意識が重要です。
