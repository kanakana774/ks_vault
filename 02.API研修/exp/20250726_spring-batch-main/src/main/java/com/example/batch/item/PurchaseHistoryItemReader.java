package com.example.batch.item;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.MyBatisPagingItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.stereotype.Component;

import com.example.batch.domain.PurchaseHistory;
import com.example.batch.mapper.PurchaseHistoryMapper;

/**
 * 購入履歴のページング対応 Reader クラス (processingDate 自動設定版)
 */
@Component
@StepScope
public class PurchaseHistoryItemReader implements ItemReader<PurchaseHistory>, ItemStream {

    // reader専用session
    // private final SqlSession sqlSession;
    // mapper
    private final PurchaseHistoryMapper mapper;
    // プログラム内で取得する処理対象日付
    private final LocalDate processingDate;
    // 1ページあたりの取得件数 (チャンクサイズと一致)
    private final int pageSize;

    // 読み込みの開始位置を管理するオフセット
    private long currentOffset;
    // pageを保持するiterator
    private Iterator<PurchaseHistory> iterator;

    /**
     * コンストラクタ
     * 
     * @param sqlSessionFactory
     */
    public PurchaseHistoryItemReader(SqlSessionFactory sqlSessionFactory, PurchaseHistoryMapper mapper) {
        // try {
        // // reader用のsessionを開く
        // sqlSession = sqlSessionFactory.openSession();
        // mapper = sqlSession.getMapper(PurchaseHistoryMapper.class);
        // } catch (Exception e) {
        // // 例外発生時は初期化失敗としてランタイム例外をスロー
        // throw new RuntimeException("Failed to initialize PurchaseHistoryItemReader: "
        // + e.getMessage(), e);
        // }
        // 初期化処理
        this.mapper = mapper;
        this.processingDate = LocalDate.now();
        this.pageSize = 3;
    }

    /**
     * open 初期化処理
     * 初回と異常終了後などの後の再起動時に呼ばれる
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // Job の再起動時に前回のオフセット値を復元
        this.currentOffset = executionContext.getLong("currentPurchaseHistoryOffset", 0L);
        System.out.println("Reading from offset: " + currentOffset);
    }

    /**
     * read データを一つずつ読み込み
     * chunk回数分呼ばれ、nullを返すとそこで呼ばれなくなる
     */
    @Override
    public PurchaseHistory read() {
        // iteratorがnull（初回呼び出し時）または現在のページを読み終えた場合、次のページをフェッチ
        if (iterator == null || !iterator.hasNext()) {
            fetchDataBatch(); // 次のページのデータを取得
        }

        // 次のページをフェッチした後でもデータがなければ、読み込み終了 (nullを返す)
        if (!iterator.hasNext()) {
            return null;
        }

        return iterator.next();
    }

    /**
     * update 読み込みオフセットを保存し、必要であれば次のページをフェッチ
     * chunk終了時のwriterコミット後に呼ばれる
     */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // currentOffset を保存
        executionContext.putLong("currentPurchaseHistoryOffset", this.currentOffset);
        System.out.println("Saved progress: currentPurchaseHistoryOffset=" + this.currentOffset);
    }

    /**
     * close リソースの開放
     * step終了または、エラー発生時に呼ばれる
     */
    @Override
    public void close() throws ItemStreamException {
        // try {
        // if (sqlSession != null) {
        // sqlSession.close();
        // }
        // iterator = null;
        // } catch (Exception e) {
        // throw new ItemStreamException("Failed to close PurchaseHistoryItemReader
        // resources", e);
        // }
    }

    /**
     * データをデータベースからフェッチする
     * open() 時または update() 時のみ呼び出す
     */
    private void fetchDataBatch() throws ItemStreamException {
        try {
            // 対象期間で検索
            List<PurchaseHistory> page = mapper.findPurchaseHistoryUntilDatePaged(processingDate,
                    currentOffset,
                    pageSize);

            if (page.isEmpty()) {
                // データがない場合は空のiterator
                iterator = Collections.emptyIterator();
            } else {
                // データがある場合
                iterator = page.iterator();
                // 実際に読み込んだ件数分オフセットを進める
                this.currentOffset += page.size();
            }
        } catch (Exception e) {
            // DBアクセスエラーが発生した場合、Readerのセッションも閉じるべき
            // ただし、close()で呼ばれるためここでは再スローするだけに
            throw new ItemStreamException("Error fetching data batch from database", e);
        }
    }
}