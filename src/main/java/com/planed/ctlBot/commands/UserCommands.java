package com.planed.ctlBot.commands;

import com.planed.ctlBot.commands.data.CommandCall;
import com.planed.ctlBot.common.GameResult;
import com.planed.ctlBot.common.GameStatus;
import com.planed.ctlBot.discord.DiscordCommand;
import com.planed.ctlBot.discord.DiscordController;
import com.planed.ctlBot.discord.DiscordService;
import com.planed.ctlBot.domain.Match;
import com.planed.ctlBot.domain.User;
import com.planed.ctlBot.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Created by Julian Peters on 23.04.16.
 *
 * @author julian.peters@westernacher.com
 */
@DiscordController
public class UserCommands {
    Logger LOG = LoggerFactory.getLogger(UserCommands.class);

    @Autowired
    private UserService userService;
    @Autowired
    private DiscordService discordService;

    @DiscordCommand(name = {"setRace", "changeRace"}, help = "This command lets you change your race. Specify whether you play Zerg, Terran, Protoss or Random")
    public void changeRaceCommand(final CommandCall call) {
        Assert.isTrue(call.getParameters().size() >= 1);
        userService.changeRace(call.getAuthor(), call.getParameters().get(0));
    }

    @DiscordCommand(name = {"challenge"}, help = "Challenge a player. Just type @ and his name!", minMentions = 1)
    public void issueChallenge(final CommandCall call) {
        final User challenger = call.getAuthor();
        final User challengee = call.getMentions().get(0);
        if (needNoMatch(call, call.getAuthor(), "You are already in a match: '" + challenger.getMatch())
                && needNoMatch(call, call.getMentions().get(0), "The challengee is already in a match: '" + challengee)) {
            userService.issueChallenge(challenger, challengee);
        }
    }


    @DiscordCommand(name = {"reject", "rejectchallenge"}, help = "Reject your current challenge")
    public void rejectChallenge(final CommandCall call) {
        if (needMatch(call, "No match found. Type !status to see current Matches\"!") &&
                needToBeChallengee(call, "You can not reject a challenge you did not make! Your current match is " + call.getAuthor().getMatch()) &&
                needGameStatus(call, "You can only reject recently extended challenges. Current match is "
                        + call.getAuthor().getMatch(), GameStatus.challengeExtended)) {
            userService.rejectChallenge(call.getAuthor());
        }
    }

    @DiscordCommand(name = {"revoke", "revokechallenge"}, help = "Revoke your current challenge")
    public void revokeChallenge(final CommandCall call) {
        final User author = call.getAuthor();
        final Match match = author.getMatch();
        if (needMatch(call, "No match currently assigned to you. You need to !challenge somebody first") &&
                needGameStatus(call, "You can only revoke recently extended challenges. Type !status to learn about your current match", GameStatus.challengeExtended) &&
                needToBeChallenger(call, "You can not revoke a challenge that has been extended to you! Your current match is " + author.getMatch())) {
            userService.revokeChallenge(call.getAuthor());
        }
    }

    @DiscordCommand(name = {"accept", "acceptchallenge"}, help = "Accept the challenge extended to you!")
    public void acceptChallenge(final CommandCall call) {
        if (needMatch(call, "This command is to accept a challenge, that has been extended to you. Currently there is none") &&
                needToBeChallengee(call, "You can not accept a challenge that has you made! Your current match is "
                        + call.getAuthor().getMatch()) &&
                needGameStatus(call, "You can only recently extended challenges. Current match is "
                        + call.getAuthor().getMatch(), GameStatus.challengeExtended)) {
            userService.acceptChallenge(call.getAuthor());
        }
    }

    private boolean needGameStatus(final CommandCall call, final String message, final GameStatus... statusses) {
        for (final GameStatus status : statusses) {
            if (call.getAuthor().getMatch().getGameStatus() == status) {
                return true;
            }
        }
        discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
        return false;
    }

    private boolean needToBeChallengee(final CommandCall call, final String message) {
        if (call.getAuthor().getUserId() == call.getAuthor().getMatch().getPlayers().get(0).getUserId()) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needToBeChallenger(final CommandCall call, final String message) {
        if (call.getAuthor().getUserId() == call.getAuthor().getMatch().getPlayers().get(1).getUserId()) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needNoMatch(final CommandCall call, final User user, final String message) {
        if (user.getMatch() != null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needMatch(final CommandCall call, final String message) {
        if (call.getAuthor().getMatch() == null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needParameters(final CommandCall call, final int numParamsNeeded, final String message) {
        if (call.getParameters().size() < numParamsNeeded) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    @DiscordCommand(name = {"report", "reportresult"}, help = "Report either a 'win' or a 'loss' in the current game")
    public void reportResult(final CommandCall call) {
        if (needMatch(call, "This command is to report a result for game. No game found for you!") &&
                needGameStatus(call, "You can only recently extended challenges. Current match is "
                        + call.getAuthor().getMatch(), GameStatus.challengeAccepted, GameStatus.partiallyReported) &&
                needParameters(call, 1, "Did you win or loose? Type 'win' or 'loss'.") &&
                needGameResultParameter(call, "Did you win or loose? Type 'win' or 'loss'.") &&
                needNoReportedResultForUser(call, "You already reported a result! If this is a mistake, contact an admin asap!")) {
            userService.reportResult(call.getAuthor().getMatch(), call.getAuthor(), GameResult.parse(call.getParameters().get(0)));
        }
    }

    private boolean needNoReportedResultForUser(final CommandCall call, final String message) {
        if (call.getAuthor().getMatch().didUserReportResult(call.getAuthor())) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    private boolean needGameResultParameter(final CommandCall call, final String message) {
        if (GameResult.parse(call.getParameters().get(0)) == null) {
            discordService.whisperToUser(call.getAuthor().getDiscordId(), message);
            return false;
        }
        return true;
    }

    @DiscordCommand(name = {"status"}, help = "This displays your current status (open challenges, league position etc)")
    public void showStatusCommand(final CommandCall call) {
        discordService.whisperToUser(call.getAuthor().getDiscordId(), buildStatusString(call));
    }

    private String buildStatusString(final CommandCall call) {
        final Match match = call.getAuthor().getMatch();
        final StringBuilder builder = new StringBuilder();
        builder.append("Hello! You are a registered user.\n");
        builder.append("You have spoken to me " + call.getAuthor().getNumberOfInteractions() + " times\n");
        builder.append("Your race currently is " + call.getAuthor().getRace() + "\n");
        builder.append("You are rated at "+call.getAuthor().getElo()+ " Elo\n");
        if (match == null) {
            builder.append("You have no open matches\n");
        } else {
            builder.append("Your current match is " + match + "\n");
        }
        return builder.toString();
    }
}
