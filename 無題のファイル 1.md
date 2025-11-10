## Spring BootとMyBatisを使ったWeb API開発 説明資料

この資料では、Spring BootとMyBatisを使用してWeb APIを開発する際に、それぞれのフレームワークがどのような役割を果たし、開発をどのように効率化してくれるのかを解説します。

### 1. Spring Bootとは？

Spring Bootは、Spring Frameworkをより簡単かつ迅速に利用できるようにするためのフレームワークです。WebアプリケーションやAPI開発に必要な様々な設定や機能を、面倒な手間なく利用できるようにしてくれます。

#### 1.1. 何をしてくれるのか？

一言で言うと「**Webアプリケーションの面倒な設定や準備を肩代わりしてくれるもの**」です。

*   **組み込みWebサーバー:**
    *   従来はTomcatなどのWebサーバーを別途用意し、そこにアプリケーションを配置（デプロイ）する必要がありました。
    *   Spring BootはTomcatなどを内蔵しているため、特別な準備なしにアプリケーションを単独で実行できます。
*   **設定の自動化 (Auto-Configuration):**
    *   データベース接続やWeb MVCの設定など、一般的なアプリケーションで必要となる大量の設定を、クラスパスに含まれるライブラリを検知して自動的に設定してくれます。
    *   これにより、開発者は煩雑なXML設定ファイルから解放されます。
*   **依存関係管理の簡素化:**
    *   「Starter」と呼ばれる依存関係のセットが用意されており、例えば `spring-boot-starter-web` を追加するだけで、Web API開発に必要なライブラリ群が一括で導入されます。ライブラリ間のバージョンの互換性を気にする必要もありません。

#### 1.2. 使わない場合との比較 (具体例)

簡単な「Hello, World!」を返すAPIを例に見てみましょう。

##### 👎 Spring Bootを使わない場合 (従来のServlet)

Webサーバー(Tomcatなど)の準備や、`web.xml`という設定ファイルにどのURLでどのプログラムを動かすかを定義する必要があり、多くの定型的なコードと設定が求められます。

**`web.xml` (設定ファイル)**
```xml
<servlet>
    <servlet-name>HelloWorldServlet</servlet-name>
    <servlet-class>com.example.HelloWorldServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>HelloWorldServlet</servlet-name>
    <url-pattern>/hello</url-pattern>
</servlet-mapping>
```

**`HelloWorldServlet.java` (Javaコード)**
```java
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloWorldServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write("Hello, World!");
    }
}
```

##### 👍 Spring Bootを使う場合

設定ファイルはほぼ不要で、アノテーション（`@RestController`や`@GetMapping`など）を付けたシンプルなJavaクラスを作成するだけでAPIが完成します。

**`HelloController.java`**
```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}
```
`main`メソッドのあるクラスを実行するだけで、内蔵サーバーが起動し、すぐにAPIが利用可能になります。

---

### 2. MyBatisとは？

MyBatisは、データベース操作（SQLの実行）を簡単に行うためのフレームワークで、ORマッパー（O/R Mapper）の一種です。SQLとJavaのコードを分離し、定型的なJDBCの処理を隠蔽してくれます。

#### 2.1. 何をしてくれるのか？

一言で言うと「**データベースとJavaプログラムの間の面倒なデータ変換や接続処理を肩代わりしてくれるもの**」です。

*   **SQLの分離:**
    *   Javaのコード内にSQL文を直接書き込むのではなく、XMLファイルにSQLを記述できます。これにより、SQLの管理や修正が容易になります。
*   **定型処理の自動化:**
    *   データベースへの接続、`PreparedStatement`の作成、`ResultSet`からのデータ取得、リソース（接続など）の解放といった、JDBCにおける定型的なコードをフレームワークが自動で行ってくれます。
*   **自動マッピング:**
    *   SQLの実行結果を、Javaのオブジェクト（DTOなど）に自動的に詰めてくれます。カラム名とプロパティ名を一致させるだけで、面倒な詰め替え作業が不要になります。

#### 2.2. 使わない場合との比較 (具体例)

`users`テーブルからIDを指定してユーザー情報を取得する処理を例に見てみましょう。

##### 👎 MyBatisを使わない場合 (素のJDBC)

データベースへの接続から切断まで、全て手動で記述する必要があります。特に、リソースの解放処理 (`finally`ブロック) は記述が漏れると重大な問題につながる可能性があります。

**`UserDao.java` (Javaコード)**
```java
public User findById(int id) {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    User user = null;

    try {
        // 1. データベース接続
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");

        // 2. SQL準備
        String sql = "SELECT id, name, email FROM users WHERE id = ?";
        pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);

        // 3. SQL実行
        rs = pstmt.executeQuery();

        // 4. 結果をオブジェクトに詰める
        if (rs.next()) {
            user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
        }
    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        // 5. リソース解放 (非常に重要！)
        if (rs != null) try { rs.close(); } catch (SQLException e) {}
        if (pstmt != null) try { pstmt.close(); } catch (SQLException e) {}
        if (conn != null) try { conn.close(); } catch (SQLException e) {}
    }
    return user;
}
```

##### 👍 MyBatisを使う場合

開発者が書くのは、簡単なインターフェースとSQLを記述したXMLだけです。上記の定型処理は全てMyBatisが内部で行います。

**`UserMapper.java` (インターフェース)**
```java
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findById(@Param("id") int id);
}
```

**`UserMapper.xml` (SQLを記述するXML)**
```xml
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.UserMapper">
    <select id="findById" resultType="com.example.User">
        SELECT
            id,
            name,
            email
        FROM
            users
        WHERE
            id = #{id}
    </select>
</mapper>
```
`UserMapper`インターフェースの`findById`メソッドを呼び出すだけで、MyBatisがXMLに定義されたSQLを実行し、結果を`User`オブジェクトにマッピングして返してくれます。

---

### 3. 全体の仕組みとプログラムの位置関係

Spring BootとMyBatisを組み合わせたWeb APIは、一般的に以下のような流れで処理が行われます。開発者が主に実装するのは、青色で示された部分です。

```mermaid
graph TD
    subgraph クライアント
        A[ブラウザ/モバイルアプリ]
    end

    subgraph "Spring Bootアプリケーション (フレームワークが提供)"
        B[組み込みTomcat] --> C{DispatcherServlet};
    end

    subgraph "開発者が実装するプログラム"
        style C fill:#fff,stroke:#333,stroke-width:2px
        C -- URLに応じて振り分け --> D[Controllerクラス<br>@RestController];
        D -- ビジネスロジックを依頼 --> E[Serviceクラス<br>@Service];
        E -- DB操作を依頼 --> F[Mapperインターフェース<br>@Mapper];
    end

    subgraph "MyBatis (フレームワークが提供)"
        F -- 連携 --> G[MyBatisエンジン];
        G -- SQLを実行 --> H[Mapper XML];
    end
    
    subgraph データベース
        H -- JDBC --> I[(データベース)];
    end

    A -- HTTPリクエスト --> B;
    I -- 結果 --> H;
    H -- 結果をオブジェクトにマッピング --> G;
    G -- オブジェクトを返す --> F;
    F -- 結果を返す --> E;
    E -- 結果を返す --> D;
    D -- 結果をJSON等に変換して返す --> C;
    C -- HTTPレスポンス --> A;

    style D fill:#cde4ff
    style E fill:#cde4ff
    style F fill:#cde4ff
```

**処理の流れ:**

1.  **リクエスト:** クライアント（ブラウザなど）からHTTPリクエストが送られます。
2.  **受付 (Spring Boot):** Spring Bootに内蔵されたWebサーバー(Tomcat)がリクエストを受け付け、中心的な役割を担う`DispatcherServlet`に渡します。
3.  **Controller (自作):** `DispatcherServlet`はURLに対応する`Controller`クラスのメソッドを呼び出します。ここでリクエストのパラメータを受け取ります。
4.  **Service (自作):** `Controller`は、具体的なビジネスロジック（業務処理）を`Service`クラスに依頼します。
5.  **Mapper (自作インターフェース + MyBatis):** `Service`は、データベース操作が必要な場合、MyBatisによって実装が自動生成された`Mapper`インターフェースのメソッドを呼び出します。
6.  **SQL実行 (MyBatis):** MyBatisは、呼び出されたメソッドに対応するXML内のSQLを実行し、データベースから結果を取得します。
7.  **結果返却:** 取得した結果をJavaオブジェクトにマッピングし、`Service`→`Controller`へと返却します。
8.  **レスポンス:** `Controller`は受け取ったオブジェクトをJSON形式などに変換し、クライアントにレスポンスとして返します。

このように、Spring BootとMyBatisは、開発者がビジネスロジックの実装に集中できるよう、定型的で面倒な処理を裏側で支えてくれる強力な土台の役割を果たします。


```mermaid
graph TD
    subgraph クライアント
        A[ブラウザ/モバイルアプリ]
    end

    subgraph "Spring Bootアプリケーション"
        subgraph "フレームワークが提供"
            B[組み込みTomcat]
            C{DispatcherServlet}
            G[MyBatisエンジン]
            H[Mapper XML]
        end

        subgraph "開発者が実装するプログラム"
            D[Controllerクラス<br>@RestController]
            E[Serviceクラス<br>@Service]
            F[Mapperインターフェース<br>@Mapper]
        end
        
        %% リクエストフロー
        B --> C;
        C -- URLに応じて振り分け --> D;
        D -- ビジネスロジックを依頼 --> E;
        E -- DB操作を依頼 --> F;
        F -- 連携 --> G;
        H -- SQL定義を読み込み --> G;
        G -- SQLを実行 --> I[(データベース)];
        
        %% レスポンスフロー
        I -- 実行結果 --> G;
        G -- 結果をオブジェクトにマッピング --> F;
        F -- 結果を返す --> E;
        E -- 結果を返す --> D;
        D -- 結果(Javaオブジェクト)を返す --> C;
        C -- HTTPレスポンス<br>(JSON等に変換) --> A;
    end
    
    subgraph データベース
        I
    end

    A -- HTTPリクエスト --> B;

    style D fill:#cde4ff
    style E fill:#cde4ff
    style F fill:#cde4ff
```