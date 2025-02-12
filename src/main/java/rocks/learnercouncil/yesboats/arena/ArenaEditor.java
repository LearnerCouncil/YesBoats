package rocks.learnercouncil.yesboats.arena;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.Messages;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.*;

import static rocks.learnercouncil.yesboats.Messages.Editor;
import static rocks.learnercouncil.yesboats.Messages.Editor.Items;
import static rocks.learnercouncil.yesboats.Messages.Editor.Validator;
import static rocks.learnercouncil.yesboats.Messages.formattedTime;

public class ArenaEditor {

    private static final YesBoats plugin = YesBoats.getPlugin();

    public static HashMap<Player, ArenaEditor> editors = new HashMap<>();
    public final Arena arena;
    private final LinkedList<ItemStack> editorItems = new LinkedList<>();
    private final List<Boat> startBoats = new LinkedList<>();
    private final HashMap<Block, Material> oldLightMaterials = new HashMap<>();
    private final ItemStack[] playerInv;
    private final Player player;
    private Vector boxCorner1 = null, boxCorner2 = null;
    private BoundingBox createdBox;
    private BoundingBox selectedBox;
    private BukkitTask displayTask;
    private boolean offsetDisplayBox = false;

    public ArenaEditor(Player player, Arena arena) {
        this.player = player;
        playerInv = player.getInventory().getContents();
        this.arena = arena;
        if (this.arena.world == null) this.arena.world = player.getWorld();
        initializeInventory();
        startBoxDisplay();
        spawnStartBoats();
    }

    protected void spawnStartBoats() {
        startBoats.forEach(boat -> {
            if (boat.isInsideVehicle()) Objects.requireNonNull(boat.getVehicle()).remove();
            boat.remove();
        });
        startBoats.clear();
        arena.startLocations.forEach(location -> {
            ArmorStand stand = (ArmorStand) arena.world.spawnEntity(location, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(true);
            Boat boat = (Boat) arena.world.spawnEntity(location, EntityType.BOAT);
            stand.addPassenger(boat);
            startBoats.add(boat);
        });
    }

    /**
     * Initializes the player's inventory with all the items used for editing.
     */
    protected void initializeInventory() {
        player.getInventory().clear();
        editorItems.clear();
        Inventory inv = player.getInventory();

        inv.setItem(0, getItem(Material.ENDER_CHEST, Items.REGENERATE));

        inv.setItem(1, getItem(Material.IRON_AXE, Items.SELECTOR));

        inv.setItem(2, getItem(Material.BARRIER, Items.DEATH_BARRIER));

        inv.setItem(3, getItem(Material.LIGHT_BLUE_BANNER, Items.CHECKPOINT));

        inv.setItem(27, getItem(Material.RED_CARPET, arena.minPlayers, Items.MIN_PLAYERS));

        inv.setItem(28, getItem(Material.SPECTRAL_ARROW, arena.laps, Items.LAPS));

        inv.setItem(29, getItem(Material.CLOCK, Items.TIME.formattedName(formattedTime(arena.time))));

        inv.setItem(4, getItem(Material.OAK_BOAT, Items.START));

        inv.setItem(5, getItem(Material.ENDER_PEARL, Items.LOBBY));

        inv.setItem(6, getItem(Material.REDSTONE_BLOCK, Items.START_LINE));

        inv.setItem(7, getItem(Material.REDSTONE_LAMP, Items.LIGHT));

        inv.setItem(16, getItem(Material.RED_CONCRETE, Items.CANCEL));

        inv.setItem(17, getItem(Material.LIME_CONCRETE, Items.SAVE));

        inv.setItem(9, getItem(Material.FEATHER, Items.DEBUG));

    }

    private ItemStack getItem(Material material, Messages.Editor.Item item) {
        return getItem(material, 1, item);
    }

    /**
     * Gets an {@link ItemStack} from a {@link Material}, name, and lore.
     *
     * @param material The material of the item.
     * @param itemData The data of the item.
     * @return The ItemStack
     */
    private ItemStack getItem(Material material, int amount, Messages.Editor.Item itemData) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(itemData.name());
        meta.setLore(Arrays.asList(itemData.lore()));
        item.setItemMeta(meta);
        editorItems.add(item);
        return item;
    }

    private void setBoxCorner1(Vector boxCorner1) {
        this.boxCorner1 = boxCorner1;
        if (boxCorner2 != null) createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    private void setBoxCorner2(Vector boxCorner2) {
        this.boxCorner2 = boxCorner2;
        if (boxCorner1 != null) createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    /**
     * Adds a bounding box of the specified type to the {@link Arena}.
     *
     * @param type The {@link BoundingBoxType}.
     * @return whether the bounding box was added.
     */
    private boolean addBoundingBox(BoundingBoxType type) {
        if (selectedBox == null) {
            player.sendMessage(Editor.NO_BOX_SELECTED);
            return false;
        }
        BoundingBoxType selectedType;
        if (arena.checkpointBoxes.contains(selectedBox)) selectedType = BoundingBoxType.CHECKPOINT;
        else if (arena.deathBarriers.contains(selectedBox)) selectedType = BoundingBoxType.DEATH_BARRIER;
        else selectedType = BoundingBoxType.REMOVE;


        boxCorner1 = boxCorner2 = null;
        createdBox = null;

        if (selectedType == type) {
            player.sendMessage();
            return false;
        }
        switch (type) {
            case CHECKPOINT:
                if (selectedType == BoundingBoxType.DEATH_BARRIER) arena.deathBarriers.remove(selectedBox);
                arena.checkpointBoxes.add(selectedBox.clone());
                break;
            case DEATH_BARRIER:
                if (selectedType == BoundingBoxType.CHECKPOINT) {
                    arena.checkpointSpawns.remove(arena.checkpointBoxes.indexOf(selectedBox));
                    arena.checkpointBoxes.remove(selectedBox);
                }
                arena.deathBarriers.add(selectedBox.clone());
                break;
            case REMOVE:
                if (selectedType == BoundingBoxType.CHECKPOINT) {
                    arena.checkpointSpawns.remove(arena.checkpointBoxes.indexOf(selectedBox));
                    arena.checkpointBoxes.remove(selectedBox);
                }
                if (selectedType == BoundingBoxType.DEATH_BARRIER) arena.deathBarriers.remove(selectedBox);
                break;
        }
        return true;
    }

    /**
     * Starts displaying the bounding boxes of the {@link Arena}
     */
    private void startBoxDisplay() {
        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                offsetDisplayBox = !offsetDisplayBox;
                HashSet<BoundingBox> boxes = new HashSet<>();
                boxes.add(createdBox);
                boxes.addAll(arena.deathBarriers);
                boxes.addAll(arena.checkpointBoxes);

                boolean clearSelection = true;
                for (BoundingBox box : boxes) {
                    if (boxRaycast(box)) {
                        selectedBox = box;
                        displayBoundingBox(selectedBox, new Particle.DustOptions(Color.YELLOW, 1));
                        clearSelection = false;
                        break;
                    }
                }
                if (clearSelection) selectedBox = null;

                if (createdBox != null) displayBoundingBox(createdBox, new Particle.DustOptions(Color.WHITE, 1));
                arena.deathBarriers.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.RED, 1)));
                arena.checkpointBoxes.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.AQUA, 1)));
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private boolean boxRaycast(BoundingBox box) {
        if (box == null) return false;
        RayTraceResult rayTraceResult = box.rayTrace(
                player.getEyeLocation().toVector(),
                player.getEyeLocation().getDirection(),
                10
        );
        return rayTraceResult != null;
    }

    /**
     * Displays the specified {@link BoundingBox} using particles.
     *
     * @param box   the bounding box to display.
     * @param color the color and size on the particles in the for of a {@link Particle.DustOptions} object.
     */
    private void displayBoundingBox(BoundingBox box, Particle.DustOptions color) {
        Vector corner1 = box.getMin();
        Vector corner2 = box.getMax().add(new Vector(1, 1, 1));

        double offset = offsetDisplayBox ? 0.5 : 0;

        //x axis lines
        for (double x = corner1.getX() + offset; x <= corner2.getX(); x += 1) {
            spawnParticle(x, corner1.getY(), corner1.getZ(), color);
            spawnParticle(x, corner1.getY(), corner2.getZ(), color);
            spawnParticle(x, corner2.getY(), corner1.getZ(), color);
            spawnParticle(x, corner2.getY(), corner2.getZ(), color);
        }
        //y axis lines
        for (double y = corner1.getY() + offset; y <= corner2.getY(); y += 1) {
            spawnParticle(corner1.getX(), y, corner1.getZ(), color);
            spawnParticle(corner1.getX(), y, corner2.getZ(), color);
            spawnParticle(corner2.getX(), y, corner1.getZ(), color);
            spawnParticle(corner2.getX(), y, corner2.getZ(), color);
        }
        //z axis lines
        for (double z = corner1.getZ() + offset; z <= corner2.getZ(); z += 1) {
            spawnParticle(corner1.getX(), corner1.getY(), z, color);
            spawnParticle(corner1.getX(), corner2.getY(), z, color);
            spawnParticle(corner2.getX(), corner1.getY(), z, color);
            spawnParticle(corner2.getX(), corner2.getY(), z, color);
        }
    }

    private void spawnParticle(double x, double y, double z, Particle.DustOptions color) {
        player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, y, z), 1, color);
    }

    public void restore(boolean save) {
        if (save) {
            List<String> errors = validate();
            if (!errors.isEmpty()) {
                player.sendMessage(Editor.VALIDATION_FAILED.formatted("\n" + String.join("\n", errors)));
                return;
            }
            Optional<Arena> arenaOptional = Arena.get(arena.name);
            arenaOptional.ifPresent(Arena.arenas::remove);
            Arena.arenas.add(arena);
            player.sendMessage(Editor.SAVED);
        } else {
            oldLightMaterials.forEach(Block::setType);
            player.sendMessage(Editor.CANCELED);
        }
        player.getInventory().setContents(playerInv);
        player.setItemOnCursor(null);
        displayTask.cancel();
        editors.remove(player);
        boxCorner1 = null;
        boxCorner2 = null;
        selectedBox = null;
        createdBox = null;
        for (Boat boat : startBoats) {
            if (boat.isInsideVehicle()) Objects.requireNonNull(boat.getVehicle()).remove();
            boat.remove();
        }
        startBoats.clear();
    }

    private List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (arena.minPlayers < 1) errors.add(Validator.MIN_PLAYERS_TOO_LOW.formatted(arena.minPlayers));
        if (arena.laps < 1) errors.add(Validator.LAPS_TOO_LOW.formatted(arena.laps));
        if (arena.time < 30)
            errors.add(Validator.TIME_TOO_LOW.formatted(formattedTime(arena.time)));
        if (arena.time > 3600)
            errors.add(Validator.TIME_TOO_HIGH);
        if (arena.lobbyLocation == null) errors.add(Validator.NO_LOBBY_LOCATION);
        if (arena.world == null) errors.add(Validator.NO_WORLD);
        if (arena.startLineActivator == null) errors.add(Validator.NO_START_LINE_ACTIVATOR);
        if (arena.startLocations == null || arena.startLocations.isEmpty())
            errors.add(Validator.NO_START_LOCATIONS);
        if (arena.lightLocations == null || arena.lightLocations.isEmpty())
            errors.add(Validator.NO_LIGHT_LOCATIONS);
        if (arena.checkpointBoxes == null || arena.checkpointBoxes.size() < 2)
            errors.add(Validator.CHECKPOINTS_TOO_LOW.formatted(arena.checkpointBoxes == null ? 0 : arena.checkpointBoxes.size()));
        else if (arena.checkpointSpawns == null || arena.checkpointSpawns.size() != arena.checkpointBoxes.size()) {
            errors.add(Validator.CHECKPOINT_SPAWNS_INCONSISTENT.formatted(
                    arena.checkpointBoxes.size(),
                    (arena.checkpointSpawns == null ? 0 : arena.checkpointSpawns.size())
            ));
        }
        return errors;
    }

    private enum BoundingBoxType {
        DEATH_BARRIER, CHECKPOINT, REMOVE
    }

    public static class Events implements Listener {

        private final List<Player> settingCheckpoint = new ArrayList<>();

        @EventHandler
        public void onVehicleDestroy(VehicleDestroyEvent event) {
            if (!(event.getAttacker() instanceof Player)) return;
            if (!(event.getVehicle() instanceof Boat)) return;
            if (!editors.containsKey((Player) event.getAttacker())) return;

            ArenaEditor editor = editors.get((Player) event.getAttacker());
            if (editor.startBoats.contains((Boat) event.getVehicle())) {
                Boat boat = (Boat) event.getVehicle();
                if (boat.isInsideVehicle()) Objects.requireNonNull(boat.getVehicle()).remove();
                editor.arena.startLocations.remove(boat.getLocation());
            }
        }

        @EventHandler
        public void onVehicleEnter(VehicleEnterEvent event) {
            if (!(event.getEntered() instanceof Player)) return;
            if (editors.containsKey((Player) event.getEntered())) event.setCancelled(true);
        }

        @EventHandler
        public void onItemDrop(PlayerDropItemEvent event) {
            if (!editors.containsKey(event.getPlayer())) return;
            ArenaEditor editor = editors.get(event.getPlayer());
            ItemStack item = event.getItemDrop().getItemStack();
            if (!editor.editorItems.contains(item)) return;

            event.setCancelled(true);
            if (item.getType() == Material.IRON_AXE) editor.addBoundingBox(BoundingBoxType.REMOVE);
        }

        @EventHandler
        public void onInventoryClick(InventoryCreativeEvent event) {
            if (!editors.containsKey((Player) event.getWhoClicked())) return;
            ArenaEditor editor = editors.get((Player) event.getWhoClicked());
            if (event.getCurrentItem() == null) return;
            if (!editor.editorItems.contains(event.getCurrentItem())) return;

            if (event.getCurrentItem().getType() == Material.ENDER_CHEST) {
                editor.initializeInventory();
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {
            Action action = event.getAction();
            Player player = event.getPlayer();
            if (!editors.containsKey(player)) return;
            ArenaEditor editor = editors.get(player);
            if (!editor.editorItems.contains(event.getItem())) return;
            if (event.getItem() == null) return;
            Arena arena = editor.arena;

            event.setCancelled(true);
            switch (event.getItem().getType()) {
                case IRON_AXE:
                    handleSelection(event, action, player, editor);
                    break;
                case BARRIER:
                    handleDeathBarrier(action, player, editor);
                    break;
                case LIGHT_BLUE_BANNER:
                    handleCheckpoint(action, player, editor, arena);
                    break;
                case OAK_BOAT:
                    handleStartLocations(event, action, player, editor, arena);
                    break;
                case ENDER_PEARL:
                    handleLobbyLocation(player, arena);
                    break;
                case REDSTONE_BLOCK:
                    handleStartLineActivator(event, action, player, arena);
                    break;
                case REDSTONE_LAMP:
                    handleLightLocations(event, action, editor, arena);
                    break;
                case RED_CONCRETE:
                    editor.restore(false);
                    break;
                case LIME_CONCRETE:
                    editor.restore(true);
                    break;
                case RED_CARPET:
                    handleMinPlayers(event, action, editor, arena);
                    break;
                case SPECTRAL_ARROW:
                    handleLaps(event, action, editor, arena);
                    break;
                case CLOCK:
                    handleTime(event, action, editor, arena);
                    break;
                case ENDER_CHEST:
                    editor.initializeInventory();
                    editor.spawnStartBoats();
                    break;
                case FEATHER:
                    handleDebugToggle(arena, player);
                default:
                    event.setCancelled(false);
            }
        }

        //Handler methods
        private void handleSelection(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                //noinspection ConstantConditions
                Location location = e.getClickedBlock().getLocation();
                editor.setBoxCorner1(location.toVector());
                player.spigot()
                        .sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(Editor.POSITION_1_SET.formatted(
                                        location.getBlockX(),
                                        location.getBlockY(),
                                        location.getBlockZ()
                                ))
                        );
            }
            if (action == Action.RIGHT_CLICK_BLOCK) {
                //noinspection ConstantConditions
                Location location = e.getClickedBlock().getLocation();
                editor.setBoxCorner2(location.toVector());
                player.spigot()
                        .sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent(Editor.POSITION_2_SET.formatted(
                                        location.getBlockX(),
                                        location.getBlockY(),
                                        location.getBlockZ()
                                ))
                        );
            }
        }

        private void handleDeathBarrier(Action action, Player player, ArenaEditor editor) {
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
            boolean added = editor.addBoundingBox(BoundingBoxType.DEATH_BARRIER);
            if (added)
                player.sendMessage(Editor.DEATH_BARRIER_ADDED);
        }

        private void handleCheckpoint(Action action, Player player, ArenaEditor editor, Arena arena) {
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
            if (!settingCheckpoint.contains(player)) {
                boolean added = editor.addBoundingBox(BoundingBoxType.CHECKPOINT);
                if (added) {
                    player.sendMessage(Editor.CHECKPOINT_BOX_SET.formatted(arena.checkpointBoxes.size()));
                    settingCheckpoint.add(player);
                }
            } else {
                Location playerLocation = player.getLocation();
                float yaw = (float) (Math.round(playerLocation.getYaw() / 22.5) * 22.5);
                arena.checkpointSpawns.add(new Location(
                        player.getWorld(),
                        playerLocation.getBlockX(),
                        playerLocation.getBlockY(),
                        playerLocation.getBlockZ(),
                        yaw,
                        0
                ));
                player.sendMessage(Editor.CHECKPOINT_SPAWN_SET.formatted(
                        arena.checkpointBoxes.size(),
                        playerLocation.getBlockX(),
                        playerLocation.getBlockY(),
                        playerLocation.getBlockZ()
                ));
                settingCheckpoint.remove(player);
            }
        }

        private void handleMinPlayers(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            ItemStack minPlayers = e.getItem();
            if (minPlayers == null) return;
            int index = editor.editorItems.indexOf(minPlayers);
            if (index == -1) return;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (arena.minPlayers > 1) {
                    arena.minPlayers--;
                    minPlayers.setAmount(arena.minPlayers);
                }
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if (arena.minPlayers < arena.startLocations.size()) {
                    arena.minPlayers++;
                    minPlayers.setAmount(arena.minPlayers);
                }
            }
            editor.editorItems.set(index, minPlayers);
        }

        private void handleLaps(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            ItemStack laps = e.getItem();
            if (laps == null) return;
            int index = editor.editorItems.indexOf(laps);
            if (index == -1) return;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (arena.laps > 1) {
                    arena.laps--;
                    laps.setAmount(arena.laps);
                }
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if (arena.laps < 10) {
                    arena.laps++;
                    laps.setAmount(arena.laps);
                }
            }
            editor.editorItems.set(index, laps);
        }

        private void handleTime(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            ItemStack time = e.getItem();
            if (time == null) return;
            ItemMeta meta = time.getItemMeta();
            if (meta == null) return;
            int index = editor.editorItems.indexOf(time);
            if (index == -1) return;
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (arena.time > 30) {
                    arena.time -= 30;
                    meta.setDisplayName(Items.TIME.name()
                            .formatted(formattedTime(arena.time)));
                }
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if (arena.time < 3600) {
                    arena.time += 30;
                    meta.setDisplayName(Items.TIME.name()
                            .formatted(formattedTime(arena.time)));
                }

            }
            time.setItemMeta(meta);
            editor.editorItems.set(index, time);
        }

        private void handleStartLocations(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor, Arena arena) {
            if (action != Action.RIGHT_CLICK_BLOCK) return;
            if (e.getBlockFace() != BlockFace.UP) return;
            if (e.getClickedBlock() == null) return;
            Location boatLocation = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(boatLocation, EntityType.ARMOR_STAND);
            Entity boat = player.getWorld().spawnEntity(boatLocation, EntityType.BOAT);
            stand.setInvisible(true);
            stand.setMarker(true);
            boat.setRotation((float) ((Math.round(player.getLocation().getYaw() / 45)) * 45), 0);
            editor.startBoats.add((Boat) boat);
            arena.startLocations.add(boat.getLocation());
            arena.world = boat.getWorld();
            stand.addPassenger(boat);
        }

        private void handleLobbyLocation(Player player, Arena arena) {
            arena.lobbyLocation = player.getLocation();
            player.sendMessage(Editor.LOBBY_LOCATION_SET.formatted(
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            ));
        }

        private void handleStartLineActivator(PlayerInteractEvent e, Action action, Player player, Arena arena) {
            if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
            if (e.getClickedBlock() == null) return;
            Location blockLocation = e.getClickedBlock().getLocation();
            arena.startLineActivator = blockLocation;
            player.sendMessage(Editor.START_LINE_SET.formatted(
                    blockLocation.getBlockX(),
                    blockLocation.getBlockY(),
                    blockLocation.getBlockZ()
            ));
        }

        private void handleLightLocations(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            if (e.getClickedBlock() == null) return;
            Block block = e.getClickedBlock();

            if (action == Action.RIGHT_CLICK_BLOCK) {
                if (arena.lightLocations.contains(block.getLocation())) {
                    e.getPlayer()
                            .spigot()
                            .sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Editor.LIGHT_EXISTS));
                    return;
                }
                editor.oldLightMaterials.put(block, block.getType());
                block.setType(Material.REDSTONE_LAMP);
                ((Lightable) block.getBlockData()).setLit(true);
                arena.lightLocations.add(block.getLocation());
                e.getPlayer().spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(Editor.LIGHT_PLACED.formatted(arena.lightLocations.size()))
                );
                return;
            }
            if (action == Action.LEFT_CLICK_BLOCK) {
                if (!editor.arena.lightLocations.contains(block.getLocation())) {
                    e.getPlayer()
                            .spigot()
                            .sendMessage(
                                    ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(Editor.LIGHT_NOT_EXIST)
                            );
                    return;
                }
                int lightIndex = editor.arena.lightLocations.indexOf(block.getLocation()) + 1;
                e.getPlayer().spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(Editor.LIGHT_REMOVED.formatted(lightIndex))
                );
                arena.lightLocations.remove(block.getLocation());
                if (editor.oldLightMaterials.containsKey(block)) {
                    block.setType(editor.oldLightMaterials.get(block));
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(false);
            }
        }

        private void handleDebugToggle(Arena arena, Player player) {
            arena.debug = !arena.debug;
            player.sendMessage(Editor.DEBUG_TOGGLED.formatted(arena.debug ? Messages.ON : Messages.OFF));
        }
    }
}
