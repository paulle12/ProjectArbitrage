package com.predictionTooling.predictionTooling.dto;

import java.util.List;

public record BatchedOrderRequest(List<SingleOrderRequest> orders) {
}
