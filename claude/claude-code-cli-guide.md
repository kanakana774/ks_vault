# Claude Code CLI 完全使い方ガイド

> 最終更新: 2026-06-03

---

## 目次

1. [インストール・起動](#1-インストール起動)
2. [起動フラグ一覧](#2-起動フラグ一覧)
3. [セッション管理](#3-セッション管理)
4. [スラッシュコマンド](#4-スラッシュコマンド)
5. [キーボードショートカット](#5-キーボードショートカット)
6. [特殊入力モード](#6-特殊入力モード)
7. [CLAUDE.md の設定](#7-claudemd-の設定)
8. [Auto Memory](#8-auto-memory)
9. [Hooks（自動化）](#9-hooks自動化)
10. [MCP サーバー連携](#10-mcp-サーバー連携)
11. [非対話モード（CI/自動化）](#11-非対話モードci自動化)
12. [パーミッションモード](#12-パーミッションモード)
13. [おすすめワークフロー](#13-おすすめワークフロー)
14. [コスト節約のコツ](#14-コスト節約のコツ)

---

## 1. インストール・起動

```bash
# インストール
npm install -g @anthropic-ai/claude-code

# 起動
claude
```

---

## 2. 起動フラグ一覧

### セッション関連

| フラグ | 説明 |
|-------|------|
| `--continue` | 直近セッションをそのまま再開 |
| `--resume` | セッション一覧を表示して選択 |
| `--resume <name>` | 名前付きセッションを直接再開 |
| `--branch <name>` | 現在のセッションから並列セッション（worktree）を作成 |
| `-n <name>` | セッションに名前をつけて開始 |

### モデル・品質

| フラグ | 説明 | 例 |
|-------|------|-----|
| `--model <model-id>` | 使用するモデルを指定 | `--model claude-opus-4-8` |
| `--effort <level>` | 推論の深さを設定 | `--effort high` (low/medium/high/max) |

### 入出力

| フラグ | 説明 |
|-------|------|
| `-p "プロンプト"` | 非対話モードで1回だけ実行 |
| `--output-format <format>` | 出力形式: `default` / `json` / `stream-json` / `text` |
| `--verbose` | 詳細ログを有効化 |

### 権限・セキュリティ

| フラグ | 説明 |
|-------|------|
| `--permission-mode <mode>` | パーミッションモードを指定 |
| `--allowed-tools <tools>` | 使用できるツールをカンマ区切りで限定 |
| `--add-dir <path>` | プロジェクト外のディレクトリへのアクセスを許可 |
| `--sandbox` | サンドボックスモードで起動 |

### システムプロンプト

| フラグ | 説明 |
|-------|------|
| `--append-system-prompt <text>` | システムプロンプトにテキストを追記 |
| `--system-prompt-file <path>` | ファイルからカスタムシステムプロンプトを読み込む |

---

## 3. セッション管理

### 基本操作

```bash
claude                    # 新規セッション
claude --continue         # 直近セッションを再開
claude --resume           # セッション一覧から選択（↑↓で移動、Enterで選択）
claude -n auth-refactor   # 名前付きセッション
```

### セッション内での管理

| コマンド | 説明 |
|---------|------|
| `/rename <名前>` | 現在のセッションに名前をつける |
| `/branch <名前>` | 現在セッションをコピーして分岐 |
| `/resume` | 別のセッションに切り替え |
| `/export` | 会話をファイル or クリップボードに保存 |

### 保存場所

- 会話履歴: `~/.claude/projects/<project-path>/` 以下に JSONL ファイルで保存
- デフォルト保持期間: 30日（`cleanupPeriodDays` で変更可）

---

## 4. スラッシュコマンド

### コンテキスト管理（最重要）

| コマンド | 説明 |
|---------|------|
| `/clear` | コンテキストをリセットして新鮮な状態に |
| `/compact [指示]` | 会話を要約してコンテキストを節約 |
| `/context` | コンテキスト使用量の内訳を確認 |
| `/rewind` | 過去のチェックポイントに巻き戻す（ファイル変更も元に戻る） |
| `/recap` | セッションの進捗サマリーを生成 |

**使い分けの目安:**
- 無関係なタスクに移るとき → `/clear`
- コンテキストが溜まってきたが続けたいとき → `/compact`
- 実装が間違った方向に進んだとき → `/rewind`

### 設定・初期化

| コマンド | 説明 |
|---------|------|
| `/init` | プロジェクト分析してCLAUDE.mdを自動生成 |
| `/config` | テーマ・エディタ・レンダリング等のUI設定 |
| `/model` | セッション中にモデルを変更 |
| `/effort` | 推論の深さをその場で変更 |
| `/memory` | CLAUDE.mdやauto memoryの閲覧・編集 |
| `/permissions` | ツール実行の事前許可設定 |
| `/settings` | 設定の解決済み値を確認 |
| `/hooks` | 設定済みhooksを確認 |
| `/mcp` | MCPサーバーの接続状況・認証 |

### 開発フロー

| コマンド | 説明 |
|---------|------|
| `/plan` | プランモード（読み取り専用）に入る |
| `/review` | 現在のdiffをコードレビュー |
| `/code-review` | バグ・改善点のコードレビュー（`--comment`でPRコメント投稿） |
| `/simplify` | リファクタリング・簡略化の提案と適用 |
| `/verify` | アプリを実際に動かして変更を確認 |

### ユーティリティ

| コマンド | 説明 |
|---------|------|
| `/doctor` | 診断チェックを実行 |
| `/status` | セッションステータスを表示 |
| `/theme` | カラーテーマを変更 |
| `/terminal-setup` | Shift+Enter設定などのターミナル設定 |
| `/keybindings` | キーボードショートカットを編集 |
| `/help` | ヘルプを表示 |
| `/feedback` | バグ報告・フィードバック送信 |

---

## 5. キーボードショートカット

### 基本操作

| ショートカット | 動作 |
|------------|------|
| `Ctrl+C` | 実行中断 / 入力クリア |
| `Ctrl+D` | Claude Code を終了 |
| `Esc` | レスポンスを途中で止める（コンテキストは保持） |
| `Esc + Esc` | 入力ドラフトをクリア / rewindメニューを開く |
| `Ctrl+L` | ターミナル表示を再描画（表示崩れ修正） |

### 入力・履歴

| ショートカット | 動作 |
|------------|------|
| `Ctrl+R` | コマンド履歴を逆検索 |
| `Ctrl+G` / `Ctrl+X Ctrl+E` | プロンプトを外部エディタで開く |
| `↑` / `↓` | 履歴をナビゲート |
| `Ctrl+A` | 行頭に移動 |
| `Ctrl+E` | 行末に移動 |
| `Ctrl+K` | カーソルから行末まで削除 |
| `Ctrl+U` | カーソルから行頭まで削除 |
| `Ctrl+W` | 前の単語を削除 |
| `Ctrl+Y` | 削除したテキストを貼り付け |
| `Alt+B` / `Alt+F` | 単語単位で移動 |

### モード・設定切り替え

| ショートカット | 動作 |
|------------|------|
| `Shift+Tab` | パーミッションモードを循環 |
| `Alt+P` (Win/Linux) / `Option+P` (Mac) | モデルを切り替え |
| `Alt+T` / `Option+T` | 拡張思考（Extended Thinking）のON/OFF |
| `Alt+O` / `Option+O` | Fast モードのON/OFF |
| `Ctrl+O` | 詳細トランスクリプトビューの切り替え |
| `Ctrl+T` | タスクリストの表示切り替え |
| `Ctrl+B` | 実行中のタスクをバックグラウンドへ |

### 複数行入力

| 方法 | ショートカット |
|------|------------|
| バックスラッシュ | `\ + Enter` （全ターミナル対応） |
| Shift+Enter | iTerm2, WezTerm, Ghostty, Windows Terminal 等 |
| Ctrl+J | `Ctrl+J` （全ターミナル対応） |

### カスタムキーバインド

`~/.claude/keybindings.json` で変更可能:

```json
{
  "bindings": [
    {
      "context": "Chat",
      "bindings": {
        "ctrl+shift+c": "chat:submit",
        "ctrl+f": "chat:modelPicker"
      }
    }
  ]
}
```

---

## 6. 特殊入力モード

### `!` シェルモード

プロンプトを `!` で始めるとシェルコマンドを直接実行し、結果をコンテキストに追加:

```bash
! npm test
! git status
! ls -la src/
! cat package.json
```

- コマンドと出力が両方コンテキストに入る
- `Ctrl+B` でバックグラウンド実行可能
- Tabキーで以前の `!` コマンドを補完

### `#` メモモード

プロンプトを `#` で始めると auto memory にメモを追記:

```
# このプロジェクトはpnpmを使う
# テストはvitest（jestではない）
# 本番DBはPostgreSQL 15
```

---

## 7. CLAUDE.md の設定

Claudeに永続的なコンテキストを与えるファイル。セッション開始時に自動読み込み。

### 保存場所と優先度

| ファイルパス | 対象 | 用途 |
|------------|------|------|
| `~/.claude/CLAUDE.md` | 全プロジェクト（自分のみ） | 個人のコーディング設定 |
| `./CLAUDE.md` | 現在のプロジェクト（チーム共有） | プロジェクト標準 |
| `./.claude/CLAUDE.md` | 現在のプロジェクト（チーム共有） | 上記の別パス |
| `./CLAUDE.local.md` | 現在のプロジェクト（自分のみ） | 個人メモ（.gitignoreに追加） |

### 書き方の例

```markdown
# ビルド・テストコマンド
- テスト実行: `pnpm test`
- ビルド: `pnpm build`
- 開発サーバー: `pnpm dev`
- Lint: `pnpm lint`

# コーディングルール
- インデント: 2スペース
- 言語: TypeScript必須（JSファイル新規作成禁止）
- モジュール: ES modules（import/export）、CommonJS禁止
- 型: `any` 使用禁止

# ディレクトリ構成
- API層: `src/api/`
- UIコンポーネント: `src/components/`
- ユーティリティ: `src/utils/`
- ビジネスロジックは `utils/` に置く（コンポーネントに書かない）

# Git ルール
- コミット前に必ずテスト実行
- コミットメッセージ: `feat:`, `fix:`, `refactor:` プレフィックス必須
- PR説明には変更理由を記載

# 注意事項
- `migrations/` ディレクトリは直接編集しない
- 環境変数は `.env.example` に必ず追記する
```

### 自動生成

```
/init
```

プロジェクトを解析して雛形を自動生成。その後手動で調整する。

### ベストプラクティス

- **200行以内に収める**（超えるとコンテキストを圧迫）
- コードから自明なことは書かない（ファイル構成など）
- 同じ間違いを2度されたルールを追記していく
- 定期的に古いルールを削除する

---

## 8. Auto Memory

Claudeが会話から自動的に学習し、次回セッションに引き継ぐ仕組み。

### 保存場所

```
~/.claude/projects/<プロジェクトパス>/memory/
```

### 学習のさせ方

訂正するだけで自動的に記憶される:

```
ユーザー: "うちはnpmじゃなくてpnpmを使ってるよ"
→ 次回から自動的にpnpmを使う

ユーザー: "テストはvitestだよ、jestじゃない"
→ 以降vitestを使う

ユーザー: "このプロジェクトのAPI設計はRESTじゃなくてtRPCを使ってる"
→ tRPCパターンで提案するようになる
```

### 管理

```
/memory         # 全メモリファイルを閲覧・編集
```

---

## 9. Hooks（自動化）

特定のイベントに合わせてシェルコマンドを自動実行。**Claudeの判断に依存せず確実に実行**したい処理に使う。

### 設定ファイル

`.claude/settings.json`:

```json
{
  "hooks": [
    {
      "event": "PostToolUse",
      "if": "tool.name == 'Edit' && tool.args.path.endsWith('.ts')",
      "command": "npx prettier --write {tool.args.path}"
    },
    {
      "event": "PostToolUse",
      "if": "tool.name == 'Edit' && tool.args.path.endsWith('.py')",
      "command": "black {tool.args.path}"
    },
    {
      "event": "PreToolUse",
      "if": "tool.name == 'Bash' && tool.args.command.includes('DROP TABLE')",
      "command": "echo 'BLOCKED: DROP TABLE is not allowed' && exit 1"
    }
  ]
}
```

### イベント一覧

| イベント | タイミング | 主な用途 |
|---------|----------|---------|
| `SessionStart` | セッション開始時 | 環境変数ロード、前提チェック |
| `PreToolUse` | ツール実行前 | 入力バリデーション、危険操作のブロック |
| `PostToolUse` | ツール実行後 | auto-format、追加チェック、ログ |
| `UserPromptSubmit` | プロンプト送信後 | コンテキスト注入、入力変換 |
| `PermissionRequest` | 権限ダイアログ前 | 信頼済みパターンの自動許可 |
| `PostCompact` | コンテキスト圧縮後 | 重要な指示を再注入 |
| `Stop` | セッション終了前 | 最終バリデーション、クリーンアップ |

### 使い分け

| 処理の性質 | 使うべき仕組み |
|----------|-------------|
| 毎回確実に実行（auto-format等） | Hooks |
| 状況によって判断が必要 | Claudeに任せる（スキル等） |
| CI/CDで自動化 | 非対話モード + Hooks |

---

## 10. MCP サーバー連携

外部ツール（DB、GitHub、Figma、ブラウザ等）をClaude Codeに接続するプロトコル。

### サーバー追加

```bash
# HTTP サーバー
claude mcp add --transport http github https://mcp-github.example.com/mcp

# stdio サーバー（ローカル）
claude mcp add playwright -- npx -y @playwright/mcp@latest

# チーム共有（プロジェクトスコープ）
claude mcp add --scope project --transport http sentry https://mcp.sentry.dev/mcp

# 全プロジェクト共有（ユーザースコープ）
claude mcp add --scope user --transport http github https://mcp.example.com/mcp
```

### 管理コマンド

```bash
claude mcp list              # 接続中サーバー一覧
claude mcp get <name>        # サーバー詳細
claude mcp remove <name>     # サーバー削除
```

### `.mcp.json`（チーム共有設定）

```json
{
  "mcpServers": {
    "playwright": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@playwright/mcp@latest"]
    },
    "github": {
      "type": "http",
      "url": "https://mcp-github.example.com/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

---

## 11. 非対話モード（CI/自動化）

### 基本

```bash
# 単発クエリ
claude -p "package.jsonの依存関係を確認して古いものを報告して"

# 標準入力から
cat error.log | claude -p "このエラーをデバッグして"

# JSONで出力（パース用）
claude -p "全Pythonファイルを列挙して" --output-format json

# ストリーミングJSON（リアルタイム処理）
claude -p "テスト実行して結果を表示" --output-format stream-json --verbose
```

### CI/CDパイプライン

```bash
claude -p "Lintエラーを全部修正して" \
  --allowed-tools "Bash(npm run lint),Edit,Read" \
  --permission-mode auto \
  --output-format stream-json
```

### ファイルを一括処理

```bash
# Bash
for file in $(find src -name "*.ts"); do
  claude -p "このファイルにTypeScript型を追加: $file" \
    --allowed-tools "Edit,Read,Bash"
done
```

### よく使う `--allowed-tools` パターン

```bash
# 読み取り専用
--allowed-tools "Read,Glob,Grep"

# ファイル編集あり
--allowed-tools "Read,Edit,Write,Glob,Grep"

# テスト実行あり
--allowed-tools "Read,Edit,Bash(npm test),Bash(npm run lint)"

# フルアクセス（信頼済み環境のみ）
--permission-mode bypassPermissions
```

---

## 12. パーミッションモード

| モード | 動作 | 使うタイミング |
|-------|------|-------------|
| `default` | 全操作を確認 | 監視しながら作業するとき |
| `acceptEdits` | ファイル編集は自動許可、Bashは確認 | 通常の開発作業 |
| `plan` | 読み取り専用（変更不可） | コードベースの調査・計画 |
| `auto` | AIが判断して許可/確認 | 日常的なルーチン作業 |
| `dontAsk` | 事前許可済みツールのみ実行 | 限定的な自動化 |
| `bypassPermissions` | 全操作を無確認で実行 | 信頼済み自動化環境のみ |

セッション中の切り替え: `Shift+Tab`

---

## 13. おすすめワークフロー

### Explore → Plan → Code（大規模タスク向け）

```
1. /plan でプランモードに入る（読み取り専用）
2. コードベースを調査し、Claude に質問する
3. 実装計画を詳細に立てる
4. /plan を終了して通常モードへ
5. 計画に沿って実装 → テスト実行 → Lint
```

**なぜ有効か:** 間違った実装を進めるコストを防ぎ、方向性を確認してから動ける。

---

### TDD ワークフロー

```
"ログイン処理を実装して。
まず失敗するテストを書いて、次に通るように実装して。
最後に npm test を実行して全テストがパスすることを確認して。"
```

**ポイント:** 検証手段を与えることでClaude自身が修正できる。

---

### 大規模リファクタリング

```bash
# 1. 現状を把握
claude --resume または claude -n refactor-auth

# 2. プランモードで計画
/plan

# 3. 段階的に実装（コンテキストが溜まったら）
/compact "認証モジュールのリファクタリング計画は保持して"

# 4. テストで検証
! npm test
```

---

### デバッグセッション

```bash
# エラーログをコンテキストに入れる
! cat error.log

# または
cat error.log | claude -p "このエラーの根本原因を特定して修正案を示して"
```

---

### コードレビュー

```bash
# セッション内
/review          # 現在のdiffをレビュー
/code-review     # バグ・改善点を詳細レビュー

# PRのレビュー（コメント自動投稿）
/code-review --comment

# 非対話でレビュー
claude -p "この変更のセキュリティ上の問題を指摘して" \
  --allowed-tools "Read,Glob,Grep" \
  --output-format text
```

---

### 繰り返し作業の自動化

カスタムスキルを作成して `/fix-issue 1234` のように呼べる:

```markdown
<!-- .claude/skills/fix-issue/SKILL.md -->
---
name: fix-issue
description: GitHub issueを修正してPRを作る
---

GitHub issue #$ARGUMENTS を修正する:

1. `gh issue view $ARGUMENTS` で内容を確認
2. 関連コードを読む
3. 失敗するテストを書く
4. 修正を実装する
5. `npm test` で全テストがパスすることを確認
6. PRを作成する
```

---

## 14. コスト節約のコツ

### コンテキストを節約する

```
- 無関係なタスク間は /clear を使う
- コンテキストが溜まったら /compact を使う
- 調査作業はサブエージェントに任せる（メインコンテキストを守る）
- /btw で脇道の質問をする（履歴に残らない）
- /context でどこがコンテキストを消費しているか確認する
```

### CLAUDE.md を短くする

```
- 200行以内を目標にする
- コードから自明なことは書かない
- 古くなったルールは削除する
```

### モデルを使い分ける

```
# 簡単な作業は軽量モデル
claude --model claude-haiku-4-5-20251001 -p "変数名をリネームして"

# 複雑な設計はOpus
claude --model claude-opus-4-8 -p "このシステムのアーキテクチャを設計して"

# Fast モード（Alt+O）でシンプルなタスクを高速化
```

### 使用量の確認

```
/context        # 現在のセッションのコンテキスト内訳
```

---

## クイックリファレンス

```bash
# 起動
claude                          # 新規
claude --continue               # 直近再開
claude --resume                 # 選んで再開
claude -n <name>                # 名前付き

# セッション内（頻出）
/clear                          # コンテキストリセット
/compact                        # 要約して節約
/context                        # 使用量確認
/rewind                         # 巻き戻し
/plan                           # 計画モード
/memory                         # メモリ管理
/model                          # モデル切り替え

# キーボード（最重要）
Ctrl+C                          # 中断
Esc                             # レスポンスを止める
Esc + Esc                       # 巻き戻しメニュー
Ctrl+R                          # 履歴検索
Shift+Tab                       # パーミッションモード切替
Alt+P                           # モデル切替

# 非対話
claude -p "..."                             # 単発
claude -p "..." --output-format json        # JSON出力
claude -p "..." --permission-mode auto      # 自動許可
```

---

*公式ドキュメント: https://code.claude.com/docs*
