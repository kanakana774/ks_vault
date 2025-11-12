package com.example.demo.entity;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class GameHistory {
    // ゲーム履歴ID
    private int historyId;
    // 総合プレイヤー数
    private int totalPlayNumbers;
    // 当日プレイヤー数
    private int todayPlayNumbers;
    // 登録ユーザID
    private String insertUserId;
    // 登録年月日
    private Timestamp insertDate;
    // 更新ユーザID
    private String updateUserId;
    // 更新年月日
    private Timestamp updateDate;
    // 削除フラグ
    private char deleteFlg;
    // 更新回数
    private Integer updateCount;

}
