package terraria.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.OctaveGenerator;
import org.bukkit.util.noise.PerlinNoiseGenerator;

import java.util.Random;

public class CachedOctaveGenerator {
    protected final CachedNoiseGenerator[] octaves;

    public CachedOctaveGenerator(World world, int octaves, double scaleInitial) {
        this(new Random(world.getSeed()), octaves, scaleInitial);
    }
    public CachedOctaveGenerator(long seed, int octaves, double scaleInitial) {
        this(new Random(seed), octaves, scaleInitial);
    }
    public CachedOctaveGenerator(Random rand, int octaves, double scaleInitial) {
        this.octaves = createOctaves(rand, octaves, scaleInitial);
    }


    public double noise(int x, int y, int z) {
        double result = 0.0;
        CachedNoiseGenerator[] var23;
        int var22 = (var23 = this.octaves).length;

        for(int var21 = 0; var21 < var22; ++var21) {
            CachedNoiseGenerator octave = var23[var21];
            result += octave.noise(x, y, z);
        }

        return result;
    }


    // this is the noise generator part
    private static CachedNoiseGenerator[] createOctaves(Random rand, int octaves, double scaleInitial) {
        CachedNoiseGenerator[] result = new CachedNoiseGenerator[octaves];

        double currFreq = scaleInitial, currAmpli = 1;
        for(int i = 0; i < octaves; ++i) {
            result[i] = new CachedNoiseGenerator(rand, currFreq, currAmpli);
            currFreq *= 0.5;
            currAmpli *= 0.5;
        }

        return result;
    }


    // the special generator used to generate
    public static class CachedNoiseGenerator {
        protected static final int[][] grad3 = new int[][]{{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
        private static final CachedNoiseGenerator instance = new CachedNoiseGenerator();

        protected final int[] perm = new int[512];
        protected double offsetX;
        protected double offsetY;
        protected double offsetZ;
        private double frequency, amplitude;
        static long test_before = 0, test_before_time = 0,
                    test_fade = 0, test_fade_time = 0,
                    test_lerp = 0, test_lerp_time = 0;

        protected CachedNoiseGenerator() {
            int[] p = new int[]{151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180};

            for(int i = 0; i < 512; ++i) {
                this.perm[i] = p[i & 255];
            }
        }

        public CachedNoiseGenerator(Random rand, double frequency, double amplitude) {
            this.frequency = frequency;
            this.amplitude = amplitude;
            this.offsetX = rand.nextInt(256);
            this.offsetY = rand.nextInt(256);
            this.offsetZ = rand.nextInt(256);

            int i;
            for(i = 0; i < 256; ++i) {
                this.perm[i] = rand.nextInt(256);
            }

            for(i = 0; i < 256; ++i) {
                int pos = rand.nextInt(256 - i) + i;
                int old = this.perm[i];
                this.perm[i] = this.perm[pos];
                this.perm[pos] = old;
                this.perm[i + 256] = this.perm[i];
            }
        }

        public static int floor(double x) {
            return x >= 0.0 ? (int)x : (int)x - 1;
        }

        protected static double fade(double x) {
            return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
        }

        protected static double lerp(double x, double y, double z) {
            return y + x * (z - y);
        }

        protected static double grad(int hash, double x, double y, double z) {
            hash &= 15;
            double u = hash < 8 ? x : y;
            double v = hash < 4 ? y : (hash != 12 && hash != 14 ? z : x);
            return ((hash & 1) == 0 ? u : -u) + ((hash & 2) == 0 ? v : -v);
        }
        public double noise(int blockX, int blockY, int blockZ) {
            long timing = System.nanoTime();

            double x = (blockX + this.offsetX);
            double y = blockY + this.offsetY;
            double z = blockZ + this.offsetZ;
            x *= this.frequency;
            y *= this.frequency;
            z *= this.frequency;
            int floorX = floor(x);
            int floorY = floor(y);
            int floorZ = floor(z);
            int X = floorX & 255;
            int Y = floorY & 255;
            int Z = floorZ & 255;
            x -= floorX;
            y -= floorY;
            z -= floorZ;


            test_before += (System.nanoTime() - timing);
            test_before_time ++;
            if (test_before_time % 25600 == 0)
                Bukkit.broadcastMessage("Time elapsed for setup basic noise info: " + test_before / test_before_time);
            timing = System.nanoTime();

            double fX = fade(x);
            double fY = fade(y);
            double fZ = fade(z);

            test_fade += (System.nanoTime() - timing);
            test_fade_time ++;
            if (test_fade_time % 25600 == 0)
                Bukkit.broadcastMessage("Time elapsed for fade: " + test_fade / test_fade_time);


            int A = this.perm[X] + Y;
            int AA = this.perm[A] + Z;
            int AB = this.perm[A + 1] + Z;
            int B = this.perm[X + 1] + Y;
            int BA = this.perm[B] + Z;
            int BB = this.perm[B + 1] + Z;


            timing = System.nanoTime();

            double result = lerp(fZ,
                    lerp(fY,
                        lerp(fX, grad(this.perm[AA], x, y, z), grad(this.perm[BA], x - 1.0, y, z)),
                        lerp(fX, grad(this.perm[AB], x, y - 1.0, z), grad(this.perm[BB], x - 1.0, y - 1.0, z))),
                    lerp(fY,
                        lerp(fX, grad(this.perm[AA + 1], x, y, z - 1.0), grad(this.perm[BA + 1], x - 1.0, y, z - 1.0)),
                        lerp(fX, grad(this.perm[AB + 1], x, y - 1.0, z - 1.0), grad(this.perm[BB + 1], x - 1.0, y - 1.0, z - 1.0))));


            test_lerp += (System.nanoTime() - timing);
            test_lerp_time ++;
            if (test_lerp_time % 25600 == 0)
                Bukkit.broadcastMessage("Time elapsed for lerp: " + test_lerp / test_lerp_time);

            return result * this.amplitude;
        }
    }
}
