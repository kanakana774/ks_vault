## HTMLの`<label>`とグループ入力要素について

### 結論から言うと

「ステータス」というグループ全体のラベルには、**`<label>`ではなく`<legend>`を使うべき**です。

---

### なぜ`<label>`はグループに向かないのか

`<label>`は**1つの入力要素**と1対1で紐付けるものです。`for`属性（または`htmlFor`）に指定できるIDは1つだけなので、複数のチェックボックス・ラジオボタンのグループ全体を指すには設計上合いません。

---

### 正しい構造：`<fieldset>` + `<legend>`

```html
<fieldset>
  <legend>ステータス</legend>

  <label>
    <input type="checkbox" name="status" value="in_progress"> 進行中
  </label>

  <label>
    <input type="checkbox" name="status" value="done"> 完了
  </label>
</fieldset>
```

|要素|役割|
|---|---|
|`<fieldset>`|関連する入力要素をグループ化|
|`<legend>`|そのグループの見出し・ラベル|
|`<label>`|個々のチェックボックス/ラジオと1対1で紐付け|

---

### 各`<label>`のリンク方法（2通り）

**① 内包する方法（implicit label）** ← グループ要素ではこちらが簡潔

```html
<label>
  <input type="checkbox" name="status" value="in_progress"> 進行中
</label>
```

**② `for`/`id`で紐付ける方法（explicit label）**

```html
<input type="checkbox" id="status-in-progress" name="status" value="in_progress">
<label for="status-in-progress">進行中</label>
```

---

### アクセシビリティ上の効果

- スクリーンリーダーが「ステータス、グループ」→「進行中、チェックボックス」のように読み上げてくれる
- `<label>`をクリックするとチェックボックスがトグルされる（クリック領域が広がる）
- `<legend>`なしだと各チェックボックスが何のグループか文脈が失われる

---

### まとめ

- **グループ全体の名前** → `<legend>`（`<label>`ではない）
- **個々の選択肢** → それぞれ`<label>`で紐付ける
- `<label>`を`<input>`なしで飾りとして使うのは避ける（`<span>`や`<p>`で代替）

## デザイン上`<legend>`を表示したくない場合

### 一般的なアプローチ：視覚的に隠す（Visually Hidden）

CSSで**見た目から消しつつ、スクリーンリーダーには読ませる**のが最も一般的なベストプラクティスです。

```css
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

```html
<fieldset>
  <legend class="visually-hidden">ステータス</legend>
  <label><input type="checkbox" name="status" value="in_progress"> 進行中</label>
  <label><input type="checkbox" name="status" value="done"> 完了</label>
</fieldset>
```

`display: none`や`visibility: hidden`と違い、スクリーンリーダーのアクセシビリティツリーには残ります。

---

### `aria-group` + `aria-labelledby` で代替する方法

`<fieldset>`/`<legend>`を使わず、`role="group"`と`aria-labelledby`で同等のセマンティクスを作る方法もあります。

```html
<div role="group" aria-labelledby="status-label">
  <span id="status-label" class="visually-hidden">ステータス</span>
  <label><input type="checkbox" name="status" value="in_progress"> 進行中</label>
  <label><input type="checkbox" name="status" value="done"> 完了</label>
</div>
```

デザイン上`<fieldset>`のデフォルトスタイル（枠線など）をリセットする手間も省けます。

---

### `display: none` や `visibility: hidden` はNG

```css
/* ❌ これらはスクリーンリーダーにも読まれなくなる */
legend { display: none; }
legend { visibility: hidden; }
```

「デザイン上不要」と「アクセシビリティ上不要」は別の話なので、視覚的に消すだけなら必ず visually-hidden パターンを使います。

---

### どれを選ぶか

|状況|推奨|
|---|---|
|`<fieldset>`の枠線などのスタイルリセットが許容できる|`<fieldset>` + `<legend class="visually-hidden">`|
|CSSリセットが面倒、デザイン自由度を上げたい|`role="group"` + `aria-labelledby`|
|ラベルが視覚的にも明示されている（見出しなど）|`aria-labelledby`で既存要素を参照|

実務では **visually-hiddenクラスはプロジェクト共通のユーティリティ** として定義しておくことが多く、TailwindCSSなら`sr-only`クラスがそのまま使えます。