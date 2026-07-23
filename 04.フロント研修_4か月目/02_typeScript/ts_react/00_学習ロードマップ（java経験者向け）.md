### TypeScript / React開発に向けた学習ロードマップ

#### 1. 導入と基本構文
Javaとの最大の違いは、JSがベースであり「動的型付け言語に静的な型を後付けしている」点です。

*   **let / const による変数宣言 [JS]**
    *   `var` は使わない。再代入の有無による使い分け。
*   **プリミティブ型 [TS]**
    *   `number`, `string`, `boolean`, `null`, `undefined` の指定方法。
*   **型推論 (Type Inference) [TS]**
    *   Javaよりも強力な型推論の仕組み。

#### 2. オブジェクトと配列（ReactのStateで多用）
JS/TSにおいて、オブジェクトはクラスのインスタンスだけでなく、単なる「データの塊（ハッシュマップ）」として頻繁に扱います。

*   **オブジェクトリテラル [JS]**
    *   `{ key: value }` 形式のデータ保持。
*   **インターフェース (interface) と型エイリアス (type) [TS]**
    *   オブジェクトの形状を定義する。ReactのProps定義で必須。
*   **配列の型定義 [TS]**
    *   `string[]` や `Array<number>`。
*   **オプショナルプロパティ [TS]**
    *   `property?: string`（あってもなくても良いプロパティ）。

#### 3. 関数（Reactコンポーネントの本質）
Reactは現在「関数」でコンポーネントを書くのが主流です。

*   **アロー関数 [JS]**
    *   `const func = () => { ... }` 形式の記述。
*   **関数の型定義 [TS]**
    *   引数と戻り値への型付与。
*   **分割代入 (Destructuring Assignment) [JS]** ★重要
    *   オブジェクトから特定のプロパティを抜き出す手法（ReactのProps受け取りで必須）。
*   **デフォルト引数 [JS]**

#### 4. モダンJavaScriptの必須構文（React開発の基礎）
TSの型以前に、Reactを書く上で避けて通れないJSの便利な構文です。

*   **スプレッド構文 (...) [JS]** ★重要
    *   配列やオブジェクトのコピー・結合。ReactのState更新で多用。
*   **テンプレートリテラル [JS]**
    *   バッククォート（` `）による文字列内への変数埋め込み。
*   **三項演算子 / 論理演算子による条件分岐 [JS]**
    *   JSX（HTML内）での表示切り替えに使用。
*   **map / filter / find [JS]** ★重要
    *   JavaのStream APIに近い。特に `map` は配列をループしてJSX（Component）を出すために必須。

#### 5. 高度な型システム
Javaの型よりも柔軟（かつ曖昧さを許容する）な機能です。

*   **ユニオン型 (Union Types) [TS]**
    *   `string | number`（AまたはB）。
*   **リテラル型 [TS]**
    *   `type OrderStatus = "pending" | "shipped" | "delivered";`（特定の文字列のみ許可）。
*   **Generics [TS]**
    *   Javaのジェネリクスと同様だが、Reactの `useState<T>` などで頻出。
*   **Optional Chaining (?.) [JS]**
    *   `obj?.prop`（null/undefinedならエラーにせずundefinedを返す）。
*   **Nullish Coalescing (??) [JS]**
    *   `value ?? "default"`（null/undefinedの時だけデフォルト値を採用）。

#### 6. 非同期処理
API通信（Javaで作ったバックエンドとの通信など）で必須です。

*   **Promise [JS]**
*   **async / await [JS]**
    *   Javaの同期処理に慣れている人にとって、非同期処理を同期的に書けるこの構文は必須。

#### 7. Reactに向けた準備
*   **モジュールシステム (import / export) [JS]**
    *   ファイルの分割と読み込み。
*   **JSX / TSX [JS (拡張)]**
    *   JavaScriptの中にHTMLを書くための構文。
*   **Componentの型定義 [TS]**
    *   `React.FC` や Propsの型指定の基礎。

---

### 資料構成のアドバイス

Javaエンジニア向けに説明する際、以下の「マインドセットの切り替え」を伝えるとスムーズです。

1.  **「コンパイル」の意味:** Javaのコンパイルはバイトコードへの変換ですが、TSのコンパイル（トランスパイル）は「型チェックをして、型情報を消してJSにするだけ」であること。
2.  **構造的部分型 (Structural Subtyping):** Javaは「クラス名」で型を判定（名前論理）しますが、TSは「持っているプロパティが同じなら同じ型」とみなす（構造論理）という違い。
3.  **Anyは負け:** Javaの `Object` 型のように何でも入る `any` がありますが、これを使うとTSの恩恵が消えるため、極力避ける文化であること。

まずはこの項目順で、サバイバルTypeScriptの該当箇所をピックアップして資料化するのが効率的かと思います。どの項目を深掘りしたいか等あれば、さらに詳細を作成可能です。