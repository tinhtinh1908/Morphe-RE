// SPDX-License-Identifier: GPL-3.0-only
package vn.dtinh.messenger;

/** Process-local retry state. Stores an account ID, never a push token. */
public final class RegistrationPolicy {
    private static final long[] BACKOFF = {20000, 60000, 120000, 300000};
    private String completedUser;
    private String lastUser;
    private int failures;
    private long nextAttempt;
    private boolean running;

    public synchronized boolean begin(long now, String user) {
        if (running || (user != null && user.equals(completedUser))) return false;
        if (user != null && !user.equals(lastUser)) {
            failures = 0;
            nextAttempt = 0;
            lastUser = user;
        }
        if (now < nextAttempt) return false;
        running = true;
        return true;
    }

    public synchronized void tokenReady(String user) {
        if (user != null && !user.isEmpty()) completedUser = user;
    }

    public synchronized boolean finish(long now, String user) {
        running = false;
        if (user != null && user.equals(completedUser)) {
            failures = 0;
            nextAttempt = 0;
            return false;
        }
        long delay = BACKOFF[Math.min(failures, BACKOFF.length - 1)];
        if (failures < BACKOFF.length) failures++;
        nextAttempt = now + delay;
        return true;
    }

    public synchronized long delay(long now) {
        return Math.max(20000, nextAttempt - now);
    }
}
