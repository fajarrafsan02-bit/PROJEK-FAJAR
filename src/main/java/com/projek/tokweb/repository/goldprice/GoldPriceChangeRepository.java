package com.projek.tokweb.repository.goldprice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.projek.tokweb.models.goldPrice.GoldPriceChange;

@Repository
public interface GoldPriceChangeRepository extends JpaRepository<GoldPriceChange, Long>{
     // Cari perubahan terbaru untuk karat tertentu
    Optional<GoldPriceChange> findFirstByPurityOrderByChangeDateDesc(String purity);
    
    // Cari semua perubahan untuk karat tertentu
    List<GoldPriceChange> findByPurityOrderByChangeDateDesc(String purity);
    
    // Cari perubahan dalam rentang tanggal
    @Query("SELECT gpc FROM GoldPriceChange gpc WHERE gpc.changeDate BETWEEN :startDate AND :endDate ORDER BY gpc.changeDate DESC")
    List<GoldPriceChange> findByChangeDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Cari perubahan terbaru untuk semua karat
    @Query("SELECT gpc FROM GoldPriceChange gpc WHERE gpc.id IN (SELECT MAX(gpc2.id) FROM GoldPriceChange gpc2 GROUP BY gpc2.purity)")
    List<GoldPriceChange> findLatestChangesForAllPurities();
}
