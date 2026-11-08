package io.github.defective4.cubebabel.room;

import java.util.Arrays;

import io.github.defective4.cubebabel.LibraryServer;
import io.github.defective4.cubebabel.generator.LibraryGenerator;
import net.minestom.server.coordinate.BlockVec;

public class HexagonManager {
    private HexagonManager() {
    }

    public static Sides calculateRoomEntrances(int absX, int absZ) {
        int rx = absX - LibraryServer.CENTER_ROOM.blockX();
        int rz = absZ - LibraryServer.CENTER_ROOM.blockZ();
        if (rx == 0) {
            if (rz == 0) return Sides.NS;
            if (rz == 1)
                return Sides.NNE;
            else if (rz == -1)
                return Sides.SSW;
            else
                return rz < 0 ? Sides.SESW : Sides.NENW;
        }

        BlockVec centerRoomLocation = getRoomLocation(absX, absZ).sub(LibraryServer.CENTER_ROOM_LOCATION.blockX(), 0,
                LibraryServer.CENTER_ROOM_LOCATION.blockZ());
        BlockVec offsetRoomLocation = getRoomLocation(absX, absZ + (rx > 0 ? 1 : -1))
                .sub(LibraryServer.CENTER_ROOM_LOCATION.blockX(), 0, LibraryServer.CENTER_ROOM_LOCATION.blockZ());

        double deg = Math.toDegrees(Math.atan2(centerRoomLocation.blockX(), centerRoomLocation.blockZ()));
        if (deg == -LibraryGenerator.HIGH_THRESHOLD)
            return Sides.SNE;
        else if (deg == LibraryGenerator.LOW_THRESHOLD) return Sides.NSW;

        double offsetDeg = Math.toDegrees(Math.atan2(offsetRoomLocation.blockX(), offsetRoomLocation.blockZ()));
        if (offsetDeg == LibraryGenerator.HIGH_THRESHOLD)
            return Sides.SNW;
        else if (offsetDeg == -LibraryGenerator.LOW_THRESHOLD)
            return Sides.NSE;
        else {
            if (offsetDeg < -LibraryGenerator.LOW_THRESHOLD && deg > -LibraryGenerator.HIGH_THRESHOLD
                    || deg > LibraryGenerator.LOW_THRESHOLD && offsetDeg < LibraryGenerator.HIGH_THRESHOLD)
                return Sides.NS;
            if (deg < -LibraryGenerator.HIGH_THRESHOLD) return Sides.NESW;
            if (offsetDeg > LibraryGenerator.HIGH_THRESHOLD) return Sides.NWSE;
            if (offsetDeg > -LibraryGenerator.LOW_THRESHOLD && offsetDeg < LibraryGenerator.LOW_THRESHOLD)
                return rx > 0 ? Sides.NESW : Sides.NWSE;
        }

        return new Sides();
    }

    public static BlockVec getRoomIndex(BlockVec pos) {
        int z = pos.blockZ() + 7;
        boolean b = false;
        if ((pos.blockX() + 7) / 11 % 2 == 1) {
            z -= 7;
            b = true;
        }

        int cx = (pos.blockX() + 7) / 11 * 11;
        int cz = z / 14 * 14;
        if (b) {
            cz += 7;
        }

        int y = pos.blockY();

        BlockVec[] vectors = new BlockVec[] { new BlockVec(cx, y, cz), new BlockVec(cx + 11, y, cz + 7),
                new BlockVec(cx + 11, y, cz - 7), new BlockVec(cx - 11, y, cz + 7), new BlockVec(cx - 11, y, cz - 7),
                new BlockVec(cx, y, cz + 14), new BlockVec(cx, y, cz - 14), };

        BlockVec closest = Arrays.stream(vectors).sorted((v1, v2) -> {
            double diff = v1.distance(pos) - v2.distance(pos);
            return diff > 0 ? 1 : diff < 0 ? -1 : 0;
        }).findFirst().get();

        int roomX = closest.blockX() / 11;
        int roomZ = closest.blockZ() / 14;

        return new BlockVec(roomX, roomZ);
    }

    public static BlockVec getRoomLocation(int x, int z) {
        int roomX = x * 11;
        int roomZ = z * 14 + roomX % 2 * 7;

        return new BlockVec(roomX, roomZ);
    }

}
