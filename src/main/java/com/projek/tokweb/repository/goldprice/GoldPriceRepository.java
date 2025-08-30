package com.projek.tokweb.repository.goldprice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.goldPrice.GoldPrice;

@Repository
public interface GoldPriceRepository extends JpaRepository<GoldPrice, Long> {
    Optional<GoldPrice> findFirstByOrderByTanggalAmbilDescIdDesc();
    List<GoldPrice> findByTanggalAmbilBetweenOrderByTanggalAmbilDesc(LocalDateTime start, LocalDateTime end);
    List<GoldPrice> findByTanggalAmbilOrderByTanggalAmbilDesc(LocalDateTime tanggalAmbil);
}
