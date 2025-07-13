# LevelPlugin

This plugin manages town and building progression. Building stages can be saved and loaded from FastAsyncWorldEdit (FAWE) `.schem` files stored in the plugin's `schematics` folder.

Use `/buildingstage schem <building> <level> <stage>` to export the selected region as a schematic and register the stage automatically. The resulting `buildingstages.yml` entry references the schematic file instead of thousands of block lines.

