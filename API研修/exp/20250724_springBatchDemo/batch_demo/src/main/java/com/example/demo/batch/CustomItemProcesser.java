package com.example.demo.batch;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.demo.entity.GameHistory;
import com.example.demo.entity.TScore;

@Component
public class CustomItemProcesser implements ItemProcessor<TScore, GameHistory> {

    private final Timestamp startTodayTimestamp;
    private final Timestamp endTodayTimestamp;

    public CustomItemProcesser() {
        // 本日の日付を取得
        LocalDateTime nowLocalDate = LocalDateTime.now();
        // 本日の日付の開始時刻（00:00:00）
        LocalDateTime startTodayLocalDateTime = nowLocalDate.toLocalDate().atTime(LocalTime.MIN);
        // 本日の日付の終了時刻（23:59:59）
        LocalDateTime endTodayLocalDateTime = nowLocalDate.toLocalDate().atTime(LocalTime.MAX);
        // LocalDateTime を Timestamp に変換
        startTodayTimestamp = Timestamp.valueOf(startTodayLocalDateTime);
        endTodayTimestamp = Timestamp.valueOf(endTodayLocalDateTime);
    }

    /**
     * データ加工
     * 
     */
    @Override
    public GameHistory process(TScore tScore) throws Exception {
        // ゲームの履歴を登録するためにGameHistoryのインスタンスを生成する。
        // すべてのフィールドに対して値を設定する。
        GameHistory gameHistory = new GameHistory();

        // 本日プレイした人数の計算
        if (!tScore.getInsertDate().after(startTodayTimestamp)
                && tScore.getInsertDate().before(endTodayTimestamp)) {
            return null;
        }
        return gameHistory;
    }

}
