# LevelPlugin

This plugin manages town and building progression. Stage data can be exported to
FastAsyncWorldEdit (FAWE) `.schem` files stored in the plugin's `schematics`
folder.

Use `/buildingstage schem <building> <level> <stage>` or `/townstage schem <town> <level>`
to export the selected region as a schematic and register the stage automatically.
The resulting YAML files reference the schematic instead of thousands of block
lines.

