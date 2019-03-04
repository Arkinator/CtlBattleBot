package com.planed.ctlBot.services;

import com.planed.ctlBot.common.*;
import com.planed.ctlBot.discord.DiscordService;
import com.planed.ctlBot.domain.Match;
import com.planed.ctlBot.domain.MatchRepository;
import com.planed.ctlBot.domain.User;
import com.planed.ctlBot.domain.UserRepository;
import com.planed.ctlBot.utils.StringConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class UserService {
    private static final String WELCOME_MESSAGE = "I would like to know which race you are playing in your Starcraft 2 endeavours? " +
            "(Click on one of the symbols. They signify Terran, Zerg and Protoss)";
    private static final String RACE_CHANGE_MESSAGE = "Want to change your race? Just let me know which race you are playing in your Starcraft 2 endeavours! " +
            "(Click on one of the symbols. They signify Terran, Zerg and Protoss)";
    private static final String TERRAN_EMOJI = "🏢";
    private static final String ZERG_EMOJI = "\uD83D\uDC09";
    private static final String PROTOSS_EMOJI = "\uD83D\uDCA0";
    private static final String RANDOM_EMOJI = "\uD83C\uDFB2";

    @Value("${dasLigaBot.loginBaseUrl}")
    private String loginBaseUrl;

    @Autowired
    private DiscordService discordService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private PrService prService;

    public void giveUserAccessLevel(final String discordId, final AccessLevel accessLevel) {
        User user = findUserAndCreateIfNotFound(discordId);
        user.setAccessLevel(accessLevel);
        userRepository.save(user);
    }

    public User findUserAndCreateIfNotFound(final String discordId) {
        User result = userRepository.findByDiscordId(discordId);
        if (result == null) {
            addNewUser(discordId);
            result = userRepository.findByDiscordId(discordId);
        }
        return result;
    }

    public void addNewUser(final String discordId) {
        final User entity = new User();
        entity.setDiscordId(discordId);
        userRepository.save(entity);

        if (discordService.isUserYourself(discordId)) {
            return;
        }

        discordService.whisperToUser(discordId, StringConstants.INFO_STRING);
        discordService.whisperToUser(discordId, WELCOME_MESSAGE)
                .map(msg -> discordService.addReactionWithMapper(msg,
                        Arrays.asList(TERRAN_EMOJI, ZERG_EMOJI, PROTOSS_EMOJI, RANDOM_EMOJI),
                        str -> updateUserRaceByEmoji(str, discordId)));
    }

    public void whisperChangeRaceMessageToUser(User author) {
        discordService.whisperToUser(author.getDiscordId(), RACE_CHANGE_MESSAGE)
                .map(msg -> discordService.addReactionWithMapper(msg,
                        Arrays.asList(TERRAN_EMOJI, ZERG_EMOJI, PROTOSS_EMOJI, RANDOM_EMOJI),
                        str -> updateUserRaceByEmoji(str, author.getDiscordId())));
    }

    private void updateUserRaceByEmoji(String emoji, String userDiscordId) {
        User user = findUserAndCreateIfNotFound(userDiscordId);
        switch (emoji) {
            case TERRAN_EMOJI:
                user.setRace(Race.TERRAN);
                break;
            case ZERG_EMOJI:
                user.setRace(Race.ZERG);
                break;
            case PROTOSS_EMOJI:
                user.setRace(Race.PROTOSS);
                break;
            case RANDOM_EMOJI:
                user.setRace(Race.RANDOM);
                break;
            default:
                return;
        }
        userRepository.save(user);
        sendRaceChangeMessage(user);
    }

    private void sendRaceChangeMessage(User user) {
        switch (user.getRace()) {
            case ZERG:
                discordService.whisperToUser(user.getDiscordId(), "Too old for micro? You now play as Zerg.");
                break;
            case TERRAN:
                discordService.whisperToUser(user.getDiscordId(), "Feeling whiny? You now play as Terran.");
                break;
            case PROTOSS:
                discordService.whisperToUser(user.getDiscordId(), "Did you study the changelog of the next patch? You now play as Protoss");
                break;
            case RANDOM:
                discordService.whisperToUser(user.getDiscordId(), "Somebody wants to show all the 1-base-all ins... You now play as Random");
                break;
        }
    }

    public void changeRace(final User author, final String newRace) {
        author.setRace(Race.valueOf(newRace.toUpperCase()));
        userRepository.save(author);
    }

    public void issueChallenge(final User author, final User challengee, Long serverId, Long channelId) {
        final Match match = matchRepository.addMatch(author, challengee, serverId, channelId);
        author.setMatchId(match.getMatchId());
        challengee.setMatchId(match.getMatchId());
        userRepository.save(author, challengee);
        discordService.whisperToUser(challengee.getDiscordId(),
                "You have been challenged by "
                        + shortInfo(author, serverId)
                        + ". You can !reject or !accept the challenge");
        prService.printChallengeExtendedMessage(match);
    }

    public void rejectChallenge(final User author) {
        matchRepository.findMatchById(author.getMatchId())
                .ifPresent(match -> {
                    prService.printChallengeRejectedMessage(match);
                    match.setGameStatus(GameStatus.CHALLENGE_REJECTED);
                    matchRepository.saveMatch(match);
                    clearMatchOfInvolvedPlayers(match);
                });
    }

    public void revokeChallenge(final User author) {
        matchRepository.findMatchById(author.getMatchId())
                .ifPresent(match -> {
                    match.setGameStatus(GameStatus.CHALLENGE_REVOKED);
                    matchRepository.saveMatch(match);
                    clearMatchOfInvolvedPlayers(match);
                    discordService.whisperToUser(author.getDiscordId(), "Your challenge has been revoked.");
                    discordService.whisperToUser(match.getPlayerB().getDiscordId(),
                            "The challenge from " + author.toString() + " has just been revoked.");
                });
    }

    public void acceptChallenge(final User author) {
        matchRepository.findMatchById(author.getMatchId())
                .ifPresent(match -> {
                    prService.printGameIsOnMessage(match);
                    match.setGameStatus(GameStatus.CHALLENGE_ACCEPTED);
                    matchRepository.saveMatch(match);
                });
    }

    public void reportResult(final Match match, final User author, final GameResult result) {
        match.reportResult(author, result);
        if (match.getGameStatus() == GameStatus.GAME_PLAYED) {
            prService.printMessageResultMessage(match);
            finalizeGame(match);
        }
        matchRepository.saveMatch(match);
    }

    private void finalizeGame(final Match match) {
        if (match.getGameStatus() == GameStatus.GAME_PLAYED) {
            final User playerA = match.getPlayerA();
            final User playerB = match.getPlayerB();

            final double expectancy = 1 / (1 + Math.pow(10,
                    (playerB.getElo() - playerA.getElo()) / 400.));
            final double result = match.getEloResult();

            playerA.setElo(playerA.getElo() + LigaConstants.K_FACTOR * (result - expectancy));
            playerB.setElo(playerB.getElo() + LigaConstants.K_FACTOR * ((1 - result) - (1 - expectancy)));

            userRepository.save(playerA, playerB);
            clearMatchOfInvolvedPlayers(match);
        }
    }

    private void clearMatchOfInvolvedPlayers(final Match match) {
        match.getPlayers().forEach(p -> {
            p.setMatchId(null);
            userRepository.save(p);
        });
    }

    public String getLeagueString() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getElo))
                .map(user -> shortInfo(user))
                .collect(Collectors.joining("\n"));
    }

    public void incrementCallsForUserByDiscordId(String discordId) {
        User user = findUserAndCreateIfNotFound(discordId);
        user.setNumberOfInteractions(user.getNumberOfInteractions() + 1);
        userRepository.save(user);
    }


    public String shortInfo(final User user, Long serverId) {
        String result = discordService.getDiscordName(user.getDiscordId(), serverId);
        result += " (" + user.getElo() + ")";
        return result;
    }

    public String shortInfo(final User user) {
        return shortInfo(user, null);
    }

    public String generateLoginLink(User user) {
        String loginAuthorizationCode = RandomStringUtils.randomAlphanumeric(20);
        user.setLoginAuthorizationCode(loginAuthorizationCode);
        userRepository.save(user);

        return loginBaseUrl + "?authCode=" + loginAuthorizationCode;
    }

    public void loginUserByAuthCode(String authCode, String battleNetId, String tokenValue) {
        User user = userRepository.findUserByAuthCode(authCode);

        user.setBattleNetId(battleNetId);
        user.setBattleNetTokenValue(tokenValue);

        userRepository.save(user);
    }
}
