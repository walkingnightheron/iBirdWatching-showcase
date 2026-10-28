package showcase.map;

import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 公开 demo 默认使用本地占位瓦片，不访问任何地图服务。
 */
@Component
@ConditionalOnProperty(name = "app.map.provider", havingValue = "mock", matchIfMissing = true)
public class MockMapTileProvider implements MapTileProvider {

    // 1x1 透明 PNG，仅用于展示网关链路。
    private static final byte[] PLACEHOLDER = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk"
            + "YAAAAAYAAjCB0C8AAAAASUVORK5CYII=");

    @Override
    public byte[] load(String layer, int z, int x, int y) {
        return PLACEHOLDER;
    }
}
