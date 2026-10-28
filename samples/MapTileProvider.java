package showcase.map;

/**
 * 地图瓦片提供者接口。
 *
 * 业务层只依赖此接口，demo 和真实天地图实现可以互换。
 */
public interface MapTileProvider {

    byte[] load(String layer, int z, int x, int y);
}
