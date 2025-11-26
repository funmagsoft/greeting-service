# greeting-service

Prosty serwis Java Spring Boot zwracający powitania.

## Endpoint

```bash
GET /greeting?name=John
# Response: {"greeting": "hello you John"}
```

## Development

```bash
# Build
mvn clean package

# Run
java -jar target/greeting-service-0.1.0.jar

# Test
curl http://localhost:8080/greeting?name=World
```

## Deployment (GitOps)

### Automatyczny deploy do DEV
```bash
git commit -m "feat: my feature"
git push origin main
# ✅ Auto-deploy do dev (~5-8 min)
```

### Status deploymentu
- **GitHub Actions**: https://github.com/funmagsoft/greeting-service/actions
- **GitOps repo**: https://github.com/funmagsoft/gitops
- **Konfiguracja**: `gitops/apps/greeting-service/`

### Co jest wdrożone?
```bash
# W repo gitops:
cat apps/greeting-service/values-dev.yaml | grep tag
```

### Rollback
W repo `gitops`:
```bash
git revert HEAD
git push origin main
# ✅ Automatyczny rollback
```

---

**Więcej**: [GitOps Documentation](https://github.com/funmagsoft/gitops)
