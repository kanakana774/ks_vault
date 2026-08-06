# Excalidraw ファイル形式リファレンス

Obsidianのexcalidrawプラグインが直接読める`.excalidraw.md`のスキーマ。2026-08-06、公式リモートMCP
（`mcp.excalidraw.com`）への`export_to_excalidraw`が成功したペイロードと、Obsidianでの表示検証（矩形+
ラベルが正しく中央表示された）で確認済み。

## 重要: `label`ショートハンドは本物のスキーマではない

コミュニティ製MCP（`mcp_excalidraw`等）は`{"label": {"text": "..."}}`という簡易記法を提供するが、
これは**そのツール内部だけの表現**。本物のExcalidrawは、図形とテキストを**別要素**にして
`containerId`で紐付ける。Obsidianプラグインが読めるのは後者のみなので、直接ファイルを書くときは
必ず後者の形を使う。

## `.excalidraw.md`ファイルの骨格

```markdown
---

excalidraw-plugin: parsed
tags: [excalidraw]

---
==⚠  Switch to EXCALIDRAW VIEW in the menu of this document. ⚠==
# Excalidraw Data

## Text Elements
%%
## Drawing
```json
{ ... 中身は下記 ... }
```
%%
```

`json`コードブロックの中身が本体。トップレベルは以下の4キー:

```json
{
  "type": "excalidraw",
  "version": 2,
  "source": "https://excalidraw.com",
  "elements": [ /* 下記参照 */ ],
  "appState": { "viewBackgroundColor": "#ffffff", "gridSize": null },
  "files": {}
}
```

## ラベル付き矩形（最頻出パターン）

矩形要素の`boundElements`でテキスト要素idを指定し、テキスト要素側は`containerId`で矩形に戻り紐付ける。
テキストの`x,y,width,height`は矩形の内側中央に**自分で計算して**置く（自動センタリングはされない）。

```json
{
  "id": "rect-api",
  "type": "rectangle",
  "x": 100, "y": 100, "width": 160, "height": 80,
  "angle": 0,
  "strokeColor": "#1e1e1e",
  "backgroundColor": "#a5d8ff",
  "fillStyle": "solid",
  "strokeWidth": 2,
  "strokeStyle": "solid",
  "roughness": 1,
  "opacity": 100,
  "groupIds": [],
  "frameId": null,
  "roundness": { "type": 3 },
  "seed": 123456789,
  "version": 1,
  "versionNonce": 987654321,
  "isDeleted": false,
  "boundElements": [{ "id": "text-api", "type": "text" }],
  "updated": 1754460000000,
  "link": null,
  "locked": false
},
{
  "id": "text-api",
  "type": "text",
  "x": 135, "y": 127, "width": 90, "height": 25,
  "angle": 0,
  "strokeColor": "#1e1e1e",
  "backgroundColor": "transparent",
  "fillStyle": "solid",
  "strokeWidth": 2,
  "strokeStyle": "solid",
  "roughness": 1,
  "opacity": 100,
  "groupIds": [],
  "frameId": null,
  "roundness": null,
  "seed": 223456789,
  "version": 1,
  "versionNonce": 887654321,
  "isDeleted": false,
  "boundElements": null,
  "updated": 1754460000000,
  "link": null,
  "locked": false,
  "text": "API Server",
  "fontSize": 20,
  "fontFamily": 1,
  "textAlign": "center",
  "verticalAlign": "middle",
  "containerId": "rect-api",
  "originalText": "API Server",
  "lineHeight": 1.25,
  "baseline": 18
}
```

- テキストの`width`は概算で `text.length × fontSize × 0.6` 程度、`height`は`fontSize × lineHeight`。
- `x,y`は矩形の中心からテキストの半分幅・半分高さを引いた位置（中央揃えにする場合）。
- `id`は要素間で一意であればよい（例中の`seed`/`versionNonce`は適当な固定値でよい、Obsidian側で
  再生成される）。

## 矢印（ラベルなし・シンプル接続）

```json
{
  "id": "arrow-1",
  "type": "arrow",
  "x": 260, "y": 140, "width": 100, "height": 0,
  "angle": 0,
  "strokeColor": "#1e1e1e",
  "backgroundColor": "transparent",
  "fillStyle": "solid",
  "strokeWidth": 2,
  "strokeStyle": "solid",
  "roughness": 1,
  "opacity": 100,
  "groupIds": [],
  "frameId": null,
  "roundness": { "type": 2 },
  "seed": 333, "version": 1, "versionNonce": 333, "isDeleted": false,
  "boundElements": null,
  "updated": 1754460000000, "link": null, "locked": false,
  "points": [[0, 0], [100, 0]],
  "lastCommittedPoint": null,
  "startBinding": { "elementId": "rect-a", "focus": 0, "gap": 4 },
  "endBinding": { "elementId": "rect-b", "focus": 0, "gap": 4 },
  "startArrowhead": null,
  "endArrowhead": "arrow"
}
```

矢印に文字ラベルを付ける場合は、矩形と同じ要領で別のtext要素を作り`containerId`を矢印のidにする
（`textAlign: "center"`, `verticalAlign: "middle"`のまま、矢印の中間あたりに座標を置く）。

## 楕円・ダイヤモンド

`type`を`"ellipse"`または`"diamond"`に変えるだけで、他のフィールドは矩形と同じ。

## 配色

lecturer-vault内で統一して使う色（公式MCPの`read_me`で示された配色を流用）:

| 用途 | strokeColor | backgroundColor |
|---|---|---|
| 入力・主要ノード | `#1e1e1e` | `#a5d8ff`（薄青） |
| 成功・出力 | `#1e1e1e` | `#b2f2bb`（薄緑） |
| 警告・保留 | `#1e1e1e` | `#ffd8a8`（薄橙） |
| 処理・中間層 | `#1e1e1e` | `#d0bfff`（薄紫） |
| エラー・注意 | `#1e1e1e` | `#ffc9c9`（薄赤） |
| 注釈・メモ | `#1e1e1e` | `#fff3bf`（薄黄） |

背景色を塗る場合は`fillStyle: "solid"`（`"hachure"`だと斜線塗り＝手描き感は出るが文字が読みにくくなりやすい）。

## 座標設計の考え方（重ならないようにする）

- 矩形の最小サイズは概ね120×60。詰め込みすぎると次のテキスト計算が破綻する
- 要素間は最低20〜30pxの間隔を空ける
- 縦に並べるボックスはy座標の差がフォントサイズ+パディング未満にならないよう注意
  （テキストがボックスの外にはみ出す/次の要素と重なる典型的な失敗パターン）
- 複雑な図（要素10個以上、矢印が交差する等）は目算だけで座標を決めきるのが難しい。
  `SKILL.md`のブラウザ検証ループを使うこと
