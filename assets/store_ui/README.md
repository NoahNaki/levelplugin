# Compact store UI source

The `backgrounds/`, `buttons/`, `cart/`, `gems/`, and `misc/` images are the
complete compact v7 source set supplied for this UI. Containers, artwork,
badges, mini-buttons, and small icons are all composed from these transparent
source layers; no artwork is inferred from the generated canvas.

Regenerate the 156 Nexo slices from the repository root with:

```powershell
javac -d target/store-ui-tools scratchpad/GenerateStoreDialogAssets.java
java -cp target/store-ui-tools GenerateStoreDialogAssets assets/store_ui src/main/resources/resourcepack/assets/minecraft/textures/store/dialog/slices src/main/resources/resourcepack/nexo/glyphs/store_dialog.yml
```

The generator deliberately retains the 730x350 canvas, 240-pixel chunks,
nine-pixel rows, and alpha=1 width pins. Do not crop the generated slices.
