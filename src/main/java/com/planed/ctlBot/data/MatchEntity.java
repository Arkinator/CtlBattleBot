package com.planed.ctlBot.data;


import com.planed.ctlBot.common.GameStatus;
import com.planed.ctlBot.common.Race;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.sql.DataSourceDefinitions;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@NoArgsConstructor
@Table(name = "MATCHES")
public class MatchEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long matchId;

    @Column
    private String playerA;
    @Column
    private String playerB;
    @Column(nullable = false)
    private GameStatus gameStatus;
    @Column
    private Integer finalScorePlayerA;
    @Column
    private Integer finalScorePlayerB;
    @Column
    private Race racePlayerA;
    @Column
    private Race racePlayerB;

    @Column
    private Integer playerAreportedScoreForPlayerA;
    @Column
    private Integer playerAreportedScoreForPlayerB;
    @Column
    private Integer playerBreportedScoreForPlayerA;
    @Column
    private Integer playerBreportedScoreForPlayerB;

    @Column
    private String originatingServerId;
    @Column
    private String originatingChannelId;
}
