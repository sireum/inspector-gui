package org.sireum.hamr.inspector.gui.gfx;

import javafx.scene.paint.Color;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Coloring<T> {

    @Getter
    private final int colorCount;

    @Getter
    private final Collection<Color> colors;

    @Getter
    private final Map<T, Color> map;

    @Getter
    private final Map<T, String> rgbMap;

    public static <T> Coloring<T> ofUniformlyDistantColors(Collection<T> objects) {
        return ofUniformlyDistantColors(objects, 0.0f, 1.0f, 1.0f);
    }

    /**
     *
     * @param objects list of objects which will get color mappings
     * @param hueOffset [0.0f to 1.0f] the value of the initial hue from which all other hues are stepped to
     * @param saturation [0.0f to 1.0f] the saturation of all mapped colors
     * @param brightness [0.0f to 1.0f] the brightness of all mapped colors
     * @param <T> the type of object to walkOver the mapping over
     * @return an instance of Coloring which can be used to retrieve generated object-to-color mappings
     */
    public static <T> Coloring<T> ofUniformlyDistantColors(Collection<T> objects, float hueOffset, float saturation, float brightness) {
        final List<Color> colors =
                List.of(PaintOperations.uniformlyDistantColoring(
                        Color.hsb(hueOffset * 360.0, saturation, brightness), objects.size()
                ));

        return new Coloring<T>(objects, colors);
    }

    private Coloring(Collection<T> objects, List<Color> colors) {
        this.colorCount = colors.size();
        this.map = createMapping(objects, colors);
        this.rgbMap = createRgbMapping(map);
        this.colors = colors;
    }

    public Color getColorOf(T object) {
        if (!map.containsKey(object)) {
            throw new IllegalArgumentException("passed object has no color mapped to it");
        } else {
            return map.get(object);
        }
    }

    /**
     * Returns a hexadecimal "RRGGBB"-formatted String representing the object's color.
     * The result is always 6 characters and does NOT have any prefix.
     *
     * @param object
     * @return
     */
    public String getRgbStringOf(T object) {
        if (!rgbMap.containsKey(object)) {
            throw new IllegalArgumentException("passed object has no rgb mapped to it");
        } else {
            return rgbMap.get(object);
        }
    }

    private static <T> Map<T, Color> createMapping(Collection<T> objects, List<Color> colors) {
        final Iterator<T> objectsIterator = objects.iterator();
        final Map<T, Color> mutableMap = new HashMap<>(objects.size());
        final int n = objects.size();
        for (int i=0; i < n; i++) {
            mutableMap.put(objectsIterator.next(), colors.get(i));
        }
        return Collections.unmodifiableMap(mutableMap);
    }

    private static <T> Map<T, String> createRgbMapping(Map<T, Color> referenceMap) {
        final Map<T, String> mutableMap = new HashMap<>(referenceMap.size());
        for (Map.Entry<T, Color> entry : referenceMap.entrySet()) {
            final Color color = entry.getValue();
            mutableMap.put(entry.getKey(), formatJavafxColor(color));
        }
        return Collections.unmodifiableMap(mutableMap);
    }

    private static String formatJavafxColor(Color color) {
        // convert floating point value to [0-255] channel
        final int r = (int)Math.round(color.getRed() * 255.0);
        final int g = (int)Math.round(color.getGreen() * 255.0);
        final int b = (int)Math.round(color.getBlue() * 255.0);

        // convert channel value to hex string (with leading "0" as needed)
        final String rs = StringUtils.leftPad(Integer.toHexString(r), 2, '0').toUpperCase();
        final String gs = StringUtils.leftPad(Integer.toHexString(g), 2, '0').toUpperCase();
        final String bs = StringUtils.leftPad(Integer.toHexString(b), 2, '0').toUpperCase();

        return rs + gs + bs;
    }
}
