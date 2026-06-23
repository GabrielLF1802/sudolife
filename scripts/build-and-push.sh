#!/bin/bash
set -e
cd "$(dirname "$0")/.."

#-----------------------------------------
# Build e publicação automática da imagem no Docker Hub
#-----------------------------------------

BRANCH='master'
DOCKER_IMAGE_NAME_BACKEND='sudolife'
DOCKER_REGISTRY='gabriellf1802'
DOCKERFILE="Dockerfile"

echo "-------------------------"
echo "Iniciando Pipeline de Build"
echo "-------------------------"

# Passo 1: Garantir branch correta
git checkout ${BRANCH}
git pull origin ${BRANCH}

# Passo 2: Rodar testes (pode adicionar a flag -DskipTests se quiser ser mais rápido)
echo "---- Rodando Testes ----"
mvn clean test

# Passo 3: Encontrar a próxima versão do Git
NEW_VERSION_NUMBER=$(./scripts/next-git-tag.sh)
echo "A nova versão calculada é: ${NEW_VERSION_NUMBER}"

# Passo 4: Fazer o Bump da versão nos arquivos e no Git
./scripts/bump-up-version-and-push-git-tag.sh ${NEW_VERSION_NUMBER}

# Passo 5: Build final com a versão atualizada
echo "---- Build Maven ----"
mvn clean install -DskipTests

# Passo 6: Login no Docker Hub
echo "---- Docker Login ----"
echo "${SUDOLIFE_DOCKER_PWD}" | docker login -u "${SUDOLIFE_DOCKER_USER}" --password-stdin

# Passo 7: Build da Imagem Docker e Push
DOCKER_TAG=${NEW_VERSION_NUMBER}

echo "---- Docker Build e Push (Tag: ${DOCKER_TAG}) ----"
docker buildx build --platform linux/amd64 -f ${DOCKERFILE} \
  -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME_BACKEND}:${DOCKER_TAG} \
  -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME_BACKEND}:latest --push .

# Passo 8: Logout
docker logout
echo "Finalizado! Versão ${NEW_VERSION_NUMBER} publicada com sucesso."