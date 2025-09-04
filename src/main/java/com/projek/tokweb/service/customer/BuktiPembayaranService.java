package com.projek.tokweb.service.customer;

import com.projek.tokweb.models.customer.BuktiPembayaran;
import com.projek.tokweb.repository.customer.BuktiPembayaranRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class BuktiPembayaranService {

    private final BuktiPembayaranRepository repository;

    public BuktiPembayaran saveBukti(Long orderId, MultipartFile file) throws IOException {
        BuktiPembayaran bukti = BuktiPembayaran.builder()
                .orderId(orderId)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileData(file.getBytes())
                .build();
        return repository.save(bukti);
    }

    public BuktiPembayaran getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public BuktiPembayaran getByOrderId(Long orderId) {
        return repository.findFirstByOrderId(orderId);
    }
}

