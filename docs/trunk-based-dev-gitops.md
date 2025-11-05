
# Trunk‑Based Development with GitOps promotion

Thid document describes in short Trunk‑Based Development with PR‑based CI on GitHub Actions with GitOps CD via ArgoCD.

### Goals
- **CI/CD**: CI on GitHub Actions; CD via ArgoCD to AKS.
- **Deployments**: Auto deploy to `stg`; manual promote to `prd`.
- **Principles**: Simple, trunk‑based, auditable via PRs and tags.

### Repositories
- **App repo (this project)**: source code, Dockerfile, CI (GitHub Actions).
- **Env repo (GitOps)**: Kubernetes manifests/Helm/Kustomize per env watched by ArgoCD.
  - Structure: `environments/stg/` and `environments/prd/` overlays referencing the app image/tag.

### Branching model
- **main**: trunk; always releasable; protected.
- **Feature branches**: `feat/*`, `fix/*`, `chore/*` from `main`; short‑lived.
- **Hotfixes**: `hotfix/*` from the latest production tag when needed.

### Commits
- **Conventional Commits**: `feat:`, `fix:`, `chore:`, `docs:`, `refactor:` …
- **Good hygiene**: small, focused commits; reference issue IDs when relevant.

### Pull Requests
- **Source → Target**: `feat/*` → `main`.
- **Quality gates**: 1+ code review, required checks passing.
- **Merge strategy**: squash‑merge to keep history linear.
- **PR titles**: follow Conventional Commit style; body describes impact/risks.

### Tagging and versions
- **SemVer**: `vMAJOR.MINOR.PATCH` on `main` for production releases.
- **stg identifiers**: auto image tags like `:commit-sha` or `:v1.2.3+stg.<build>` for staging.
- **prd releases**: explicit annotated tag `vX.Y.Z` triggers production release flow.

### CI (GitHub Actions) in app repo
- **On PR to main**: build, unit tests, static analysis, container scan (optional).
- **On push to main**:
  - Build Docker image; tag `:commit-sha` and optionally `:v1.2.3+stg.<run>`; push to registry.
  - Open/update an automated PR in the env repo bumping the image tag in `environments/stg/`.
- **On push of a production tag `v*`**:
  - Build/push image tagged `:vX.Y.Z` (and `:commit-sha`).
  - Open a PR in env repo bumping the image in `environments/prd/`.

Tip: Use a GitHub App/Token with write access to the env repo for opening PRs.

### CD (ArgoCD) in env repo
- **stg**: ArgoCD watches `environments/stg/` with auto‑sync ON.
  - When the stg image‑bump PR is merged, ArgoCD deploys automatically.
- **prd**: ArgoCD watches `environments/prd/` with auto‑sync OFF (manual).
  - Release is via a human‑approved merge of the prd image‑bump PR; operator clicks “Sync” (or keep auto‑sync ON but rely on manual PR merge as the gate).

### Promotion flow
1. Developer opens PR `feat/*` → `main`; CI runs; review; squash‑merge.
2. CI builds/pushes image and opens stg bump PR; merge → ArgoCD deploys to `stg` automatically.
3. For production:
   - Create annotated tag `vX.Y.Z` on the desired `main` commit.
   - CI builds/pushes `:vX.Y.Z` image and opens prd bump PR in env repo.
   - Reviewer approves and merges prd PR.
   - Operator triggers ArgoCD Sync for `prd` (if manual).

### Hotfixes
- Branch `hotfix/x.y.z` from the last production tag.
- Implement fix → PR to `main` → tag `vX.Y.(Z+1)` → CI opens prd bump PR → merge → sync.
- Ensure `main` contains the hotfix (merge or cherry‑pick if needed).

### Protections and quality gates
- **Branch protections on `main`**: required checks, required review, linear history, no direct pushes.
- **Checks**: tests, lints, SCA, container scan (optional), SBOM (optional).
- **CODEOWNERS**: for sensitive paths.
- **Dependabot**: optional for dependency updates.

### Rollback
- **Env repo revert**: revert the image tag in `stg`/`prd` overlays to a previous known‑good version; ArgoCD reconciles.
- **ArgoCD**: use ArgoCD application rollback to a healthy revision for immediate recovery.

### Minimal file touchpoints
- **App repo**: `.github/workflows/ci.yml` builds/tests, pushes image, opens env PRs.
- **Env repo**: `environments/stg/` and `environments/prd/` overlays with a single image tag field CI updates.
