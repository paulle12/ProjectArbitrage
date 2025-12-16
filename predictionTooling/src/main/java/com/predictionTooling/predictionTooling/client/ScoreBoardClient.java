package com.predictionTooling.predictionTooling.client;

import com.predictionTooling.predictionTooling.config.Config;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
public class ScoreBoardClient {

    private final RestClient nflClient;
    private final RestClient nhlClient;
     String privateKey = Config.getScoreBoardKey();

    public ScoreBoardClient(RestClient.Builder builder) {
        this.nflClient = builder
                .baseUrl("https://v1.american-football.api-sports.io")
                .build();

        this.nhlClient = builder
                .baseUrl("https://v1.hockey.api-sports.io")
                .build();
    }

    public Object getNFLGames() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        return nflClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("date", date)
                        .build()
                )
                .header("x-apisports-key", privateKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Object.class);
    }

    public Object getNHLGames() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);

//        ZoneId est = ZoneId.of("America/New_York");
//        LocalDate utcDate =
//                ZonedDateTime.of(2025, 12, 16, 19, 0, 0, 0, est)
//                        .withZoneSameInstant(ZoneOffset.UTC)
//                        .toLocalDate();

        return nhlClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
//                        .queryParam("date", utcDate)
                        .queryParam("date", date)
                        .build()
                )
                .header("x-apisports-key",privateKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Object.class);
    }
}
