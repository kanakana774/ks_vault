```log
PS C:\Users\kanamaru\Desktop\採用業務\会社説明会\研修説明資料\紹介用\yafh-app-api> mvn spring-boot:run  
[INFO] Scanning for projects...  
[WARNING]  
[WARNING] Some problems were encountered while building the effective model for jp.co.aevic:training:war:0.0.1-SNAPSHOT  
[WARNING] 'dependencies.dependency.systemPath' for com.dbunitng:dbunitng:jar should not point at files within the project directory, ${basedir}/lib/dbunitng-0.6.jar will be unresolvable by dependent projects @ line 80, column 25  
[WARNING]  
[WARNING] It is highly recommended to fix these problems because they threaten the stability of your build.  
[WARNING]  
[WARNING] For this reason, future Maven versions might no longer support building such malformed projects.  
[WARNING]  
[INFO]  
[INFO] ------------------------< jp.co.aevic:training >------------------------  
[INFO] Building training 0.0.1-SNAPSHOT  
[INFO] from pom.xml  
[INFO] --------------------------------[ war ]---------------------------------  
[INFO]  
[INFO] >>> spring-boot:3.3.2:run (default-cli) > test-compile @ training >>>  
[INFO]  
[INFO] --- jacoco:0.8.7:prepare-agent (prepare-agent) @ training ---  
[INFO] argLine set to -javaagent:C:\Users\kanamaru\.m2\repository\org\jacoco\org.jacoco.agent\0.8.7\org.jacoco.agent-0.8.7-runtime.jar=destfile=C:\Users\kanamaru\Desktop\採用業務\会社説明会\研修説明資料\紹介用\yafh-app-api\target\jacoco.exec  
[INFO]  
[INFO] --- resources:3.3.1:resources (default-resources) @ training ---  
[INFO] Copying 1 resource from src\main\resources to target\classes  
[INFO] Copying 42 resources from src\main\resources to target\classes  
[INFO]  
[INFO] --- compiler:3.13.0:compile (default-compile) @ training ---  
[INFO] Nothing to compile - all classes are up to date.  
[INFO]  
[INFO] --- resources:3.3.1:testResources (default-testResources) @ training ---  
[INFO] Copying 134 resources from src\test\resources to target\test-classes  
[INFO]  
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ training ---  
[INFO] Nothing to compile - all classes are up to date.  
[INFO]  
[INFO] <<< spring-boot:3.3.2:run (default-cli) < test-compile @ training <<<  
[INFO]  
[INFO]  
[INFO] --- spring-boot:3.3.2:run (default-cli) @ training ---  
[INFO] Attaching agents: []  
エラー: メイン・クラスjp.co.aevic.training.TrainingApplicationを検出およびロードできませんでした  
原因: java.lang.ClassNotFoundException: jp.co.aevic.training.TrainingApplication  
[INFO] ------------------------------------------------------------------------  
[INFO] BUILD FAILURE  
[INFO] ------------------------------------------------------------------------  
[INFO] Total time: 3.795 s  
[INFO] Finished at: 2025-12-03T11:30:16+09:00  
[INFO] ------------------------------------------------------------------------  
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.3.2:run (default-cli) on project training: Process terminated with exit code: 1 -> [Help 1]  
[ERROR]  
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.  
[ERROR] Re-run Maven using the -X switch to enable full debug logging.  
[ERROR]  
[ERROR] For more information about the errors and possible solutions, please read the following articles:  
[ERROR] [Help 1] [http://cwiki.apache.org/confluence/display/MAVEN/MojoExecutionException](https://www.google.com/url?sa=E&q=http%3A%2F%2Fcwiki.apache.org%2Fconfluence%2Fdisplay%2FMAVEN%2FMojoExecutionException)
```


**「コンパイル済みのクラスファイル（targetフォルダ内）が破損しているか、古い状態のまま整合性が取れなくなっている」** 可能性が最も高い。

ログに `[INFO] Nothing to compile - all classes are up to date.` （コンパイルするものはありません）と出ているにも関わらず、その後の実行で `ClassNotFoundException` （クラスが見つからない）が発生しているのがその証拠です。

以下の手順を上から順に試してください。

### 解決策1：クリーンビルドを実行する（最優先）

Mavenが「コンパイル不要」と誤認している状態をリセットするために、一度ビルド成果物を削除して再実行します。

PowerShellで以下のコマンドを実行してください。

```powershell
mvn clean spring-boot:run
```

**解説:**
`clean` を付けることで `target` フォルダが削除され、強制的に全ファイルの再コンパイルが行われます。これで解決するケースがほとんどです。

---

### 解決策2：Javaのバージョン整合性を確認する

「VS CodeがJava 21へのアップグレードを提案している」件ですが、**Spring Boot 3.3.x は Java 17以上が必須** です。

以下の3箇所でバージョンが食い違っていると、クラスファイルが正しく生成されない、または読み込めない場合があります。

1.  **pom.xml の設定**:
    ```xml
    <properties>
        <java.version>17</java.version> <!-- ここが17か21か確認 -->
    </properties>
    ```
2.  **実行環境 (PowerShell) の Java**:
    ```powershell
    java -version
    ```
    ※ここで表示されるバージョンが、pom.xmlのバージョン（またはそれ以上）である必要があります。
3.  **JAVA_HOME 環境変数**:
    環境変数が古いJava（Java 8や11など）を指していないか確認してください。

もし `pom.xml` が 21 で、手元の Java が 17 の場合はエラーになります。逆に `pom.xml` が 17 であれば、Java 17 でも 21 でも動きます。

---

### 解決策3：パッケージ宣言の確認

念のための確認ですが、メインクラスのコード `TrainingApplication.java` の1行目のパッケージ宣言が、エラーメッセージと完全に一致しているか確認してください。

*   **エラー内容**: `jp.co.aevic.training.TrainingApplication`
*   **ソースコード**:
    ```java
    package jp.co.aevic.training; // これが間違っていないか

    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;

    @SpringBootApplication
    public class TrainingApplication {
        // ...
    }
    ```
フォルダ構成が `src/main/java/jp/co/aevic/training/TrainingApplication.java` になっていることも確認してください。

---

### 補足：日本語パスについて

現在のプロジェクトパスに日本語が含まれています。
`C:\Users\kanamaru\Desktop\採用業務\会社説明会\...`

JavaやMavenは、時折**日本語（マルチバイト文字）が含まれるパスで正常に動作しない**ことや、文字化けしてクラスパスを見失うことがあります。

もし「解決策1」で直らない場合は、プロジェクトフォルダ（`yafh-app-api`）を `C:\work\yafh-app-api` のような**日本語を含まない浅い階層**に移動して実行してみてください。

### まとめ

まずは **`mvn clean spring-boot:run`** を試す。
それでも直らない場合、Javaバージョンの不一致か、日本語パスの問題である可能性が高い。