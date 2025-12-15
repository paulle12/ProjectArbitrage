package com.predictionTooling.predictionTooling.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.predictionTooling.predictionTooling.client.ScoreBoardClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/score")
public class ScoreBoardController {

    private final ScoreBoardClient scoreBoardClient;
    private final ObjectMapper objectMapper;

    public ScoreBoardController(ScoreBoardClient scoreBoardClient, ObjectMapper objectMapper) {
        this.scoreBoardClient = scoreBoardClient;
        this.objectMapper = objectMapper;
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
    public ResponseEntity<List<JsonNode>> pollNFLScoreBoard(@RequestParam String teamName // team name to filter
    ) {
        // Get the raw object
        Object api = scoreBoardClient.getNFLGames();

        // Convert to JsonNode
        JsonNode rootNode = objectMapper.valueToTree(api);

        // Get the response array
        ArrayNode responseArray = (ArrayNode) rootNode.path("response");

        List<JsonNode> filteredGames = new ArrayList<>();

        for (JsonNode item : responseArray) {
            JsonNode leagueNode = item.path("league");
            if (leagueNode.path("id").asInt(-1) != 1) continue; // skip non-NFL

            JsonNode teamsNode = item.path("teams");
            String homeName = teamsNode.path("home").path("name").asText("");
            String awayName = teamsNode.path("away").path("name").asText("");

            if (homeName.equalsIgnoreCase(teamName) || awayName.equalsIgnoreCase(teamName)) {
                filteredGames.add(item);
            }
        }

        return ResponseEntity.ok(filteredGames);
}

    @GetMapping("/nhl/games")
    public ResponseEntity<List<JsonNode>> getNHLScoreBoard() {
        // Get the raw object
        Object api = scoreBoardClient.getNHLGames();

        // Convert it to a JsonNode
        JsonNode rootNode = objectMapper.valueToTree(api);

        // Get the response array
        ArrayNode responseArray = (ArrayNode) rootNode.path("response");

        List<JsonNode> nflGames = new ArrayList<>();

        for (JsonNode item : responseArray) {
            JsonNode leagueNode = item.path("league");
            if (leagueNode.path("id").asInt(-1) == 57) { // filter league.id == 57
                nflGames.add(item);
            }
        }

        return ResponseEntity.ok(nflGames);
    }

    @GetMapping("/nhl/games/poll")
    public ResponseEntity<List<JsonNode>> pollNHLScoreBoard(@RequestParam String teamName // team name to filter
    ) {
        // Get the raw object
        Object api = scoreBoardClient.getNFLGames();

        // Convert to JsonNode
        JsonNode rootNode = objectMapper.valueToTree(api);

        // Get the response array
        ArrayNode responseArray = (ArrayNode) rootNode.path("response");

        List<JsonNode> filteredGames = new ArrayList<>();

        for (JsonNode item : responseArray) {
            JsonNode leagueNode = item.path("league");
            if (leagueNode.path("id").asInt(-1) != 57) continue; // skip non-NHL

            JsonNode teamsNode = item.path("teams");
            String homeName = teamsNode.path("home").path("name").asText("");
            String awayName = teamsNode.path("away").path("name").asText("");

            if (homeName.equalsIgnoreCase(teamName) || awayName.equalsIgnoreCase(teamName)) {
                filteredGames.add(item);
            }
        }

        return ResponseEntity.ok(filteredGames);
    }
}
