package me.nakilex.levelplugin.party;

import java.util.List;
import java.util.UUID;

/**
 * Callback interface for components that need to respond to party membership
 * changes. This allows systems such as the arena queue to react immediately
 * when parties gain or lose members without tightly coupling to the command
 * implementation.
 */
public interface PartyMembershipListener {

    /**
     * Invoked whenever a party's membership changes (members joining, leaving
     * or a leader swap). The provided party reflects the latest state.
     */
    void onPartyMembersChanged(Party party);

    /**
     * Invoked when a party is disbanded. The list contains the members prior
     * to disbanding so listeners can perform any necessary cleanup.
     */
    void onPartyDisbanded(List<UUID> formerMembers);
}

