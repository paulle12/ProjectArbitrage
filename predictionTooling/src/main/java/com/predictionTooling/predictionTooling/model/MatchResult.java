// package com.predictionTooling.predictionTooling.model;

// import java.util.Objects;

// //??????? not sure if we are going anywhere with this
// public class MatchResult {

// private String team1;
// private String team2;
// private int score1;
// private int score2;

// // Constructor
// public MatchResult(String team1, String team2, int score1, int score2) {
// this.team1 = team1;
// this.team2 = team2;
// this.score1 = score1;
// this.score2 = score2;
// }

// // Default constructor (needed if you use frameworks that require it)
// public MatchResult() {
// }

// // Getters and setters
// public String getTeam1() {
// return team1;
// }

// public void setTeam1(String team1) {
// this.team1 = team1;
// }

// public String getTeam2() {
// return team2;
// }

// public void setTeam2(String team2) {
// this.team2 = team2;
// }

// public int getScore1() {
// return score1;
// }

// public void setScore1(int score1) {
// this.score1 = score1;
// }

// public int getScore2() {
// return score2;
// }

// public void setScore2(int score2) {
// this.score2 = score2;
// }

// // Business logic methods

// // Returns the winner team name or "Draw"
// public String getWinner() {
// if (score1 > score2) {
// return team1;
// } else if (score2 > score1) {
// return team2;
// } else {
// return "Draw";
// }
// }

// // Checks if this match involves the given team (case insensitive)
// public boolean involvesTeam(String team) {
// return team1.equalsIgnoreCase(team) || team2.equalsIgnoreCase(team);
// }

// // Checks if this match is between the two given teams (in any order)
// public boolean isMatchBetween(String teamA, String teamB) {
// return (team1.equalsIgnoreCase(teamA) && team2.equalsIgnoreCase(teamB)) ||
// (team1.equalsIgnoreCase(teamB) && team2.equalsIgnoreCase(teamA));
// }

// @Override
// public String toString() {
// return String.format("%s %d - %d %s", team1, score1, score2, team2);
// }

// // Override equals and hashCode for good practice (optional, but recommended)
// @Override
// public boolean equals(Object o) {
// if (this == o) return true;
// if (!(o instanceof MatchResult)) return false;
// MatchResult that = (MatchResult) o;
// return score1 == that.score1 &&
// score2 == that.score2 &&
// team1.equalsIgnoreCase(that.team1) &&
// team2.equalsIgnoreCase(that.team2);
// }

// @Override
// public int hashCode() {
// return Objects.hash(team1.toLowerCase(), team2.toLowerCase(), score1,
// score2);
// }
// }
