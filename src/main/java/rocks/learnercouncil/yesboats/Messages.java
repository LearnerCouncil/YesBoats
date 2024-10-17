package rocks.learnercouncil.yesboats;

import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Messages {
    public static String PREFIX;
    public static String FINISH_SELF;
    public static String FINISH_OTHERS;
    public static String ALL_FINISHED;

    public static void initialize(ConfigFile config) {
        PREFIX = path(config, "prefix");
        FINISH_SELF = prefixedPath(config, "finish-self");
        FINISH_OTHERS = prefixedPath(config, "finish-others");
        ALL_FINISHED = prefixedPath(config, "all-finished");

        //region Editor...
        Editor.VALIDATOR_MIN_PLAYERS = validatorPath(config, "min-players");
        Editor.VALIDATOR_LAPS = validatorPath(config, "laps");
        Editor.VALIDATOR_TIME = validatorPath(config, "time");
        Editor.VALIDATOR_LOBBY_LOCATION = validatorPath(config, "lobby-location");
        Editor.VALIDATOR_WORLD = validatorPath(config, "world");
        Editor.VALIDATOR_START_LINE_ACTIVATOR = validatorPath(config, "start-line-activator");
        Editor.VALIDATOR_START_LOCATIONS = validatorPath(config, "start-locations");
        Editor.VALIDATOR_LIGHT_LOCATIONS = validatorPath(config, "light-locations");
        Editor.VALIDATOR_CHECKPOINT_BOXES = validatorPath(config, "checkpoint-boxes");
        Editor.VALIDATOR_CHECKPOINT_SPAWNS = validatorPath(config, "checkpoint-spawns");

        Editor.NO_BOX_SELECTED = editorPath(config, "no-box-selected", true);
        Editor.VALIDATION_FAILED = editorPath(config, "validation-failed", true);
        Editor.SAVED = editorPath(config, "saved", true);
        Editor.CANCELED = editorPath(config, "canceled", true);
        Editor.POSITION_1_SET = editorPath(config, "position-1-set", false);
        Editor.POSITION_2_SET = editorPath(config, "position-2-set", false);
        Editor.DEATH_BARRIER_ADDED = editorPath(config, "death-barrier-added", true);
        Editor.CHECKPOINT_BOX_SET = editorPath(config, "checkpoint-box-set", true);
        Editor.CHECKPOINT_SPAWN_SET = editorPath(config, "checkpoint-spawn-set", true);
        Editor.LOBBY_LOCATION_SET = editorPath(config, "lobby-location-set", true);
        Editor.START_LINE_SET = editorPath(config, "start-line-set", true);
        Editor.LIGHT_EXISTS = editorPath(config, "light-exists", false);
        Editor.LIGHT_NOT_EXIST = editorPath(config, "light-not-exist", false);
        Editor.LIGHT_PLACED = editorPath(config, "light-placed", false);
        Editor.LIGHT_REMOVED = editorPath(config, "light-removed", false);
        Editor.DEBUG_TOGGLED = editorPath(config, "debug-toggled", true);
        //endregion

        //region Editor.Items...
        Editor.Items.REGENERATE = itemPath(config, "regenerate");
        Editor.Items.SELECTOR = itemPath(config, "selector");
        Editor.Items.DEATH_BARRIER = itemPath(config, "death-barrier");
        Editor.Items.CHECKPOINT = itemPath(config, "checkpoint");
        Editor.Items.MIN_PLAYERS = itemPath(config, "min-players");
        Editor.Items.LAPS = itemPath(config, "laps");
        Editor.Items.TIME = itemPath(config, "time");
        Editor.Items.START = itemPath(config, "start");
        Editor.Items.LOBBY = itemPath(config, "lobby");
        Editor.Items.START_LINE = itemPath(config, "start-line");
        Editor.Items.LIGHT = itemPath(config, "light");
        Editor.Items.CANCEL = itemPath(config, "cancel");
        Editor.Items.SAVE = itemPath(config, "save");
        Editor.Items.DEBUG = itemPath(config, "debug");
        //endregion

        //region Commands
        Commands.CREATED = commandPath(config, "created");
        Commands.REMOVED = commandPath(config, "removed");
        Commands.JOINED = commandPath(config, "joined");
        Commands.LEFT = commandPath(config, "left");
        Commands.EDITING = commandPath(config, "editing");
        Commands.STARTED = commandPath(config, "started");
        Commands.STOPPED = commandPath(config, "stopped");
        Commands.DISPLAYING_PATH = commandPath(config, "displaying-path");
        Commands.CLEARING_PATH = commandPath(config, "clearing-path");
        Commands.JOINED_OTHER = commandPath(config, "joined-other");
        Commands.LEFT_OTHER = commandPath(config, "left-other");

        Commands.NO_PERMISSION = commandPath(config, "no-permission");
        Commands.TOO_FEW_ARGS = commandPath(config, "too-few-args");
        Commands.TOO_MANY_ARGS = commandPath(config, "too-many-args");
        Commands.ARENA_EXISTS = commandPath(config, "arena-exists");
        Commands.ARENA_NOT_EXIST = commandPath(config, "arena-not-exist");
        Commands.PLAYER_NOT_FOUND = commandPath(config, "player-not-found");
        Commands.ALREADY_RUNNING = commandPath(config, "already-running");
        Commands.NOT_RUNNING = commandPath(config, "not-running");
        Commands.TOO_FEW_PLAYERS = commandPath(config, "too-few-players");
        Commands.NOT_IN_ARENA_SELF = commandPath(config, "not-in-arena-self");
        Commands.NOT_IN_ARENA_OTHER = commandPath(config, "not-in-arena-other");
        Commands.ALREADY_IN_ARENA_SELF = commandPath(config, "already-in-arena-self");
        Commands.ALREADY_IN_ARENA_OTHER = commandPath(config, "already-in-arena-other");
        Commands.NEEDS_PLAYER = commandPath(config, "needs-player");
        Commands.INVALID_ARGS = commandPath(config, "invalid-args");
        Commands.INVALID_NUMBER = commandPath(config, "invalid-number");

        Commands.BAR = path(config, "commands.bar");

        //endregion
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
        return list.stream()
                .filter(e -> e instanceof String)
                .map(s -> ChatColor.translateAlternateColorCodes('&', (String) s))
                .toArray(String[]::new);
    }


    private static String prefixedPath(ConfigFile config, String path) {
        if (PREFIX == null) PREFIX = ChatColor.DARK_AQUA + "[YesBoats]";
        return PREFIX + ' ' + path(config, path);
    }

    private static String editorPath(ConfigFile config, String path, boolean prefixed) {
        return prefixed ? prefixedPath(config, "editor." + path) : path(config, "editor." + path);
    }

    private static String validatorPath(ConfigFile config, String path) {
        return path(config, "editor.validator." + path);
    }

    private static Editor.Item itemPath(ConfigFile config, String name) {
        return new Editor.Item(path(config, "editor.items." + name + ".name"),
                pathArray(config, "editor.items." + name + ".lore")
        );
    }

    private static String commandPath(ConfigFile config, String path) {
        return prefixedPath(config, "commands." + path);
    }

    public static class Commands {
        public static String CREATED;
        public static String REMOVED;
        public static String JOINED;
        public static String LEFT;
        public static String EDITING;
        public static String STARTED;
        public static String STOPPED;
        public static String DISPLAYING_PATH;
        public static String CLEARING_PATH;
        public static String JOINED_OTHER;
        public static String LEFT_OTHER;

        public static String NO_PERMISSION;
        public static String TOO_FEW_ARGS;
        public static String TOO_MANY_ARGS;
        public static String ARENA_EXISTS;
        public static String ARENA_NOT_EXIST;
        public static String PLAYER_NOT_FOUND;
        public static String ALREADY_RUNNING;
        public static String NOT_RUNNING;
        public static String TOO_FEW_PLAYERS;
        public static String NOT_IN_ARENA_SELF;
        public static String NOT_IN_ARENA_OTHER;
        public static String ALREADY_IN_ARENA_SELF;
        public static String ALREADY_IN_ARENA_OTHER;
        public static String NEEDS_PLAYER;
        public static String INVALID_ARGS;
        public static String INVALID_NUMBER;

        public static String BAR;
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

        public record Item(String name, String[] lore) {
            public Item formattedName(Object... args) {
                return new Item(this.name().formatted(args), this.lore());
            }
        }

        public static class Items {
            public static Item REGENERATE;
            public static Item SELECTOR;
            public static Item DEATH_BARRIER;
            public static Item CHECKPOINT;
            public static Item MIN_PLAYERS;
            public static Item LAPS;
            public static Item TIME;
            public static Item START;
            public static Item LOBBY;
            public static Item START_LINE;
            public static Item LIGHT;
            public static Item CANCEL;
            public static Item SAVE;
            public static Item DEBUG;
        }
    }
}
