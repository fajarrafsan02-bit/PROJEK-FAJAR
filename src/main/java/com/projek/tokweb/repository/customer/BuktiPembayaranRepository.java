package com.projek.tokweb.repository.customer;

import com.projek.tokweb.models.customer.BuktiPembayaran;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuktiPembayaranRepository extends JpaRepository<BuktiPembayaran, Long> {
    BuktiPembayaran findFirstByOrderId(Long orderId);
}

