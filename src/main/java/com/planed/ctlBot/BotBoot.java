package com.planed.ctlBot;


import com.planed.ctlBot.data.repositories.UserEntityRepository;
import com.planed.ctlBot.discord.DiscordService;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = {"sx.blah.discord", "com.planed.ctlBot"})
public class BotBoot {
    private static final Logger logger = LoggerFactory.getLogger(BotBoot.class);

    @Value("${discord.token}")
    private String token;
    @Value("${discord.authorUserId}")
    private String authorUserId;
    @Autowired
    private DiscordService discordService;
    @Autowired
    private UserEntityRepository userEntityRepository;

    public static void main(final String[] args) {
        SpringApplication.run(BotBoot.class, args);
    }

    @PostConstruct
    public void printApplicationReadyMessage() {
        userEntityRepository.count();

        discordService.whisperToUser(authorUserId, "Reboot successful");
    }

    @Bean
    public DiscordApi discordApi() {
        final DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();
        logger.info("Succesfully logged in. Invitation link: " + api.createBotInvite());
        return api;
    }
}
