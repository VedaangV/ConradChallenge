import java.util.*;

final class Grid {

    static final int PIXEL_SIZE_METERS = 100;
    static final double MIN_WEIGHT = 0.01;

    HashMap<Long, Pixel> pixels = new HashMap<>();

    public static class Pixel {
        int px;
        int py;
        double pH;
        double turbidity;
        double tds;
        double temp;
        double abi;
        double weight;

        public Pixel(int px, int py) {
            this.px = px;
            this.py = py;
        }

        public void updateVals(
                double pH,
                double turbidity,
                double tds,
                double temp,
                double abi,
                double w
        ) {
            if (weight == 0) {
                this.pH = pH;
                this.turbidity = turbidity;
                this.tds = tds;
                this.temp = temp;
                this.abi = abi;
            } else {
                this.pH = (this.pH * weight + pH * w) / (weight + w);
                this.turbidity = (this.turbidity * weight + turbidity * w) / (weight + w);
                this.tds = (this.tds * weight + tds * w) / (weight + w);
                this.temp = (this.temp * weight + temp * w) / (weight + w);
                this.abi = (this.abi * weight + abi * w) / (weight + w);
            }
            weight += w;
        }
    }

    public static long pixelToKey(int px, int py) {
        return (((long) px) << 32) | (py & 0xffffffffL);
    }

    private int[] latLonToPixel(double lat, double lon) {
        double latMeters = lat * 111320;
        double lonMeters = lon * 111320 * Math.cos(lat * Math.PI / 180);

        int px = (int) Math.floor(lonMeters / PIXEL_SIZE_METERS);
        int py = (int) Math.floor(latMeters / PIXEL_SIZE_METERS);

        return new int[]{px, py};
    }

    private double[] pixelToCenterLatLon(int px, int py) {
        double centerLatMeters = (py + 0.5) * PIXEL_SIZE_METERS;
        double centerLat = centerLatMeters / 111320;

        double centerLonMeters = (px + 0.5) * PIXEL_SIZE_METERS;
        double centerLon = centerLonMeters /
                (111320 * Math.cos(centerLat * Math.PI / 180));

        return new double[]{centerLat, centerLon};
    }

    private double[] snapToPixelCenter(double lat, double lon) {
        int[] p = latLonToPixel(lat, lon);
        return pixelToCenterLatLon(p[0], p[1]);
    }

    public void addData(
            int startPx,
            int startPy,
            double pH,
            double turbidity,
            double tds,
            double temp,
            double abi
    ) {
        Map<Long, Boolean> visited = new HashMap<>();
        Queue<int[]> q = new ArrayDeque<>();

        q.add(new int[]{startPx, startPy, 0});

        while (!q.isEmpty()) {
            int[] n = q.poll();
            int px = n[0];
            int py = n[1];
            int level = n[2];

            double w = Math.pow(0.5, level);
            if (w < MIN_WEIGHT) continue;

            long key = pixelToKey(px, py);
            if (visited.containsKey(key)) continue;
            visited.put(key, true);

            Pixel p = pixels.computeIfAbsent(key, k -> new Pixel(px, py));
            p.updateVals(pH, turbidity, tds, temp, abi, w);

            q.add(new int[]{px - 1, py, level + 1});
            q.add(new int[]{px + 1, py, level + 1});
            q.add(new int[]{px, py - 1, level + 1});
            q.add(new int[]{px, py + 1, level + 1});
        }
    }

    public void exportData() {
    }
}
