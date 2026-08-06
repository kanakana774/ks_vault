---
name: excalidraw
description: 講義資料（Obsidian vault、主にlecturer-vault）に載せる図をExcalidraw形式で作る・直す時に使う。Mermaidは表現力が弱く、SVGは手書き座標で文字被り等のズレが起きるため、Excalidrawの本来のJSON形式（テキストをcontainerIdで紐付ける形）を直接.excalidraw.mdファイルに書く。複雑な図は公式リモートMCP（mcp.excalidraw.com）でプレビューしchrome拡張でスクショ確認してから座標を調整し、最終版を直接ファイルに書く。「図を作って」「作図して」「excalidrawで」等の依頼で使う。
---

# 講義資料の作図（Excalidraw）

## 基本方針: 直接書きが既定

MCPサーバーやブラウザを介さず、Excalidrawの本来のJSON形式を**直接`.excalidraw.md`ファイルに書く**。
外部サービスへの依存もローカルサーバーの常駐も不要。スキーマの詳細・コピペ用テンプレは
[[excalidraw-format-reference.md]] を参照。

**やってはいけないこと**: コミュニティ製MCPが使う`{"label": {"text": "..."}}`のようなショートハンドを
そのままファイルに書かない。Obsidianのexcalidrawプラグインは本来のスキーマ（テキストを別要素にして
`containerId`で紐付ける）しか読めない（2026-08-06検証済み）。

## 置き場所

`00_運用設計.md` §9.1(c) の全vault共通ルールに従う。

- 図（`.excalidraw.md`）→ 各unit配下の `_excalidraw/`
- 画像（png/jpg/svg等）→ 各unit配下の `_attachments/`
- 共有ライブラリ（`.excalidrawlib`）のみ `_general/99_Excalidrawライブラリ/` に集約。個々の図は
  unitから動かさない

## 複雑な図はブラウザ検証ループを使う

要素数が多い・矢印が交差する等、座標の目算だけでは重なりが読みにくい図は、公式リモートMCP
（`excalidraw-remote-test`、`https://mcp.excalidraw.com`）で見た目を確認してから最終版を書く。

1. 下書きのJSON（elements配列）を作る
2. `mcp__excalidraw-remote-test__export_to_excalidraw` でexcalidraw.comにアップロードし、返ってきた
   URLをclaude-in-chromeで開いてスクリーンショットを撮る
3. 文字被り・矢印の交差・要素の重なりを目視で確認し、座標を調整して2に戻る（納得いくまで繰り返す）
4. 最終版が決まったら、**そのJSONをexcalidraw.com側からダウンロードするのではなく**、同じ要素定義を
   本来のスキーマ（[[excalidraw-format-reference.md]]）に変換してClaudeが直接`.excalidraw.md`に書く

**注意（外部送信）**: `export_to_excalidraw`は図の内容をexcalidraw.com（第三者サービス）に送信する。
講義資料の図は基本問題ないが、PII（研修生の実名等）を含む図では使わない（PIIの置き場は
`00_運用設計.md` §4.1のとおり recruiting-vault/ops-vault 90番台に限定）。

`create_view`（アニメーション付きプレビュー）は必須ではない。座標確認が目的なら`export_to_excalidraw`
+ スクショで十分。

## この正本とデプロイ

- **正本**はこの`SKILL.md`と同じフォルダ（lecturer-vault = gitリポジトリ内、個人GitHub push済み）
- `~/.claude/skills/excalidraw/SKILL.md` はそのデプロイコピー。編集は必ず**正本側**で行い、
  コピーへ同期する（storage-clean → ops-vault/_claude と同じ二層構成）
