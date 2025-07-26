package com.wildduel.game;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TpaRequest {

    private final UUID requester;
    private final UUID target;
    private final long requestTime;
    private BukkitTask timeoutTask;

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

    public BukkitTask getTimeoutTask() {
        return timeoutTask;
    }

    public void setTimeoutTask(BukkitTask timeoutTask) {
        this.timeoutTask = timeoutTask;
    }

    public void cancelTimeoutTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
    }
}
