# Dialogue HUD font/glyph registry

This registry freezes the static Lux-style dialogue HUD baseline. The renderer uses **one generic image font** (`levelplugin_dialogue:dialogue`) plus line-specific text fonts and `levelplugin_dialogue:offset_chars`. Do not add split image fonts unless the renderer is intentionally migrated as one synchronized change.

## Verification summary

- Renderer image font references resolve to `levelplugin_dialogue:dialogue` through `DialogueGlyphs.DIALOGUE_FONT_TAG` and its aliases.
- Every Java image glyph constant used by the renderer has a matching provider in `assets/levelplugin_dialogue/font/dialogue.json`.
- The negative/positive offset glyphs are synchronized as `U+F800 = -1 px` and `U+F801 = +1 px`.
- Text fonts referenced by the renderer have JSON files in `assets/levelplugin_dialogue/font/`.
- The bundled fragment intentionally contains JSON only; PNG assets must exist in the Nexo external pack under `assets/levelplugin_dialogue/textures/`.

## Image glyph font: `levelplugin_dialogue:dialogue`

| Renderer use | Java constant | Glyph | Texture file | Lux ascent | LevelPlugin ascent | LevelPlugin height | Renderer location |
| --- | --- | --- | --- | ---: | ---: | ---: | --- |
| Dialogue background | `DialogueGlyphs.DIALOGUE_BACKGROUND` | `U+E100` | `textures/dialogue/dialogue_background.png` | 37 | 37 | 57 | `ActionBarDialogueRenderer.renderMiniMessage` dialogue background layer |
| Answer background | `DialogueGlyphs.ANSWER_BACKGROUND` | `U+E101` | `textures/dialogue/answer_background.png` | 81 | 81 | 37 | `ActionBarDialogueRenderer.answerPreview` |
| Hand / arrow | `DialogueGlyphs.HAND` / `ARROW` | `U+E102` | `textures/dialogue/arrow.png` | -1 | -1 | 8 | `ActionBarDialogueRenderer.handLayer` |
| Name start | `DialogueGlyphs.NAME_START` | `U+E103` | `textures/dialogue/name_start.png` | 43 | 43 | 14 | `ActionBarDialogueRenderer.nameBoxLayer` |
| Name middle | `DialogueGlyphs.NAME_MID` | `U+E104` | `textures/dialogue/name_mid.png` | 43 | 43 | 14 | `ActionBarDialogueRenderer.nameBoxLayer` |
| Name end | `DialogueGlyphs.NAME_END` | `U+E105` | `textures/dialogue/name_end.png` | 43 | 43 | 14 | `ActionBarDialogueRenderer.nameBoxLayer` |
| Fog | `DialogueGlyphs.FOG` | `U+E106` | `textures/dialogue/fog.png` | 185 | 185 | 256 | `ActionBarDialogueRenderer.renderMiniMessage` fog layer |
| Character background | `DialogueGlyphs.CHARACTER_BACKGROUND` | `U+E107` | `textures/dialogue/character_background.png` | 60 | 60 | 32 | `ActionBarDialogueRenderer.renderMiniMessage` character layer |
| Kingdom hand | `DialogueGlyphs.KINGDOM_HAND` | `U+E108` | `textures/dialogue/kingdom_hand.png` | -1 | -1 | 8 | Registered for parity; not used by static renderer yet |
| Kingdom dialogue | `DialogueGlyphs.KINGDOM_DIALOGUE` | `U+E109` | `textures/dialogue/kingdom_dialogue.png` | 45 | 45 | 57 | Registered for parity; not used by static renderer yet |
| Kingdom answer | `DialogueGlyphs.KINGDOM_ANSWER` | `U+E10A` | `textures/dialogue/kingdom_answer.png` | 83 | 83 | 37 | Registered for parity; not used by static renderer yet |
| Kingdom character | `DialogueGlyphs.KINGDOM_CHARACTER` | `U+E10B` | `textures/dialogue/kingdom_character.png` | 60 | 60 | 32 | Registered for parity; not used by static renderer yet |
| Kingdom name start | `DialogueGlyphs.KINGDOM_NAME_START` | `U+E10C` | `textures/dialogue/kingdom_name_start.png` | 43 | 43 | 14 | Registered for parity; not used by static renderer yet |
| Kingdom name middle | `DialogueGlyphs.KINGDOM_NAME_MID` | `U+E10D` | `textures/dialogue/kingdom_name_mid.png` | 43 | 43 | 14 | Registered for parity; not used by static renderer yet |
| Kingdom name end | `DialogueGlyphs.KINGDOM_NAME_END` | `U+E10E` | `textures/dialogue/kingdom_name_end.png` | 43 | 43 | 14 | Registered for parity; not used by static renderer yet |

## Text and line fonts

LuxDialogues line calculation is `lineAscent = baseAscent - ((lineNumber - 1) * space)`.

| Renderer use | Font key | Texture file | Lux value | LevelPlugin ascent | Height | Renderer location |
| --- | --- | --- | --- | ---: | ---: | --- |
| Default text fallback | `levelplugin_dialogue:levelplugin_dialogue_default` | `textures/font/levelplugin_dialogue_font.png` | n/a | 0 | 8 | Debug/default tests only |
| Dialogue line 1 | `levelplugin_dialogue:levelplugin_dialogue_line_1` | `textures/font/levelplugin_dialogue_font.png` | 25 | 25 | 8 | `ActionBarDialogueRenderer.dialogueLines` |
| Dialogue line 2 | `levelplugin_dialogue:levelplugin_dialogue_line_2` | `textures/font/levelplugin_dialogue_font.png` | 25 - 9 = 16 | 16 | 8 | `ActionBarDialogueRenderer.dialogueLines` |
| Dialogue line 3 | `levelplugin_dialogue:levelplugin_dialogue_line_3` | `textures/font/levelplugin_dialogue_font.png` | 25 - 18 = 7 | 7 | 8 | `ActionBarDialogueRenderer.dialogueLines` |
| Dialogue line 4 | `levelplugin_dialogue:levelplugin_dialogue_line_4` | `textures/font/levelplugin_dialogue_font.png` | 25 - 27 = -2 | -2 | 8 | `ActionBarDialogueRenderer.dialogueLines` |
| Dialogue line 5 | `levelplugin_dialogue:levelplugin_dialogue_line_5` | `textures/font/levelplugin_dialogue_font.png` | 25 - 36 = -11 | -11 | 8 | `ActionBarDialogueRenderer.dialogueLines` |
| Answer line 1 | `levelplugin_dialogue:levelplugin_dialogue_answer_1` | `textures/font/levelplugin_dialogue_font.png` | 75 | 75 | 8 | `ActionBarDialogueRenderer.answerPreview` |
| Answer line 2 | `levelplugin_dialogue:levelplugin_dialogue_answer_2` | `textures/font/levelplugin_dialogue_font.png` | 75 - 9 = 66 | 66 | 8 | `ActionBarDialogueRenderer.answerPreview` |
| Answer line 3 | `levelplugin_dialogue:levelplugin_dialogue_answer_3` | `textures/font/levelplugin_dialogue_font.png` | 75 - 18 = 57 | 57 | 8 | `ActionBarDialogueRenderer.answerPreview` |
| Character name | `levelplugin_dialogue:levelplugin_dialogue_character_name` | `textures/font/levelplugin_dialogue_font.png` | 40 | 40 | 8 | `ActionBarDialogueRenderer.textLayer` for character name |
| Info line | `levelplugin_dialogue:levelplugin_dialogue_info` | `textures/font/levelplugin_dialogue_font.png` | 0 | 0 | 8 | `ActionBarDialogueRenderer.infoLine` |

## Offset font

| Renderer use | Font key | Glyph | Advance | Renderer location |
| --- | --- | --- | ---: | --- |
| Move cursor left | `levelplugin_dialogue:offset_chars` | `U+F800` | -1 | `ActionBarDialogueRenderer.offset` |
| Move cursor right | `levelplugin_dialogue:offset_chars` | `U+F801` | +1 | `ActionBarDialogueRenderer.offset` |

## Mismatches corrected in this stabilization pass

| Problem | Fix |
| --- | --- |
| Negative offset JSON exposed `U+F802`, while Java renders `U+F800`; this created placeholder boxes anywhere a negative cursor move was used. | `offset_chars.json` now exposes `U+F800 = -1` and `U+F801 = +1`. |
| Generic image font pointed at Lux source filenames (`dialogue.png`, `answer.png`, `hand.png`, `character.png`) while the restored LevelPlugin/Nexo asset baseline uses `dialogue_background.png`, `answer_background.png`, `arrow.png`, and `character_background.png`. | `dialogue.json` now points at the restored LevelPlugin texture aliases. |
| Image providers used height `256` for small cropped HUD assets, producing giant detached blocks when the asset was not a 256px Lux canvas. | Small HUD assets now use the restored display heights: dialogue `57`, answer `37`, character `32`, arrow `8`, name pieces `14`; fog remains `256`. |
| Fog inherited the dialogue background color, so a colored dialogue panel could tint the full-screen fog block. | `DialogueRenderContext` now has a dedicated `fogColor`, defaulting to black and reading the dialogue `Colors.fog` value. |
