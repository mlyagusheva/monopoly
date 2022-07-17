package com.game.monopoly.entity;

import com.game.monopoly.entity.embedded.PlayerUniqueName;
import com.game.monopoly.enums.PlayerColour;
import com.game.monopoly.enums.PlayerRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

import static com.game.monopoly.constants.InitialGameValue.INITIAL_BALANCE;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "player")
public class Player {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Embedded
    private PlayerUniqueName uniqueName;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "role", nullable = false)
    @Enumerated(STRING)
    private PlayerRole role;

    @Column(name = "colour", nullable = false)
    @Enumerated(STRING)
    private PlayerColour colour;

    @Column(name = "balance", nullable = false)
    private Long balance = INITIAL_BALANCE;
}
