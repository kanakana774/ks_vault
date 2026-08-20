# lecturer-vault

**【所有】個人（kanamaru / 個人GitHub `kanakana774/lecturer-vault`）**
**【種類】講義準備ノート（Obsidian vault ＝ 1 git repo）**
**【git】push対象（個人GitHub）。会社公式資料は `MyWork/aevic/lecturer-docs` 側**
**【開く/寿命】Obsidian / 常設**

---

研修講師の**自分用カンペ／講義準備**ノート。会社公式の到達点は company の `lecturer-docs`（設計書 §4 の④境界）。設計の全体像は `MyWork/_base/00_運用設計.md`。

> **この vault で資料を書くときのルールは直下の [[CLAUDE]] を見てください**（完成品が正本・詳細版は講師の引き出し・配布のしかた）。

## ユニット構成
| フォルダ | 内容 | 構成タイプ |
|---|---|---|
| `00_SQL研修/` | SQL(PostgreSQL) | 講義/問題/詳細版/解答/補足 |
| `01_java研修_1か月目/` | Java基礎・OOP | テーマ別 |
| `02_API研修_2か月目/` | Spring Boot API・Batch | 講義/補足 |
| `03_API研修_3か月目/` | yafh-app-api（**担当外・枠**） | 最小 |
| `04_フロント研修_4か月目/` | HTML/CSS/JS→TS→React | 技術別 |
| `09_若チャレ研修/` | 若手チャレンジ（**整備見送り中**） | 最小 |
| `_general/` | 単元横断（全般Tips・補助資料・共有Excalidrawライブラリ・claude） | — |

## 命名規約（§9準拠）
- 順序付きフォルダは `NN_名前`（半角アンダースコア・2桁）。単元横断は `_` 始まり（`_general`, `_archive`）。
- 役割語彙（当てはまる所に一貫適用）: `講義資料` / `問題`（`問題_講師用`/`問題_配布用`）/ `詳細版` / `解答` / `補足` / `画像置き場` / `_archive`。
  **役割は名前で識別し、`NN_` は unit 内の並び順。** unit をまたいで同じ番号になるとは限らない（例: `00_SQL研修` は `03_詳細版` を講義資料の隣に置くため `04_解答` / `05_補足`。詳細版が無い unit は `03_解答` / `04_補足`）。
- 技術名は英語（`TypeScript`/`react`/`html-css-js`）。作図(`.excalidraw.md`/`.canvas`)は図をユニット内に維持（§3）。
- 各フォルダに `README.md`（冒頭に【所有】【種類】【git】【開く/寿命】タグ）。

## ⚠️ リンク保守メモ（外部リネーム時の注意）
- Obsidianは**basename（ファイル名）でリンク解決**するため、通常フォルダ移動/リネームはリンクを壊さない。
- ただし**フルパス参照は例外**で、外部（git/エクスプローラ）でのリネーム時は連動修正が必要:
  - **`.canvas`** … `"file"` 値がフルパス（`02_API研修.../API研修説明用資料.canvas`）
  - **Excalidraw `.excalidraw.md`** … 一部が内部にフルパス参照（`02_API研修.../JVMのメモリ空間`、`00_SQL研修/図解用`）
  - **`00_SQL研修/01_講義資料/`** の画像embed 多数がフルパス
- **Obsidianアプリ内リネーム**（`alwaysUpdateLinks:true`）なら自動追従する。
