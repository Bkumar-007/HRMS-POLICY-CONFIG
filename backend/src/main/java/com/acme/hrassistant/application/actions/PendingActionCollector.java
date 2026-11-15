package com.acme.hrassistant.application.actions;

import com.acme.hrassistant.domain.PendingAction;
import com.acme.hrassistant.web.dto.ChatResponse;
import java.util.ArrayList;
import java.util.List;

/** Per-chat-request collector for pending actions created by write tools. */
public class PendingActionCollector {

    private final List<ChatResponse.PendingActionDto> actions = new ArrayList<>();

    public void add(PendingAction pending) {
        actions.add(new ChatResponse.PendingActionDto(
                pending.getId(),
                pending.getType().name(),
                pending.getSummary()));
    }

    public List<ChatResponse.PendingActionDto> getActions() {
        return List.copyOf(actions);
    }
}
