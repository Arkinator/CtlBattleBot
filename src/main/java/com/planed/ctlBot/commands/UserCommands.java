package com.planed.ctlBot.commands;

import com.planed.ctlBot.commands.data.DiscordMessage;
import com.planed.ctlBot.common.AccessLevel;
import com.planed.ctlBot.common.GameResult;
import com.planed.ctlBot.common.GameStatus;
import com.planed.ctlBot.discord.DiscordCommand;
import com.planed.ctlBot.discord.DiscordController;
import com.planed.ctlBot.discord.DiscordService;
import com.planed.ctlBot.domain.Match;
import com.planed.ctlBot.domain.MatchRepository;
import com.planed.ctlBot.domain.User;
import com.planed.ctlBot.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;

@DiscordController
public class UserCommands {
    @Autowired
    private UserService userService;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private DiscordService discordService;

    @DiscordCommand(name = {"setRace", "changeRace", "race"}, help = "This command lets you change your race. Specify whether you play Zerg, Terran, Protoss or Random")
    public void changeRaceCommand(final DiscordMessage call) {
        if (call.getParameters().size() > 0) {
            userService.changeRace(call.getAuthor(), call.getParameters().get(0));
        } else {
            userService.whisperChangeRaceMessageToUser(call.getAuthor());
        }
    }

    @DiscordCommand(name = {"challenge"}, help = "Challenge a player. Just type @ and his name!", minMentions = 1)
    public void issueChallenge(final DiscordMessage call) {
        final User challenger = call.getAuthor();
        final User challengee = call.getMentions().get(0);
        if (needNoMatch(call, call.getAuthor(), "You are already in a match: '" + findMatch(challenger))
                && needNoMatch(call, call.getMentions().get(0), "The challengee is already in a match: '" + challengee)
                && needDifferentUsers(call, challenger, challengee, "You can not challenge yourself!")) {
            userService.issueChallenge(challenger, challengee, call.getServerId(), call.getChannel());
        }
    }

    private Match findMatch(final User user) {
        return matchRepository.findMatchById(user.getMatchId());
    }

    @DiscordCommand(name = {"reject", "rejectchallenge", "swipeleft"}, help = "Reject your current challenge")
    public void rejectChallenge(final DiscordMessage call) {
        if (needMatch(call, "No match found. Type !status to see current Matches\"!") &&
                needToBeChallengee(call, "You can not reject a challenge you did not make! Your current match is " + findMatch(call.getAuthor())) &&
                needGameStatus(call, "You can only reject recently extended challenges. Current match is "
                        + findMatch(call.getAuthor()), GameStatus.CHALLENGE_EXTENDED)) {
            userService.rejectChallenge(call.getAuthor());
        }
    }

    @DiscordCommand(name = {"revoke", "revokechallenge"}, help = "Revoke your current challenge")
    public void revokeChallenge(final DiscordMessage call) {
        final User author = call.getAuthor();
        if (needMatch(call, "No match currently assigned to you. You need to !challenge somebody first") &&
                needGameStatus(call, "You can only revoke recently extended challenges. Type !status to learn about your current match", GameStatus.CHALLENGE_EXTENDED) &&
                needToBeChallenger(call, "You can not revoke a challenge that has been extended to you! Your current match is " + findMatch(author))) {
            userService.revokeChallenge(call.getAuthor());
        }
    }

    @DiscordCommand(name = {"accept", "acceptchallenge", "swiperight"}, help = "Accept the challenge extended to you!")
    public void acceptChallenge(final DiscordMessage call) {
        if (needMatch(call, "This command is to accept a challenge, that has been extended to you. Currently there is none") &&
                needToBeChallengee(call, "You can not accept a challenge that you have extended! Your current match is " + findMatch(call.getAuthor())) &&
                needGameStatus(call, "You can only recently extended challenges. Current match is " + findMatch(call.getAuthor()), GameStatus.CHALLENGE_EXTENDED)) {
            final User challengee = findMatch(call.getAuthor()).getPlayers().get(0);
            final User challenger = call.getAuthor();
            userService.acceptChallenge(call.getAuthor());
            discordService.whisperToUser(challenger.getDiscordId(), "You just accepted a challenge from "
                    + discordService.shortInfo(challengee, call.getServerId()) +
                    ". Now get in touch with your opponent and battle it out. The format is Best-of-three, " +
                    "maps are loosers-pick, your pick (the challengee) for the first map, the game is on. glhf");
            discordService.whisperToUser(challengee.getDiscordId(),
                    "Your challenge to " + discordService.shortInfo(challenger, call.getServerId()) + " just got accepted! " +
                            ". Now get in touch with your opponent and battle it out. The format is Best-of-three, " +
                            "maps are loosers-pick, your opponent picks (the challengee) for the first map, the game is on. glhf");
        }
    }

    private boolean needGameStatus(final DiscordMessage call, final String message, final GameStatus... statusses) {
        for (final GameStatus status : statusses) {
            if (findMatch(call.getAuthor()).getGameStatus() == status) {
                return true;
            }
        }
        discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
        return false;
    }

    private boolean needDifferentUsers(final DiscordMessage call, final User challenger, final User challengee, final String message) {
        if (challenger.getDiscordId().equals(challengee.getDiscordId())) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needToBeChallengee(final DiscordMessage call, final String message) {
        final String callAuthorId = call.getAuthor().getDiscordId();
        final String matchChallengeeId = findMatch(call.getAuthor()).getPlayers().get(0).getDiscordId();

        if (callAuthorId.equals(matchChallengeeId)) {
            discordService.whisperToUser(callAuthorId, message);
            return false;
        } else {
            return true;
        }
    }

    private boolean needToBeChallenger(final DiscordMessage call, final String message) {
        if (call.getAuthor().getDiscordId().equals(findMatch(call.getAuthor()).getPlayers().get(1).getDiscordId())) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needNoMatch(final DiscordMessage call, final User user, final String message) {
        if (findMatch(user) != null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needMatch(final DiscordMessage call, final String message) {
        if (findMatch(call.getAuthor()) == null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needParameters(final DiscordMessage call, final int numParamsNeeded, final String message) {
        if (call.getParameters().size() < numParamsNeeded) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    @DiscordCommand(name = {"report", "reportresult"}, help = "Report either a 'win' or a 'loss' in the current game")
    public void reportResult(final DiscordMessage call) {
        if (needMatch(call, "This command is to report a result for game. No game found for you!") &&
                needGameStatus(call, "You can only report results for a valid challenge. Current match is "
                        + findMatch(call.getAuthor()), GameStatus.CHALLENGE_ACCEPTED, GameStatus.PARTIALLY_REPORTED, GameStatus.CONFLICT_STATE) &&
                needParameters(call, 1, "Did you win or loose? Type 'win' or 'loss'.") &&
                needGameResultParameter(call, "Did you win or loose? Type 'win' or 'loss'.")) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), "Reporting result: " + GameResult.parse(call.getParameters().get(0)));
            userService.reportResult(findMatch(call.getAuthor()), call.getAuthor(), GameResult.parse(call.getParameters().get(0)));
        }
    }

    private boolean needNoReportedResultForUser(final DiscordMessage call, final String message) {
        if (findMatch(call.getAuthor()).didUserReportResult(call.getAuthor()) && findMatch(call.getAuthor()).getGameStatus() != GameStatus.CONFLICT_STATE) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needGameResultParameter(final DiscordMessage call, final String message) {
        if (GameResult.parse(call.getParameters().get(0)) == null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    @DiscordCommand(name = {"status"}, help = "This displays your current status (open challenges, league position etc)")
    public void showStatusCommand(final DiscordMessage call) {
        discordService.whisperToUser(call.getAuthor().getDiscordId(), buildStatusString(call));
    }

    @DiscordCommand(name = {"league", "liga", "standings"}, help = "This displays the current league standings")
    public void showStandings(final DiscordMessage call) {
        discordService.whisperToUser(call.getAuthor().getDiscordId(), userService.getLeagueString());
    }

    private String buildStatusString(final DiscordMessage call) {
        final Match match = findMatch(call.getAuthor());
        final StringBuilder builder = new StringBuilder();
        if (call.getAuthor().getAccessLevel() == AccessLevel.USER) {
            builder.append("Hello! You are a registered user.\n");
        } else if (call.getAuthor().getAccessLevel() == AccessLevel.ADMIN) {
            builder.append("Hello! You are an administrator\n");
        } else {
            builder.append("Hello! You are MY AUTHOR! YEAH!\n");
        }
        builder.append("You have spoken to me " + call.getAuthor().getNumberOfInteractions() + " times\n");
        builder.append("Your race currently is " + call.getAuthor().getRace() + "\n");
        builder.append("You are rated at " + call.getAuthor().getElo() + " Elo\n");
        if (match == null) {
            builder.append("You have no open matches\n");
        } else {
            builder.append("Your current match is " + match + "\n");
        }
        return builder.toString();
    }
}
