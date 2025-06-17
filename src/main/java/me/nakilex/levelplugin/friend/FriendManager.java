package me.nakilex.levelplugin.friend;

import java.util.*;

/**
 * Simple manager storing mutual friendships between players.
 */
public class FriendManager {
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    private final Map<UUID, UUID> requests = new HashMap<>(); // invitee -> inviter

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
        if (requests.containsKey(b)) return false; // already requested
        requests.put(b, a);
        return true;
    }

    /** Get pending request inviter for the given player. */
    public UUID getRequest(UUID invitee) {
        return requests.get(invitee);
    }

    /** Accept any pending request for the invitee. */
    public boolean acceptRequest(UUID invitee) {
        UUID inviter = requests.remove(invitee);
        if (inviter == null) return false;
        addFriend(inviter, invitee);
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
