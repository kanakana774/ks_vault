package com.example.demo.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.entity.GameHistory;

@Mapper
public interface GameHistoryMapper {

    /**
     * 登録
     * 
     * @param gameHistories
     */
    // ゲームの履歴を登録する用の抽象メソッドを作る。
    public void insertGameHistory(GameHistory gameHistory);
}
