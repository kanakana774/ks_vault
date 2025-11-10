package com.example.demo.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.example.demo.entity.GameHistory;
import com.example.demo.mapper.GameHistoryMapper;

/**
 * 独自のItemWriterクラス
 */
@Component
public class CustomItemWriter implements ItemWriter<GameHistory> {

    private final GameHistoryMapper gameHistoryMapper;

    /**
     * コンストラクタ
     * 
     * @param gameHistoryMapper
     */
    public CustomItemWriter(GameHistoryMapper gameHistoryMapper) {
        this.gameHistoryMapper = gameHistoryMapper;
    }

    /**
     * DB書き込み
     * 
     */
    @Override
    public void write(Chunk<? extends GameHistory> chunk) throws Exception {
        // 不変リストを変更可能なリストに変換
        List<GameHistory> modifiableList = new ArrayList<>(chunk.getItems());
        GameHistory gameHistory = new GameHistory();
        // 各値を設定していく。
        gameHistory.setTodayPlayNumbers(modifiableList.size());
        gameHistory.setTotalPlayNumbers(modifiableList.size());
        gameHistory.setInsertUserId("admin001");
        gameHistory.setUpdateUserId("admin001");
        gameHistory.setDeleteFlg('0');
        gameHistory.setUpdateCount(0);

        // Mapperインターフェースの登録用の抽象メソッドを呼び出す。
        gameHistoryMapper.insertGameHistory(gameHistory);

    }

}
