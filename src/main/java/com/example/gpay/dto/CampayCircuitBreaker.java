//package com.example.gpay.dto;
//
//import com.example.gpay.dto.campay.CampayResponse;
//import com.example.gpay.services.CampayService;
//import org.apache.commons.lang3.concurrent.CircuitBreaker;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//
//@Component
//public class CampayCircuitBreaker {
//
//    private final CircuitBreaker circuitBreaker;
//    private final CampayService campayService;
//
//    public CampayCircuitBreaker(CampayService campayService) {
//        this.campayService = campayService;
//        this.circuitBreaker = CircuitBreaker.ofDefaults("campay-api");
//
//        circuitBreaker.getEventPublisher()
//                .onStateTransition(event ->
//                        logger.info("Circuit breaker state transition: {}", event));
//    }
//
//    public Mono<CampayResponse> processDepositWithCircuitBreaker(DepositRequest request) {
//        return Mono.fromCallable(() -> circuitBreaker.executeSupplier(() ->
//                        campayService.processDeposit(request).block()
//                ))
//                .onErrorResume(CallNotPermittedException.class, ex -> {
//                    logger.warn("Circuit breaker is open, rejecting request");
//                    return Mono.error(new RuntimeException("Payment service temporarily unavailable"));
//                });
//    }
//}