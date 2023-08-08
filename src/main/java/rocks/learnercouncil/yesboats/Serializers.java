package rocks.learnercouncil.yesboats;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.stream.Collectors;

public class Serializers {

    public static String fromLocation(Location loc) {
        return loc.getX() + ","
                + loc.getY() + ","
                + loc.getZ() + ","
                + loc.getYaw() + ","
                + loc.getPitch();
    }
    public static Location toLocation(String string, World world) {
        String[] segments = string.split(",");
        return new Location(
                world,
                Double.parseDouble(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]),
                Float.parseFloat(segments[3]),
                Float.parseFloat(segments[4])
        );
    }

    public static List<String> fromLocationList(List<Location> locations) {
        return locations.stream().map(Serializers::fromLocation).collect(Collectors.toList());
    }
    public static List<Location> toLocationList(List<String> strings, World world) {
        return strings.stream().map(s -> toLocation(s, world)).collect(Collectors.toList());
    }

    public static String fromVectorLocation(Location location) {
        return location.getBlockX() + ","
                + location.getBlockY() + ","
                + location.getBlockZ();
    }
    public static Location toVectorLocation(String str, World world) {
        String[] segments = str.split(",");
        return new Location(world,
                Double.parseDouble(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]));
    }

    public static List<String> fromVectorLocationList(List<Location> locations) {
        return locations.stream().map(Serializers::fromVectorLocation).collect(Collectors.toList());
    }
    public static List<Location> toVectorLocationList(List<String> strings, World world) {
        return strings.stream().map(s -> toVectorLocation(s, world)).collect(Collectors.toList());
    }

    public static List<String> fromBoxList(List<BoundingBox> boxes) {
        return boxes.stream()
                .map(b -> ((int) b.getMinX()) + ","
                        + ((int) b.getMinY()) + ","
                        + ((int) b.getMinZ()) + ","
                        + ((int) b.getMaxX()) + ","
                        + ((int) b.getMaxY()) + ","
                        + ((int) b.getMaxZ()))
                .collect(Collectors.toList());
    }
    public static List<BoundingBox> toBoxList(List<String> strings) {
        return strings.stream()
                .map(s -> {
                    String[] segments = s.split(",");
                    return new BoundingBox(
                            Double.parseDouble(segments[0]),
                            Double.parseDouble(segments[1]),
                            Double.parseDouble(segments[2]),
                            Double.parseDouble(segments[3]),
                            Double.parseDouble(segments[4]),
                            Double.parseDouble(segments[5])
                    );
                }).collect(Collectors.toList());
    }

}
