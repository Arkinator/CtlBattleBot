package com.planed.ctlBot.discord;

import com.planed.ctlBot.domain.User;
import com.planed.ctlBot.domain.UserRepository;
import com.planed.ctlBot.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.HTTP429Exception;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

@Component
public class DiscordService {
    Logger LOG = LoggerFactory.getLogger(DiscordService.class);

    private String commandList;
    private IDiscordClient discordClient;
    @Autowired
    private UserService userService;

    public void replyInChannel(final String channelId, final String message) {
        LOG.info(channelId + ": " + message);
        RequestBuffer.request(() -> {
            try {
                new MessageBuilder(discordClient)
                        .withChannel(channelId)
                        .withContent(message).build();
            } catch (MissingPermissionsException | DiscordException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public void whisperToUser(final String authorId, final String message) {
        try {
            final IChannel privateChannel = discordClient.getOrCreatePMChannel(discordClient.getUserByID(authorId));
            replyInChannel(privateChannel.getID(), message);
        } catch (HTTP429Exception | DiscordException e) {
            e.printStackTrace();
        }
    }

    public void setDiscordClient(final IDiscordClient discordClient) {
        this.discordClient = discordClient;
    }

    public String shortInfo(final User user) {
        String result = discordClient.getUserByID(user.getDiscordId()).getName();
        result += " (" + user.getElo() + ")";
        return result;
    }

    public String getCommandList() {
        return commandList;
    }

    public void setCommandList(final String commandList) {
        this.commandList = commandList;
    }

    public String getDiscordName(final User user) {
        return discordClient.getUserByID(user.getDiscordId()).getName();
    }
}
