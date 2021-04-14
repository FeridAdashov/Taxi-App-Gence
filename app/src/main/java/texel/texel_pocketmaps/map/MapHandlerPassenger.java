package texel.texel_pocketmaps.map;

public class MapHandlerPassenger extends MapHandler {
    public static MapHandler getMapHandler() {
        if (mapHandler == null) {
            reset();
        }
        return mapHandler;
    }

    /**
     * reset class, build a new instance
     */
    public static void reset() {
        mapHandler = new MapHandlerPassenger();
    }
}

