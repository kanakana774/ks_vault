# Reactレビュー観点 ― 入力検証と合計の課題

> 対象課題: テキスト入力 input1 / input2 を用意し、イコールボタンで合計を出す。
> 空・半角数字以外はエラーメッセージを出す。
>
> このドキュメントは「初心者がやりがちなイマイチな書き方」を、**NG → OK → なぜ** の順で整理したレビュー用の観点集です。
> レビューは表層のバグからではなく、**思想レベル（Reactの考え方）から降りていく**と、同じ間違いの再発を防げます。

---

## レビューの見る順序

| 順 | 観点 | 問いかけ |
|---|---|---|
| 0 | コンポーネントの骨格 | そもそも動く形になっているか？（import/export・return） |
| 1 | 値の取得方法 | DOMから値を読んでいないか？（Reactの思想） |
| 2 | state設計 | 何を・どう持つかが素直か？ |
| 3 | 検証ロジック | いつ・どこで検証しているか？ |
| 4 | 計算・判定の落とし穴 | 文字列/falsyの罠を踏んでいないか？ |
| 5 | 表示・条件分岐 | falsy罠と矛盾状態はないか？ |
| 6 | TypeScript | 型が意図を語っているか？ |

「動く形か → 思想 → 設計 → 実装 → 表層バグ」の順です。0章は“そもそもコンパイル・描画されるか”という土台なので最初に見ます。

---

## 0. コンポーネントの骨格（import / export / return / JSXのまとまり）

### なぜ大事か（motivation）

ここは「ロジックの良し悪し」以前の、**そもそも動く形になっているか**の話です。
import/export・return は初心者が最も忘れがちで、しかもエラーメッセージが分かりにくい（何も表示されないだけ、等）ため、
「書いたのに動かない」で長時間ハマる典型ポイントです。土台なので最初に確認します。

### 0-1. import / export の忘れ

**NG: 使うものを import していない / export し忘れる**

```tsx
// useState を import せずに使う
function Calculator() {
  const [input1, setInput1] = useState("");  // useState is not defined エラー
  return <input value={input1} onChange={(e) => setInput1(e.target.value)} />;
}
// export がない → 別ファイル / エントリから読み込めず、画面に出ない
```

**なぜNGか**
- モジュールは「何を外に出すか（export）」「何を借りるか（import）」を明示する契約。どちらか欠けても動かない。
- `useState` などフックは `react` から名前付きで import する必要がある。忘れると `is not defined`。
- export し忘れは、コンポーネント自体は正しくてもどこからも使えない（描画されない）。

**OK: 必要なものを import し、コンポーネントを export する**

```tsx
import { useState } from "react";        // 使うフックを名前付きで借りる

export default function Calculator() {   // このコンポーネントを外に出す
  const [input1, setInput1] = useState("");
  return <input value={input1} onChange={(e) => setInput1(e.target.value)} />;
}
```

**default export と named export の取り違えにも注意**

```tsx
// 出す側が default の場合
export default function Calculator() { ... }
import Calculator from "./Calculator";      // OK: 波括弧なし

// 出す側が named の場合
export function Calculator() { ... }
import { Calculator } from "./Calculator";  // OK: 波括弧あり
```

- 出す側と借りる側で「波括弧の有無」が食い違うと、`undefined` を読み込んでしまい描画されない。

### 0-2. return がない

**NG: JSX を書いたのに return していない**

```tsx
export default function Calculator() {
  const [input1, setInput1] = useState("");
  <div>合計フォーム</div>;   // 式として書いただけ。return していない
}
// → 関数は undefined を返す。エラーにならず「何も表示されない」ので気づきにくい
```

**なぜNGか**
- 関数コンポーネントは「**JSX を return する関数**」。return がないと `undefined` を返し、画面に何も出ない。
- 構文エラーにならないぶん、「書いたのに真っ白」で悩みやすい。

**OK: return する**

```tsx
export default function Calculator() {
  const [input1, setInput1] = useState("");
  return <div>合計フォーム</div>;   // ちゃんと返す
}
```

**アロー関数の「暗黙のreturn」の取り違え**

```tsx
const App = () => { <div/> };          // NG: {} で囲むと return が必要。何も返らない
const App = () => <div/>;              // OK: {} なしなら式がそのまま返る（暗黙のreturn）
const App = () => { return <div/>; };  // OK: {} で囲むなら return を書く
```

- `() => { ... }` の波括弧は「関数の本体ブロック」。中で return を書かないと値が返らない。
- `() => (...)` の丸括弧は「返す式を囲むだけ」なので return 不要。この2つの括弧の意味の違いが混同ポイント。

### 0-3. return で囲めていない（複数要素 / 複数行）

**NG-A: トップレベルに要素を並べる**

```tsx
return (
  <input value={input1} onChange={...} />
  <input value={input2} onChange={...} />
  <button onClick={handleEqual}>＝</button>
);
// NG: return できるのは「1つのまとまり」だけ。兄弟を並べると構文エラー
```

**なぜNGか**
- JSX は「単一のルート要素」しか返せない。要素を横並びにすると、どれが返り値か決まらずエラーになる。

**OK: 親要素か Fragment で1つにまとめる**

```tsx
// 親の <div> で包む
return (
  <div>
    <input value={input1} onChange={...} />
    <input value={input2} onChange={...} />
    <button onClick={handleEqual}>＝</button>
  </div>
);

// 余計な div を増やしたくないときは Fragment（<>...</>）で包む
return (
  <>
    <input value={input1} onChange={...} />
    <input value={input2} onChange={...} />
    <button onClick={handleEqual}>＝</button>
  </>
);
```

**NG-B: return の直後で改行してしまう（セミコロン自動挿入の罠）**

```tsx
return
  <div>合計フォーム</div>;   // NG: return の直後で改行 → return; と解釈され undefined が返る
```

**なぜNGか**
- JavaScript は `return` の直後に改行があると、そこで自動的にセミコロンを補う（ASI: 自動セミコロン挿入）。
- 結果 `return;` になり、下の JSX は無視されて何も表示されない。

**OK: 複数行JSXは丸括弧で「1つの式」に囲む**

```tsx
return (
  <div>合計フォーム</div>
);
```

- `return (` と同じ行から丸括弧を開くのがポイント。括弧で囲めば改行しても1つの式として扱われる。

---

## 1. 値の取得方法（制御コンポーネント vs 非制御）

### なぜ大事か（motivation）

Reactの根っこは「**stateが真実（source of truth）、画面はその結果**」という一方向の流れです。
DOMから値を読み取った瞬間、この流れが逆流し、「今の正しい値はどこにあるのか」が曖昧になります。
初心者の多くはjQuery的な発想（DOMを直接触る）を持ち込むので、ここが最初の分岐点です。

### NG: DOMから直接値を読む

```tsx
export default function Calculator() {
  const handleEqual = () => {
    // input要素をDOMから直接掴んで値を読む
    const v1 = (document.getElementById("input1") as HTMLInputElement).value;
    const v2 = (document.getElementById("input2") as HTMLInputElement).value;
    console.log(Number(v1) + Number(v2));
  };

  return (
    <div>
      <input id="input1" type="text" />
      <input id="input2" type="text" />
      <button onClick={handleEqual}>＝</button>
    </div>
  );
}
```

**なぜNGか**
- 値の置き場所がReact（state）ではなくDOMになり、「真実が2箇所」になる。
- エラー表示や再計算のたびにDOMを読み直す必要が出て、状態管理が破綻していく。
- `getElementById` は「Reactの外」に手を伸ばす操作。React本来の書き方から外れているサイン。

### OK: 制御コンポーネント（value と onChange をセットで）

```tsx
export default function Calculator() {
  const [input1, setInput1] = useState("");
  const [input2, setInput2] = useState("");

  return (
    <div>
      <input
        type="text"
        value={input1}                                  // stateを画面に反映
        onChange={(e) => setInput1(e.target.value)}     // 入力をstateに戻す
      />
      <input
        type="text"
        value={input2}
        onChange={(e) => setInput2(e.target.value)}
      />
    </div>
  );
}
```

**なぜOKか**
- 値の真実は `input1` / `input2`（state）ただ1箇所。画面は常にその写し。
- 計算も検証も「stateを見るだけ」で済む。DOMを掴む処理が消える。

### よくある中途半端

```tsx
// value だけ付けて onChange がない → 入力しても文字が出ない（読み取り専用化）
<input type="text" value={input1} />

// onChange だけで value がない → stateは更新されるが画面と同期していない
<input type="text" onChange={(e) => setInput1(e.target.value)} />
```

制御コンポーネントは **value と onChange が必ずペア**。片方だけは成立しません。

---

## 2. state設計（何を・どう持つか）

### なぜ大事か（motivation）

stateの持ち方は後工程すべてに波及します。
「同じ性質のものは1つにまとめる」「本物のstateと導出値を区別する」の2つが崩れると、
更新漏れ・同期ずれといったバグの温床になります。

### NG-A: 同種のエラーを個別stateで乱立させる

```tsx
const [error1, setError1] = useState("");
const [error2, setError2] = useState("");
// エラー種別が増えるたびに error3, error4 ... と増殖していく
```

**なぜNGか**
- エラーは「同じ性質の情報の集まり」。増えるたびにstateとsetter両方を足すのは不経済。
- 「全エラーをクリアする」処理が `setError1(""); setError2(""); ...` と散らばり、消し忘れを生む。

### OK-A: 配列でまとめる

```tsx
const [errors, setErrors] = useState<string[]>([]);

// まとめて設定 / まとめてクリア
setErrors(["入力1が空です", "入力2に半角数字以外が入力されてます"]);
setErrors([]); // 全消しが1行
```

**なぜOKか**
- 「エラーの集合」という実体に、データ構造（配列）が一致している。
- 追加・全消し・件数判定（`errors.length`）がすべて素直に書ける。

### NG-B: state を直接書き換える（イミュータブル違反）

```tsx
const handleEqual = () => {
  errors.push("入力1が空です");  // 既存の配列を直接いじっている
  setErrors(errors);             // 同じ参照を渡すので再レンダリングされないことも
};
```

**なぜNGか**
- Reactは「前と違うオブジェクト/配列か」を参照で見分ける。中身を書き換えても参照は同じなので、更新が検知されない場合がある。
- 既習の「stateはイミュータブルに更新する」原則に反する。

### OK-B: 新しい配列を作って渡す

```tsx
const handleEqual = () => {
  const nextErrors: string[] = [];
  if (input1 === "") nextErrors.push("入力1が空です");
  // ... 検証を集める ...
  setErrors(nextErrors);   // 毎回新しい配列を渡す
};
```

### 補足: 「合計」はstateにしてよいのか？（導出値の判断）

- 今回は **「イコールを押した瞬間だけ計算する」** ので、合計を `result` state に持つのは妥当。
  「イベントの結果として確定した値」だから。
- 一方、もし要件が **「入力するたびリアルタイムに合計を表示」** なら、
  合計は input1/input2 から**導出できる値**なので、state化は既習のアンチパターン（レンダリング中に計算すべき）。

```tsx
// リアルタイム表示なら state ではなく、レンダリング時に計算するのが正しい
const total = Number(input1) + Number(input2);
```

**判断基準**: その値は「入力から毎回計算できる」か？ できるなら基本は導出。
今回はタイミングを区切っているので state 保持が理にかなう、という線引きです。

---

## 3. 検証ロジック（いつ・どこで検証するか）

### なぜ大事か（motivation）

「検証をどのタイミングで走らせるか」は要件そのもの。
初心者は「とりあえず入力のたびに検証」しがちですが、**いつ検証するかを意図して選ぶ**のがこの課題の主題です。

### NG-A: onChange のたびに検証する（要件違反）

```tsx
const handleChange1 = (e: React.ChangeEvent<HTMLInputElement>) => {
  const value = e.target.value;
  setInput1(value);
  if (value === "") setError1("入力1が空です");   // 1文字打つたびに検証が走る
  else setError1("");
};
```

**なぜNGか**
- 要件は「**イコール押下時**に検証」。入力途中で赤エラーが出るのは仕様と違う体験。
- 検証タイミングが各 onChange に散らばり、「どこで検証しているか」が追えなくなる。

### OK-A: イコール押下時にまとめて検証

```tsx
const handleEqual = () => {
  const nextErrors: string[] = [];
  const e1 = validate(input1, "入力1");
  const e2 = validate(input2, "入力2");
  if (e1) nextErrors.push(e1);
  if (e2) nextErrors.push(e2);
  // ...
};
```

**なぜOKか**
- 検証の入口が `handleEqual` 1箇所。「いつ検証するか」が一目でわかる。
- 要件どおりのタイミング。

### NG-B: 検証を関数化せずコピペする

```tsx
// input1用
if (input1 === "") { /* 空エラー */ }
else if (!/^[0-9]+$/.test(input1)) { /* 数字以外エラー */ }

// input2用（ほぼ同じコードをもう一度）
if (input2 === "") { /* 空エラー */ }
else if (!/^[0-9]+$/.test(input2)) { /* 数字以外エラー */ }
```

**なぜNGか**
- 同じ検証ルールが2箇所に。ルール変更（例: 桁数制限追加）のとき両方直す必要があり、片方忘れる。

### OK-B: ラベルだけ変えて関数化

```tsx
function validate(value: string, label: string): string | null {
  if (value === "") return `${label}が空です`;
  if (!/^[0-9]+$/.test(value)) return `${label}に半角数字以外が入力されてます`;
  return null; // 問題なし
}
```

**なぜOKか**
- 検証ルールが1箇所に集約。変えるときは1関数だけ直せばよい。
- 「差分はラベルだけ」という構造が明確になる。

### NG-C: 自前検証を放棄して type="number" に丸投げ

```tsx
<input type="number" value={input1} onChange={...} />
```

**なぜNGか**
- 課題の主題は「**自前で空チェック・半角数字チェックをして、指定文言のエラーを出す**」こと。
- `type="number"` はブラウザ任せで、要件の文言（「入力1が空です」等）も複数エラー同時表示も実現できない。主題が消える。

---

## 4. 計算・判定の落とし穴（この課題の定番バグ）

### なぜ大事か（motivation）

JavaScriptの型変換と数値判定は罠が多く、初心者が「動いたつもりで間違える」典型ポイント。
`+` の意味が文脈で変わること、空文字や途中まで数字の文字列の扱いを、具体例で押さえます。

### NG-A: 文字列のまま足して連結される

```tsx
const total = input1 + input2;  // "1" + "2" === "12"（数値の3ではない）
```

**なぜNGか**
- input は文字列。文字列同士の `+` は**連結**。`1 + 2` のつもりが `"12"` になる。

### OK-A: 数値に変換してから足す

```tsx
const total = Number(input1) + Number(input2);  // 1 + 2 === 3
```

### NG-B: 空文字を isNaN / Number で弾けたつもりになる

```tsx
if (isNaN(Number(input1))) { /* 数字以外エラー */ }
// しかし Number("") は 0。空文字がエラーをすり抜けて 0 として計算される
```

**なぜNGか**
- `Number("")` は `NaN` ではなく `0`。空チェックを別で先にやらないと、空欄が0扱いで素通りする。

### NG-C: parseInt が途中まで読んで通してしまう

```tsx
parseInt("12a", 10);   // 12（"a" を無視して途中まで解釈）
Number("12a");         // NaN（こちらは弾ける）
```

**なぜNGか**
- `parseInt` は「読めるところまで」読む。`"12a"` を有効な 12 と誤認する。
- 「半角数字だけで構成されているか」を判定したいのに、部分一致を許してしまう。

### NG-D: isNaN は空白や全角で意図とずれる

```tsx
isNaN(Number(" "));    // false（空白は 0 に変換され、数字扱いになる）
isNaN(Number("１２"));  // false（全角数字も変換されてしまう環境がある）
```

**なぜNGか**
- 「半角数字か」を数値変換で判定すると、空白・全角などの例外に振り回される。

### OK-B〜D: 正規表現で「半角数字だけか」を直接表現する

```tsx
// 空チェックを先に、そのあと「1文字以上の半角数字だけ」かを判定
if (value === "") return `${label}が空です`;
if (!/^[0-9]+$/.test(value)) return `${label}に半角数字以外が入力されてます`;
```

**なぜOKか**
- `/^[0-9]+$/` は「先頭から末尾まで半角数字だけ」を意味する。意図がそのままコードになっている。
- 全角「１２３」、空白、`"12a"` すべて弾ける。空文字は手前でチェック済み。

| 入力 | `Number()` | `parseInt()` | `/^[0-9]+$/` |
|---|---|---|---|
| `""` | 0（罠） | NaN | false ✓（別途空チェック推奨） |
| `"12a"` | NaN | 12（罠） | false ✓ |
| `" "` | 0（罠） | NaN | false ✓ |
| `"１２３"`（全角） | 環境依存 | 環境依存 | false ✓ |
| `"12"` | 12 | 12 | true ✓ |

---

## 5. 表示・条件分岐（Reactの典型トラップ）

### なぜ大事か（motivation）

「値があるときだけ表示」を `{値 && <JSX>}` で書くのは定番ですが、
**0 や空文字は falsy**なので、意図せず「何も表示されない」バグになります。
この課題は合計が 0 になり得るため、まさに踏みやすい罠です。

### NG-A: falsy な 0 が表示されない

```tsx
{result && <div>合計：{result}</div>}
// result が 0 のとき、0 は falsy なので div ごと描画されない
// （さらに && の評価結果として 0 が画面に出てしまうこともある）
```

**なぜNGか**
- `0 + 0 = 0` のとき「合計：0」を出したいのに、何も出ない。
- 「未計算」と「合計0」を `&&` では区別できない。

### OK-A: null と明示的に比較する

```tsx
{result !== null && <div>合計：{result}</div>}
// 「まだ計算していない(null)」ときだけ隠す。0 はちゃんと表示される
```

### NG-B: result の初期値を 0 にする

```tsx
const [result, setResult] = useState(0);
// 「未計算」も 0、「合計0」も 0。両者を区別できない
```

**なぜNGか**
- 「未計算」と「合計0」が同じ値になり、押す前なのに「合計：0」が出て計算済みに見える。

### OK-B: 「未計算」を null で表す

```tsx
const [result, setResult] = useState<number | null>(null);
// null = まだ計算していない / 数値 = 計算済み。状態が2つに分かれて意味が明確
```

### NG-C: エラー時に古い結果が残る（矛盾状態）

```tsx
const handleEqual = () => {
  const nextErrors = collectErrors();
  if (nextErrors.length > 0) {
    setErrors(nextErrors);
    // setResult(null) を忘れると、前回の「合計：5」とエラーが同時に画面に残る
    return;
  }
  setResult(Number(input1) + Number(input2));
};
```

**なぜNGか**
- 「エラーが出ている＝計算できていない」はずなのに、前回の合計が残ると画面が矛盾する。

### OK-C: エラー時は結果をクリアする

```tsx
if (nextErrors.length > 0) {
  setErrors(nextErrors);
  setResult(null);   // 結果を消して、画面を常に一貫させる
  return;
}
setErrors([]);       // 逆に成功時はエラーを消す
setResult(Number(input1) + Number(input2));
```

**原則**: 画面は常に「今の入力の状態」だけを映す。片方を更新したらもう片方の後始末も忘れない。

### NG-D: リストの key に index を使う（この課題では実害小）

```tsx
{errors.map((msg, index) => <div key={index}>{msg}</div>)}
```

- 今回のように「毎回作り直す静的なエラー配列」では実害はほぼない。
- ただしリストの原則として「並び替え・追加削除があると index キーは不具合の元」なので、指摘の対象にはなる。

---

## 6. TypeScript

### なぜ大事か（motivation）

型は「このstateには何が入るか」という設計意図の宣言でもあります。
型が付いていないと、`null` を許すのか、配列なのか、が読み手にも自分にも伝わりません。

### NG: 型が推論できず any になる

```tsx
const [errors, setErrors] = useState([]);        // never[] 扱いになり push で困る
const [result, setResult] = useState(null);      // null 固定で数値を入れられない

const handleChange = (e) => {                    // e が暗黙の any
  setInput1(e.target.value);
};
```

**なぜNGか**
- `useState([])` は要素型が定まらず、後で文字列を入れようとすると型エラー。
- `useState(null)` は `null` 型に固定され、数値の代入が弾かれる。
- 引数 `e` の型がないと補完も型チェックも効かない。

### OK: 意図を型で明示する

```tsx
const [errors, setErrors] = useState<string[]>([]);       // 文字列の配列
const [result, setResult] = useState<number | null>(null); // 数値 or 未計算

const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
  setInput1(e.target.value);
};
```

**なぜOKか**
- `number | null` が「計算済み or 未計算」という状態設計をそのまま表している。
- 型があることで、falsy罠（5章）の `result !== null` 判定とも一貫する。

> 補足: `input1` のように初期値が文字列 `""` の場合は `useState("")` で型が推論されるため、明示は必須ではない。
> 「推論に任せてよい所」と「明示すべき所（空配列・null初期値）」の線引きも観点になる。

---

## レビューまとめ（チェックリスト）

**骨格（そもそも動くか）**
- [ ] 使うフック（useStateなど）を import しているか
- [ ] コンポーネントを export しているか。default / named と import 側が一致しているか
- [ ] JSX を return しているか（書いただけで返していない、を見る）
- [ ] アロー関数の `{}` 本体で return を書き忘れていないか
- [ ] 複数要素を親要素か Fragment で1つに包んでいるか
- [ ] `return` の直後で改行して `return;` になっていないか（複数行は `(` で囲む）

**思想・設計・実装**
- [ ] 値をDOM（getElementById/ref）から読んでいないか。value+onChangeのペアで制御しているか
- [ ] 同種エラーを個別stateで乱立させず、配列でまとめているか
- [ ] stateを直接 push せず、新しい配列を作って渡しているか
- [ ] 検証は「イコール押下時」に一箇所で走っているか（onChangeで散らばっていないか）
- [ ] 検証ロジックを関数化し、コピペ重複を避けているか
- [ ] `Number()` で変換してから足しているか（文字列連結になっていないか）
- [ ] 空文字・`"12a"`・全角・空白を正しく弾けているか（`/^[0-9]+$/` ＋先行の空チェック）
- [ ] 合計0が表示されるか（`result &&` ではなく `result !== null`）
- [ ] result初期値は `null` か（0ではないか）
- [ ] エラー時に古い結果をクリアしているか（矛盾状態を作っていないか）
- [ ] state・イベント引数に型が付いているか（`string[]` / `number | null` / `ChangeEvent`）

### 最も教材向きな「単一変数比較」の候補

1行だけ変えて挙動が変わり、観察しやすいもの:

1. **falsy罠**: `{result && ...}` → `{result !== null && ...}`（合計0が出る/出ない）
2. **文字列連結**: `input1 + input2` → `Number(input1) + Number(input2)`（"12" / 3）
3. **数値判定**: `parseInt("12a")` → `/^[0-9]+$/.test("12a")`（通る/弾く）
4. **returnの改行罠**: `return` 改行 `<div/>` → `return (` 改行 `<div/>` 改行 `)`（真っ白 / 表示される）

いずれも「壊れた版／直した版」を横に並べると差分が1行で、初心者に効きます。
