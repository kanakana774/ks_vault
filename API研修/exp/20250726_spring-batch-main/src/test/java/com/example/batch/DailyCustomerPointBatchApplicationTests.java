// src/test/java/com/example/batch/DailyCustomerPointUpdateApplicationTests.java
package com.example.batch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles; // これは必要

@SpringBootTest
@ActiveProfiles("test") // 'test' プロファイルを有効にする
class DailyCustomerPointUpdateApplicationTests {

    @Test
    void contextLoads() {
        // Spring Boot アプリケーションのコンテキストが正常にロードされることを確認するテスト
        // ここでは実際に Job を実行するロジックは書かない
    }
}