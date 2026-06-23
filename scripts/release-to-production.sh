#!/usr/bin/env bash
set -e

# 1. Trava a execução sempre na pasta raiz do projeto
cd "$(dirname "$0")/.."

# 2. Carrega as senhas do Docker (caso você tenha criado o arquivo local)
if [ -f "./scripts/env-local.sh" ]; then
  source ./scripts/env-local.sh
fi

# 3. Trava de segurança: impede o script de rodar se as senhas estiverem vazias
if [ -z "$SUDOLIFE_DOCKER_USER" ] || [ -z "$SUDOLIFE_DOCKER_PWD" ]; then
  echo "🚨 ERRO: As variáveis DOCKER_USER e DOCKER_PWD não foram encontradas!"
  echo "Certifique-se de que elas estão exportadas no terminal ou no arquivo scripts/env-local.sh."
  exit 1
fi

#----------------------------------------------
# Seleção interativa e Deploy na VPS
#----------------------------------------------

DOCKER_IMAGE_BACKEND='gabriellf1802/sudolife'
LINUX_USER='root'
SERVER_IP='159.223.96.241'

echo "IMAGEM: ${DOCKER_IMAGE_BACKEND}"

generate_post_data() {
  cat <<EOF
  {"username": "$DOCKER_USER", "password": "${DOCKER_PWD}"}
EOF
}

# Faz o login e guarda o token de sessão
DOCKER_SESSION_TOKEN=$(
    curl -s -H "Content-Type: application/json" \
        -X POST \
        -d "$(generate_post_data)" \
        https://hub.docker.com/v2/users/login | jq -r .token
)

echo "--------------------------------------------------"
echo "Releases disponíveis no Docker Hub para: $DOCKER_IMAGE_BACKEND"
echo "--------------------------------------------------"

# Lista as versões
curl -s -H "Authorization: JWT $DOCKER_SESSION_TOKEN" https://hub.docker.com/v2/repositories/$DOCKER_IMAGE_BACKEND/tags/?page_size=100 | jq -r '.results|.[]|.name'

echo "--------------------------------------------------"
echo "Digite a versão que deseja instalar na VPS:"
echo "--------------------------------------------------"

while true; do
    read -r BACKEND_VERSION
    echo "Você selecionou: $BACKEND_VERSION"
    echo "Confirmar? (yes/no)"
    read -r confirmation
    case $confirmation in
        yes) echo "Versão confirmada."; break;;
        no) echo "Cancelado."; exit 1;;
    esac
done

echo "--------------------------------------------------"
echo "Conectando ao servidor de produção (${SERVER_IP})..."
echo "--------------------------------------------------"

ssh ${LINUX_USER}@${SERVER_IP} "bash -l" << EOF

    echo "-----------------------------------------------------"
    echo "Aplicando nova versão: ${BACKEND_VERSION}"
    echo "-----------------------------------------------------"

    if [[ "${BACKEND_VERSION}" != "" ]] ; then
        cd ~/sudolife/scripts/
        ./deploy-prod.sh ${BACKEND_VERSION}
    fi

    echo "-----------------------------------------------------"
    echo "Deploy finalizado. Bye..."
    echo "-----------------------------------------------------"

EOF