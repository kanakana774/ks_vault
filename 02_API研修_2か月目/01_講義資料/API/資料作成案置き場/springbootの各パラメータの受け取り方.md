## 1. 主要な受け取り方法一覧

| アノテーション / 方法 | データの場所 | 主な用途 | 例 |
| :--- | :--- | :--- | :--- |
| **`@RequestParam`** | クエリパラメータ / Formデータ | 検索条件、単純なフォーム入力 | `?id=10&name=test` |
| **`@PathVariable`** | URLパスの一部 | リソースの特定 (IDなど) | `/users/10` |
| **`@RequestBody`** | リクエストボディ (JSON/XML) | REST APIでのデータ登録・更新 | `{"id":1, "name":"test"}` |
| **`@ModelAttribute`** | クエリパラメータ / Formデータ | 項目数が多い検索条件やフォーム | (クラスにマッピング) |
| **`@RequestHeader`** | HTTPヘッダー | 認証トークン、User-Agent | `Authorization: Bearer ...` |
| **`@CookieValue`** | クッキー | セッションID、トラッキング | `Cookie: session_id=...` |

---

## 2. 詳細とコード例

### ① `@RequestParam`
URLの末尾につく `?key=value` や、`application/x-www-form-urlencoded` 形式のフォームデータを受け取ります。

**基本形:**
```java
@GetMapping("/search")
public String search(@RequestParam("keyword") String keyword) {
    return "検索ワード: " + keyword;
}
// リクエスト: GET /search?keyword=spring
```

**応用 (必須ではない場合やデフォルト値):**
```java
@GetMapping("/list")
public String list(
    @RequestParam(name = "page", required = false, defaultValue = "1") int page,
    @RequestParam(name = "sort", required = false) String sort
) {
    return "ページ: " + page + ", ソート: " + sort;
}
// リクエスト: GET /list (pageは1になる)
// リクエスト: GET /list?page=5&sort=desc
```

---

### ② `@PathVariable`
URLのパスそのものに埋め込まれた値を受け取ります。RESTfulな設計でよく使われます。

**基本形:**
```java
@GetMapping("/users/{id}")
public String getUser(@PathVariable("id") Long userId) {
    return "ユーザーID: " + userId;
}
// リクエスト: GET /users/123
```
※ 引数名とパス変数名が同じなら `("id")` は省略可能です。

**複数使用:**
```java
@GetMapping("/users/{userId}/posts/{postId}")
public String getPost(@PathVariable Long userId, @PathVariable Long postId) {
    return "User: " + userId + ", Post: " + postId;
}
// リクエスト: GET /users/10/posts/99
```

---

### ③ `@RequestBody`
リクエストボディに含まれるJSON（またはXML）を、Javaのオブジェクト（DTO）にマッピングします。主にREST API (POST/PUT) で使用します。

**DTOクラス:**
```java
public class UserForm {
    private String name;
    private Integer age;
    // Getter, Setter (またはLombokの@Data)
}
```

**Controller:**
```java
@PostMapping("/users")
public String createUser(@RequestBody UserForm form) {
    return "作成: " + form.getName() + "(" + form.getAge() + ")";
}
// リクエストヘッダー: Content-Type: application/json
// リクエストボディ: {"name": "Alice", "age": 25}
```

---

### ④ `@ModelAttribute` (またはPOJO直受け)
`@RequestParam` の集合体です。フォーム送信や検索パラメータが多い場合、それらをまとめて1つのクラスで受け取ります。
※JSONではなく、クエリパラメータやFormデータ (`x-www-form-urlencoded`) が対象です。

**検索条件クラス:**
```java
@Data // Lombok使用想定
public class SearchCriteria {
    private String keyword;
    private String category;
    private Integer page = 1;
}
```

**Controller:**
```java
@GetMapping("/items")
public String searchItems(@ModelAttribute SearchCriteria criteria) {
    return "検索: " + criteria.getKeyword() + ", カテゴリ: " + criteria.getCategory();
}
// リクエスト: GET /items?keyword=book&category=IT&page=2
```
※ Spring Bootでは引数が単純な型でなく独自のクラスであれば、`@ModelAttribute` を省略しても自動的にこの挙動になりますが、明示的に書く方が可読性が高いです。

---

### ⑤ `@RequestHeader`
HTTPリクエストヘッダーの値を取得します。

```java
@GetMapping("/check-agent")
public String checkAgent(@RequestHeader("User-Agent") String userAgent) {
    return "ブラウザ情報: " + userAgent;
}

// 特定のカスタムヘッダーを取得する場合
@GetMapping("/secret")
public String secret(@RequestHeader("X-API-KEY") String apiKey) {
    return "API Key: " + apiKey;
}
```

---

### ⑥ `@CookieValue`
ブラウザから送信されたCookieの値を取得します。

```java
@GetMapping("/session")
public String checkSession(@CookieValue(name = "JSESSIONID", required = false) String sessionId) {
    return "セッションID: " + sessionId;
}
```

---

### ⑦ `HttpServletRequest` (低レイヤー)
Springの機能を使わず、Servlet APIを直接触る方法です。上記の方法で対応できない特殊なケースや、IPアドレスの取得などで使います。

```java
@GetMapping("/raw")
public String raw(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    String param = request.getParameter("someParam");
    return "IP: " + ip + ", Param: " + param;
}
```

---

## 3. 使い分け比較・ベストプラクティス

### ケース別推奨パターン

| シチュエーション | 推奨パラメータ | 理由 |
| :--- | :--- | :--- |
| **特定のリソースを取得・削除**<br>(例: IDを指定してユーザー取得) | **`@PathVariable`** | RESTfulなURL設計 (`/users/1`) に適しているため。 |
| **検索フィルタ・ソート・ページング**<br>(例: 価格順、キーワード検索) | **`@RequestParam`** | リソースそのものではなく、表示条件を指定するため (`?sort=price`)。 |
| **パラメータが多い検索/GETリクエスト** | **`@ModelAttribute`** | 引数が `func(String a, String b, int c...)` と増え続けるのを防ぎ、オブジェクトとして管理するため。 |
| **データの登録・更新 (SPA/モバイルアプリ)**<br>(JSON形式) | **`@RequestBody`** | 構造化されたデータ（ネストしたJSONなど）を扱うのに適しているため。 |
| **従来のWebフォーム送信 (MPA)**<br>(`<form action="..." method="post">`) | **`@ModelAttribute`** | HTMLフォーム標準の `application/x-www-form-urlencoded` をオブジェクトにマッピングするため。 |

### よくある間違いと注意点

1.  **JSON vs Form の混同**
    *   **間違い:** JSONを送っているのに `@ModelAttribute` (またはアノテーションなし) で受け取ろうとする → 全フィールドが `null` になります。
    *   **正解:** JSONを受け取る場合は必ず **`@RequestBody`** をつけます。

2.  **`@RequestParam` の必須チェック**
    *   デフォルトでは `required = true` です。パラメータが無いと `400 Bad Request` エラーになります。
    *   必須でない場合は `@RequestParam(required = false)` をつけるか、Java 8以降なら `Optional<String>` で受け取ることも可能です。

3.  **バリデーション**
    *   `@RequestBody` や `@ModelAttribute` でオブジェクトを受け取る際、`@Valid` または `@Validated` アノテーションを併用することで、入力値チェック（Bean Validation）を自動化できます。

    ```java
    // 例: バリデーション付き
    @PostMapping("/users")
    public String create(@RequestBody @Validated UserForm form) { ... }
    ```

### まとめ

*   **URLの一部なら** → `@PathVariable`
*   **?以降の単純な値なら** → `@RequestParam`
*   **?以降の値が多いなら** → `@ModelAttribute` (POJO)
*   **JSONを送るなら** → `@RequestBody`
*   **ヘッダー/Cookieなら** → `@RequestHeader` / `@CookieValue`

これらを適切に使い分けることで、可読性が高く、保守しやすいControllerを作成できます。


---

# クエリパラメータの受け取り方（詳細）
## 1. 個別の値を受け取る (`@RequestParam`)

基本のアノテーションです。パラメータの数だけ引数に記述します。

### ① 基本形と名前の指定
変数名とパラメータ名が一致していれば、引数名だけで受け取れます。一致しない場合は `name` (または `value`) 属性で指定します。

```java
// URL: /api/search?q=spring&category_id=10
@GetMapping("/search")
public String search(
    @RequestParam String q,                      // パラメータ名 "q" と一致
    @RequestParam("category_id") Integer catId   // "category_id" を "catId" で受け取る
) {
    return "Query: " + q + ", ID: " + catId;
}
```

### ② 必須チェックと任意（Optional）
デフォルトでは `required = true` (必須) です。パラメータがないと `400 Bad Request` になります。

*   **必須ではない場合:** `required = false` をつける
*   **Java 8 Optional:** `Optional<T>` でラップする (スマートな方法)

```java
// URL: /api/filter?status=active  (typeは省略)
@GetMapping("/filter")
public String filter(
    @RequestParam String status, // 必須
    @RequestParam(required = false) String type, // 任意 (なければ null)
    @RequestParam Optional<String> code          // 任意 (なければ Optional.empty)
) {
    if (code.isPresent()) { /* ... */ }
    return "Status: " + status;
}
```

### ③ デフォルト値 (`defaultValue`)
パラメータが指定されなかった場合に使う初期値を設定できます。ページング処理などで多用します。
※ `defaultValue` を指定すると、自動的に `required = false` 扱いになります。

```java
// URL: /api/list または /api/list?page=2
@GetMapping("/list")
public String list(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "20") int size
) {
    return "Page: " + page + ", Size: " + size;
}
```
**注意:** `defaultValue` は文字列で指定しますが、Springが自動的に型変換（intなど）を行います。

### ④ リスト・配列 (`List<T>`)
同じパラメータ名で複数の値が送られてくる場合や、カンマ区切りの値を受け取る場合です。

```java
// パターンA: /api/tags?id=1&id=2&id=3
// パターンB: /api/tags?id=1,2,3
@GetMapping("/tags")
public String tags(@RequestParam List<Integer> id) {
    // id は [1, 2, 3] のリストになる
    return "Count: " + id.size();
}
```

### ⑤ 全パラメータをMapで受け取る
パラメータ名が事前には不明な場合や、全てをログに出したい場合に使います。

```java
// URL: /api/anything?a=1&b=2&foo=bar
@GetMapping("/anything")
public String anything(@RequestParam Map<String, String> allParams) {
    return "パラメータ数: " + allParams.size(); // 全て文字列として格納される
}
```
※ `MultiValueMap<String, String>` を使うと、1つのキーに対する複数の値も受け取れます。

### ⑥ 日付・時間のフォーマット (`@DateTimeFormat`)
GETリクエストの日付文字列を `LocalDate` 等に変換します。

```java
// URL: /api/history?from=2023-10-01
@GetMapping("/history")
public String history(
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from
    // または (pattern = "yyyy/MM/dd") など
) {
    return "From: " + from.toString();
}
```

---

## 2. オブジェクトにまとめて受け取る (POJO / `@ModelAttribute`)

検索条件が多い場合（例：キーワード、期間、カテゴリ、ソート順、ページ番号...）、引数が長くなりすぎるのを防ぐために、クラスを作成してまとめて受け取ります。

### ① 基本形
専用のクラス（DTO/Formクラス）を用意します。

**条件クラス:**
```java
import lombok.Data;

@Data // Setter, Getterが必須
public class SearchCondition {
    private String keyword;
    private Integer page = 1; // デフォルト値もフィールド初期化で対応可能
    private String sort;
    private List<String> tags;
}
```

**Controller:**
```java
// URL: /api/items?keyword=book&page=2&tags=java,spring
@GetMapping("/items")
public String searchItems(SearchCondition condition) { 
    // @ModelAttribute は省略可能ですが、明示的につけるのが丁寧です
    return "Keyword: " + condition.getKeyword();
}
```

**仕組み:** Springがデフォルトコンストラクタでインスタンス化し、setterメソッドを使って値をセットします（データバインディング）。

### ② フィールド名とパラメータ名が異なる場合
クラスで受ける場合、`@RequestParam(name=...)` のようなエイリアス機能は標準ではありません。フィールド名をパラメータ名に合わせるのが原則です。

### ③ バリデーション (`@Validated`)
クラスで受け取る最大のメリットの一つは、アノテーションによる入力チェックができることです。

**条件クラス:**
```java
public class UserSearchForm {
    @NotBlank(message = "名前は必須です")
    private String name;

    @Min(18)
    private Integer age;
}
```

**Controller:**
```java
@GetMapping("/users")
public String searchUsers(
    @Validated UserSearchForm form, // @Validated を付与
    BindingResult result            // エラー結果を受け取る（必ず直後に記述）
) {
    if (result.hasErrors()) {
        return "入力エラーがあります: " + result.getAllErrors();
    }
    return "OK";
}
```

---

## 3. 特殊なケースと注意点

### 空文字 (`?param=`) と Null の違い
*   URL: `/api?val=abc` -> "abc"
*   URL: `/api?val=` -> **"" (空文字)**
*   URL: `/api` -> **null** (required=falseの場合)

空文字を `null` として扱いたい場合、Controller内で変換するか、CustomEditorの設定が必要ですが、基本的には「空文字チェック」をロジックに組み込むのが安全です。

### `Boolean` の扱い
`Boolean` 型や `boolean` 型で受け取る場合、以下の値が `true` として解釈されます（大文字小文字区別なし）。
*   `true`, `on`, `yes`, `1`
それ以外は `false` になります。

```java
// URL: /api/check?flag=on  -> true
// URL: /api/check?flag=yes -> true
@GetMapping("/check")
public String check(@RequestParam boolean flag) { ... }
```

---

## 4. 使い分けのベストプラクティスまとめ

| パターン | 推奨方法 | 理由 |
| :--- | :--- | :--- |
| **パラメータが1〜2個** | **`@RequestParam`** | クラスを作る手間が省け、コードが直感的になる。 |
| **パラメータが3個以上** | **クラス (POJO)** | 引数が多すぎると可読性が落ちる。拡張（項目の追加）が容易。 |
| **ページネーション** | **`Pageable`** | Spring Data JPAを使う場合、`Pageable` インターフェースを引数にすると `page`, `size`, `sort` を自動で処理してくれる便利な機能があります。 |
| **必須チェック・形式チェック** | **クラス + `@Validated`** | Controller内に `if (param == null)` を大量に書くのを避けるため。 |

**（参考）Pageableの使用例:**
Spring Dataを使っている場合、これだけで `?page=0&size=10&sort=name,desc` を受け取れます。
```java
@GetMapping("/products")
public List<Product> getProducts(Pageable pageable) {
    // pageableを使ってDB検索
    return repository.findAll(pageable).getContent();
}
```