package com.projek.tokweb.service.goldprice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projek.tokweb.models.admin.Product;
import com.projek.tokweb.models.goldPrice.GoldPrice;
import com.projek.tokweb.models.goldPrice.GoldPriceChange;
import com.projek.tokweb.models.goldPrice.GoldPriceEnum;
import com.projek.tokweb.repository.goldprice.GoldPriceChangeRepository;
import com.projek.tokweb.repository.goldprice.GoldPriceRepository;
import com.projek.tokweb.utils.NumberFormatter;
import com.projek.tokweb.repository.admin.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationContext;
import com.projek.tokweb.config.ApplicationContextProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.ApplicationEventPublisher;
import com.projek.tokweb.event.GoldPriceUpdateEvent;


@Service
@RequiredArgsConstructor
@Slf4j
public class GoldPriceService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private final GoldPriceRepository goldPriceRepository;
    private final GoldPriceChangeRepository goldPriceChangeRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final String API_URL = "https://gold.g.apised.com/v1/latest?metals=XAU&base_currency=IDR&weight_unit=gram";
    private final String API_KEY = "sk_Ac8d607d6205eBe1DEb1b0B627Aaa04c3D51FB8bD70951e3";

    public GoldPrice fetchAndSaveLatestPrice() {
        return fetchAndSaveLatestPrice(GoldPriceEnum.SISTEM);
    }

    /**
     * Update harga emas dari API eksternal dengan update otomatis harga produk
     */
    @Transactional
    public GoldPrice fetchAndSaveLatestPrice(GoldPriceEnum goldPriceEnum) {
        try {
            log.info(">> Memulai fetch dan save harga emas terbaru");
            
            // Fetch harga dari Metal Price API
            String apiUrl = "https://api.metalpriceapi.com/v1/latest?api_key=8690a386dcff535d89d68325dc10367e&base=IDR&currencies=EUR,XAU,XAG";
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                
                // Parse response dari Metal Price API
                if (rootNode.has("success") && rootNode.get("success").asBoolean() && rootNode.has("rates")) {
                    JsonNode ratesNode = rootNode.get("rates");
                    
                    if (ratesNode.has("XAU")) {
                        // XAU = Gold (troy ounce), 1 troy ounce = 31.1035 gram
                        double xauRate = ratesNode.get("XAU").asDouble();
                        log.info(">> XAU rate dari Metal Price API: {}", xauRate);
                        
                        if (xauRate <= 0) {
                            throw new RuntimeException("Invalid XAU rate: " + xauRate);
                        }
                        
                        // Convert dari troy ounce ke gram dan dari IDR base
                        // 1 IDR = XAU troy ounce
                        // 1 gram = 1 / (31.1035 * XAU) IDR
                        double goldPricePerGram = 1 / (31.1035 * xauRate);
                        log.info(">> Harga emas per gram (IDR): {}", goldPricePerGram);
                        
                        // Validasi harga yang masuk akal (1jt - 10jt per gram)
                        if (goldPricePerGram < 1000000 || goldPricePerGram > 10000000) {
                            throw new RuntimeException("Harga yang dihitung tidak masuk akal: " + goldPricePerGram + " IDR/gram");
                        }
                        
                        // Cek apakah ada data harga emas sebelumnya
                        GoldPrice oldPrice = null;
                        try {
                            oldPrice = getLatestGoldPrice();
                            log.info(">> Harga emas lama: {}", oldPrice);
                        } catch (RuntimeException e) {
                            log.info(">> Tidak ada harga emas sebelumnya, ini adalah data pertama");
                        }

                        // Hitung harga dengan pembulatan
                        Map<String, Double> hargaJual = calculatePricesByPurity(goldPricePerGram, true);
                        Map<String, Double> hargaBeli = calculatePricesByPurity(goldPricePerGram, false);

                        GoldPrice goldPrice = GoldPrice.builder()
                                .hargaJual24k(hargaJual.get("24k"))
                                .hargaBeli24k(hargaBeli.get("24k"))
                                .hargaJual22k(hargaJual.get("22k"))
                                .hargaBeli22k(hargaBeli.get("22k"))
                                .hargaJual18k(hargaJual.get("18k"))
                                .hargaBeli18k(hargaBeli.get("18k"))
                                .tanggalAmbil(LocalDateTime.now())
                                .goldPriceEnum(goldPriceEnum)
                                .build();

                        log.info(">> Menyimpan harga emas baru: {}", goldPrice);
                        GoldPrice saved = goldPriceRepository.save(goldPrice);
                        log.info(">> Harga emas berhasil disimpan dengan ID: {}", saved.getId());

                        // Simpan perubahan harga ke database
                        if (oldPrice != null) {
                            savePriceChangesFromAPI(oldPrice, goldPrice);
                        }

                        // Publish event untuk update harga produk (bukan langsung call service)
                        eventPublisher.publishEvent(new GoldPriceUpdateEvent(this, saved));

                        return saved;
                    } else {
                        log.error(">> Metal Price API tidak mengembalikan XAU rate yang valid");
                        throw new RuntimeException("Metal Price API tidak mengembalikan XAU rate yang valid");
                    }
                } else {
                    log.error(">> Metal Price API tidak mengembalikan response yang valid");
                    throw new RuntimeException("Metal Price API tidak mengembalikan response yang valid");
                }
            } else {
                log.error(">> Metal Price API returned non-OK status: {}", response.getStatusCode());
                throw new RuntimeException("Gagal mengambil harga emas dari Metal Price API. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error(">> Error in fetchAndSaveLatestPrice: {}", e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gagal mengambil atau menyimpan harga emas: " + e.getMessage());
        }
    }

    /**
     * Menghitung harga emas berdasarkan kadar kemurnian
     * 
     * @param harga24k Harga emas 24k per gram
     * @param isJual   true untuk harga jual, false untuk harga beli
     * @return Map dengan key kadar kemurnian dan value harga
     */
    private Map<String, Double> calculatePricesByPurity(double harga24k, boolean isJual) {
        Map<String, Double> prices = new HashMap<>();

        // Faktor markup untuk harga jual (5% markup)
        double markupFactor = isJual ? 1.05 : 0.95;

        // Hitung harga berdasarkan kadar kemurnian dan BULATKAN
        double harga24kCalculated = harga24k * markupFactor;
        double harga22kCalculated = (harga24k * 22.0 / 24.0) * markupFactor;
        double harga18kCalculated = (harga24k * 18.0 / 24.0) * markupFactor;

        // BULATKAN harga sebelum disimpan ke database
        prices.put("24k", NumberFormatter.roundGoldPrice(harga24kCalculated));
        prices.put("22k", NumberFormatter.roundGoldPrice(harga22kCalculated));
        prices.put("18k", NumberFormatter.roundGoldPrice(harga18kCalculated));

        return prices;
    }

    /**
     * Mendapatkan harga jual terbaru berdasarkan kadar kemurnian
     */
    public double getLatestHargaJual(int purity) {
        GoldPrice latestPrice = goldPriceRepository.findFirstByOrderByTanggalAmbilDescIdDesc()
                .orElseThrow(() -> new RuntimeException("Harga emas belum tersedia"));

        return getHargaJualByPurity(latestPrice, purity);
    }

    /**
     * Mendapatkan harga beli terbaru berdasarkan kadar kemurnian
     */
    public double getLatestHargaBeli(int purity) {
        GoldPrice latestPrice = goldPriceRepository.findFirstByOrderByTanggalAmbilDescIdDesc()
                .orElseThrow(() -> new RuntimeException("Harga emas belum tersedia"));

        return getHargaBeliByPurity(latestPrice, purity);
    }

    /**
     * Mendapatkan harga jual terbaru berdasarkan kadar kemurnian (return 0 jika tidak ada data)
     */
    public double getLatestHargaJualOrDefault(int purity) {
        GoldPrice latestPrice = getLatestGoldPriceOrNull();
        return latestPrice != null ? getHargaJualByPurity(latestPrice, purity) : 0.0;
    }

    /**
     * Mendapatkan harga beli terbaru berdasarkan kadar kemurnian (return 0 jika tidak ada data)
     */
    public double getLatestHargaBeliOrDefault(int purity) {
        GoldPrice latestPrice = getLatestGoldPriceOrNull();
        return latestPrice != null ? getHargaBeliByPurity(latestPrice, purity) : 0.0;
    }

    /**
     * Mendapatkan harga jual berdasarkan kadar kemurnian dari objek GoldPrice
     */
    private double getHargaJualByPurity(GoldPrice goldPrice, int purity) {
        switch (purity) {
            case 24:
                return goldPrice.getHargaJual24k();
            case 22:
                return goldPrice.getHargaJual22k();
            case 18:
                return goldPrice.getHargaJual18k();
            default:
                throw new RuntimeException("Kadar kemurnian " + purity + "k tidak didukung");
        }
    }

    /**
     * Mendapatkan harga beli berdasarkan kadar kemurnian dari objek GoldPrice
     */
    private double getHargaBeliByPurity(GoldPrice goldPrice, int purity) {
        switch (purity) {
            case 24:
                return goldPrice.getHargaBeli24k();
            case 22:
                return goldPrice.getHargaBeli22k();
            case 18:
                return goldPrice.getHargaBeli18k();
            default:
                throw new RuntimeException("Kadar kemurnian " + purity + "k tidak didukung");
        }
    }

    /**
     * Mendapatkan semua harga terbaru
     */
    public GoldPrice getLatestGoldPrice() {
        return goldPriceRepository.findFirstByOrderByTanggalAmbilDescIdDesc()
                .orElseThrow(() -> new RuntimeException("Harga emas belum tersedia"));
    }

    /**
     * Mendapatkan semua harga terbaru tanpa exception (return null jika tidak ada)
     */
    public GoldPrice getLatestGoldPriceOrNull() {
        return goldPriceRepository.findFirstByOrderByTanggalAmbilDescIdDesc().orElse(null);
    }

    public double getLatestHargaJual() {
        return getLatestHargaJual(24);
    }

    public double getLatestHargaBeli() {
        return getLatestHargaBeli(24);
    }

    /**
     * Mendapatkan riwayat harga emas dengan pagination
     */
    public Page<GoldPrice> getGoldPriceHistory(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return goldPriceRepository.findAll(pageable);
    }

    /**
     * Mendapatkan harga emas berdasarkan rentang tanggal
     */
    public List<GoldPrice> getGoldPriceByDateRange(String startDate, String endDate, int page, int size) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);

            if (start.isAfter(end)) {
                throw new RuntimeException("Tanggal mulai tidak boleh setelah tanggal akhir");
            }

            return goldPriceRepository.findByTanggalAmbilBetweenOrderByTanggalAmbilDesc(
                    start.atStartOfDay(),
                    end.atTime(23, 59, 59));

        } catch (Exception e) {
            throw new RuntimeException("Format tanggal tidak valid. Gunakan format yyyy-MM-dd");
        }
    }

    /**
     * Mendapatkan harga emas hari ini
     */
    public GoldPrice getTodayGoldPrice() {
        LocalDate today = LocalDate.now();
        return goldPriceRepository.findByTanggalAmbilBetweenOrderByTanggalAmbilDesc(
                today.atStartOfDay(),
                today.atTime(23, 59, 59)).stream().findFirst().orElse(null);
    }

    /**
     * Mendapatkan harga emas kemarin
     */
    public GoldPrice getYesterdayGoldPrice() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return goldPriceRepository.findByTanggalAmbilBetweenOrderByTanggalAmbilDesc(
                yesterday.atStartOfDay(),
                yesterday.atTime(23, 59, 59)).stream().findFirst().orElse(null);
    }

    /**
     * Mendapatkan perbandingan harga emas (hari ini vs kemarin)
     */
    public Map<String, Object> getPriceComparison() {
        GoldPrice todayPrice = getTodayGoldPrice();
        GoldPrice yesterdayPrice = getYesterdayGoldPrice();

        Map<String, Object> comparison = new HashMap<>();

        if (todayPrice != null && yesterdayPrice != null) {
            comparison.put("today", todayPrice);
            comparison.put("yesterday", yesterdayPrice);

            // Hitung perubahan harga 24k
            double change24k = todayPrice.getHargaJual24k() - yesterdayPrice.getHargaJual24k();
            double changePercent24k = (change24k / yesterdayPrice.getHargaJual24k()) * 100;

            comparison.put("change24k", change24k);
            comparison.put("changePercent24k", changePercent24k);
            comparison.put("trend", change24k > 0 ? "naik" : change24k < 0 ? "turun" : "stabil");
        }

        return comparison;
    }

    /**
     * Update harga emas secara manual berdasarkan karat dengan membuat data baru
     */
    public GoldPrice updateGoldPriceManual(int purity, double hargaJual, double hargaBeli) {
        try {
            // Ambil harga emas terbaru untuk perbandingan
            GoldPrice latestPrice = null;
            try {
                latestPrice = getLatestGoldPrice();
            } catch (RuntimeException e) {
                log.info(">> Tidak ada harga emas sebelumnya untuk perbandingan");
            }

            // Simpan harga lama sebelum update
            double oldHargaJual = latestPrice != null ? getHargaJualByPurity(latestPrice, purity) : 0.0;

            // BULATKAN harga yang diinput manual
            double roundedHargaJual = NumberFormatter.roundGoldPrice(hargaJual);
            double roundedHargaBeli = NumberFormatter.roundGoldPrice(hargaBeli);

            // Buat data baru
            GoldPrice newPrice;
            if (latestPrice != null) {
                // Copy data dari harga terbaru
                newPrice = GoldPrice.builder()
                        .hargaJual24k(latestPrice.getHargaJual24k())
                        .hargaBeli24k(latestPrice.getHargaBeli24k())
                        .hargaJual22k(latestPrice.getHargaJual22k())
                        .hargaBeli22k(latestPrice.getHargaBeli22k())
                        .hargaJual18k(latestPrice.getHargaJual18k())
                        .hargaBeli18k(latestPrice.getHargaBeli18k())
                        .tanggalAmbil(LocalDateTime.now())
                        .goldPriceEnum(GoldPriceEnum.ADMIN)
                        .build();
            } else {
                // Jika tidak ada data sebelumnya, buat data baru dengan harga default
                newPrice = GoldPrice.builder()
                        .hargaJual24k(0.0)
                        .hargaBeli24k(0.0)
                        .hargaJual22k(0.0)
                        .hargaBeli22k(0.0)
                        .hargaJual18k(0.0)
                        .hargaBeli18k(0.0)
                        .tanggalAmbil(LocalDateTime.now())
                        .goldPriceEnum(GoldPriceEnum.ADMIN)
                        .build();
            }

            // Update harga berdasarkan karat yang diminta (dengan harga yang sudah dibulatkan)
            switch (purity) {
                case 24:
                    newPrice.setHargaJual24k(roundedHargaJual);
                    newPrice.setHargaBeli24k(roundedHargaBeli);
                    break;
                case 22:
                    newPrice.setHargaJual22k(roundedHargaJual);
                    newPrice.setHargaBeli22k(roundedHargaBeli);
                    break;
                case 18:
                    newPrice.setHargaJual18k(roundedHargaJual);
                    newPrice.setHargaBeli18k(roundedHargaBeli);
                    break;
                default:
                    throw new RuntimeException("Kadar kemurnian tidak valid: " + purity);
            }

            savePriceChange(purity + "k", oldHargaJual, roundedHargaJual, "MANUAL", "Update manual harga emas (dibulatkan)");

            return goldPriceRepository.save(newPrice);

        } catch (Exception e) {
            throw new RuntimeException("Gagal mengupdate harga emas manual: " + e.getMessage(), e);
        }
    }

    /**
     * Update harga emas secara manual untuk semua karat sekaligus
     */
    public GoldPrice updateGoldPriceManualAll(double hargaJual24k, double hargaJual22k, double hargaJual18k) {
        try {
            log.info(">> Memulai update manual semua harga emas");

            // Ambil harga emas terbaru untuk perbandingan
            GoldPrice latestPrice = null;
            try {
                latestPrice = getLatestGoldPrice();
            } catch (RuntimeException e) {
                log.info(">> Tidak ada harga emas sebelumnya untuk perbandingan");
            }

            // BULATKAN semua harga yang diinput manual
            double roundedHargaJual24k = NumberFormatter.roundGoldPrice(hargaJual24k);
            double roundedHargaJual22k = NumberFormatter.roundGoldPrice(hargaJual22k);
            double roundedHargaJual18k = NumberFormatter.roundGoldPrice(hargaJual18k);

            // Jika tidak ada data sebelumnya, langsung buat data baru
            if (latestPrice == null) {
                GoldPrice newPrice = GoldPrice.builder()
                        .hargaJual24k(roundedHargaJual24k)
                        .hargaBeli24k(NumberFormatter.roundGoldPrice(roundedHargaJual24k * 0.95))
                        .hargaJual22k(roundedHargaJual22k)
                        .hargaBeli22k(NumberFormatter.roundGoldPrice(roundedHargaJual22k * 0.95))
                        .hargaJual18k(roundedHargaJual18k)
                        .hargaBeli18k(NumberFormatter.roundGoldPrice(roundedHargaJual18k * 0.95))
                        .tanggalAmbil(LocalDateTime.now())
                        .goldPriceEnum(GoldPriceEnum.ADMIN)
                        .build();

                GoldPrice savedPrice = goldPriceRepository.save(newPrice);
                log.info(">> Data harga emas pertama berhasil dibuat (dibulatkan)");
                return savedPrice;
            }

            // Cek apakah semua harga sama dengan data terbaru (bandingkan dengan harga yang sudah dibulatkan)
            if (latestPrice != null) {
                boolean allPricesSame = Math.abs(roundedHargaJual24k - latestPrice.getHargaJual24k()) < 0.01 &&
                        Math.abs(roundedHargaJual22k - latestPrice.getHargaJual22k()) < 0.01 &&
                        Math.abs(roundedHargaJual18k - latestPrice.getHargaJual18k()) < 0.01;

                // Jika semua harga sama, throw exception
                if (allPricesSame) {
                    throw new RuntimeException(
                            "Semua harga emas (24k, 22k, 18k) saat ini sudah sama dengan data terbaru. Tidak perlu update.");
                }
            }

            
            // Buat data baru karena ada perubahan (dengan harga yang sudah dibulatkan)
            GoldPrice newPrice = GoldPrice.builder()
                    .hargaJual24k(roundedHargaJual24k)
                    .hargaBeli24k(NumberFormatter.roundGoldPrice(roundedHargaJual24k * 0.95))
                    .hargaJual22k(roundedHargaJual22k)
                    .hargaBeli22k(NumberFormatter.roundGoldPrice(roundedHargaJual22k * 0.95))
                    .hargaJual18k(roundedHargaJual18k)
                    .hargaBeli18k(NumberFormatter.roundGoldPrice(roundedHargaJual18k * 0.95))
                    .tanggalAmbil(LocalDateTime.now())
                    .goldPriceEnum(GoldPriceEnum.ADMIN)
                    .build();
                    
            savePriceChangesFromAPI(latestPrice, newPrice);

            GoldPrice savedPrice = goldPriceRepository.save(newPrice);
            log.info(">> Update manual semua harga emas berhasil disimpan (dibulatkan)");
            return savedPrice;

        } catch (Exception e) {
            log.error(">> Error dalam update manual semua harga emas: {}", e.getMessage());
            throw new RuntimeException("Gagal mengupdate harga emas manual: " + e.getMessage(), e);
        }
    }

    /**
     * Simpan perubahan harga dari API ke database
     */
    private void savePriceChangesFromAPI(GoldPrice oldPrice, GoldPrice newPrice) {
        try {
            // Track perubahan untuk semua karat
            String[] purities = { "24k", "22k", "18k"};

            for (String purity : purities) {
                double oldHargaJual = getHargaJualByPurity(oldPrice, Integer.parseInt(purity.replace("k", "")));
                double newHargaJual = getHargaJualByPurity(newPrice, Integer.parseInt(purity.replace("k", "")));

                if (Math.abs(newHargaJual - oldHargaJual) > 0.01) { // Hanya simpan jika ada perubahan
                    savePriceChange(purity, oldHargaJual, newHargaJual, "API", "Update otomatis dari API eksternal");
                }
            }

            System.out.println(">> Perubahan harga dari API berhasil disimpan ke database");

        } catch (Exception e) {
            System.err.println(">> Gagal menyimpan perubahan harga dari API: " + e.getMessage());
        }
    }

    /**
     * Simpan perubahan harga ke database
     */
    private void savePriceChange(String purity, double oldPrice, double newPrice, String source, String notes) {
        try {
            double changeAmount = newPrice - oldPrice;
            double changePercent = oldPrice > 0 ? ((changeAmount / oldPrice) * 100) : 0;
            String changeType = changeAmount > 0 ? "INCREASE" : changeAmount < 0 ? "DECREASE" : "STABLE";

            GoldPriceChange priceChange = GoldPriceChange.builder()
                    .purity(purity)
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .changeAmount(changeAmount)
                    .changePercent(changePercent)
                    .changeType(changeType)
                    .changeDate(LocalDateTime.now())
                    .changeSource(source)
                    .notes(notes)
                    .build();

            goldPriceChangeRepository.save(priceChange);
            System.out.println(">> Perubahan harga " + purity + " tersimpan: " + oldPrice + " -> " + newPrice);

        } catch (Exception e) {
            System.err.println(">> Gagal menyimpan perubahan harga: " + e.getMessage());
        }
    }

    /**
     * Ambil perubahan harga terbaru untuk semua karat dengan informasi lengkap
     */
    public List<GoldPriceChange> getLatestChangesForAllPurities() {
        try {
            // Ambil perubahan terbaru untuk setiap karat
            List<GoldPriceChange> changes = new ArrayList<>();
            
            String[] purities = {"24k", "22k", "18k"};
            for (String purity : purities) {
                GoldPriceChange change = getLatestPriceChange(purity);
                if (change != null) {
                    changes.add(change);
                }
            }
            
            // Sort berdasarkan tanggal perubahan terbaru
            changes.sort((a, b) -> b.getChangeDate().compareTo(a.getChangeDate()));
            
            return changes;
        } catch (Exception e) {
            log.error("Error getting latest changes for all purities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Ambil perubahan harga terbaru untuk karat tertentu
     */
    public GoldPriceChange getLatestPriceChange(String purity) {
        try {
            return goldPriceChangeRepository.findFirstByPurityOrderByChangeDateDesc(purity).orElse(null);
        } catch (Exception e) {
            log.error("Error getting latest price change for {}: {}", purity, e.getMessage());
            return null;
        }
    }

    /**
     * Ambil semua perubahan harga untuk karat tertentu
     */
    public List<GoldPriceChange> getPriceChanges(String purity) {
        return goldPriceChangeRepository.findByPurityOrderByChangeDateDesc(purity);
    }

    /**
     * Update harga emas dari request dengan trigger event
     */
    @Transactional
    public GoldPrice updateGoldPriceFromRequest(double newHarga24k) {
        try {
            log.info(">> Service: Memulai update harga emas dari request: {}", newHarga24k);
            
            // Validasi input
            if (newHarga24k <= 0) {
                throw new RuntimeException("Harga 24K harus lebih dari 0");
            }
            
            // Ambil harga emas terbaru
            GoldPrice latestGoldPrice = null;
            try {
                latestGoldPrice = getLatestGoldPrice();
            } catch (RuntimeException e) {
                throw new RuntimeException("Tidak ada data harga emas yang tersedia");
            }
            
            if (latestGoldPrice == null) {
                throw new RuntimeException("Tidak ada data harga emas yang tersedia");
            }
            
            log.info(">> Service: Harga emas lama: {}", latestGoldPrice);
            
            // Update harga 24K
            double oldHarga24k = latestGoldPrice.getHargaJual24k();
            
            // Hitung perubahan
            double changeAmount = newHarga24k - oldHarga24k;
            double changePercent = (changeAmount / oldHarga24k) * 100;
            
            log.info(">> Service: Perubahan harga: {} -> {} ({}%, {})", 
                oldHarga24k, newHarga24k, changePercent, changeAmount);
            
            // Update harga emas
            latestGoldPrice.setHargaJual24k(newHarga24k);
            latestGoldPrice.setTanggalAmbil(LocalDateTime.now());
            
            // Hitung ulang harga berdasarkan kadar kemurnian
            Map<String, Double> newPrices = calculatePricesByPurity(newHarga24k, true);
            latestGoldPrice.setHargaJual24k(newPrices.get("24k"));
            latestGoldPrice.setHargaJual22k(newPrices.get("22k"));
            latestGoldPrice.setHargaJual18k(newPrices.get("18k"));
            
            newPrices = calculatePricesByPurity(newHarga24k, false);
            latestGoldPrice.setHargaBeli24k(newPrices.get("24k"));
            latestGoldPrice.setHargaBeli22k(newPrices.get("22k"));
            latestGoldPrice.setHargaBeli18k(newPrices.get("18k"));
            
            log.info(">> Service: Harga baru yang akan disimpan: {}", latestGoldPrice);
            
            // Simpan ke database
            GoldPrice savedGoldPrice = goldPriceRepository.save(latestGoldPrice);
            
            // Simpan riwayat perubahan
            savePriceChange("24k", oldHarga24k, newHarga24k, "MANUAL", "Update manual harga emas");
            
            log.info(">> Service: Update harga emas berhasil: {} -> {}", oldHarga24k, newHarga24k);
            
            // Publish event untuk update harga produk
            eventPublisher.publishEvent(new GoldPriceUpdateEvent(this, savedGoldPrice));
            
            return savedGoldPrice;
            
        } catch (Exception e) {
            log.error(">> Service: Error dalam update harga emas: {}", e.getMessage(), e);
            throw new RuntimeException("Gagal mengambil atau menyimpan harga emas: " + e.getMessage());
        }
    }

    /**
     * Fetch harga emas dari Metal Price API (metalpriceapi.com)
     */
    public Map<String, Object> fetchExternalPriceOnly() {
        try {
            log.info(">> Service: Memulai fetch harga emas dari Metal Price API");
            
            // URL API metalpriceapi.com dengan parameter yang benar
            String apiUrl = "https://api.metalpriceapi.com/v1/latest?api_key=8690a386dcff535d89d68325dc10367e&base=IDR&currencies=EUR,XAU,XAG";
            RestTemplate restTemplate = new RestTemplate();
            
            log.info(">> Service: Mengirim request ke Metal Price API: {}", apiUrl);
            
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            
            log.info(">> Service: Response status: {}", response.getStatusCode());
            log.info(">> Service: Response body length: {}", response.getBody() != null ? response.getBody().length() : 0);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    log.error(">> Service: Response body kosong");
                    throw new RuntimeException("Metal Price API mengembalikan response kosong");
                }
                
                // Log response body untuk debugging (hanya 200 karakter pertama)
                String logResponse = responseBody.length() > 200 ? 
                    responseBody.substring(0, 200) + "..." : responseBody;
                log.info(">> Service: Response body preview: {}", logResponse);
                
                // Cek apakah response dimulai dengan HTML (biasanya error page)
                if (responseBody.trim().startsWith("<")) {
                    log.error(">> Service: API mengembalikan HTML, kemungkinan error page");
                    throw new RuntimeException("Metal Price API mengembalikan halaman error HTML");
                }
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode;
                
                try {
                    rootNode = mapper.readTree(responseBody);
                } catch (Exception e) {
                    log.error(">> Service: Error parsing JSON: {}", e.getMessage());
                    log.error(">> Service: Response body yang gagal di-parse: {}", responseBody);
                    throw new RuntimeException("Gagal parse response JSON dari Metal Price API: " + e.getMessage());
                }
                
                // Parse response dari Metal Price API
                if (rootNode.has("success") && rootNode.get("success").asBoolean() && rootNode.has("rates")) {
                    JsonNode ratesNode = rootNode.get("rates");
                    
                    if (ratesNode.has("XAU")) {
                        // XAU = Gold (troy ounce), 1 troy ounce = 31.1035 gram
                        double xauRate = ratesNode.get("XAU").asDouble();
                        log.info(">> Service: XAU rate dari Metal Price API: {}", xauRate);
                        
                        if (xauRate <= 0) {
                            throw new RuntimeException("Invalid XAU rate: " + xauRate);
                        }
                        
                        // Convert dari troy ounce ke gram dan dari IDR base
                        // 1 IDR = XAU troy ounce
                        // 1 gram = 1 / (31.1035 * XAU) IDR
                        double goldPricePerGram = 1 / (31.1035 * xauRate);
                        log.info(">> Service: Harga emas per gram (IDR): {}", goldPricePerGram);
                        
                        // Validasi harga yang masuk akal (1jt - 10jt per gram)
                        if (goldPricePerGram < 1000000 || goldPricePerGram > 10000000) {
                            throw new RuntimeException("Harga yang dihitung tidak masuk akal: " + goldPricePerGram + " IDR/gram");
                        }
                        
                        // Hitung harga dengan pembulatan
                        Map<String, Double> hargaJual = calculatePricesByPurity(goldPricePerGram, true);
                        Map<String, Double> hargaBeli = calculatePricesByPurity(goldPricePerGram, false);

                        Map<String, Object> result = new HashMap<>();
                        result.put("harga24k", NumberFormatter.roundGoldPrice(goldPricePerGram));
                        result.put("hargaJual24k", hargaJual.get("24k"));
                        result.put("hargaBeli24k", hargaBeli.get("24k"));
                        result.put("hargaJual22k", hargaJual.get("22k"));
                        result.put("hargaBeli22k", hargaBeli.get("22k"));
                        result.put("hargaJual18k", hargaJual.get("18k"));
                        result.put("hargaBeli18k", hargaBeli.get("18k"));
                        result.put("timestamp", rootNode.has("timestamp") ? rootNode.get("timestamp").asLong() : System.currentTimeMillis() / 1000);
                        result.put("source", "Metal Price API");
                        result.put("apiResponse", rootNode.toString());

                        log.info(">> Service: Harga emas berhasil diambil dari Metal Price API: {}", result);
                        return result;
                        
                    } else {
                        log.error(">> Service: Metal Price API tidak mengembalikan XAU rate yang valid");
                        log.error(">> Service: Rates node: {}", ratesNode);
                        throw new RuntimeException("Metal Price API tidak mengembalikan XAU rate yang valid");
                    }
                } else {
                    log.error(">> Service: Metal Price API tidak mengembalikan response yang valid");
                    log.error(">> Service: Root node: {}", rootNode);
                    throw new RuntimeException("Metal Price API tidak mengembalikan response yang valid");
                }
            } else {
                log.error(">> Service: Metal Price API returned non-OK status: {}", response.getStatusCode());
                log.error(">> Service: Response body: {}", response.getBody());
                throw new RuntimeException("Gagal mengambil harga emas dari Metal Price API. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error(">> Service: Error in fetchExternalPriceOnly: {}", e.getMessage(), e);
            
            // Jika ada masalah dengan Metal Price API, gunakan harga default atau harga terbaru dari database
            try {
                log.info(">> Service: Mencoba menggunakan harga terbaru dari database sebagai fallback");
                GoldPrice latestPrice = null;
                try {
                    latestPrice = getLatestGoldPrice();
                } catch (RuntimeException ex) {
                    log.info(">> Service: Tidak ada data fallback dari database");
                }
                
                if (latestPrice != null) {
                    Map<String, Object> fallbackResult = new HashMap<>();
                    fallbackResult.put("harga24k", latestPrice.getHargaJual24k());
                    fallbackResult.put("hargaJual24k", latestPrice.getHargaJual24k());
                    fallbackResult.put("hargaBeli24k", latestPrice.getHargaBeli24k());
                    fallbackResult.put("hargaJual22k", latestPrice.getHargaJual22k());
                    fallbackResult.put("hargaBeli22k", latestPrice.getHargaBeli22k());
                    fallbackResult.put("hargaJual18k", latestPrice.getHargaJual18k());
                    fallbackResult.put("hargaBeli18k", latestPrice.getHargaBeli18k());
                    fallbackResult.put("timestamp", LocalDateTime.now());
                    fallbackResult.put("source", "Database Fallback");
                    fallbackResult.put("note", "Metal Price API tidak tersedia, menggunakan harga terbaru dari database");
                    
                    log.info(">> Service: Menggunakan fallback dari database: {}", fallbackResult);
                    return fallbackResult;
                } else {
                    throw new RuntimeException("Gagal mengambil harga emas dari Metal Price API dan tidak ada fallback dari database: " + e.getMessage());
                }
            } catch (Exception fallbackError) {
                log.error(">> Service: Error dalam fallback: {}", fallbackError.getMessage());
                throw new RuntimeException("Gagal mengambil harga emas dari Metal Price API: " + e.getMessage());
            }
        }
    }
}
