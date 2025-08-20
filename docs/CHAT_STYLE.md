# Chat Message Style Guide

Consistent chat styling improves clarity. Use `ChatMessageUtil` for standard color and prefix formatting.

## Message Types

| Type    | Color          | Prefix            | Purpose                |
|---------|----------------|------------------|------------------------|
| INFO    | white          | none             | Neutral information    |
| SUCCESS | green          | none             | Confirmed actions      |
| WARNING | yellow         | none             | Cautionary notices     |
| ERROR   | red            | none             | Failures or mistakes   |
| REWARD  | gold           | `<glyph:star>`   | Rewards and payouts    |

Example usage:

```java
ChatMessageUtil.send(player, MessageType.SUCCESS, "Quest complete!");
player.sendMessage(ChatMessageUtil.format(MessageType.ERROR, "Invalid input."));
```

## Centered vs. Inline

`ChatMessageUtil.send` indents messages for readability. To center a message, format it and pass it to `ChatFormatter.sendCenteredMessage`:

```java
ChatFormatter.sendCenteredMessage(player,
        ChatMessageUtil.format(MessageType.INFO, "Welcome!"));
```

Use `ChatFormatter.getCenteredText` if you need the centered string without sending it immediately.
