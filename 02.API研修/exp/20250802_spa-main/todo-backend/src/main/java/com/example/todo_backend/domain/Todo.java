package com.example.todo_backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity // Javaクラスを DBテーブルとしてマッピングするためのJPAアノテーション。このクラスがテーブルに対応しているとJPAに教えます。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    @Id // このフィールドが 主キー（Primary Key） であることを示します。
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主キー id の 自動採番ルールを指定します。
    private Long id;

    private String title;

    private boolean completed;
}
