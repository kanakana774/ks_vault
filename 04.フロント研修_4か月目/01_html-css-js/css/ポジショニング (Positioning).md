「ポジショニング (Positioning)」は、通常の要素の並び順（上から下へ、左から右へ）というルールから**「特定の要素だけを脱出させて、自由な場所に配置する」**ための機能です。

Flexboxが「整列」なら、Positioningは「重ね合わせ」や「固定」に真価を発揮します。

---

### 1. ポジショニングの5つの値

`position` プロパティには5つの値がありますが、実務で使うのは主に4つです。

| 値 | 性質 | よく使うシーン |
| :--- | :--- | :--- |
| **`static`** | デフォルト。通常のルールに従う。 | リセットしたい時 |
| **`relative`** | **「基準点」**になる。少しだけ位置をずらす。 | `absolute` の親として |
| **`absolute`** | **「親」を基準**に自由に飛び回る。 | バッジ、アイコン、重なる文字 |
| **`fixed`** | **「画面（ブラウザ）」を基準**に固定される。 | ヘッダー、トップへ戻るボタン |
| **`sticky`** | スクロールすると**途中で固定**される。 | 見出し、サイドバー |

---

### 2. `relative`（相対位置）と `absolute`（絶対位置）の黄金コンビ

この2つはセットで使うことが圧倒的に多いです。
*   **親要素に `relative`** を指定すると、そこが「基準の枠」になります。
*   **子要素に `absolute`** を指定すると、その枠内での自由な配置が可能になります。

#### 【例：カード画像の右上に「New」バッジを置く】

**HTML:**
```html
<div class="card">
  <span class="badge">New</span>
  <img src="https://via.placeholder.com/150" alt="商品画像">
  <p>商品タイトル</p>
</div>
```

**CSS:**
```css
.card {
  position: relative; /* 【重要】ここを基準にする宣言 */
  width: 150px;
  border: 1px solid #ccc;
}

.badge {
  position: absolute; /* 親（.card）を基準に飛び回る */
  top: -10px;         /* 上から-10px（少しはみ出す） */
  right: -10px;       /* 右から-10px（少しはみ出す） */
  
  background: red;
  color: white;
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 4px;
}
```
*   **ポイント:** もし親に `relative` がないと、バッジは**「画面全体の右上」**まで飛んでいってしまいます。

---

### 3. `fixed`（固定位置）

要素をブラウザの画面に対して固定します。スクロールしても位置が変わりません。

#### 【例：画面右下にずっと表示される「トップへ戻る」ボタン】

**HTML:**
```html
<a href="#" class="back-to-top">↑</a>
```

**CSS:**
```css
.back-to-top {
  position: fixed; /* 画面に対して固定 */
  bottom: 20px;    /* 画面の一番下から20px */
  right: 20px;     /* 画面の一番右から20px */
  
  width: 50px;
  height: 50px;
  background: #333;
  color: white;
  text-align: center;
  line-height: 50px;
  border-radius: 50%;
  text-decoration: none;
}
```

---

### 4. `sticky`（粘着位置：現代の便利機能）

「最初は普通にスクロールされるが、画面の端に来たらそこに張り付く」というハイブリッドな挙動をします。

#### 【例：スクロールしても消えない見出し】

**HTML:**
```html
<section>
  <h2 class="sticky-header">1月のお知らせ</h2>
  <p>lorem1000</p>
</section>
<section>
  <h2 class="sticky-header">2月のお知らせ</h2>
  <p>lorem1000</p>
</section>
```

**CSS:**
```css
.sticky-header {
  position: sticky; /* 粘着配置 */
  top: 0;           /* 画面の一番上に来たら固定される */
  
  background: #f0f0f0;
  padding: 10px;
  margin: 0;
}
```
*   **ポイント:** 親要素（この場合 `section`）の範囲内だけで動きます。次の `section` が来ると、前の見出しは押し出されるように消えていきます。

---

### 5. 重なり順を操る `z-index`

Positioning（`static`以外）を使うと、要素同士が重なることがあります。その上下関係を決めるのが `z-index` です。

```css
.under {
  position: absolute;
  z-index: 1; /* 数字が小さい方が下 */
}

.over {
  position: absolute;
  z-index: 10; /* 数字が大きい方が上 */
}
```

---

### 6. 現代の設計・整理における注意点（癖付けるべきこと）

#### ① レイアウト（横並びなど）に Positioning を使わない
昔は `absolute` で要素を並べる手法もありましたが、現代では**横並びは Flexbox/Grid** で行います。Positioning はあくまで「重ねる」「固定する」ためだけの道具と考えましょう。

#### ② `z-index` の管理をシンプルにする
`z-index: 9999;` のような「とりあえず大きくする」書き方は、後で別の要素を上に重ねたい時に管理不能になります。
*   **癖付け:** `1`, `10`, `100` のように間隔をあけて指定するか、CSS変数（`--z-index-header: 100;`）で一括管理するのが現代的です。

#### ③ `absolute` を使ったら `top/bottom` と `left/right` をセットで書く
片方（例えば `top` だけ）だと、横位置がブラウザによって不安定になることがあります。必ず上下どちらかと左右どちらかの**2方向を指定**する癖をつけましょう。

#### ④ `sticky` が効かない時のチェック
`sticky` は、親要素に `overflow: hidden;` が設定されていると動かなくなります。これは非常によくある「ハマりポイント」です。

---

### まとめ：Positioning の使い分け
1.  **「親要素の一部に重ねたい」** → 親に `relative` ＋ 子に `absolute`
2.  **「画面にずっと出しておきたい」** → `fixed`
3.  **「スクロールで見えなくなるのを防ぎたい（見出しなど）」** → `sticky`
4.  **「ちょっとだけ今の位置からずらしたい」** → `relative`（あまり多用しません）

次は、現代的な設計に欠かせない **「CSS変数（カスタムプロパティ）」** や **「レスポンシブデザインの基礎（メディアクエリ）」** に進むのはいかがでしょうか？