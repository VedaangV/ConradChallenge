package watermap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Main {

    public static void main(String[] args) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> root = mapper.readValue(new File("input.json"), Map.class);

        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) root.get("entries");

        Grid grid = new Grid();

        for (Map<String, Object> e : entries) {
            double lat = ((Number) e.get("lat")).doubleValue();
            double lon = ((Number) e.get("lon")).doubleValue();
            double pH = ((Number) e.get("pH")).doubleValue();
            double turbidity = ((Number) e.get("turbidity")).doubleValue();
            double tds = ((Number) e.get("tds")).doubleValue();
            double temp = ((Number) e.get("temp")).doubleValue();
            String timestamp = (String) e.get("timestamp");

            int[] coords = grid.latLonToPixel(lat, lon);
            int px = coords[0];
            int py = coords[1];

            grid.addData(px, py, pH, turbidity, tds, temp);
        }

        grid.exportData();
    }
}

final class WaterMask {

    private static BufferedImage mask;

    static {
        try {
            mask = ImageIO.read(new File("water_mask.png"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isWater(double lat, double lon) {

        if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
            return false;
        }

        int width = mask.getWidth();
        int height = mask.getHeight();

        int x = (int) ((lon + 180.0) / 360.0 * width);
        int y = (int) ((90.0 - lat) / 180.0 * height);

        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }

        int value = mask.getRGB(x, y) & 0xFF;
        return value > 128;
    }
}

final class Grid {

    static final int PIXEL_SIZE_METERS = 100;
    static final double MIN_WEIGHT = 0.01;

    HashMap<Long, Pixel> pixels = new HashMap<>();

    static class Pixel {
        int px;
        int py;
        double pH;
        double turbidity;
        double tds;
        double temp;
        double abi;
        double weight;

        Pixel(int px, int py) {
            this.px = px;
            this.py = py;
        }

        void updateVals(
                double pH,
                double turbidity,
                double tds,
                double temp,
                double w
        ) {
            if (weight == 0) {
                this.pH = pH;
                this.turbidity = turbidity;
                this.tds = tds;
                this.temp = temp;
            } else {
                this.pH = (this.pH * weight + pH * w) / (weight + w);
                this.turbidity = (this.turbidity * weight + turbidity * w) / (weight + w);
                this.tds = (this.tds * weight + tds * w) / (weight + w);
                this.temp = (this.temp * weight + temp * w) / (weight + w);
            }

            this.abi = getABI(this.pH, this.turbidity, this.tds, this.temp);
            weight += w;
        }
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static double getABI(double ph, double turbidity, double tds, double temp) {
        double tempScore = clamp((temp - 15.0) / 15.0, 0.0, 1.0);
        double turbScore = clamp(turbidity / 5.0, 0.0, 1.0);
        double tdsScore = clamp(tds / 500.0, 0.0, 1.0);
        double pHScore = clamp((ph - 7.0) / 2.5, 0.0, 1.0);

        return
                0.35 * tempScore +
                0.30 * turbScore +
                0.20 * tdsScore +
                0.15 * pHScore;
    }

    boolean isWaterPixel(int px, int py) {
        double[] latLon = pixelToCenterLatLon(px, py);
        return WaterMask.isWater(latLon[0], latLon[1]);
    }

    static long pixelToKey(int px, int py) {
        return (((long) px) << 32) | (py & 0xffffffffL);
    }

    int[] latLonToPixel(double lat, double lon) {
        double latMeters = lat * 111320;
        double lonMeters = lon * 111320 * Math.cos(lat * Math.PI / 180);

        int px = (int) Math.floor(lonMeters / PIXEL_SIZE_METERS);
        int py = (int) Math.floor(latMeters / PIXEL_SIZE_METERS);

        return new int[]{px, py};
    }

    double[] pixelToCenterLatLon(int px, int py) {
        double centerLatMeters = (py + 0.5) * PIXEL_SIZE_METERS;
        double centerLat = centerLatMeters / 111320;

        double centerLonMeters = (px + 0.5) * PIXEL_SIZE_METERS;
        double centerLon =
                centerLonMeters / (111320 * Math.cos(centerLat * Math.PI / 180));

        return new double[]{centerLat, centerLon};
    }

    public void addData(
            int startPx,
            int startPy,
            double pH,
            double turbidity,
            double tds,
            double temp
    ) {

        if (!isWaterPixel(startPx, startPy)) {
            return;
        }

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

            if (!isWaterPixel(px, py)) continue;

            Pixel p = pixels.computeIfAbsent(key, k -> new Pixel(px, py));
            p.updateVals(pH, turbidity, tds, temp, w);

            q.add(new int[]{px - 1, py, level + 1});
            q.add(new int[]{px + 1, py, level + 1});
            q.add(new int[]{px, py - 1, level + 1});
            q.add(new int[]{px, py + 1, level + 1});
        }
    }

    public void exportData() throws Exception {

        List<Map<String, Object>> outPixels = new ArrayList<>();

        for (Pixel p : pixels.values()) {
            double[] latLon = pixelToCenterLatLon(p.px, p.py);

            Map<String, Object> obj = new HashMap<>();
            obj.put("lat", latLon[0]);
            obj.put("lon", latLon[1]);
            obj.put("px", p.px);
            obj.put("py", p.py);
            obj.put("pH", p.pH);
            obj.put("turbidity", p.turbidity);
            obj.put("tds", p.tds);
            obj.put("temp", p.temp);
            obj.put("abi", p.abi);
            obj.put("weight", p.weight);

            outPixels.add(obj);
        }

        Map<String, Object> root = new HashMap<>();
        root.put("pixelSizeMeters", PIXEL_SIZE_METERS);
        root.put("pixelCount", outPixels.size());
        root.put("pixels", outPixels);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File("pixels.json"), root);
    }
}
