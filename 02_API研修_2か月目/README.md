# 02_API研修_2か月目

**【所有】個人（講師カンペ）**
**【種類】講義準備ノート（Obsidian）**
**【git】lecturer-vault（個人GitHub）内。push対象**
**【開く/寿命】Obsidian / 常設**

---

研修2か月目（Spring Boot API ＋ Batch）の講師用資料。

## 構成
- `01_講義資料/` （旧 `Lecture/`）
  - `API/` … API講義本体（`_attachments/`=画像・`_excalidraw/`=作図・`資料作成案置き場/`・`API研修説明用資料.canvas`）
  - `Batch/` … Batch講義（`_attachments/`=画像・`資料作成案置き場/`）
- `04_補足/` … `レビュー観点/`・`過去にあった不具合/`（MyBatis/Springのトラブルシューティング・ナレッジ）・`改善案/`
- `postman動作確認用テストシナリオ/` … 動作確認用 Postman コレクション

## リンクに関する注意（保守メモ）
- `_excalidraw/API研修説明用資料.canvas`（※canvasは`API/`直下）と Excalidraw の一部は**フルパス参照**を持つ（basename解決ではない）。このunitのフォルダを再度リネームする際は、`API研修説明用資料.canvas` の `"file"` 値と `_excalidraw/JVMのメモリ空間のイメージ.excalidraw.md` の自己参照を**連動修正**すること（外部リネーム時）。Obsidianアプリ内リネームなら自動追従。

> 命名規約: 順序付きフォルダは `NN_名前`。役割語彙 `01_講義資料`/`04_補足`。画像は `_attachments/`、Excalidrawは `_excalidraw/`。
> 講義ファイル名の全角コロン`：`・`、、、`・`～` は除去済み。残る `NN.` ドット始まりの講義ファイル（`00.アジェンダのような` 等）は、canvasのフルパス参照が多く連動コストが高いため据え置き。
