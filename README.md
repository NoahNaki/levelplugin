# Level Plugin

This plugin lets each player have a client-side settlement. To create a town and buildings:

1. **Select the area** for the town using `/townpos1` and `/townpos2`.
2. Run `/townstage create <town>` while standing where you want the origin.
3. For buildings, select their area with `/buildingstage wand` or the pos commands and run
   `/buildingstage create <town> <building> <level> <stage>` while standing at the origin.
4. A player starts their personal town with `/town start <town>`.
   They are teleported to `2010 -59 -1242` in `flatland` and the structures appear.
5. Upgrade a building by right‑clicking its hologram (it costs 1 oak log in this example).
6. Use `/town reset` to remove your settlement if you want to start over.

### Testing
* Ensure the plugin is built with `mvn -DskipTests package` and load it on a test server.
* Run `/town start <town>` and confirm you teleport and see the town spawn at the fixed location.
