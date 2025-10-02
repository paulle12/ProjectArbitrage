package com.predictionTooling.predictionTooling.service;


import com.predictionTooling.predictionTooling.model.MatchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    private final List<MatchResult> matches = List.of(
            new MatchResult("Arsenal", "Chelsea", 2, 1),
            new MatchResult("Barcelona", "Real Madrid", 0, 2),
            new MatchResult("QWER", "ASDF", 3, 3)
    );

    public String didTeamWin(String team) {
        for (MatchResult match : matches) {
            if (match.involvesTeam(team)) {
                String winner = match.getWinner();
                if (winner.equalsIgnoreCase(team)) {
                    return "Yes, " + team + " won. Match: " + match;
                } else if (winner.equals("Draw")) {
                    return "No, it was a draw. Match: " + match;
                } else {
                    return "No, " + winner + " won. Match: " + match;
                }
            }
        }
        return "No match found for " + team;
    }

    public String didTeamBeat(String teamA, String teamB) {
        for (MatchResult match : matches) {
            if (match.isMatchBetween(teamA, teamB)) {
                String winner = match.getWinner();
                if (winner.equalsIgnoreCase(teamA)) {
                    return "Yes, " + teamA + " beat " + teamB + ". Match: " + match;
                } else if (winner.equalsIgnoreCase(teamB)) {
                    return "No, " + teamB + " won. Match: " + match;
                } else {
                    return "No, it was a draw between " + teamA + " and " + teamB + ". Match: " + match;
                }
            }
        }
        return "No match found between " + teamA + " and " + teamB;
    }
}
