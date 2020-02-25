//package org.sireum.hamr.inspector.gui;
//
//import art.Bridge;
//import art.UConnection;
//import art.UPort;
//import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.sireum.hamr.inspector.common.ArtUtils;
//import org.sireum.hamr.inspector.gui.gfx.Coloring;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//public class ArtUtil {
//
//    @Nullable
//    private static volatile Coloring<Bridge> bridgeColoring;
//
//    @Nullable
//    private static volatile ArtUtils artUtils = null;
//
//    public ArtUtil(@NotNull ArtUtils artUtils) {
//        if (ArtUtil.artUtils != null) {
//            log.error("Can only be initialized once.");
//            throw new IllegalStateException("ArtUtil can only be initialized once.");
//        }
//
//        ArtUtil.artUtils = artUtils;
//        bridgeColoring = Coloring.ofUniformlyDistantColors(artUtils.getBridges(), App.COLOR_SCHEME_HUE_OFFSET,
//                App.COLOR_SCHEME_SATURATION, App.COLOR_SCHEME_BRIGHTNESS);
//    }
//
//    @NotNull
//    public static Coloring<Bridge> getBridgeColoring() {
//        return bridgeColoring;
//    }
//
//    @NotNull
//    public static List<Bridge> getBridges() {
//        return artUtils.getBridges();
//    }
//
//    @NotNull
//    public static List<UPort> getPorts() {
//        return artUtils.getPorts();
//    }
//
//    @NotNull
//    public static List<UConnection> getConnections() {
//        return artUtils.getConnections();
//    }
//
//    public static Bridge getBridge(int bridgeId) {
//        return artUtils.getBridge(bridgeId);
//    }
//
//    public static Bridge getBridge(@NotNull UPort port) {
//        return artUtils.getBridge(port);
//    }
//
//    public static UPort getPort(int portId) {
//        return artUtils.getPort(portId);
//    }
//
//    public static String prettyPrint(@NotNull Bridge bridge) {
//        return artUtils.prettyPrint(bridge);
//    }
//
//    public static String prettyPrint(@NotNull UPort port, @NotNull Bridge bridge) {
//        return ArtUtils.prettyPrint(port, bridge);
//    }
//
//    public static String prettyPrint(@NotNull UPort port) {
//        return artUtils.prettyPrint(port);
//    }
//
//    public static String informativePrettyPrint(@NotNull UPort port) {
//        return artUtils.informativePrettyPrint(port);
//    }
//
//    public static String informativePrettyPrint(@NotNull Bridge bridge) {
//        return artUtils.informativePrettyPrint(bridge);
//    }
//
//    public static String formatTime(long timeInMillis) {
//        return ArtUtils.formatTime(timeInMillis);
//    }
//
//}
