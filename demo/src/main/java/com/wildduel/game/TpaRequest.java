package com.wildduel.game;

import java.util.UUID;

public class TpaRequest {

    private final UUID requester;
    private final UUID target;
    private final long requestTime;

    public TpaRequest(UUID requester, UUID target) {
        this.requester = requester;
        this.target = target;
        this.requestTime = System.currentTimeMillis();
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public long getRequestTime() {
        return requestTime;
    }
}
