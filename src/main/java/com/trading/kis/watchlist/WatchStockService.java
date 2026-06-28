package com.trading.kis.watchlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchStockService {

    private final WatchStockRepository repository;

    /** DB가 비어 있을 때 fallback용 기본 종목 (application.yml) */
    @Value("${trading.stocks}")
    private List<String> defaultStocks;

    /**
     * 스케줄러용 — 활성화된 종목코드 목록 반환.
     * DB에 종목이 하나도 없으면 yml 기본값으로 fallback.
     */
    public List<String> getActiveStockCodes() {
        List<WatchStockEntity> active = repository.findByEnabledTrueOrderByCreatedAtAsc();
        if (active.isEmpty()) {
            log.info("[WatchList] DB 종목 없음 → yml 기본값 사용: {}", defaultStocks);
            return defaultStocks;
        }
        return active.stream().map(WatchStockEntity::getStockCode).toList();
    }

    /**
     * 특정 종목의 1회 주문 수량.
     * 종목별 설정이 없으면 null 반환 → 호출부에서 global default 사용.
     */
    public Integer getOrderQty(String stockCode) {
        return repository.findByStockCode(stockCode)
                .map(WatchStockEntity::getOrderQty)
                .orElse(null);
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public List<WatchStockEntity> findAll() {
        return repository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public WatchStockEntity add(WatchStockRequest req) {
        if (req.getStockCode() == null || req.getStockCode().isBlank()) {
            throw new IllegalArgumentException("stockCode는 필수입니다");
        }
        if (repository.existsByStockCode(req.getStockCode())) {
            throw new IllegalArgumentException("이미 등록된 종목: " + req.getStockCode());
        }
        WatchStockEntity entity = WatchStockEntity.builder()
                .stockCode(req.getStockCode().trim())
                .stockName(req.getStockName())
                .enabled(req.getEnabled() != null ? req.getEnabled() : true)
                .orderQty(req.getOrderQty())
                .note(req.getNote())
                .build();
        log.info("[WatchList] 종목 추가: {}", entity.getStockCode());
        return repository.save(entity);
    }

    @Transactional
    public WatchStockEntity update(String stockCode, WatchStockRequest req) {
        WatchStockEntity entity = findOrThrow(stockCode);
        if (req.getStockName() != null) entity.setStockName(req.getStockName());
        if (req.getEnabled() != null)   entity.setEnabled(req.getEnabled());
        if (req.getOrderQty() != null)  entity.setOrderQty(req.getOrderQty());
        if (req.getNote() != null)      entity.setNote(req.getNote());
        log.info("[WatchList] 종목 수정: {}", stockCode);
        return repository.save(entity);
    }

    @Transactional
    public void delete(String stockCode) {
        repository.delete(findOrThrow(stockCode));
        log.info("[WatchList] 종목 삭제: {}", stockCode);
    }

    @Transactional
    public WatchStockEntity setEnabled(String stockCode, boolean enabled) {
        WatchStockEntity entity = findOrThrow(stockCode);
        entity.setEnabled(enabled);
        log.info("[WatchList] 종목 {} {}", stockCode, enabled ? "활성화" : "비활성화");
        return repository.save(entity);
    }

    private WatchStockEntity findOrThrow(String stockCode) {
        return repository.findByStockCode(stockCode)
                .orElseThrow(() -> new NoSuchElementException("종목 없음: " + stockCode));
    }
}
