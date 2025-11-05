# Description of GitHub Actions CI/CD

## Files:

There are three GitHub Actions workflows implementing the trunk-based + GitOps flow targeting ACR and AKS.

- Added .github/workflows/ci.yml: PR CI (Maven test + build, docker build smoke)
- Added .github/workflows/build-and-stage.yml: on push to main, build/push image to ACR and open a PR in env repo to bump staging image tag
- Added .github/workflows/release-prd.yml: on tag v, build/push versioned images and open a PR in env repo to bump production tag

## Required repo secrets/vars:

- ACR_LOGIN_SERVER: e.g., myregistry.azurecr.io
- ACR_USERNAME, ACR_PASSWORD: ACR credentials (or use a federated workload identity and docker/login action accordingly)
- ENV_REPO: org/env-repo-name (the GitOps repository)
- ENV_REPO_TOKEN: a token/app with write access to ENV_REPO
- Optional ENV_REPO_BRANCH: default main
- Optional STG_PATH: default environments/stg
- Optional PRD_PATH: default environments/prd
- Optional repo var IMAGE_NAME: defaults to the repo name

### Notes:

- The bump step tries Kustomize (kustomization.yaml images[].name/newTag), then Helm values.yaml (.image.registry/repository/tag), then falls back to text replace in manifests.
- If you want exact update logic, let me know your env repo format and I’ll tailor the yq/sed commands precisely.
- If you prefer Azure OIDC/federated login over username/password, I can switch to az acr login via azure/login + azure/cli.
