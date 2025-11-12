package com.example.batch.item;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.example.batch.domain.AggregatedPointDto;
import com.example.batch.domain.CustomerPointDto;
import com.example.batch.mapper.CustomerMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerPointWriter implements ItemWriter<CustomerPointDto> {

    private final CustomerMapper customerMapper;

    @Override
    public void write(Chunk<? extends CustomerPointDto> chunk) throws Exception {

        // map<key:ユーザーID,value:map<key:年月,value:合計ポイント数>>
        Map<String, Map<YearMonth, Integer>> totalAmountByUserAndMonth = new HashMap<>();

        // for (CustomerPoint customerPoint : chunk.getItems()) {
        // String customerId = customerPoint.getCustomerId();
        // YearMonth pointsAddedMonth = customerPoint.getPointsAddedMonth();
        // Integer addedPoints = customerPoint.getAddedPoints();

        // if (!totalAmountByUserAndMonth.containsKey(customerId)) {
        // totalAmountByUserAndMonth.put(customerId, new HashMap<>());
        // }

        // Map<YearMonth, Integer> monthlyMap =
        // totalAmountByUserAndMonth.get(customerId);

        // if (!monthlyMap.containsKey(pointsAddedMonth)) {
        // monthlyMap.put(pointsAddedMonth, addedPoints);
        // } else {
        // monthlyMap.put(pointsAddedMonth, monthlyMap.get(pointsAddedMonth) +
        // addedPoints);
        // }
        // }

        // 集計
        for (CustomerPointDto customerPoint : chunk.getItems()) {
            String customerId = customerPoint.getCustomerId();
            YearMonth pointsAddedMonth = customerPoint.getPointsAddedMonth();
            Integer addedPoints = customerPoint.getAddedPoints();

            Map<YearMonth, Integer> outerMap = totalAmountByUserAndMonth.computeIfAbsent(customerId,
                    k -> new HashMap<>());
            outerMap.merge(pointsAddedMonth, addedPoints, Integer::sum);
        }

        // バルクupsert
        List<AggregatedPointDto> list = new ArrayList<>();
        for (Map.Entry<String, Map<YearMonth, Integer>> entry : totalAmountByUserAndMonth.entrySet()) {
            String userId = entry.getKey();
            for (Map.Entry<YearMonth, Integer> monthEntry : entry.getValue().entrySet()) {
                AggregatedPointDto aggregatedPoint = new AggregatedPointDto();
                aggregatedPoint.setCustomerId(userId);
                aggregatedPoint.setMonth(monthEntry.getKey());
                aggregatedPoint.setTotalPoints(monthEntry.getValue());
                list.add(aggregatedPoint);
            }

            customerMapper.upsertUserMonthlyPoints(list);
        }
    }
}