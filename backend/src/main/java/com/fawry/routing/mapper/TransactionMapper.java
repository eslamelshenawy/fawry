package com.fawry.routing.mapper;

import com.fawry.routing.domain.entity.Transaction;
import com.fawry.routing.dto.response.TransactionView;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionView toView(Transaction transaction) {
        return TransactionView.builder()
                .id(transaction.getId())
                .billerCode(transaction.getBiller().getCode())
                .gatewayCode(transaction.getGateway().getCode())
                .gatewayName(transaction.getGateway().getName())
                .amount(transaction.getAmount())
                .commission(transaction.getCommission())
                .totalCharged(transaction.getTotalCharged())
                .status(transaction.getStatus())
                .urgency(transaction.getUrgency())
                .splitGroupId(transaction.getSplitGroupId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
