package com.k1per32.example;

import com.google.gson.*;

import java.io.IOException;
import java.util.concurrent.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.*;

public class CrptApi {
    private static final String CREATE_DOCUMENT_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Semaphore rateLimiter;
    private final Gson gson = new Gson();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.rateLimiter = new Semaphore(requestLimit);

        // запускаем периодическое восстановление разрешений
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                this.rateLimiter::release,
                0,
                timeUnit.toMillis(1),
                TimeUnit.MILLISECONDS
        );
    }

    public String createDocument(Document document, String signature) throws InterruptedException, IOException {
        // получаем разрешение перед выполнением запроса
        rateLimiter.acquire();

        try {
            // сериализуем объект документа в JSON
            String jsonDocument = gson.toJson(document);

            // создаем HTTP запрос
            RequestBody body = RequestBody.create(jsonDocument, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(CREATE_DOCUMENT_URL)
                    .post(body)
                    .addHeader("Signature", signature)
                    .build();

            // выполняем запрос и возвращаем результат
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                return response.body().string();
            }
        } finally {
            // Отпускаем разрешение только после выполнения запроса
        }
    }

    public static void main(String[] args) throws Exception {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
        // Демонстрационный вызов метода
        // Предположим, что у нас есть объект документа и подпись для него
        String result = api.createDocument(new Document(), "signature");
        System.out.println(result);
    }

    @Data
    @NoArgsConstructor
    private static class Document {

        private String description;

        private String participantInn;

        private String docId;

        private String docStatus;

        private String docType;

        private String importRequest;

        private String ownerInn;

        private String producerInn;

        private String productionDate;

        private String productionType;

        private String regDate;

        private String regNumber;

        private Products products;
    }

    @Data
    @NoArgsConstructor
    private static class Products {

        private CertificateType certificateDocument;

        private String certificateDocumentDate;

        private String certificateDocumentNumber;

        private String productionDate;

        private String tnvedCode;

        private String uitCode;

        private String uituCode;

        public enum CertificateType {
            CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
        }
    }
}