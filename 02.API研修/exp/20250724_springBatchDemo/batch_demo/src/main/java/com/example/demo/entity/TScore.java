package com.example.demo.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;

/**
 * TScoreエンティティクラス
 *
 */
@Data
public class TScore {

    /** 成績ID */
    private Long scoreId;

    /** ユーザID */
    private String userId;

    /** 正答数 */
    private Integer ansNumCorrect;

    /** 誤答数 */
    private Integer ansNumWrong;

    /** 記録タイム */
    private BigDecimal time;

    /** 得点 */
    private Integer score;

    /** ランク */
    private String userRank;

    /** 登録ユーザID */
    private String insertUserId;

    /** 登録年月日 */
    private Timestamp insertDate;

    /** 更新ユーザID */
    private String updateUserId;

    /** 更新年月日 */
    private Timestamp updateDate;

    /** 削除フラグ */
    private String deleteFlg;

    /** 更新回数 */
    private Integer updateCount;
}