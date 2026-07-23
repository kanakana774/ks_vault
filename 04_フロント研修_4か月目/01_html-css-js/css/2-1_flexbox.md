「Flexbox（フレキシブルボックスレイアウト）」は、現代のWebレイアウトの**最重要技術**です。
その名の通り「柔軟な（Flexible）」箱を並べるための仕組みで、要素の数やサイズがバラバラでも、綺麗に整列させたり、余ったスペースを分け合ったりすることができます。

前回の「配置（Alignment）」の知識をベースに、**「どう伸び縮みさせるか」「どう折り返すか」**というFlexbox独自の強力な機能を解説します。

---

### 1. Flexboxの基本構造
Flexboxを使うには、**親要素（コンテナ）**に `display: flex;` を書きます。すると、その直下の**子要素（アイテム）**たちが「Flexアイテム」になり、横に並ぶようになります。

#### 【例：基本の横並び】
**HTML:**
```html
<div class="flex-container">
  <div class="item">1</div>
  <div class="item">2</div>
  <div class="item">3</div>
</div>
```

**CSS:**
```css
.flex-container {
  display: flex; /* これだけで子が横に並ぶ */
  background-color: #eee;
  padding: 10px;
  gap: 10px;
}

.item {
  background-color: #007bff;
  color: white;
  padding: 20px;
}
```

---

### 2. 並べる向きと折り返し (`flex-direction`, `flex-wrap`)

Flexboxはデフォルトでは「左から右へ、折り返さずに一列」に並ぼうとします。これを変更するのがこれらのプロパティです。

#### 【例：スマホでは縦、PCでは横に並べる（レスポンシブの基礎）】

**HTML:**
```html
<div class="responsive-stack">
  <div class="card">Card 1</div>
  <div class="card">Card 2</div>
  <div class="card">Card 3</div>
</div>
```

**CSS:**
```css
.responsive-stack {
  display: flex;
  
  /* 向きの指定 */
  flex-direction: row;      /* デフォルト：横並び */
  /* flex-direction: column;   /* 縦並び。スマホサイトで多用 */

  /* 折り返しの指定 */
  flex-wrap: wrap;          /* 幅が足りなくなったら自動で次の行へ */
  /* flex-wrap: nowrap;      /* デフォルト：絶対にはみ出しても1行に収める */

  gap: 15px;
}
```

---

### 3. アイテムの伸び縮みを操る (`flex` プロパティ)
ここがFlexboxの真骨頂です。子要素に指定することで、**「余ったスペースをどれくらい分け合うか」**を決められます。

現代では `flex: 1;` というショートハンド（一括指定）をよく使います。

#### 【例：メインコンテンツを広げ、サイドバーを固定する】

**HTML:**
```html
<div class="layout">
  <aside class="sidebar">サイドバー (固定幅)</aside>
  <main class="main-content">メイン (余白をすべて埋める)</main>
</div>
```

**CSS:**
```css
.layout {
  display: flex;
}

.sidebar {
  width: 200px; /* サイドバーは幅を固定 */
  background: #ccc;
}

.main-content {
  /* 
    flex: 1; は「flex-grow: 1;」を意味し、
    他の要素が場所を取った後の「残りのスペース」をすべて占有します。
  */
  flex: 1; 
  background: #f9f9f9;
}
```

---

### 4. 便利なショートハンド：`flex` の中身
子要素に書く `flex` プロパティは、実は以下の3つの組み合わせです。

1.  **`flex-grow`**: 余白があるとき、どれくらい**伸びる**か（0なら伸びない）。
2.  **`flex-shrink`**: スペースが足りないとき、どれくらい**縮む**か（1なら縮む、0なら絶対縮まない）。
3.  **`flex-basis`**: 伸び縮みする前の**基準のサイズ**。

**【よく使うパターン】**
*   `flex: 1;`（`1 1 0%`）: 均等に広がる。
*   `flex: 0 0 100px;` : 伸びも縮みもしない、ぴったり100pxの箱。
*   `flex: none;` : `0 0 auto` と同じ。中身のサイズを維持し、絶対に伸び縮みさせない。

---

### 5. 現代の設計・整理に役立つFlexboxのパターン

初期から癖付けるべき「設計のコツ」です。

#### ① ボタンの中のアイコンと文字
ボタンなどの小さなパーツこそFlexboxの出番です。
```css
.button {
  display: inline-flex; /* 自分自身はインラインのように振る舞いつつ、中はFlex */
  align-items: center;  /* アイコンと文字を上下中央で揃える */
  gap: 8px;             /* アイコンと文字の隙間 */
}
```

#### ② 画面下部に固定されるフッター (Sticky Footer)
中身が少なくてもフッターを画面の一番下に置きたい場合の定番テクニックです。
```css
body {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* 画面全体の高さ */
}

main {
  flex: 1; /* メインエリアが伸びることで、footerが一番下に押し出される */
}
```

#### ③ `margin: auto` との組み合わせ
Flexboxの中で `margin-left: auto;` を使うと、**「その要素だけを右端に追いやる」**ことができます。
```css
.nav {
  display: flex;
}
.login-button {
  margin-left: auto; /* ログインボタンだけ右端へ！ */
}
```

---

### まとめ：Flexbox学習のチェックリスト

1.  **`display: flex;` を親に書いたか？**（これがすべての始まり）
2.  **縦並びにしたいなら `flex-direction: column;` を使っているか？**
3.  **複数行にしたいなら `flex-wrap: wrap;` を設定しているか？**
4.  **特定の要素だけ広げたいなら `flex: 1;` を活用しているか？**
5.  **要素の隙間は `gap` で制御しているか？**

Flexboxをマスターすれば、Webサイトのレイアウトの大部分を自由に作れるようになります。

---

「Flexbox（フレキシブルボックスレイアウト）」は、現代のCSSレイアウトの**中心**となる機能です。
1次元（行または列のいずれか一方）のレイアウトを組むためのツールで、要素のサイズが不明だったり、画面サイズが動的に変わったりする場合でも、要素をきれいに整列・分配できます。

---

### 1. Flexboxの基本構造：親と子の関係

Flexboxを理解する最大の鍵は、**「Flexコンテナ（親）」**と**「Flexアイテム（子）」**を明確に分けることです。

*   **Flexコンテナ**: `display: flex;` を指定した要素。
*   **Flexアイテム**: その直下にある要素すべて。

#### 【基本の並び方】

```html
<div class="flex-container">
  <div class="item">1</div>
  <div class="item">2</div>
  <div class="item">3</div>
</div>

<style>
.flex-container {
  display: flex; /* これだけで子が横並びになります */
  background-color: #f0f0f0;
  padding: 10px;
  gap: 10px; /* 要素間の隙間 */
}

.item {
  background-color: #007bff;
  color: white;
  padding: 20px;
  text-align: center;
}
</style>
```

---

### 2. 軸の方向を決める：`flex-direction` と `flex-wrap`

Flexboxには「主軸（要素が並ぶ方向）」があります。

*   **`flex-direction: row`**: 横並び（デフォルト）。
*   **`flex-direction: column`**: 縦並び。
*   **`flex-wrap: wrap`**: 親の幅を超えたら自動で改行する。

#### 【実践：レスポンシブなカードレイアウト】
スマホでは縦、PCでは横に並べ、入り切らなくなったら改行する設計です。

```html
<div class="card-wrapper">
  <div class="card">Card 1</div>
  <div class="card">Card 2</div>
  <div class="card">Card 3</div>
  <div class="card">Card 4</div>
</div>

<style>
.card-wrapper {
  display: flex;
  /* 横並び(row)で、入り切らなくなったら改行(wrap)する */
  flex-flow: row wrap; /* directionとwrapの一括指定 */
  gap: 20px;
  padding: 20px;
}

.card {
  /* 幅を「最低200px、余れば均等に広がる」ように設定 */
  flex: 1 1 200px; 
  height: 150px;
  background: #e3f2fd;
  border: 2px solid #2196f3;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
```

---

### 3. 子要素の伸び縮みを制御する：`flex` プロパティ

Flexアイテム（子）が、親の余ったスペースをどう分け合うかを決めます。
`flex: (grow) (shrink) (basis);` のショートハンドで書くのがベストプラクティスです。

*   **`flex-grow`**: 余ったスペースをどれくらいの比率で埋めるか（0なら伸びない）。
*   **`flex-shrink`**: スペースが足りないときにどれくらい縮むか。
*   **`flex-basis`**: 伸び縮みする前の「基準のサイズ」。

#### 【アンチパターン vs ベストプラクティス：メニューバー】

**アンチパターン（`width: 33.3%` などで固定する）**
```html
<div class="bad-menu">
  <div>Home</div>
  <div>About Us (長いテキスト)</div>
  <div>Contact</div>
</div>

<style>
.bad-menu { display: flex; }
.bad-menu div {
  width: 33.3%; /* 文字が入り切らなくても幅を固定してしまう */
  border: 1px solid red;
}
</style>
```

**ベストプラクティス（`flex` プロパティで柔軟に）**
```html
<div class="good-menu">
  <div class="expand">Home</div>
  <div class="expand">About Us (長いテキスト)</div>
  <div class="fixed">Login</div>
</div>

<style>
.good-menu {
  display: flex;
  gap: 10px;
  background: #eee;
  padding: 10px;
}

.expand {
  /* 余ったスペースを均等に分け合って伸びる */
  flex: 1; 
  background: white;
}

.fixed {
  /* 伸びない(0)、縮まない(0)、幅は中身に合わせる(auto) */
  flex: 0 0 auto;
  background: #ffcc00;
  padding: 0 20px;
}
</style>
```

*   **違いの観点：**
    *   **柔軟性:** `flex: 1` を使うと、中身の量に関わらず親のスペースをきれいに埋めてくれます。
    *   **制御:** 「ここは固定、ここは伸びる」という指定が直感的にできます。

---

### 4. 究極の便利テクニック：`margin: auto` による整列

Flexboxの中で `margin: auto` を使うと、**「余っているスペースをすべてそのマージンが吸い取る」**という魔法のような挙動をします。

#### 【例：右端にだけボタンを寄せる】
ナビゲーションでロゴを左、メニューを右に寄せたいときに非常に便利です。

```html
<div class="navbar">
  <div class="logo">MY LOGO</div>
  <div class="nav-links">Links</div>
  <div class="login-btn">Login</div>
</div>

<style>
.navbar {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  background: #333;
  color: white;
}

.nav-links {
  /* 左側に auto マージンを入れることで、
     それより左にある要素(logo)を左端へ、
     自分を右側へ押し出す */
  margin-left: auto; 
}

.login-btn {
  margin-left: 20px;
  background: orange;
  padding: 5px 15px;
}
</style>
```

---

### 5. 現代の設計・整理：Flexboxの使いどころルール

1.  **「1次元」ならFlexbox、「2次元」ならGrid**
    *   一列に並べる、あるいは折り返して並べるだけならFlexboxが最適です。
    *   新聞の紙面のように、縦と横の線をきっちり合わせるならGridを使いましょう。
2.  **`gap` を積極的に使う**
    *   `margin-right` などを子要素につけると、最後の要素の余白を消す処理が必要になります。`gap` なら親に書くだけで解決します。
3.  **垂直中央揃えのデフォルト化**
    *   ボタンの中のアイコンと文字など、小さなパーツを作るときはとりあえず `display: inline-flex; align-items: center;` を書く癖をつけると、1pxのズレに悩まされなくなります。

---

### まとめ：Flexbox 実行可能サンプル

すべての要素を組み合わせた実用的なカードリストの例です。

```html
<section class="container">
  <div class="flex-list">
    <article class="flex-item">
      <h3>Title 1</h3>
      <p>Flexboxは非常に強力です。</p>
      <button>Read More</button>
    </article>
    <article class="flex-item">
      <h3>Title 2</h3>
      <p>短い文章。</p>
      <button>Read More</button>
    </article>
    <article class="flex-item">
      <h3>Title 3</h3>
      <p>これは少し長い文章です。Flexboxを使うと、隣のカードと高さを自動で揃えることができます（align-items: stretch）。</p>
      <button>Read More</button>
    </article>
  </div>
</section>

<style>
.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.flex-list {
  display: flex;
  flex-wrap: wrap; /* スマホで折り返す */
  gap: 20px;
}

.flex-item {
  flex: 1 1 250px; /* 最低250px、空きがあれば均等に伸びる */
  background: #f9f9f9;
  border: 1px solid #ddd;
  padding: 20px;
  border-radius: 8px;

  /* アイテム自体もFlexコンテナにする（中身を整列させるため） */
  display: flex;
  flex-direction: column;
}

.flex-item p {
  flex-grow: 1; /* 文章が短くても、ボタンを常に一番下に押し下げる */
}

.flex-item button {
  align-self: flex-start; /* ボタンを左寄せにする */
  margin-top: 15px;
  padding: 8px 16px;
  background-color: #333;
  color: white;
  border: none;
  cursor: pointer;
}
</style>
```

次は、ページ全体の骨組みを作るのに最適な **「CSSグリッドレイアウト（Grid）」** について解説しましょうか？