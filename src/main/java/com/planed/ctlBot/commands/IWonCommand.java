package com.planed.ctlBot.commands;

import com.planed.ctlBot.data.CtlMatch;
import com.planed.ctlBot.services.CtlMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

/**
 * Created by Julian Peters on 10.04.16.
 *
 * @author julian.peters@westernacher.com
 */
@Component
public class IWonCommand extends AbstractMatchCommand {
    private Logger LOG = LoggerFactory.getLogger(IWonCommand.class);

    public static final String COMMAND_STRING = "iWon";

    @Autowired
    IWonCommand(CtlMatchService ctlMatchService, BotCommandParser parser){
        super(ctlMatchService);
        parser.register(COMMAND_STRING, this);
    }

    @Override
    public void manipulateMatch(MessageReceivedEvent event, CtlMatch match) {
        if (getAuthor(event).equals(match.getPlayer1())) {
            match.setWinner(match.getPlayer1());
            exitLogging(event, match, match.getPlayer2());
        }else{
            match.setWinner(match.getPlayer2());
            exitLogging(event, match, match.getPlayer1());
        }
    }

    private void exitLogging(MessageReceivedEvent event, CtlMatch match, String looser) {
        LOG.info("Setting winner on match "+match+" to "+getAuthor(event));
        replyInChannel(event, "You won your match against "+looser+". Way to go!");
    }

    @Override
    public String getHelpText() {
        return "This signals that you schooled your opponent hard this week! YEAH!";
    }
}
