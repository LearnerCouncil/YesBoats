package rocks.learnercouncil.yesboats;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Messages {
    public static String FINISH_SELF;
    public static String FINISH_OTHERS;
    public static String ALL_FINISHED;
    private static String PREFIX;

    public static void initialize(ConfigFile config) {
        PREFIX = config.get().getString("prefix");
        FINISH_SELF = prefixedPath(config, "finish-self");
        FINISH_OTHERS = prefixedPath(config, "finish-others");
        ALL_FINISHED = prefixedPath(config, "all-finished");
        // Editor
        Editor.NO_BOX_SELECTED = prefixedPath(config, "editor.no-box-selected");
        Editor.VALIDATION_FAILED = prefixedPath(config, "editor.validation-failed");
        Editor.VALIDATOR_MIN_PLAYERS = path(config, "editor.validator.min-players");
        Editor.VALIDATOR_LAPS = path(config, "editor.validator.laps");
        Editor.VALIDATOR_TIME = path(config, "editor.validator.time");
        Editor.VALIDATOR_LOBBY_LOCATION = path(config, "editor.validator.lobby-location");
        Editor.VALIDATOR_WORLD = path(config, "editor.validator.world");
        Editor.VALIDATOR_START_LINE_ACTIVATOR = path(config, "editor.validator.start-line-activator");
        Editor.VALIDATOR_START_LOCATIONS = path(config, "editor.validator.start-locations");
        Editor.VALIDATOR_LIGHT_LOCATIONS = path(config, "editor.validator.light-locations");
        Editor.VALIDATOR_CHECKPOINT_BOXES = path(config, "editor.validator.checkpoint-boxes");
        Editor.VALIDATOR_CHECKPOINT_SPAWNS = path(config, "editor.validator.checkpoint-spawns");
        Editor.SAVED = prefixedPath(config, "editor.saved");
        Editor.CANCELED = prefixedPath(config, "editor.canceled");
        Editor.POSITION_1_SET = path(config, "editor.position-1-set");
        Editor.POSITION_2_SET = path(config, "editor.position-2-set");
        Editor.DEATH_BARRIER_ADDED = prefixedPath(config, "editor.death-barrier-added");
        Editor.CHECKPOINT_BOX_SET = prefixedPath(config, "checkpoint-box-set");
        Editor.CHECKPOINT_SPAWN_SET = prefixedPath(config, "checkpoint-spawn-set");
        Editor.LOBBY_LOCATION_SET = prefixedPath(config, "editor.lobby-location-set");
        Editor.START_LINE_SET = prefixedPath(config, "editor.start-line-set");
        Editor.LIGHT_EXISTS = path(config, "editor.light-exists");
        Editor.LIGHT_NOT_EXIST = path(config, "editor.light-not-exist");
        Editor.LIGHT_PLACED = path(config, "editor.light-placed");
        Editor.LIGHT_REMOVED = path(config, "editor.light-removed");
        Editor.DEBUG_TOGGLED = prefixedPath(config, "editor.debug-toggled");

        // Editor Items
        Editor.Items.REGENERATE_NAME = path(config, "editor.items.regenerate.name");
        Editor.Items.REGENERATE_LORE = pathArray(config, "editor.items.regenerate.lore");
        Editor.Items.SELECTOR_NAME = path(config, "editor.items.selector.name");
        Editor.Items.SELECTOR_LORE = pathArray(config, "editor.items.selector.lore");
        Editor.Items.DEATH_BARRIER_NAME = path(config, "editor.items.death-barrier.name");
        Editor.Items.DEATH_BARRIER_LORE = pathArray(config, "editor.items.death-barrier.lore");
        Editor.Items.CHECKPOINT_NAME = path(config, "editor.items.checkpoint.name");
        Editor.Items.CHECKPOINT_LORE = pathArray(config, "editor.items.checkpoint.lore");
        Editor.Items.MIN_PLAYERS_NAME = path(config, "editor.items.min-players.name");
        Editor.Items.MIN_PLAYERS_LORE = pathArray(config, "editor.items.min-players.lore");
        Editor.Items.LAPS_NAME = path(config, "editor.items.laps.name");
        Editor.Items.LAPS_LORE = pathArray(config, "editor.items.laps.lore");
        Editor.Items.TIME_NAME = path(config, "editor.items.time.name");
        Editor.Items.TIME_LORE = pathArray(config, "editor.items.time.lore");
        Editor.Items.START_NAME = path(config, "editor.items.start.name");
        Editor.Items.START_LORE = pathArray(config, "editor.items.start.lore");
        Editor.Items.LOBBY_NAME = path(config, "editor.items.lobby.name");
        Editor.Items.LOBBY_LORE = pathArray(config, "editor.items.lobby.lore");
        Editor.Items.START_LINE_NAME = path(config, "editor.items.start-line.name");
        Editor.Items.START_LINE_LORE = pathArray(config, "editor.items.start-line.lore");
        Editor.Items.LIGHT_NAME = path(config, "editor.items.light.name");
        Editor.Items.LIGHT_LORE = pathArray(config, "editor.items.light.lore");
        Editor.Items.CANCEL_NAME = path(config, "editor.items.cancel.name");
        Editor.Items.CANCEL_LORE = pathArray(config, "editor.items.cancel.lore");
        Editor.Items.SAVE_NAME = path(config, "editor.items.save.name");
        Editor.Items.SAVE_LORE = pathArray(config, "editor.items.save.lore");
        Editor.Items.DEBUG_NAME = path(config, "editor.items.debug.name");
        Editor.Items.DEBUG_LORE = pathArray(config, "editor.items.debug.lore");

    }

    private static String prefixedPath(ConfigFile config, String path) {
        if (PREFIX == null) PREFIX = ChatColor.DARK_AQUA + "[YesBoats]";
        return PREFIX + ' ' + path(config, path);
    }

    private static String path(ConfigFile config, String path) {
        if (!(config.get().isString(path))) return path;
        String string = Objects.requireNonNull(config.get().getString(path));
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private static String[] pathArray(ConfigFile config, String path) {
        if (!config.get().isList(path)) return new String[]{ path };
        List<?> list = Objects.requireNonNull(config.get().getList(path, Collections.emptyList()));
        if (list.isEmpty()) return new String[0];
        return list.stream().filter(e -> e instanceof String).toArray(String[]::new);
    }

    public static class Commands {

    }

    public static class Editor {
        public static String NO_BOX_SELECTED;
        public static String VALIDATION_FAILED;
        public static String VALIDATOR_MIN_PLAYERS;
        public static String VALIDATOR_LAPS;
        public static String VALIDATOR_TIME;
        public static String VALIDATOR_LOBBY_LOCATION;
        public static String VALIDATOR_WORLD;
        public static String VALIDATOR_START_LINE_ACTIVATOR;
        public static String VALIDATOR_START_LOCATIONS;
        public static String VALIDATOR_LIGHT_LOCATIONS;
        public static String VALIDATOR_CHECKPOINT_BOXES;
        public static String VALIDATOR_CHECKPOINT_SPAWNS;
        public static String SAVED;
        public static String CANCELED;
        public static String POSITION_1_SET;
        public static String POSITION_2_SET;
        public static String DEATH_BARRIER_ADDED;
        public static String CHECKPOINT_BOX_SET;
        public static String CHECKPOINT_SPAWN_SET;
        public static String LOBBY_LOCATION_SET;
        public static String START_LINE_SET;
        public static String LIGHT_EXISTS;
        public static String LIGHT_NOT_EXIST;
        public static String LIGHT_PLACED;
        public static String LIGHT_REMOVED;
        public static String DEBUG_TOGGLED;

        public static class Items {
            public static String REGENERATE_NAME;
            public static String[] REGENERATE_LORE;
            public static String SELECTOR_NAME;
            public static String[] SELECTOR_LORE;
            public static String DEATH_BARRIER_NAME;
            public static String[] DEATH_BARRIER_LORE;
            public static String CHECKPOINT_NAME;
            public static String[] CHECKPOINT_LORE;
            public static String MIN_PLAYERS_NAME;
            public static String[] MIN_PLAYERS_LORE;
            public static String LAPS_NAME;
            public static String[] LAPS_LORE;
            public static String TIME_NAME;
            public static String[] TIME_LORE;
            public static String START_NAME;
            public static String[] START_LORE;
            public static String LOBBY_NAME;
            public static String[] LOBBY_LORE;
            public static String START_LINE_NAME;
            public static String[] START_LINE_LORE;
            public static String LIGHT_NAME;
            public static String[] LIGHT_LORE;
            public static String CANCEL_NAME;
            public static String[] CANCEL_LORE;
            public static String SAVE_NAME;
            public static String[] SAVE_LORE;
            public static String DEBUG_NAME;
            public static String[] DEBUG_LORE;
        }
    }
}
