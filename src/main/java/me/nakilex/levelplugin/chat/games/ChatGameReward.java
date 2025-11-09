package me.nakilex.levelplugin.chat.games;

/** Simple data holder for chat game rewards. */
public record ChatGameReward(int coins, int experience, int intellect) {

    public boolean isEmpty() {
        return coins <= 0 && experience <= 0 && intellect <= 0;
    }
}
