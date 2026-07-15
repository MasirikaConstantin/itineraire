import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Génère les icônes Android à partir de l'image source haute résolution. */
public final class GenerateLauncherIcons {
    private static final Map<String, Integer> LEGACY_SIZES = new LinkedHashMap<>();
    private static final Map<String, Integer> ADAPTIVE_SIZES = new LinkedHashMap<>();

    static {
        LEGACY_SIZES.put("mdpi", 48);
        LEGACY_SIZES.put("hdpi", 72);
        LEGACY_SIZES.put("xhdpi", 96);
        LEGACY_SIZES.put("xxhdpi", 144);
        LEGACY_SIZES.put("xxxhdpi", 192);

        ADAPTIVE_SIZES.put("mdpi", 108);
        ADAPTIVE_SIZES.put("hdpi", 162);
        ADAPTIVE_SIZES.put("xhdpi", 216);
        ADAPTIVE_SIZES.put("xxhdpi", 324);
        ADAPTIVE_SIZES.put("xxxhdpi", 432);
    }

    private GenerateLauncherIcons() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                "Usage : java tools/GenerateLauncherIcons.java <image-source> <dossier-res>"
            );
        }

        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        if (source == null || source.getWidth() != source.getHeight()) {
            throw new IllegalArgumentException("L'image source doit être un PNG carré valide.");
        }

        Path resources = Path.of(args[1]);
        BufferedImage legacy = transparentCorners(source);
        BufferedImage adaptiveForeground = extractForeground(source);

        for (Map.Entry<String, Integer> entry : LEGACY_SIZES.entrySet()) {
            Path directory = resources.resolve("mipmap-" + entry.getKey());
            Files.createDirectories(directory);

            BufferedImage resized = resize(legacy, entry.getValue(), entry.getValue());
            writePng(resized, directory.resolve("ic_launcher.png"));
            writePng(roundIcon(resized), directory.resolve("ic_launcher_round.png"));
        }

        for (Map.Entry<String, Integer> entry : ADAPTIVE_SIZES.entrySet()) {
            Path directory = resources.resolve("mipmap-" + entry.getKey());
            BufferedImage canvas = placeInsideAdaptiveCanvas(adaptiveForeground, entry.getValue());
            writePng(canvas, directory.resolve("ic_launcher_foreground.png"));
        }
    }

    private static BufferedImage transparentCorners(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color color = new Color(source.getRGB(x, y));
                int maximum = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
                int alpha = maximum <= 8 ? 0 : Math.min(255, (maximum - 8) * 32);
                result.setRGB(x, y, (alpha << 24) | (color.getRGB() & 0x00FFFFFF));
            }
        }
        return result;
    }

    private static BufferedImage extractForeground(BufferedImage source) {
        BufferedImage extracted = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                Color color = new Color(source.getRGB(x, y));
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();

                int cyanScore = Math.min(green, blue) - red;
                int cyanAlpha = clamp((cyanScore - 4) * 12);
                int lightness = Math.min(red, Math.min(green, blue));
                int lightAlpha = red > 100 ? clamp((lightness - 65) * 5) : 0;
                int alpha = Math.max(cyanAlpha, lightAlpha);

                extracted.setRGB(x, y, (alpha << 24) | (color.getRGB() & 0x00FFFFFF));
            }
        }
        return extracted;
    }

    private static BufferedImage placeInsideAdaptiveCanvas(BufferedImage foreground, int size) {
        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int symbolSize = Math.round(size * 0.72f);
        BufferedImage resized = resize(foreground, symbolSize, symbolSize);
        int offset = (size - symbolSize) / 2;

        Graphics2D graphics = canvas.createGraphics();
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.drawImage(resized, offset, offset, null);
        graphics.dispose();
        return canvas;
    }

    private static BufferedImage roundIcon(BufferedImage source) {
        BufferedImage result = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D graphics = result.createGraphics();
        applyQuality(graphics);
        graphics.setClip(new Ellipse2D.Float(0, 0, source.getWidth(), source.getHeight()));
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return result;
    }

    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        applyQuality(graphics);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }

    private static void applyQuality(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static void writePng(BufferedImage image, Path destination) throws IOException {
        ImageIO.write(image, "png", destination.toFile());
    }
}
