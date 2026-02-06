# Redis Sentinel Migration Guide for RTTM Service

## Overview
Migrate RTTM service from standalone Redis to Redis Sentinel for high availability and automatic failover.

---

## Required Changes

### 1. Docker Compose Configuration

Replace standalone Redis with Sentinel cluster in `docker-compose.yml`:

```yaml
services:
  redis-master:
    image: redis:7.4
    container_name: redis-master
    hostname: redis-master
    command: redis-server /redis/redis-master.conf
    volumes:
      - ./redis:/redis
      - redis-master-data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s

  redis-replica-1:
    image: redis:7.4
    container_name: redis-replica-1
    hostname: redis-replica-1
    command: redis-server /redis/redis-replica.conf
    volumes:
      - ./redis:/redis
      - redis-replica-1-data:/data
    depends_on:
      redis-master:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis-replica-2:
    image: redis:7.4
    container_name: redis-replica-2
    hostname: redis-replica-2
    command: redis-server /redis/redis-replica.conf
    volumes:
      - ./redis:/redis
      - redis-replica-2-data:/data
    depends_on:
      redis-master:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  sentinel-1:
    image: redis:7.4
    container_name: sentinel-1
    hostname: sentinel-1
    command: redis-sentinel /redis/sentinel-1.conf
    volumes:
      - ./redis:/redis
      - sentinel-1-data:/data
    depends_on:
      redis-master:
        condition: service_healthy
    ports:
      - "26379:26379"
    healthcheck:
      test: ["CMD", "redis-cli", "-p", "26379", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  sentinel-2:
    image: redis:7.4
    container_name: sentinel-2
    hostname: sentinel-2
    command: redis-sentinel /redis/sentinel-2.conf
    volumes:
      - ./redis:/redis
      - sentinel-2-data:/data
    depends_on:
      redis-master:
        condition: service_healthy
    ports:
      - "26380:26379"
    healthcheck:
      test: ["CMD", "redis-cli", "-p", "26379", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  sentinel-3:
    image: redis:7.4
    container_name: sentinel-3
    hostname: sentinel-3
    command: redis-sentinel /redis/sentinel-3.conf
    volumes:
      - ./redis:/redis
      - sentinel-3-data:/data
    depends_on:
      redis-master:
        condition: service_healthy
    ports:
      - "26381:26379"
    healthcheck:
      test: ["CMD", "redis-cli", "-p", "26379", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  redis-master-data:
  redis-replica-1-data:
  redis-replica-2-data:
  sentinel-1-data:
  sentinel-2-data:
  sentinel-3-data:
```

---

### 2. Redis Configuration Files

Create directory: `docker/redis/` or `redis/`

#### `redis-master.conf`
```conf
port 6379
bind 0.0.0.0
protected-mode no

# Persistence
save 3600 1
save 300 100
save 60 10000
dir "/data"

# Logging
loglevel notice
```

#### `redis-replica.conf`
```conf
port 6379
bind 0.0.0.0
protected-mode no

# Replication
replicaof redis-master 6379

# Persistence
save 3600 1
save 300 100
save 60 10000
dir "/data"

# Logging
loglevel notice
```

#### `sentinel-1.conf`, `sentinel-2.conf`, `sentinel-3.conf` (same content)
```conf
port 26379
dir "/data"

sentinel resolve-hostnames yes
sentinel announce-hostnames yes

sentinel monitor mymaster redis-master 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
```

---

### 3. Application Configuration (application.yml)

**Update Redis configuration:**

```yaml
spring:
  data:
    redis:
      timeout: ${REDIS_TIMEOUT:2s}
      password: ${REDIS_PASSWORD:}
      
      # Redis Sentinel configuration
      sentinel:
        master: ${REDIS_SENTINEL_MASTER:mymaster}
        nodes: ${REDIS_SENTINEL_NODES:sentinel-1:26379,sentinel-2:26379,sentinel-3:26379}
        password: ${REDIS_SENTINEL_PASSWORD:}
```

---

### 4. Java Redis Configuration Class

**Create or update `RedisConfig.java`:**

```java
package com.your.package.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.sentinel.master}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes}")
    private String sentinelNodes;

    @Value("${spring.data.redis.timeout}")
    private Duration redisTimeout;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.sentinel.password:}")
    private String sentinelPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.master(sentinelMaster);

        // Parse sentinel nodes
        String[] nodes = sentinelNodes.split(",");
        for (String node : nodes) {
            String[] parts = node.trim().split(":");
            if (parts.length == 2) {
                sentinelConfig.sentinel(new RedisNode(parts[0], Integer.parseInt(parts[1])));
            }
        }

        // Set passwords if provided
        if (StringUtils.hasText(redisPassword)) {
            sentinelConfig.setPassword(redisPassword);
        }
        
        if (StringUtils.hasText(sentinelPassword)) {
            sentinelConfig.setSentinelPassword(sentinelPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig);
        factory.setTimeout(redisTimeout.toMillis());
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
```

---

### 5. Environment Variables

#### `.env` (Local Development)
```bash
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
REDIS_SENTINEL_PASSWORD=
REDIS_PASSWORD=
REDIS_TIMEOUT=2s
```

#### `.env.production` (Kubernetes)
```bash
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=sentinel-1.default.svc.cluster.local:26379,sentinel-2.default.svc.cluster.local:26379,sentinel-3.default.svc.cluster.local:26379
REDIS_SENTINEL_PASSWORD=
REDIS_PASSWORD=
REDIS_TIMEOUT=2s
```

**Note:** Adjust namespace and service names based on your K8s setup.

---

### 6. Kubernetes ConfigMap (Production)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rttm-service-config
  namespace: default
data:
  REDIS_SENTINEL_MASTER: "mymaster"
  REDIS_SENTINEL_NODES: "sentinel-1.default.svc.cluster.local:26379,sentinel-2.default.svc.cluster.local:26379,sentinel-3.default.svc.cluster.local:26379"
  REDIS_TIMEOUT: "2s"
---
apiVersion: v1
kind: Secret
metadata:
  name: rttm-service-secrets
  namespace: default
type: Opaque
stringData:
  REDIS_PASSWORD: ""
  REDIS_SENTINEL_PASSWORD: ""
```

---

## Testing

### 1. Start Local Environment
```bash
cd docker  # or wherever docker-compose.yml is
docker-compose up -d
```

### 2. Verify Sentinel Cluster
```bash
# Check sentinel status
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL masters

# Check current master
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
# Should return: 1) "redis-master" 2) "6379"

# Check replicas
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster

# Check sentinels
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL sentinels mymaster
```

### 3. Run Application
```bash
mvn spring-boot:run
# or
./mvnw spring-boot:run
```

### 4. Test Failover (Optional)
```bash
# Stop master
docker stop redis-master

# Watch failover happen
docker logs -f sentinel-1

# Check new master (should be replica-1 or replica-2)
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster

# Verify app still works (automatic reconnection)

# Restart original master (will join as replica)
docker start redis-master
```

---

## Key Points

1. **Master Name**: Always use `mymaster` in application config - this is the Sentinel cluster name, not the hostname
2. **Hostnames**: Use container/service names (`sentinel-1`, `sentinel-2`) not IPs
3. **Port Mapping**: 
   - Local: Map to different host ports (26379, 26380, 26381)
   - K8s: Use same port 26379 for all (different services)
4. **Quorum**: Set to 2 (requires 2 out of 3 sentinels to agree for failover)
5. **Auto-Discovery**: Sentinels will auto-discover each other and update their configs at runtime
6. **Config Files**: Initial configs are clean templates. Sentinels will add runtime info (myid, known-sentinel, etc.)

---

## Troubleshooting

### App can't connect
```bash
# Check sentinels are running
docker ps | grep sentinel

# Check sentinel logs
docker logs sentinel-1

# Verify environment variables in app
docker exec -it <app-container> env | grep REDIS
```

### Master shows as down
```bash
# Check master container
docker logs redis-master

# Force failover if needed
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL failover mymaster
```

### Sentinel discovery issues
```bash
# Check if sentinels know each other
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL sentinels mymaster

# Should show 2 other sentinels (not itself)
```

---

## Dependencies Required

Ensure `pom.xml` has:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Lettuce is default, but if using Jedis: -->
<!-- <dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency> -->
```

---

## Migration Checklist

- [ ] Create `redis/` directory with 5 config files
- [ ] Update `docker-compose.yml` with Sentinel cluster
- [ ] Update `application.yml` Redis configuration
- [ ] Create/update `RedisConfig.java`
- [ ] Update `.env` for local development
- [ ] Update `.env.production` or K8s ConfigMap
- [ ] Test local deployment
- [ ] Test failover scenario
- [ ] Deploy to K8s and verify
- [ ] Update documentation/README

---

## Benefits

✅ **High Availability** - Service continues during Redis failures  
✅ **Automatic Failover** - No manual intervention needed  
✅ **Monitoring** - Sentinels monitor master health  
✅ **Consistency** - Same setup in dev and prod  
✅ **Scalability** - Read from replicas

---

## Reference

See working implementation in **pms-validation** service for complete example.

**Questions?** Check validation service's:
- `docker/docker-compose.yml`
- `docker/redis/*.conf`
- `src/main/java/com/pms/validation/config/RedisConfig.java`
- `src/main/resources/application.yml`
- `.env` and `.env.prod`
