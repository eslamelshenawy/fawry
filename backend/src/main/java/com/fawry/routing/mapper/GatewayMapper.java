package com.fawry.routing.mapper;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.vo.AvailabilityWindow;
import com.fawry.routing.domain.vo.CommissionRule;
import com.fawry.routing.dto.request.GatewayRequest;
import com.fawry.routing.dto.response.GatewayResponse;
import org.springframework.stereotype.Component;

@Component
public class GatewayMapper {

    public Gateway toEntity(GatewayRequest request) {
        return Gateway.builder()
                .code(request.code())
                .name(request.name())
                .commissionRule(new CommissionRule(request.fixedFee(), request.percentageFee()))
                .dailyLimit(request.dailyLimit())
                .minTransaction(request.minTransaction())
                .maxTransaction(request.maxTransaction())
                .processingTimeMinutes(request.processingTimeMinutes())
                .availability(toAvailability(request))
                .active(request.active() == null || request.active())
                .build();
    }

    public void applyUpdate(Gateway target, GatewayRequest request) {
        target.setName(request.name());
        target.setCommissionRule(new CommissionRule(request.fixedFee(), request.percentageFee()));
        target.setDailyLimit(request.dailyLimit());
        target.setMinTransaction(request.minTransaction());
        target.setMaxTransaction(request.maxTransaction());
        target.setProcessingTimeMinutes(request.processingTimeMinutes());
        target.setAvailability(toAvailability(request));
        if (request.active() != null) {
            target.setActive(request.active());
        }
    }

    public GatewayResponse toResponse(Gateway gateway) {
        AvailabilityWindow window = gateway.getAvailability();
        CommissionRule rule = gateway.getCommissionRule();
        return GatewayResponse.builder()
                .id(gateway.getId())
                .code(gateway.getCode())
                .name(gateway.getName())
                .fixedFee(rule.getFixedFee())
                .percentageFee(rule.getPercentageFee())
                .dailyLimit(gateway.getDailyLimit())
                .minTransaction(gateway.getMinTransaction())
                .maxTransaction(gateway.getMaxTransaction())
                .processingTimeMinutes(gateway.getProcessingTimeMinutes())
                .available24x7(window.isAlwaysAvailable())
                .availableDays(window.getAllowedDaysCsv())
                .availableFromHour(window.getFromHour())
                .availableToHour(window.getToHour())
                .active(gateway.isActive())
                .build();
    }

    private AvailabilityWindow toAvailability(GatewayRequest request) {
        if (Boolean.TRUE.equals(request.available24x7())) {
            return AvailabilityWindow.alwaysOn();
        }
        return new AvailabilityWindow(
                false,
                request.availableDays(),
                request.availableFromHour(),
                request.availableToHour());
    }
}
