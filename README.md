# LevelPlugin

This plugin manages town and building progression. Building stages can now be loaded from FastAsyncWorldEdit (FAWE) `.schem` files. Add a `schem: <file>` entry under each stage in `buildingstages.yml` and place the schematic file inside the plugin's data folder. When a schematic is specified the stage will paste it using FAWE instead of the huge `blocks` list.

