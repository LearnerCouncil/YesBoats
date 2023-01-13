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
import org.bukkit.event.inventory.InventoryClickEvent;
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
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.*;
import java.util.function.Supplier;

import static org.bukkit.ChatColor.*;

public class ArenaEditor {

    private static final YesBoats plugin = YesBoats.getInstance();

    public static HashMap<Player, ArenaEditor> editors = new HashMap<>();

    private final LinkedList<ItemStack> editorItems = new LinkedList<>();
    private final List<Boat> startBoats = new LinkedList<>();
    private final HashMap<Block, Material> oldLightMaterials = new HashMap<>();
    private final ItemStack[] playerInv;
    private final Player player;
    public final Arena arena;

    private Vector boxCorner1 = null, boxCorner2 = null;
    private BoundingBox createdBox;
    private BoundingBox selectedBox;
    private BukkitTask displayTask;


    public ArenaEditor(Player player, Arena arena) {
        this.player = player;
        playerInv = player.getInventory().getContents();
        this.arena = arena;
        initializeInventory();
        startBoxDisplay();
        arena.startLocations.forEach(l -> {
            ArmorStand stand = (ArmorStand) arena.startWorld.spawnEntity(l, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setMarker(true);
            Boat boat = (Boat) arena.startWorld.spawnEntity(l, EntityType.BOAT);
            stand.addPassenger(boat);
            startBoats.add(boat);
        });
    }

    /**
     * Initializes the player's inventory with all the items used for editing.
     */
    @SuppressWarnings("ConstantConditions")
    private void initializeInventory() {
        player.getInventory().clear();
        editorItems.clear();
        Inventory inv = player.getInventory();

        inv.setItem(0, getItem(Material.IRON_AXE,
                BOLD.toString() + YELLOW + "Area Selector",
                GOLD + "Left click a block to select the 1st corner",
                GOLD + "Right click a block to select the 2nd corner",
                GOLD + "Drop to remove selected bounding box"));

        inv.setItem(1, getItem(Material.BARRIER,
                BOLD.toString() + RED + "Death Barrier",
                DARK_RED + "Click to set the selected",
                DARK_RED + "bouding box to a death barrier"));

        inv.setItem(2, getItem(Material.LIGHT_BLUE_BANNER,
                BOLD.toString() + AQUA + "Checkpoint",
                DARK_AQUA + "Click to set the selected",
                DARK_AQUA + "bouding box to a checkpoint"));

        inv.setItem(3, getItem(Material.RED_CARPET,
                BOLD.toString() + RED + "Minimum Players",
                YELLOW + "Left click to increase",
                YELLOW + "Right click to decrease"));
        inv.getItem(3).setAmount(arena.minPlayers);

        inv.setItem(4, getItem(Material.OAK_BOAT,
                BOLD.toString() + YELLOW + "Add start location",
                GOLD + "Place to add a start location"));

        inv.setItem(5, getItem(Material.ENDER_PEARL,
                BOLD.toString() + YELLOW + "Set lobby Location",
                GOLD + "Click to set the Lobby Location"));

        inv.setItem(6, getItem(Material.REDSTONE_BLOCK,
                BOLD.toString() + RED + "Start Line Activator.",
                YELLOW + "Click a block to set that block as the start line activator"));

        inv.setItem(7, getItem(Material.REDSTONE_LAMP,
                BOLD.toString() + YELLOW + "Add Light Location",
                GOLD + "Click a block to add a light there,",
                GOLD + "Lights are turned on in the order they were placed"));

        inv.setItem(20, getItem(Material.RED_CONCRETE,
                BOLD.toString() + DARK_RED + "Cancel",
                RED + "Stops editing without saving. ",
                RED + "(Cannot be undone)"));

        inv.setItem(24, getItem(Material.LIME_CONCRETE,
                BOLD.toString() + GREEN + "Save",
                DARK_GREEN + "Stops editing, saving changes.",
                DARK_GREEN + "(Arena must be valid)"));
    }

    /**
     * Gets an {@link ItemStack} from a {@link Material}, name, and lore.
     * @param material The material of the item.
     * @param name The name of the item.
     * @param lore The lore of the Item.
     * @return The ItemStack
     */
    private ItemStack getItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        editorItems.add(item);
        return item;
    }

    private void setBoxCorner1(Vector boxCorner1) {
        this.boxCorner1 = boxCorner1;
        if(boxCorner2 != null)
            createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    private void setBoxCorner2(Vector boxCorner2) {
        this.boxCorner2 = boxCorner2;
        if(boxCorner1 != null)
            createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    private enum BoundingBoxType {
        DEATH_BARRIER, CHECKPOINT, REMOVE
    }
    /**
     * Adds a bounding box the the specified type to the {@link Arena}.
     * @param type The {@link BoundingBoxType}.
     */
    private void addBoundingBox(BoundingBoxType type) {
        if(selectedBox == null) {
            player.sendMessage(DARK_RED + "[YesBoats] " + RED + "There is no bounding box selected");
            return;
        }
        BoundingBoxType selectedType = ((Supplier<BoundingBoxType>) () -> {
            if(arena.checkpointBoxes.contains(selectedBox)) return BoundingBoxType.CHECKPOINT;
            if(arena.deathBarriers.contains(selectedBox)) return BoundingBoxType.DEATH_BARRIER;
            return BoundingBoxType.REMOVE;
        }).get();


        boxCorner1 = boxCorner2 = null;
        createdBox = null;

        if(selectedType == type) return;
        switch (type) {
            case CHECKPOINT:
                if(selectedType == BoundingBoxType.DEATH_BARRIER) arena.deathBarriers.remove(selectedBox);
                arena.checkpointBoxes.add(selectedBox.clone());
                break;
            case DEATH_BARRIER:
                if(selectedType == BoundingBoxType.CHECKPOINT) arena.checkpointBoxes.remove(selectedBox);
                arena.deathBarriers.add(selectedBox.clone());
                break;
            case REMOVE:
                if(selectedType == BoundingBoxType.CHECKPOINT) arena.checkpointBoxes.remove(selectedBox);
                if(selectedType == BoundingBoxType.DEATH_BARRIER) arena.deathBarriers.remove(selectedBox);
                break;
        }
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
                for(BoundingBox box : boxes) {
                    if(boxRaycast(box)) {
                        selectedBox = box;
                        displayBoundingBox(selectedBox, new Particle.DustOptions(Color.YELLOW,1));
                        clearSelection = false;
                        break;
                    }
                }
                if(clearSelection) selectedBox = null;

                if(createdBox != null)
                    displayBoundingBox(createdBox, new Particle.DustOptions(Color.WHITE, 1));
                arena.deathBarriers.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.RED, 1)));
                arena.checkpointBoxes.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.AQUA, 1)));
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private boolean boxRaycast(BoundingBox box) {
        if(box == null) return false;
        for(double i = 0; i < 10; i += 0.5) {
            Vector directionVector = player.getEyeLocation().getDirection();
            Vector locationVector = player.getEyeLocation().toVector().add(directionVector.multiply(i));
            if(box.contains(locationVector.toBlockVector())) return true;
        }
        return false;
    }

    private boolean offsetDisplayBox = false;
    /**
     * Displays the specified {@link BoundingBox} using particles.
     * @param box the bounding box to display.
     * @param color the color and size on the particles in the for of a {@link Particle.DustOptions} object.
     */
    private void displayBoundingBox(BoundingBox box, Particle.DustOptions color) {
        Vector c1 = box.getMin();
        Vector c2 = box.getMax().add(new Vector(1, 1, 1));

        double offset = offsetDisplayBox ? 0.5 : 0;

        //x axis lines
        for(double x = c1.getX() + offset; x <= c2.getX(); x += 1) {
            spawnParticle(x, c1.getY(), c1.getZ(), color);
            spawnParticle(x, c1.getY(), c2.getZ(), color);
            spawnParticle(x, c2.getY(), c1.getZ(), color);
            spawnParticle(x, c2.getY(), c2.getZ(), color);
        }
        //y axis lines
        for(double y = c1.getY() + offset; y <= c2.getY(); y += 1) {
            spawnParticle(c1.getX(), y, c1.getZ(), color);
            spawnParticle(c1.getX(), y, c2.getZ(), color);
            spawnParticle(c2.getX(), y, c1.getZ(), color);
            spawnParticle(c2.getX(), y, c2.getZ(), color);
        }
        //z axis lines
        for(double z = c1.getZ() + offset; z <= c2.getZ(); z += 1) {
            spawnParticle(c1.getX(), c1.getY(), z, color);
            spawnParticle(c1.getX(), c2.getY(), z, color);
            spawnParticle(c2.getX(), c1.getY(), z, color);
            spawnParticle(c2.getX(), c2.getY(), z, color);
        }
    }
    private void spawnParticle(double x, double y, double z, Particle.DustOptions color) {
        player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, y, z), 1, color);
    }



    public void restore(boolean save) {
        if(save) {
            if(!validate().isEmpty()) {
                player.sendMessage(DARK_AQUA + "[YesBoats] " + RED + "Arena validation failed. Validator found problems with the following fields: " + validate());
                return;
            }
            if(!Arena.arenas.contains(arena)) Arena.arenas.add(arena);
        } else
            oldLightMaterials.forEach(Block::setType);
        player.getInventory().setContents(playerInv);
        displayTask.cancel();
        boxCorner1 = null;
        boxCorner2 = null;
        selectedBox = null;
        createdBox = null;
        startBoats.forEach(b -> {
            if(b.isInsideVehicle())
                Objects.requireNonNull(b.getVehicle()).remove();
            b.remove();
        });
    }

    private String validate() {
        StringBuilder result = new StringBuilder();
        if(arena.minPlayers < 1) result.append("minPlayers, ");
        if(arena.lobbyLocation == null) result.append("lobbyLoation, ");
        if(arena.startWorld == null) result.append("startWorld, ");
        if(arena.startLineActivator == null) result.append("startLineActivator, ");
        if(arena.startLocations == null || arena.startLocations.isEmpty()) result.append("startLocations, ");
        if(arena.lightLocations == null ||arena.lightLocations.isEmpty()) result.append("lightLocations, ");
        if(arena.deathBarriers == null || arena.deathBarriers.isEmpty()) result.append("deathBarriers, ");
        if(arena.checkpointBoxes == null || arena.checkpointBoxes.isEmpty()) result.append("checkpointBoxes, ");
        if(arena.checkpointSpawns == null || (arena.checkpointBoxes != null && arena.checkpointSpawns.size() != arena.checkpointBoxes.size())) result.append("checkpointSpawns, ");
        return result.substring(0, result.length() - 2);
    }

    public static class Events implements Listener {
        
        private final List<Player> settingCheckpoint = new ArrayList<>();

        @EventHandler
        public void onVehicleDestroy(VehicleDestroyEvent e) {
            if(!(e.getAttacker() instanceof Player)) return;
            if(!(e.getVehicle() instanceof Boat)) return;
            if(!editors.containsKey((Player) e.getAttacker())) return;

            ArenaEditor editor = editors.get((Player) e.getAttacker());
            if(editor.startBoats.contains((Boat) e.getVehicle())) {
                Boat boat = (Boat) e.getVehicle();
                if(boat.isInsideVehicle())
                    Objects.requireNonNull(boat.getVehicle()).remove();
                editor.arena.startLocations.remove(boat.getLocation());
            }
        }

        @EventHandler
        public void onVehicleEnter(VehicleEnterEvent e) {
            if(!(e.getEntered() instanceof Player)) return;
            if(editors.containsKey((Player) e.getEntered()))
                e.setCancelled(true);
        }

        @EventHandler
        public void onItemDrop(PlayerDropItemEvent e) {
            if(!editors.containsKey(e.getPlayer())) return;
            ArenaEditor editor = editors.get(e.getPlayer());
            ItemStack item = e.getItemDrop().getItemStack();
            if(!editor.editorItems.contains(item)) return;

            e.setCancelled(true);
            if(item.getType() == Material.IRON_AXE) editor.addBoundingBox(BoundingBoxType.REMOVE);
        }
        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(!editors.containsKey((Player) e.getWhoClicked())) return;
            ArenaEditor editor = editors.get((Player) e.getWhoClicked());
            if(e.getCurrentItem() == null) return;
            if(!editor.editorItems.contains(e.getCurrentItem())) return;

            e.setCancelled(true);
            if(e.getCurrentItem().getType() == Material.RED_CONCRETE) editor.restore(false);
            if(e.getCurrentItem().getType() == Material.LIME_CONCRETE) editor.restore(true);
        }

        @EventHandler
        public void onClick(PlayerInteractEvent e) {
            Action action = e.getAction();
            Player player = e.getPlayer();
            if(!editors.containsKey(player)) return;
            ArenaEditor editor = editors.get(player);
            if(!editor.editorItems.contains(e.getItem())) return;
            if(e.getItem() == null) return;
            Arena arena = editor.arena;

            switch (e.getItem().getType()) {
                case IRON_AXE:
                    handleSelection(e, action, player, editor);
                    break;
                case BARRIER:
                    handleDeathBarrier(e, action, player, editor);
                    break;
                case LIGHT_BLUE_BANNER:
                    handleCheckpoint(e, action, player, editor, arena);
                    break;
                case RED_CARPET:
                    handleMinPlayers(e, action, editor, arena);
                    break;
                case OAK_BOAT:
                    handleStartLocations(e, action, player, editor, arena);
                    break;
                case ENDER_PEARL:
                    handleLobbyLocation(e, player, arena);
                    break;
                case REDSTONE_BLOCK:
                    handleStartLineActivator(e, action, player, arena);
                    break;
                case REDSTONE_LAMP:
                    handleLightLocations(e, action, editor, arena);
            }
        }
        //Handler methods
        private void handleSelection(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor) {
            if(action == Action.LEFT_CLICK_BLOCK) {
                //noinspection ConstantConditions
                Location location = e.getClickedBlock().getLocation();
                editor.setBoxCorner1(location.toVector());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(AQUA + "Position 1 set. (" +
                        location.getBlockX() + ", " +
                        location.getBlockY() + ", " +
                        location.getBlockZ() + ")"));
                e.setCancelled(true);
            }
            if(action == Action.RIGHT_CLICK_BLOCK) {
                //noinspection ConstantConditions
                Location location = e.getClickedBlock().getLocation();
                editor.setBoxCorner2(location.toVector());
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(AQUA + "Position 2 set. (" +
                        location.getBlockX() + ", " +
                        location.getBlockY() + ", " +
                        location.getBlockZ() + ")"));
                e.setCancelled(true);
            }
        }

        private void handleDeathBarrier(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor) {
            if(action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
            editor.addBoundingBox(BoundingBoxType.DEATH_BARRIER);
            player.sendMessage(DARK_AQUA + "[YesBoats] " + AQUA + "Death barrier added.");
            e.setCancelled(true);
        }

        private void handleCheckpoint(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor, Arena arena) {
            if(action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
            if(!settingCheckpoint.contains(player)) {
                editor.addBoundingBox(BoundingBoxType.CHECKPOINT);
                player.sendMessage(DARK_AQUA + "[YesBoats]" + AQUA +" Bounding box for chectpoint #" + arena.checkpointBoxes.size() + " set. Click again to set the spawnpoint.");
                settingCheckpoint.add(player);
            } else {
                Location playerLocation = player.getLocation();
                float yaw = (float) (Math.round(playerLocation.getYaw() / 22.5) * 22.5);
                arena.checkpointSpawns.add(new Location(player.getWorld(), playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ(), yaw, 0));
                player.sendMessage(DARK_AQUA + "[YesBoats]" + AQUA +" Spawnpoint for chectpoint #" + arena.checkpointBoxes.size() + " set. (" +
                        playerLocation.getBlockX() + ", " +
                        playerLocation.getBlockY() + ", " +
                        playerLocation.getBlockZ() + ")");
                settingCheckpoint.remove(player);
            }
            e.setCancelled(true);
        }

        private void handleMinPlayers(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            ItemStack minPlayers = e.getItem();
            if(minPlayers == null) return;
            int index = editor.editorItems.indexOf(minPlayers);
            if(index == -1) return;
            if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if(arena.minPlayers > 1) {
                    arena.minPlayers--;
                    minPlayers.setAmount(arena.minPlayers);
                }
                e.setCancelled(true);
            } else if(action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                if(arena.minPlayers < arena.startLocations.size()) {
                    arena.minPlayers++;
                    minPlayers.setAmount(arena.minPlayers);
                }
                editor.editorItems.set(index, minPlayers);
                e.setCancelled(true);
            }
        }

        private void handleStartLocations(PlayerInteractEvent e, Action action, Player player, ArenaEditor editor, Arena arena) {
            if(action != Action.RIGHT_CLICK_BLOCK) return;
            if(e.getBlockFace() != BlockFace.UP) return;
            if(e.getClickedBlock() == null) return;
            Location boatLocation = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(boatLocation, EntityType.ARMOR_STAND);
            Entity boat = player.getWorld().spawnEntity(boatLocation, EntityType.BOAT);
            stand.setInvisible(true);
            stand.setMarker(true);
            boat.setRotation((float) ((Math.round(player.getLocation().getYaw() / 45)) * 45), 0);
            editor.startBoats.add((Boat) boat);
            arena.startLocations.add(boat.getLocation());
            arena.startWorld = boat.getWorld();
            stand.addPassenger(boat);
            e.setCancelled(true);
        }

        private void handleLobbyLocation(PlayerInteractEvent e, Player player, Arena arena) {
            arena.lobbyLocation = player.getLocation();
            player.sendMessage(DARK_AQUA + "[YesBoats] " + AQUA + " Set lobby location. (" +
                    player.getLocation().getBlockX() + ", " +
                    player.getLocation().getBlockY() + ", " +
                    player.getLocation().getBlockZ() + ")");
            e.setCancelled(true);
        }

        private void handleStartLineActivator(PlayerInteractEvent e, Action action, Player player, Arena arena) {
            if(action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
            if(e.getClickedBlock() == null) return;
            Location blockLocation = e.getClickedBlock().getLocation();
            arena.startLineActivator = blockLocation;
            player.sendMessage(DARK_AQUA + "[YesBoats] " + AQUA + " Set start line activator. (" +
                    blockLocation.getBlockX() + ", " +
                    blockLocation.getBlockY() + ", " +
                    blockLocation.getBlockZ() + ")");
            e.setCancelled(true);
        }

        private void handleLightLocations(PlayerInteractEvent e, Action action, ArenaEditor editor, Arena arena) {
            if(action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;
            if(e.getClickedBlock() == null) return;
            Block block = e.getClickedBlock();
            editor.oldLightMaterials.put(block, block.getType());
            block.setType(Material.REDSTONE_LAMP);
            ((Lightable) block.getBlockData()).setLit(true);
            arena.lightLocations.add(block.getLocation());
            e.setCancelled(true);
        }
    }
}
