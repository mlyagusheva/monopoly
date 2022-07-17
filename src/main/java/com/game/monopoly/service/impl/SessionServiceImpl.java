package com.game.monopoly.service.impl;

import com.game.monopoly.dao.CardStateDAO;
import com.game.monopoly.dao.SessionDAO;
import com.game.monopoly.dto.response.BuyCardDTO;
import com.game.monopoly.dto.response.RollDiceResultDTO;
import com.game.monopoly.entity.CardState;
import com.game.monopoly.entity.CompanyCard;
import com.game.monopoly.entity.Player;
import com.game.monopoly.entity.Session;
import com.game.monopoly.exception.ResourceAlreadyExistsException;
import com.game.monopoly.exception.ResourceNotFoundException;
import com.game.monopoly.helper.PlayerPositionHelper;
import com.game.monopoly.helper.RandomHelper;
import com.game.monopoly.helper.SortHelper;
import com.game.monopoly.mapper.CardActionMapper;
import com.game.monopoly.mapper.RoleDicesMapper;
import com.game.monopoly.service.PlayerService;
import com.game.monopoly.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.game.monopoly.constants.ErrorMessage.*;
import static com.game.monopoly.constants.InitialGameValue.INITIAL_CURRENT_PLAYER_NAME;
import static com.game.monopoly.constants.PlayingFieldParam.MAX_BORDER;
import static com.game.monopoly.constants.PlayingFieldParam.MIN_BORDER;
import static com.game.monopoly.enums.PlayerRole.ADMIN;
import static com.game.monopoly.enums.PlayerRole.USER;
import static com.game.monopoly.enums.SessionState.IN_PROGRESS;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {
    private final SessionDAO sessionDAO;
    private final CardStateDAO cardStateDAO;
    private final PlayerService playerService;

    @Transactional(readOnly = true)
    @Override
    public Session getSession(String sessionId) {
        return sessionDAO.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public void checkSessionExists(String sessionId) {
        if (sessionDAO.existsSessionById(sessionId)) {
            throw new ResourceAlreadyExistsException(SESSION_ALREADY_EXISTS);
        }
    }

    @Transactional
    @Override
    public void saveSession(String sessionId, String playerName, String colour, List<CardState> cardStates) {
        playerService.savePlayer(sessionId, playerName, colour, ADMIN);
        Player player = playerService.getPlayer(sessionId, playerName);

        Session session = new Session()
                .setId(sessionId)
                .setCurrentPlayer(INITIAL_CURRENT_PLAYER_NAME)
                .setCardStates(cardStates);
        session.getPlayers().add(player);
        sessionDAO.save(session);
    }

    @Transactional
    @Override
    public Player addPlayerToSession(String sessionId, String playerName, String colour) {
        playerService.savePlayer(sessionId, playerName, colour, USER);
        Session session = getSession(sessionId);
        Player player = playerService.getPlayer(sessionId, playerName);
        session.getPlayers().add(player);

        return player;
    }

    @Transactional
    @Override
    public RollDiceResultDTO rollDices(String sessionId, String playerName) {
        Player player = playerService.getPlayer(sessionId, playerName);

        int firstRoll = RandomHelper.getRandomDiceValue(MIN_BORDER, MAX_BORDER);
        int secondRoll = RandomHelper.getRandomDiceValue(MIN_BORDER, MAX_BORDER);
        int newPosition = PlayerPositionHelper.getNewPosition(player.getPosition(), firstRoll, secondRoll);

        playerService.updatePlayerPosition(newPosition, sessionId, playerName);

        return RoleDicesMapper.rollResultTODTO(List.of(firstRoll, secondRoll), playerName, newPosition);
    }

    @Transactional
    @Override
    public void startGame(String sessionId, String nextPlayer) {
        sessionDAO.updateSessionStateAndCurrentPlayer(IN_PROGRESS, nextPlayer, sessionId);
    }

    @Transactional
    @Override
    public BuyCardDTO buyCard(String sessionId, String playerName, Long cardId) {
        Session session = getSession(sessionId);
        CardState cardState = session.getCardStates()
                .stream()
                .filter(cs -> cs.getCard().getId() == cardId)
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
        CompanyCard card = cardState.getCard();
        Player player = playerService.getPlayer(sessionId, playerName);

        Integer level = cardState.getLevel();
        Long fine = card.getFines().get(level).getValue();
        Long newBalance = player.getBalance() - card.getPrice();

        cardStateDAO.saveAndFlush(cardState
                .setCurrentFine(fine)
                .setLevel(level + 1)
                .setOwnerName(playerName));
        playerService.updatePlayerBalance(newBalance, sessionId, playerName);

        return CardActionMapper.cardActionTODTO(playerName, newBalance, cardState);
    }

    @Transactional
    @Override
    public String getNextPlayer(String sessionId, String previousPLayer) {
        List<Player> players = SortHelper.getSortedPlayers(getSession(sessionId).getPlayers());
        int count = players.size();
        Player nextPlayer = null;

        for (int i = 0; i < count; i++) {
            if (previousPLayer.equals(players.get(i).getUniqueName().getName())) {
                nextPlayer = i == count - 1 ? players.get(0) : players.get(i + 1);
                break;
            }
        }
        String nextPlayerName = nextPlayer.getUniqueName().getName();
        sessionDAO.updateCurrentPlayer(nextPlayerName, sessionId);

        return nextPlayerName;
    }

}
