package com.example.batch.mapper;

import com.example.batch.domain.AggregatedPointDto;
import com.example.batch.domain.CustomerPointDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CustomerMapper {
    void upsertUserMonthlyPoints(@Param("list") List<AggregatedPointDto> list);

    List<CustomerPointDto> findCustomersByRank(@Param("rank") String rank);
}