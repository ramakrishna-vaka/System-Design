
package com.project.urlshortner.service;

import com.project.urlshortner.model.UrlData;
import com.project.urlshortner.repository.UrlDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class UrlShortnerConcurrencyTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UrlDataRepository urlDataRepository;

    private String shortCode;

    @BeforeEach
    void setupTestData() {
        shortCode = "test";

        urlDataRepository.save(
                new UrlData("https://example.com", shortCode)
        );
    }

    @Test
    void twoConcurrentRedirectRequests() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<ResponseEntity<Map>> request = () -> {
                ready.countDown();
                start.await();

                return restTemplate.postForEntity(
                        "http://localhost:" + port + "/redirect",
                        Map.of("shortCode", shortCode),
                        Map.class
                );
            };

            Future<ResponseEntity<Map>> requestA =
                    executor.submit(request);

            Future<ResponseEntity<Map>> requestB =
                    executor.submit(request);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            ResponseEntity<Map> responseA =
                    requestA.get(15, TimeUnit.SECONDS);

            ResponseEntity<Map> responseB =
                    requestB.get(15, TimeUnit.SECONDS);

            assertEquals(HttpStatus.ACCEPTED, responseA.getStatusCode());
            assertEquals(HttpStatus.ACCEPTED, responseB.getStatusCode());

            UrlData result = urlDataRepository.findById(shortCode)
                    .orElseThrow();

            System.out.println(
                    "Final view count: " + result.getViews()
            );

            // With two successful redirects, the intended count is 2.
            assertEquals(2, result.getViews());

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
