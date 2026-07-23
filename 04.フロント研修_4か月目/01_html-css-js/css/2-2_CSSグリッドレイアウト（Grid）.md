「CSSグリッドレイアウト（Grid）」は、**「2次元（縦と横の両方）」**のレイアウトを自由自在に操るための非常に強力なツールです。

Flexboxが「一列に並べる（1次元）」のを得意とするのに対し、Gridは**「ページ全体の骨組み」や「タイル状の並び」**を作るのに最適です。

---

### 1. Gridの基本：親要素で「枠（網目）」を定義する

Gridの最大の特徴は、親要素（コンテナ）側で、あらかじめ**「何列、何行の網目を作るか」**をすべて定義できる点です。

#### 【基本：3列のグリッドを作る】

```html
<div class="grid-parent">
  <div class="grid-item">1</div>
  <div class="grid-item">2</div>
  <div class="grid-item">3</div>
  <div class="grid-item">4</div>
  <div class="grid-item">5</div>
</div>

<style>
.grid-parent {
  display: grid; /* Gridの有効化 */
  
  /* 3列作る。幅は 1:1:1 (fr単位) */
  grid-template-columns: 1fr 1fr 1fr;
  
  /* 行の高さ（指定しなくても中身に合わせて自動で作られます） */
  grid-template-rows: auto;
  
  gap: 15px; /* 要素の間隔（Flexboxと同じ） */
  background: #eee;
  padding: 10px;
}

.grid-item {
  background: #3f51b5;
  color: white;
  padding: 20px;
  text-align: center;
}
</style>
```

*   **`fr` (Fraction) 単位:** 現代CSSの魔法の単位です。「空いているスペースの比率」を表します。`1fr 2fr` と書けば、1:2の幅になります。

---

### 2. 現代の最強レシピ：メディアクエリなしのレスポンシブ

Gridの真骨頂は、**「画面幅に応じて列数を自動で変える」**という魔法の1行です。これを使えば、スマホ・タブレット・PC対応が非常に楽になります。

#### 【アンチパターン vs ベストプラクティス：カードリスト】

**アンチパターン（メディアクエリで必死に書き換える）**
```css
/* PC用 */
.list { grid-template-columns: 1fr 1fr 1fr; }
/* タブレット用 */
@media (max-width: 800px) { .list { grid-template-columns: 1fr 1fr; } }
/* スマホ用 */
@media (max-width: 500px) { .list { grid-template-columns: 1fr; } }
```

**ベストプラクティス（`repeat` + `auto-fit` + `minmax`）**
メディアクエリを一行も書かずに、全デバイスに対応させます。

```html
<div class="responsive-grid">
  <div class="card">Card A</div>
  <div class="card">Card B</div>
  <div class="card">Card C</div>
  <div class="card">Card D</div>
</div>

<style>
.responsive-grid {
  display: grid;
  /* 
    魔法の1行：
    - repeat(auto-fit, ...): 入るだけ繰り返す
    - minmax(200px, 1fr): 各要素の幅は「最低200px、最大で余り全部」
  */
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.card {
  background: #fff;
  border: 1px solid #ddd;
  height: 100px;
  display: flex; /* カードの中身を整列させるのはFlexboxが最適 */
  align-items: center;
  justify-content: center;
}
</style>
```
*   **違いの観点：** メンテナンス性が劇的に向上します。「カードを少し大きくしたいな」と思ったら、`200px` を `250px` に変えるだけで、全デバイスの挙動が自動で調整されます。

---

### 3. 設計のベストプラクティス：`grid-template-areas`

複雑なページレイアウト（ヘッダー、サイドバー、メイン、フッター）を組む際、**「名前でレイアウトを配置する」**ことができます。これが現代のCSS設計において最も視認性が高い手法です。

#### 【実践：ページ全体のレイアウト】

```html
<div class="page-layout">
  <header class="page-header">HEADER</header>
  <aside class="page-sidebar">SIDEBAR</aside>
  <main class="page-main">MAIN CONTENT</main>
  <footer class="page-footer">FOOTER</footer>
</div>

<style>
.page-layout {
  display: grid;
  gap: 10px;
  /* レイアウトの「地図」をテキストで描く（直感的！） */
  grid-template-areas: 
    "header header"
    "sidebar main"
    "footer footer";
  /* 1列目（サイドバー用）は200px、2列目（メイン用）は残り全部 */
  grid-template-columns: 200px 1fr;
  height: 400px;
}

/* 各要素に「名前」を割り当てる */
.page-header  { grid-area: header; background: #ffcdd2; }
.page-sidebar { grid-area: sidebar; background: #c8e6c9; }
.page-main    { grid-area: main; background: #bbdefb; }
.page-footer  { grid-area: footer; background: #e1bee7; }

/* スマホでは1列に並べ替えるのも一瞬！ */
@media (max-width: 600px) {
  .page-layout {
    grid-template-areas: 
      "header"
      "main"
      "sidebar"
      "footer";
    grid-template-columns: 1fr;
  }
}
</style>
```
*   **違いの観点：** 従来の「HTMLの並び順に縛られる」制限から解放されます。スマホのときだけサイドバーをメインの下に持ってくる、といった変更がCSSだけで完結します。

---

### 4. Flexbox と Grid の使い分け：現代のスタンダード

設計の初期段階からこの「癖」をつけておくと、コードが整理されます。

1.  **Gridは「外枠（マクロ）」に使う**
    *   ページ全体の2カラム、3カラムレイアウト。
    *   ギャラリーや商品リストのようなタイル状の並び。
2.  **Flexboxは「中身（ミクロ）」に使う**
    *   ボタン内のアイコンとテキストの整列。
    *   ナビゲーションメニューのリンクの横並び。
    *   「左にタイトル、右に日付」のような1行内の配置。

---

### まとめ：CSS Grid 実行可能サンプル

「2次元レイアウト」の強力さがわかる、不揃いなタイルレイアウトの例です。

```html
<div class="bento-box">
  <div class="box main-box">Main Feature (2x2)</div>
  <div class="box">Sub 1</div>
  <div class="box">Sub 2</div>
  <div class="box wide-box">Wide Promotion (1x2)</div>
</div>

<style>
.bento-box {
  display: grid;
  /* 3列、各列が同じ幅 */
  grid-template-columns: repeat(3, 1fr);
  /* 最小 100px の高さを持つ行を自動生成 */
  grid-auto-rows: minmax(100px, auto);
  gap: 10px;
  max-width: 800px;
  margin: 0 auto;
}

.box {
  background: #333;
  color: white;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 特定の要素を「連結」させる */
.main-box {
  /* 縦2行分、横2列分を占領する */
  grid-row: span 2;
  grid-column: span 2;
  background: #ff5722;
}

.wide-box {
  /* 横3列分（全幅）を占領する */
  grid-column: span 3;
  background: #4caf50;
}
</style>
```

次は、ここまでの「仕組み」「単位」「レイアウト」を総動員して、実際のWebサイト制作で避けて通れない **「レスポンシブデザイン（メディアクエリとコンテナクエリ）」** のトピックを深掘りしましょうか？