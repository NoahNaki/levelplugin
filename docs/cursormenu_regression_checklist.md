# CursorMenu Regression Checklist

Use this checklist after any CursorMenu changes:

- [ ] Native menu format loads (`menus:` root).
- [ ] Imported CustomScreenMenu format loads (top-level menu keys + `layout`).
- [ ] Auto-commands execute when menu opens.
- [ ] Delayed commands run at configured delays.
- [ ] Condition checks block execution when false.
- [ ] Random commands select based on weighted chances.
- [ ] Next-menu transitions open the target menu.
- [ ] Stop-menu closes menu and teleport semantics are correct.
- [ ] Item preview supports imported item fields (material, cmd, glow, rotate, offset, scale).
- [ ] Sound one-shot and loop playback stop correctly on close.
- [ ] Join-run opens menu/commands after delay.
- [ ] Command whitelist blocks non-allowed commands during menu mode.
- [ ] Creature spawn protection blocks spawns near configured menu cameras.
- [ ] Camera block-check clears obstructions and restores blocks on close.
