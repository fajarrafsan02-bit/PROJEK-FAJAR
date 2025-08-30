package com.projek.tokweb.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Service
public class ValidasiService {

    private final String API_KEY = "daa03a38bb0746f8af19d26305243b31";
    private final String API_URL = "https://phonevalidation.abstractapi.com/v1";
    private final WebClient webClient;
    private final Map<String, Boolean> cache = new ConcurrentHashMap<>();

    public ValidasiService() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .followRedirect(true)
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                                .responseTimeout(Duration.ofSeconds(5))))
                .build();
    }

    public boolean nomorHandphoneValid(String nomorHandphone) {

        if (cache.containsKey(nomorHandphone)) {
            System.out.println("Sudah Digunakan cache untuk : " + nomorHandphone);
            return cache.get(nomorHandphone);
        }

        String formattedNumber = formatPhoneNumber(nomorHandphone);
        System.out.println(formattedNumber);
        System.out.println(nomorHandphone);

        System.out.println("Masuk pengecekan nomor hp");
        System.out.println(nomorHandphone);
        String url = API_URL + "?api_key=" + API_KEY + "&phone=" + formattedNumber + "&country=ID";

        try {
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            System.out.println("DEBUG response: " + response);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            boolean validKah = root.path("valid").asBoolean(false);
            cache.put(nomorHandphone, validKah);
            return validKah;
        } catch (Exception e) {
            System.out.println("Gagal validasi nomor: " + e.getMessage());
            throw new RuntimeException("Gagal Validasi Nomor: " + e.getMessage());
            // return false;
        }
    }



        private String formatPhoneNumber(String nomorHandphone) {
        String cleanNumber = nomorHandphone.replaceAll("[^0-9]", "");
        if (cleanNumber.startsWith("0")) {
            cleanNumber = "62" + cleanNumber.substring(1);
        }
        if (!cleanNumber.startsWith("62")) {
            cleanNumber = "62" + cleanNumber;
        }
        return "+" + cleanNumber;
    }
}
