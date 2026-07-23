### VS CodeでJDK 17を使用するための設定方法

VS CodeのJava拡張機能はJava 21以降を要求しますが、`java.configuration.runtimes`設定を使うことで、プロジェクトのコンパイルにはJDK 17を使用することができます。

#### ステップ1: JDK 21 (またはそれ以降) のインストール

まず、Java拡張機能自体を動作させるために、**JDK 21以降の新しいJDKをインストールする必要があります**。これは必須です。

1.  **新しいJDKのダウンロード**:
    *   Oracle JDK (商用利用の場合はライセンスに注意): [https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)
    *   OpenJDKディストリビューション (Adoptium Temurin, Azul Zuluなど):
        *   Adoptium Temurin: [https://adoptium.net/](https://adoptium.net/) (LTS版の21を選ぶのが良いでしょう)
        *   Azul Zulu: [https://www.azul.com/downloads/?package=jdk](https://www.azul.com/downloads/?package=jdk)
    お使いのOSに合ったものをダウンロードし、インストールしてください。

2.  **環境変数の設定 (推奨)**:
    インストールした新しいJDKのパスをシステム環境変数`JAVA_HOME`に設定することをお勧めします。これにより、VS Codeが自動的に新しいJDKを検出する可能性が高まります。
    *   **Windows**:
        1.  「環境変数を編集」を検索して開きます。
        2.  「システム環境変数」セクションで「新規」をクリックし、変数名`JAVA_HOME`、変数値にJDK 21のインストールパス（例: `C:\Program Files\Java\jdk-21`）を入力します。
        3.  `Path`変数を選択して「編集」をクリックし、`%JAVA_HOME%\bin`を追加します。
    *   **macOS/Linux**:
        ターミナルを開き、`.bash_profile`、`.zshrc`、または`.profile`などのシェル設定ファイルに以下の行を追加します。
        ```bash
        export JAVA_HOME="/path/to/your/jdk-21"
        export PATH="$JAVA_HOME/bin:$PATH"
        ```
        変更を適用するために、`source ~/.zshrc` (使用しているシェルに合わせて) を実行するか、ターミナルを再起動します。

#### ステップ2: VS Codeの設定を構成する

VS Codeで`java.configuration.runtimes`を設定し、JDK 17をコンパイル用に使用できるようにします。

1.  **VS Codeを開きます。**
2.  **設定を開く**:
    *   Windows/Linux: `Ctrl + ,`
    *   macOS: `Cmd + ,`
    または、`ファイル` > `ユーザー設定` > `設定` を選択します。
3.  **設定の検索**:
    検索バーに `java.configuration.runtimes` と入力します。
4.  **設定を編集**:
    「Edit in settings.json」リンクをクリックして、`settings.json`ファイルを直接編集します。
5.  **`java.configuration.runtimes` の追加または編集**:
    `settings.json`ファイルに、以下のような設定を追加または更新します。

    ```json
    {
        // 他の設定...

        "java.configuration.runtimes": [
            {
                "name": "JavaSE-17",
                "path": "/path/to/your/jdk-17", // ここをJDK 17の実際のパスに置き換えてください
                "default": true // これをデフォルトのコンパイル環境として設定します
            },
            {
                "name": "JavaSE-21",
                "path": "/path/to/your/jdk-21" // ここをJDK 21の実際のパスに置き換えてください
                // "default": false は不要です、上でJDK 17をデフォルトにしています
            }
            // 必要に応じて他のJDKバージョンも追加できます
        ]
    }
    ```

    **重要**:
    *   `/path/to/your/jdk-17` と `/path/to/your/jdk-21` は、ご自身のシステムにおける実際のJDK 17とJDK 21のインストールパスに置き換えてください。
        *   **Windowsの例**: `C:\\Program Files\\Java\\jdk-17` (バックスラッシュはエスケープが必要です)
        *   **macOS/Linuxの例**: `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` または `/usr/lib/jvm/java-17-openjdk` など
    *   `"default": true` を設定することで、VS CodeはJDK 17をプロジェクトのデフォルトのコンパイル・実行環境として使用します。
    *   `"name"` の値（例: "JavaSE-17"）は、プロジェクトの`.classpath`ファイルやMaven/Gradleの設定で参照される可能性があります。

#### ステップ3: VS Codeの再起動

設定変更を有効にするために、VS Codeを一度完全に閉じてから再度開いてください。

---

**確認:**

*   VS Codeを再起動した後、新しいJDK 21がVS CodeのJava拡張機能によって検出されているか確認します。
*   JDK 17を使用するプロジェクトを開き、問題なくビルドおよび実行できるか確認します。
*   （オプション）コマンドパレット（`Ctrl+Shift+P`または`Cmd+Shift+P`）を開き、「Java: Configure Java Runtime」と入力して、設定が正しく適用されているか確認することもできます。

---


結論から申し上げますと、**「最新版の拡張機能を使いたい場合は、JDK 21 のインストールが必須」**です。

少しややこしいのですが、ここで重要なのは**「2つの異なる用途」**があるという点です。

1.  **VS Code（拡張機能）自体を動かすための Java**: これに **JDK 21** が必要。
2.  **あなたのプログラムを動かすための Java**: これは **JDK 17** のままでOK。

つまり、拡張機能という「道具」を動かすための電池として JDK 21 が必要ですが、その道具を使って作る「作品」は JDK 17 のままで大丈夫、という意味です。

---

### 「どうしても JDK 21 を入れたくない」場合の裏技

もし、会社のPCで勝手にインストールできない、あるいはどうしても手間を省きたいという場合は、**「拡張機能のバージョンを下げる（ダウングレードする）」**ことで、JDK 17 のまま使い続けることが可能です。

拡張機能を「JDK 17 で動いていた頃のバージョン」に戻す方法です。

#### ダウングレードの手順

1.  VS Code 左側の**拡張機能アイコン**（テトリスのブロックのようなマーク）をクリックします。
2.  インストール済みの **"Extension Pack for Java"** (または含まれている **"Language Support for Java(TM) by Red Hat"**) を探します。
3.  **"Language Support for Java(TM) by Red Hat"** の右下にある**歯車マーク（管理）**をクリックします。
4.  メニューから **「特定のバージョンをインストール... (Install Specific Version...)」** を選びます。
5.  リストから **`1.34.0`** 以前のバージョン（例えば `1.34.x` や `1.33.x`）を選んでインストールします。
    *   *※バージョン 1.35 から JDK 21 が必須になりました。*
6.  VS Code の再起動を求められたら再起動します。

**注意点:**
この方法はあくまで一時しのぎです。古いバージョンを使い続けると、新しい機能が使えなかったり、バグが直らなかったり、セキュリティ上の問題が残ったりする可能性があります。

基本的には、**「JDK 21 をインストールして設定する（前回の回答の方法）」**のが、長期的にはトラブルが少なくおすすめです。