# Redis Sentinel Migration - Complete Guide

## Overview
The validation service has been **fully migrated to Redis Sentinel** for both local development and production. This ensures:
- High availability
- Automatic failover
- Consistent architecture across all environments

## Architecture

```
┌─────────────────┐
│  Application    │
└────────┬────────┘
         │
    ┌────┴─────┐
    │ Sentinels │ (3 instances)
    └────┬─────┘
         │ Discover Master
    ┌────┴─────────────────┐
    │                      │
┌───▼────┐         ┌──────▼────┐
│ Master │◄────────│ Replica 1 │
└────────┘         └───────────┘
                   ┌───────────┐
                   │ Replica 2 │
                   └───────────┘
```

- **1 Redis Master** - Primary read/write node
- **2 Redis Replicas** - Read replicas for failover
- **3 Sentinels** - Monitor and manage automatic failover

---

## What Changed

### Before (Standalone Redis)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### After (Sentinel)
```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes: sentinel-1:26379,sentinel-2:26379,sentinel-3:26379
```

---

## Local Development

### 1. Start the Services

```bash
cd docker
docker-compose up -d
```

This starts:
- ✅ Postgres
- ✅ Redis Master + 2 Replicas
- ✅ 3 Sentinel instances
- ✅ Kafka + Schema Registry
- ✅ Control Center

### 2. Verify Sentinel Cluster

```bash
# Check sentinel status
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL masters

# Check current master
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster

# Check replicas
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL replicas mymaster

# Check sentinels
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL sentinels mymaster
```

### 3. Environment Variables

Use `.env.local`:
```bash
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
REDIS_PASSWORD=
REDIS_SENTINEL_PASSWORD=
```

### 4. Run Your Application

```bash
# With Maven
./mvnw spring-boot:run

# With Java
java -jar target/validation-service.jar
```

---

## Production (Kubernetes)

### 1. Apply ConfigMap and Secrets

```bash
kubectl apply -f notes/k8s-configmap-example.yaml
```

### 2. Update Sentinel Node Addresses

Edit the ConfigMap to match your K8s service names:

**Same namespace:**
```yaml
REDIS_SENTINEL_NODES: "sentinel-1:26379,sentinel-2:26379,sentinel-3:26379"
```

**Different namespace:**
```yaml
REDIS_SENTINEL_NODES: "sentinel-1.redis.svc.cluster.local:26379,sentinel-2.redis.svc.cluster.local:26379,sentinel-3.redis.svc.cluster.local:26379"
```

### 3. Deploy Your Application

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: validation-service
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: validation
        image: your-registry/validation-service:latest
        envFrom:
        - configMapRef:
            name: validation-service-config
        - secretRef:
            name: validation-service-secrets
        ports:
        - containerPort: 8083
```

---

## Testing Failover

### Local Environment

**1. Check current master:**
```bash
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
# Output: 1) "redis-master" 2) "6379"
```

**2. Kill the master:**
```bash
docker stop redis-master
```

**3. Watch automatic failover:**
```bash
# Watch sentinel logs
docker logs -f sentinel-1

# Check new master (should be a replica now)
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
# Output: 1) "redis-replica-1" 2) "6379"
```

**4. Verify app still works:**
- Your application should automatically reconnect to the new master
- No manual intervention required!

**5. Restart original master:**
```bash
docker start redis-master
# It will rejoin as a replica
```

---

## Configuration Files

### File Structure
```
docker/
├── docker-compose.yml
└── redis/
    ├── redis-master.conf
    ├── redis-replica.conf
    ├── sentinel-1.conf
    ├── sentinel-2.conf
    └── sentinel-3.conf
```

### Port Mapping

| Service       | Internal Port | External Port (Local) |
|---------------|---------------|-----------------------|
| Redis Master  | 6379          | 6379                  |
| Sentinel 1    | 26379         | 26379                 |
| Sentinel 2    | 26379         | 26380                 |
| Sentinel 3    | 26379         | 26381                 |
| Postgres      | 5432          | 5432                  |
| Kafka         | 9092          | 9092                  |

---

## Code Changes

### RedisConfig.java (Sentinel-only)
```java
@Bean
public LettuceConnectionFactory redisConnectionFactory() {
    RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
    sentinelConfig.master(sentinelMaster);
    
    // Parse sentinel nodes
    String[] nodes = sentinelNodes.split(",");
    for (String node : nodes) {
        String[] parts = node.trim().split(":");
        if (parts.length == 2) {
            sentinelConfig.sentinel(
                new RedisNode(parts[0], Integer.parseInt(parts[1]))
            );
        }
    }
    
    LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig);
    factory.setTimeout(redisTimeout.toMillis());
    return factory;
}
```

### application.yml (Sentinel-only)
```yaml
spring:
  data:
    redis:
      timeout: ${REDIS_TIMEOUT:2s}
      password: ${REDIS_PASSWORD:}
      sentinel:
        master: ${REDIS_SENTINEL_MASTER:mymaster}
        nodes: ${REDIS_SENTINEL_NODES}
        password: ${REDIS_SENTINEL_PASSWORD:}
```

---

## Troubleshooting

### Issue: Cannot connect to Redis

**Check sentinels are running:**
```bash
docker ps | grep sentinel
docker exec -it sentinel-1 redis-cli -p 26379 ping
# Should return: PONG
```

**Check sentinel knows about master:**
```bash
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL masters
```

**Check application logs:**
```bash
docker logs validation-service | grep -i redis
# Or kubectl logs validation-pod | grep -i redis
```

### Issue: Master shows as down

**Check master container:**
```bash
docker ps -a | grep redis-master
docker logs redis-master
```

**Check network connectivity:**
```bash
docker exec -it sentinel-1 redis-cli -h redis-master -p 6379 ping
```

**Force failover if needed:**
```bash
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL failover mymaster
```

### Issue: Application can't find sentinel nodes

**Verify environment variables:**
```bash
# In local Docker
docker exec -it validation-service env | grep REDIS

# In Kubernetes
kubectl exec -it validation-pod -- env | grep REDIS
```

**Check network connectivity:**
```bash
# Local
nc -zv localhost 26379
nc -zv localhost 26380
nc -zv localhost 26381

# Kubernetes
kubectl exec -it validation-pod -- nc -zv sentinel-1 26379
```

### Issue: Sentinel config keeps changing

This is **normal**! Sentinels rewrite their config files dynamically as they:
- Discover other sentinels
- Detect failovers
- Update master/replica information

The initial config files are templates. Don't worry about changes.

---

## Monitoring

### Check Sentinel Health
```bash
# All sentinels should agree on master
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
docker exec -it sentinel-2 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
docker exec -it sentinel-3 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
```

### Check Replication
```bash
# On master
docker exec -it redis-master redis-cli INFO replication

# On replicas
docker exec -it redis-replica-1 redis-cli INFO replication
docker exec -it redis-replica-2 redis-cli INFO replication
```

### Application Metrics
```bash
# Check Spring Boot actuator
curl http://localhost:8083/actuator/health

# Check Redis connection pool
curl http://localhost:8083/actuator/metrics/lettuce.connections
```

---

## Migration Checklist

Configuration:
- [x] Created Redis Sentinel configuration files
- [x] Updated docker-compose.yml with Sentinel setup
- [x] Updated application.yml (removed standalone config)
- [x] Updated RedisConfig.java (Sentinel-only)
- [x] Updated .env.local
- [x] Updated .env.production
- [x] Updated k8s-configmap-example.yaml

Testing:
- [ ] Start local environment with docker-compose
- [ ] Verify sentinel cluster formation
- [ ] Verify application connects successfully
- [ ] Test failover (stop redis-master)
- [ ] Verify automatic reconnection
- [ ] Deploy to Kubernetes
- [ ] Verify production connectivity

---

## Benefits

✅ **High Availability** - Service continues during node failures  
✅ **Automatic Failover** - No manual intervention needed  
✅ **Monitoring** - Sentinels constantly check master health  
✅ **Consistency** - Same architecture in dev and prod  
✅ **Scalability** - Read from replicas, write to master  

---

## Quick Commands Reference

```bash
# Start local environment
cd docker && docker-compose up -d

# Check sentinel status
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL masters

# Get current master
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster

# Test failover
docker stop redis-master

# Watch logs
docker logs -f sentinel-1

# Deploy to K8s
kubectl apply -f notes/k8s-configmap-example.yaml

# Check K8s pod
kubectl logs validation-pod | grep -i redis
```

---

## Support

For issues:
1. Check troubleshooting section above
2. Review sentinel logs: `docker logs sentinel-1`
3. Verify environment variables
4. Check network connectivity to sentinels
5. Ensure sentinel quorum is met (2 out of 3)
