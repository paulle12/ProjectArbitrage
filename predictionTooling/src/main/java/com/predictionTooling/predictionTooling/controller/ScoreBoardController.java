package com.predictionTooling.predictionTooling.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.predictionTooling.predictionTooling.client.ScoreBoardClient;
import com.predictionTooling.predictionTooling.redis.NHLStreamPublisher;
import com.predictionTooling.predictionTooling.redis.NFLStreamPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/score")
public class ScoreBoardController {

    private final ScoreBoardClient scoreBoardClient;
    private final ObjectMapper objectMapper;
    private final NFLStreamPublisher NFLStreamPublisher;
    private final NHLStreamPublisher nhlStreamPublisher;

    public ScoreBoardController(ScoreBoardClient scoreBoardClient, ObjectMapper objectMapper,
            NFLStreamPublisher NFLStreamPublisher, NHLStreamPublisher nhlStreamPublisher) {
        this.scoreBoardClient = scoreBoardClient;
        this.objectMapper = objectMapper;
        this.NFLStreamPublisher = NFLStreamPublisher;
        this.nhlStreamPublisher = nhlStreamPublisher;
    }

    @GetMapping("/nfl/games")
    public ResponseEntity<List<JsonNode>> getNFLScoreBoard() {
        // Get the raw object
        Object api = scoreBoardClient.getNFLGames();

        // Convert it to a JsonNode
        JsonNode rootNode = objectMapper.valueToTree(api);

        // Get the response array
        ArrayNode responseArray = (ArrayNode) rootNode.path("response");

        List<JsonNode> nflGames = new ArrayList<>();

        for (JsonNode item : responseArray) {
            JsonNode leagueNode = item.path("league");
            if (leagueNode.path("id").asInt(-1) == 1) { // filter league.id == 1
                nflGames.add(item);
            }
        }

        return ResponseEntity.ok(nflGames);
    }

    @GetMapping("/nfl/games/poll")
    public ResponseEntity<JsonNode> pollNFLScoreBoard(@RequestParam String teamName, @RequestParam int minLead) {
        // Start the publisher only when this endpoint is called
        NFLStreamPublisher.startPublishing(teamName, minLead);

        // Return the latest cached game if available
        return NFLStreamPublisher.getLatestGame()
                .map(ResponseEntity::ok)
                // todo fix response
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    @GetMapping("/nfl/latest-game")
    public ResponseEntity<JsonNode> getLatestNFLGame() {
        return NFLStreamPublisher.getLatestGame()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    @GetMapping("/nhl/games")
    public ResponseEntity<List<JsonNode>> getNHLScoreBoard() {
        // Get the raw object
        Object api = scoreBoardClient.getNHLGames();

        // Convert it to a JsonNode
        JsonNode rootNode = objectMapper.valueToTree(api);

        // Get the response array
        ArrayNode responseArray = (ArrayNode) rootNode.path("response");

        List<JsonNode> nhlGames = new ArrayList<>();

        for (JsonNode item : responseArray) {
            JsonNode leagueNode = item.path("league");
            if (leagueNode.path("id").asInt(-1) == 57) { // filter league.id == 57
                nhlGames.add(item);
            }
        }

        return ResponseEntity.ok(nhlGames);
    }

    @GetMapping("/nhl/games/poll")
    public ResponseEntity<JsonNode> pollNHLScoreBoard(@RequestParam String teamName, @RequestParam int minLead) {
        System.out.println("i am publishing:");
        // Start the publisher only when this endpoint is called
        nhlStreamPublisher.startPublishing(teamName, minLead);

        // Return the latest cached game if available
        return nhlStreamPublisher.getLatestGame()
                .map(ResponseEntity::ok)
                // todo fix response
                .orElseGet(() -> ResponseEntity.accepted().build());
    }
}
