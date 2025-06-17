package me.nakilex.levelplugin.friend;

import java.util.*;

/**
 * Simple manager storing mutual friendships between players.
 */
public class FriendManager {
    private static final long EXPIRY = 5 * 60 * 1000L; // 5 minutes
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    private final Map<UUID, Request> requests = new HashMap<>(); // invitee -> request

    private static class Request {
        final UUID inviter;
        final long time;
        Request(UUID inviter) {
            this.inviter = inviter;
            this.time = System.currentTimeMillis();
        }
    }

    private Set<UUID> getList(UUID id) {
        return friends.computeIfAbsent(id, k -> new HashSet<>());
    }

    /** Add two players as friends. */
    public boolean addFriend(UUID a, UUID b) {
        if (a.equals(b)) return false;
        Set<UUID> fa = getList(a);
        if (!fa.add(b)) return false;
        getList(b).add(a);
        return true;
    }

    /** Send a friend request from a -> b. */
    public boolean sendRequest(UUID a, UUID b) {
        if (a.equals(b) || areFriends(a, b)) return false;
        Request existing = requests.get(b);
        if (existing != null) {
            if (System.currentTimeMillis() - existing.time < EXPIRY) return false;
            requests.remove(b);
        }
        requests.put(b, new Request(a));
        return true;
    }

    /** Get pending request inviter for the given player, or null if none/expired. */
    public UUID getRequest(UUID invitee) {
        Request r = requests.get(invitee);
        if (r == null) return null;
        if (System.currentTimeMillis() - r.time > EXPIRY) {
            requests.remove(invitee);
            return null;
        }
        return r.inviter;
    }

    /** Accept any pending request for the invitee. */
    public boolean acceptRequest(UUID invitee) {
        Request r = requests.remove(invitee);
        if (r == null) return false;
        if (System.currentTimeMillis() - r.time > EXPIRY) {
            return false;
        }
        addFriend(r.inviter, invitee);
        return true;
    }

    /** Deny a pending request. */
    public boolean denyRequest(UUID invitee) {
        return requests.remove(invitee) != null;
    }

    /** Remove the friendship between two players. */
    public boolean removeFriend(UUID a, UUID b) {
        Set<UUID> fa = getList(a);
        Set<UUID> fb = getList(b);
        boolean removed = fa.remove(b);
        fb.remove(a);
        return removed;
    }

    /** Get the set of friends for the given player. */
    public Set<UUID> getFriends(UUID id) {
        return Collections.unmodifiableSet(getList(id));
    }

    /** Check if two players are friends. */
    public boolean areFriends(UUID a, UUID b) {
        return getList(a).contains(b);
    }
}
