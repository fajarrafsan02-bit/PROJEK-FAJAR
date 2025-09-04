package com.projek.tokweb.models.customer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bukti_pembayaran")
public class BuktiPembayaran {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String fileName;

    private String contentType;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] fileData;
}

