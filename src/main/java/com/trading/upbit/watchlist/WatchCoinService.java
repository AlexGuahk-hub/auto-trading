package com.trading.upbit.watchlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchCoinService {

    private final WatchCoinRepository repository;

    /** DB가 비어 있을 때 fallback용 기본 종목 (application.yml) */
    @Value("${trading.coins}")
    private List<String> defaultCoins;

    /**
     * 스케줄러용 — 활성화된 마켓 코드 목록 반환.
     * DB에 종목이 하나도 없으면 yml 기본값으로 fallback.
     */
    public List<String> getActiveMarkets() {
        List<WatchCoinEntity> active = repository.findByEnabledTrueOrderByCreatedAtAsc();
        if (active.isEmpty()) {
            log.info("[WatchList-Coin] DB 종목 없음 → yml 기본값 사용: {}", defaultCoins);
            return defaultCoins;
        }
        return active.stream().map(WatchCoinEntity::getMarket).toList();
    }

    /**
     * 특정 마켓의 1회 주문금액.
     * 종목별 설정이 없으면 null 반환 → 호출부에서 global default 사용.
     */
    public BigDecimal getOrderAmountKrw(String market) {
        return repository.findByMarket(market)
                .map(WatchCoinEntity::getOrderAmountKrw)
                .orElse(null);
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public List<WatchCoinEntity> findAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public WatchCoinEntity add(WatchCoinRequest req) {
        if (req.getMarket() == null || req.getMarket().isBlank()) {
            throw new IllegalArgumentException("market은 필수입니다");
        }
        if (repository.existsByMarket(req.getMarket())) {
            throw new IllegalArgumentException("이미 등록된 마켓: " + req.getMarket());
        }
        WatchCoinEntity entity = WatchCoinEntity.builder()
                .market(req.getMarket().trim().toUpperCase())
                .coinName(req.getCoinName())
                .enabled(req.getEnabled() != null ? req.getEnabled() : true)
                .orderAmountKrw(req.getOrderAmountKrw())
                .note(req.getNote())
                .build();
        log.info("[WatchList-Coin] 종목 추가: {}", entity.getMarket());
        return repository.save(entity);
    }

    @Transactional
    public WatchCoinEntity update(String market, WatchCoinRequest req) {
        WatchCoinEntity entity = findOrThrow(market);
        if (req.getCoinName() != null)      entity.setCoinName(req.getCoinName());
        if (req.getEnabled() != null)       entity.setEnabled(req.getEnabled());
        if (req.getOrderAmountKrw() != null) entity.setOrderAmountKrw(req.getOrderAmountKrw());
        if (req.getNote() != null)          entity.setNote(req.getNote());
        log.info("[WatchList-Coin] 종목 수정: {}", market);
        return repository.save(entity);
    }

    @Transactional
    public void delete(String market) {
        repository.delete(findOrThrow(market));
        log.info("[WatchList-Coin] 종목 삭제: {}", market);
    }

    @Transactional
    public WatchCoinEntity setEnabled(String market, boolean enabled) {
        WatchCoinEntity entity = findOrThrow(market);
        entity.setEnabled(enabled);
        log.info("[WatchList-Coin] 종목 {} {}", market, enabled ? "활성화" : "비활성화");
        return repository.save(entity);
    }

    private WatchCoinEntity findOrThrow(String market) {
        return repository.findByMarket(market)
                .orElseThrow(() -> new NoSuchElementException("마켓 없음: " + market));
    }
}
