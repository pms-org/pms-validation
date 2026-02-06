# Quick Start - Redis Sentinel

## Start Everything
```bash
cd docker
docker-compose up -d
```

## Verify Setup
```bash
# Check all containers are running
docker ps

# Verify sentinel cluster
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL masters

# Check current master
docker exec -it sentinel-1 redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster
```

## Environment Variables (Already set in .env.local)
```bash
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
```

## Run Application
```bash
./mvnw spring-boot:run
```

## Test Failover (Optional)
```bash
# 1. Stop master
docker stop redis-master

# 2. Watch sentinel elect new master
docker logs -f sentinel-1

# 3. Verify app still works (check logs)

# 4. Restart original master
docker start redis-master
```

## Stop Everything
```bash
docker-compose down
```

---

## Kubernetes Deployment

```bash
# 1. Apply ConfigMap
kubectl apply -f notes/k8s-configmap-example.yaml

# 2. Update sentinel nodes in ConfigMap to match your K8s service names

# 3. Deploy your app (reference the ConfigMap in your deployment)

# 4. Verify
kubectl logs <pod-name> | grep -i redis
```

---

See [REDIS-SENTINEL-MIGRATION-GUIDE.md](REDIS-SENTINEL-MIGRATION-GUIDE.md) for complete documentation.
