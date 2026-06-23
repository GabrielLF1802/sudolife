#!/bin/bash
#----------------------------------------------
# $1: novo número de versão
#----------------------------------------------

echo "Branch atual no git: $(git branch --show-current)"
echo "Próxima versão: $1"

escapedParameter=$(echo "$1" | sed -e 's/[]$.*[\^]/\\&/g' )

# 1. Atualiza a versão no application.properties
sed -i "/app.version=/ s/=.*/=${escapedParameter}/" ./src/main/resources/application.properties
git add ./src/main/resources/application.properties

# 2. Atualiza a versão no pom.xml
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=$1
git add pom.xml

# 3. Commita e gera a Tag
git commit -m "release: v$1"
echo "Criando tag e enviando para o repositório remoto..."
git tag -a "$1" -m "release v$1"
git push --follow-tags