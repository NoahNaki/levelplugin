# LuxDialogues dialogue HUD resource-pack reference

The requested `references/luxdialogues/LuxDialogues-source.zip` was not present in this workspace. The available trimmed reference zip was `references/luxdialogues/LuxDialogues-important-no-assets.zip`; it intentionally omits binary PNG/font assets and keeps the YAML/font-generation evidence needed for HUD ascent parity.

## Reference paths inspected

- `LuxDialogues-important-only/reference-resources/Pack/Images/images.yml` lines 1-78: image IDs, source PNG names, `is-arrow`, `reduction-ratio`, and Lux image `ascent` values.
- `LuxDialogues-important-only/reference-resources/Pack/Lines/lines.yml` lines 1-15: text font ascent/count/spacing values for character name, dialogue lines, info line, and answer lines.
- `LuxDialogues-important-only/reference-resources/Dialogues/kingdom_example.yml` lines 39-47: the kingdom example maps dialogue image roles to the `kingdom-*` image IDs.
- `LuxDialogues-important-only/javap-readable-bytecode/org_aselstudios_luxdialogues_Utils_ResourceUtil.javap.txt` lines 90-120: reads `Pack/Lines/lines.yml` values.
- `LuxDialogues-important-only/javap-readable-bytecode/org_aselstudios_luxdialogues_Utils_ResourceUtil.javap.txt` lines 143-184: generates character/dialogue/answer font files and computes `lineAscent = baseAscent - ((lineNumber - 1) * space)`.
- `LuxDialogues-important-only/javap-readable-bytecode/org_aselstudios_luxdialogues_Utils_ResourceUtil.javap.txt` lines 290-380: reads `Pack/Images/images.yml`, calculates image width/height from `reduction-ratio`, and starts bitmap provider generation.
- `LuxDialogues-important-only/javap-readable-bytecode/org_aselstudios_luxdialogues_Utils_ResourceUtil.javap.txt` lines 956-1135: loads `Pack/Lines/example_font.json` and replaces its `ascent` placeholders while generating line font JSON.
- `LuxDialogues-important-only/README_CODEX.md` lines 20-27: confirms binary PNG assets were removed from the reference zip and should come from the existing Nexo external pack.

## Extracted image values

Lux `ResourceUtil` derives bitmap height as `256 / reduction-ratio`; all provided image entries use `reduction-ratio: 1`, so the generated LevelPlugin image providers use height `256`.

| id | file | glyph | ascent | height | reduction-ratio |
| --- | --- | --- | ---: | ---: | ---: |
| dialogue-background | dialogue.png | `U+E100` | 37 | 256 | 1 |
| answer-background | answer.png | `U+E101` | 81 | 256 | 1 |
| hand | hand.png | `U+E102` | -1 | 256 | 1 |
| name-start | name_start.png | `U+E103` | 43 | 256 | 1 |
| name-mid | name_mid.png | `U+E104` | 43 | 256 | 1 |
| name-end | name_end.png | `U+E105` | 43 | 256 | 1 |
| fog | fog.png | `U+E106` | 185 | 256 | 1 |
| character-background | character.png | `U+E107` | 60 | 256 | 1 |
| kingdom-hand | kingdom_hand.png | `U+E108` | -1 | 256 | 1 |
| kingdom-dialogue | kingdom_dialogue.png | `U+E109` | 45 | 256 | 1 |
| kingdom-answer | kingdom_answer.png | `U+E10A` | 83 | 256 | 1 |
| kingdom-character | kingdom_character.png | `U+E10B` | 60 | 256 | 1 |
| kingdom-name-start | kingdom_name_start.png | `U+E10C` | 43 | 256 | 1 |
| kingdom-name-mid | kingdom_name_mid.png | `U+E10D` | 43 | 256 | 1 |
| kingdom-name-end | kingdom_name_end.png | `U+E10E` | 43 | 256 | 1 |

## Extracted line values

| group | count | base ascent | space | generated ascents |
| --- | ---: | ---: | ---: | --- |
| Character-Name | 1 | 40 | n/a | 40 |
| Dialogue-Lines | 5 | 25 | 9 | 25, 16, 7, -2, -11 |
| Information-Line | 1 | 0 | n/a | 0 |
| Answer-Lines | 3 | 75 | 9 | 75, 66, 57 |
