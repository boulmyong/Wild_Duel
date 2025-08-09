package com.wildduel.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class EmptyWorldGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        // Set bottom layer to bedrock
        if (world.getEnvironment() == World.Environment.NORMAL) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    chunkData.setBlock(i, 0, j, Material.BEDROCK);
                }
            }
        }
        return chunkData;
    }
}
