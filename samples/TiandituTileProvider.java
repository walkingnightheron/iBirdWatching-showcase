package showcase.map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 真实天地图适配器示意。
 *
 * 生产环境必须从环境变量读取 TIANDITU_TK；没有配置时禁用该适配器。
 */
@Component
@ConditionalOnProperty(name = "app.map.provider", havingValue = "tianditu")
public class TiandituTileProvider implements MapTileProvider {

    private final String token;

    public TiandituTileProvider(
            @org.springframework.beans.factory.annotation.Value("${TIANDITU_TK:}") String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("TIANDITU_TK is required when MAP_PROVIDER=tianditu");
        }
        this.token = token;
    }

    @Override
    public byte[] load(String layer, int z, int x, int y) {
        // 实际实现在这里调用天地图并完成超时、限流和磁盘缓存。
        // 不要把 token 拼接到客户端返回值或日志中。
        throw new UnsupportedOperationException("Configure the real upstream client in production");
    }
}
