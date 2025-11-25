# Migration Notes - greeting-service CI/CD

## Stan przed migracją (backup: migration-backup/phase0/greeting-cicd.yml)

**1 job**: `run-shared-workflow`
- Wywołuje: `build-and-deploy.yml` (CI + CD w jednym)
- Wykonuje: build → test → push ACR → deploy do AKS
- Secrets: Azure (OIDC) + AKS credentials

## Stan po Kroku 2.1 (Dual Mode)

**3 joby**:

### 1. `legacy-build-and-deploy` (WYŁĄCZONY - fallback)
```yaml
if: false
```
- **Cel**: Backup na wypadek problemów z nowym flow
- **Jak włączyć**: Zmień `if: false` → `if: true`
- **Wywołuje**: `build-and-deploy.yml` (stary workflow)

### 2. `build` (NOWY - CI only)
```yaml
uses: .../build.yml@main
```
- **Cel**: Tylko CI (build + test + push ACR)
- **Output**: `image_tag` (SHORT_SHA)
- **Wykonuje się**: Na PR i Push do main
- **Secrets**: Tylko Azure OIDC (nie potrzebuje AKS credentials)

### 3. `deploy-to-dev` (NOWY - GitOps trigger)
```yaml
needs: build
if: github.event_name == 'push'
```
- **Cel**: Trigger deployment w gitops repo
- **Wykonuje się**: Tylko na push do main (nie na PR)
- **Wymaga**: Secret `GITOPS_PAT` (Personal Access Token z dostępem do gitops repo)
- **Wysyła**: `repository_dispatch` do `funmagsoft/gitops`

## Flow

### Na PR (Pull Request):
```
PR → build job → Maven test + Docker build (bez push) → ✅
     ↓
     (deploy-to-dev NIE wykonuje się - if: github.event_name == 'push')
```

### Na Push do main:
```
Push → build job → Maven test + Docker build + Push ACR → output: image_tag
       ↓
       deploy-to-dev job → repository_dispatch → gitops/deploy.yml
                            ↓
                            Update values-dev.yaml
                            ↓
                            Helm upgrade AKS
```

## Secrets wymagane w greeting-service

### Istniejące (już skonfigurowane):
- `AZURE_CLIENT_ID` - dla OIDC do Azure (ACR push)
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`
- ~~`AKS_RG`~~ - **NIE UŻYWANE** w nowym flow (tylko w legacy)
- ~~`AKS_NAME`~~ - **NIE UŻYWANE** w nowym flow (tylko w legacy)

### Nowe (do dodania):
- `GITOPS_PAT` - GitHub Personal Access Token z uprawnieniem `repo` do gitops repo

## Variables wymagane w greeting-service

- `ACR_LOGIN_SERVER` - np. `myacr.azurecr.io` (dla client-payload do gitops)

## Rollback plan

### Jeśli nowy flow nie działa:

1. **Natychmiastowy rollback** (w cicd.yml):
   ```yaml
   legacy-build-and-deploy:
     if: true  # ← zmień z false na true
   
   build:
     if: false  # ← dodaj ten wiersz
   
   deploy-to-dev:
     if: false  # ← dodaj ten wiersz
   ```

2. **Commit i push**:
   ```bash
   git add .github/workflows/cicd.yml
   git commit -m "rollback: revert to legacy build-and-deploy workflow"
   git push origin main
   ```

3. **Pełny rollback** (przywrócenie z backupu):
   ```bash
   cp ../../migration-backup/phase0/greeting-cicd.yml .github/workflows/cicd.yml
   git add .github/workflows/cicd.yml
   git commit -m "rollback: restore original cicd.yml from backup"
   git push origin main
   ```

## Ważne uwagi

### Dlaczego stary workflow jest wyłączony (if: false)?

- Chcemy **dual mode** - nowy flow jako primary, stary jako fallback
- Ale nie chcemy, żeby oba się wykonywały jednocześnie (redundancja)
- `if: false` oznacza "jest gotowy, ale nie wykonuj się automatycznie"

### Co się stanie na pierwszym push po tej zmianie?

1. Job `build` wykona się (nowy workflow)
2. Job `deploy-to-dev` wykona się i wyśle `repository_dispatch`
3. W gitops uruchomi się workflow `deploy.yml`
4. Deployment do AKS nastąpi z gitops (nie z greeting-service)

### Jak przetestować przed merge do main?

1. Utwórz branch testowy
2. Zmodyfikuj cicd.yml tam
3. Otwórz PR
4. Workflow `build` wykona się (ale bez deploy-to-dev - bo PR)
5. Sprawdź, czy build przeszedł
6. Merge do main → pełny test z deployem

## Next steps (po zakończeniu migracji)

1. Po 1-2 tygodniach stabilnej pracy:
   - Usuń job `legacy-build-and-deploy` (już niepotrzebny)
   
2. Usuń niepotrzebne secrets:
   - `AKS_RG`, `AKS_NAME` (przeniesione do gitops)
   
3. Dokumentacja zaktualizowana

## Historia zmian

- **2025-11-25**: Krok 2.1 - Dodanie dual mode (nowy + legacy workflow)

