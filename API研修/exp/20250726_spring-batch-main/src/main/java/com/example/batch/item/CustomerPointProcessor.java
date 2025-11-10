package com.example.batch.item;

import java.time.YearMonth;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.example.batch.domain.CustomerPointDto;
import com.example.batch.domain.PurchaseHistory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerPointProcessor implements ItemProcessor<PurchaseHistory, CustomerPointDto> {

    // 仮のポイント換算率（例: 100円につき1ポイント）
    private static final double POINT_RATE = 0.01;

    @Override
    public CustomerPointDto process(PurchaseHistory purchaseHistory) throws Exception {

        // 新しいポイントを加算
        int purchaseAmount = purchaseHistory.getAmount();
        int addedPoints = (int) (purchaseAmount * POINT_RATE);

        CustomerPointDto customerPoint = new CustomerPointDto();
        customerPoint.setCustomerId(purchaseHistory.getCustomerId());
        customerPoint.setPointsAddedMonth(YearMonth.from(purchaseHistory.getPurchaseDate()));
        customerPoint.setAddedPoints(addedPoints);
        return customerPoint;
    }
}