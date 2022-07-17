package com.game.monopoly.service.impl;

import com.game.monopoly.dao.ChatDAO;
import com.game.monopoly.dao.PlayerDAO;
import com.game.monopoly.dao.SessionDAO;
import com.game.monopoly.entity.Message;
import com.game.monopoly.entity.Player;
import com.game.monopoly.entity.Session;
import com.game.monopoly.entity.embedded.PlayerUniqueName;
import com.game.monopoly.exception.ResourceNotFoundException;
import com.game.monopoly.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.game.monopoly.constants.ErrorMessage.PLAYER_NOT_FOUND;
import static com.game.monopoly.constants.ErrorMessage.SESSION_NOT_FOUND;
import static com.game.monopoly.enums.MessageType.COMMON;
import static com.game.monopoly.enums.MessageType.SINGLE;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatDAO chatDAO;
    private final SessionDAO sessionDAO;
    private final PlayerDAO playerDAO;

    @Override
    public void saveCommonMessage(String sessionId, String sender, String message) {
        Session session = sessionDAO.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND));
        Player player = playerDAO.findPlayerByUniqueName(new PlayerUniqueName(sessionId, sender))
                .orElseThrow(() -> new ResourceNotFoundException(PLAYER_NOT_FOUND));

        Message playerMessage = new Message()
                .setContent(message)
                .setSender(player)
                .setType(COMMON);

        chatDAO.save(playerMessage);
        session.getMessages()
                .add(playerMessage);
    }

    ;

    @Override
    public void saveSingleMessage(String sessionId, String sender, String receiver, String message) {
        Session session = sessionDAO.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(SESSION_NOT_FOUND));
        Player sendingPlayer = playerDAO.findPlayerByUniqueName(new PlayerUniqueName(sessionId, sender))
                .orElseThrow(() -> new ResourceNotFoundException(PLAYER_NOT_FOUND));
        Player receivingPlayer = playerDAO.findPlayerByUniqueName(new PlayerUniqueName(sessionId, receiver))
                .orElseThrow(() -> new ResourceNotFoundException(PLAYER_NOT_FOUND));

        Message playerMessage = new Message()
                .setContent(message)
                .setSender(sendingPlayer)
                .setReceiver(receivingPlayer)
                .setType(SINGLE);

        chatDAO.save(playerMessage);
        session.getMessages()
                .add(playerMessage);
    }

    ;
}
