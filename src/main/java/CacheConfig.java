import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

/**
 * Created by Seto on 2017/1/12.
 */
public class CacheConfig {
    @ConfigureCache("test-cache")
    @TestCache
    @Produces
    public Configuration idCacheConfig() {
        return distributeConfig(defaultConfig()).build();
    }

    private ConfigurationBuilder distributeConfig(ConfigurationBuilder builder)
    {
        builder.clustering().cacheMode(CacheMode.DIST_SYNC);
        return builder;
    }

    private ConfigurationBuilder defaultConfig()
    {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.storeAsBinary().enable();
        return builder;
    }

    @Produces
    @ApplicationScoped
    public EmbeddedCacheManager defaultClusteredCacheManager() {
        System.out.println("new cache manager");
        GlobalConfigurationBuilder global = GlobalConfigurationBuilder.defaultClusteredBuilder();
        ConfigurationBuilder builder = distributeConfig(defaultConfig());
        return new DefaultCacheManager(global.build());
    }

    public void killCacheManager(@Disposes EmbeddedCacheManager cacheManager) {
        System.out.println("kill cache manager");
        cacheManager.stop();
    }
}
