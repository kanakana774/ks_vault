package com.example.batch.mapper;

import com.example.batch.domain.PurchaseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PurchaseHistoryMapper {

    List<PurchaseHistory> findPurchaseHistoryUntilDatePaged(
        @Param("processingDate") LocalDate processingDate,
        @Param("offset") long offset,
        @Param("limit") int limit
    );
}