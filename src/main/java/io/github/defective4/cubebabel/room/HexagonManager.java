package io.github.defective4.cubebabel.room;

import java.util.Arrays;

import net.minestom.server.coordinate.BlockVec;

public class HexagonManager {
    private HexagonManager() {
    }

    public static Sides calculateRoomEntrances(int x, int z) {

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
