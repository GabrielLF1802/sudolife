#!/bin/bash
#----------------------------------------------
# Obtém a próxima tag de versão baseada no repositório Git
#----------------------------------------------

highestTag=0.0.0
hasVersionTag=0
re='^[0-9]+\.[0-9]+\.[0-9]+$'

function versionGreater() {
   version1=$1
   version2=$2

   arrVersion=(${version1//./ })
   a1=${arrVersion[0]}
   a2=${arrVersion[1]}
   a3=${arrVersion[2]}

   arrVersion=(${version2//./ })
   b1=${arrVersion[0]}
   b2=${arrVersion[1]}
   b3=${arrVersion[2]}

   if [[ $a1 -gt $b1 ]] ; then
      echo "$a1.$a2.$a3"
      return 1
   elif [[ $a1 -eq $b1 ]] ; then
      if [[ $a2 -gt $b2 ]] ; then
         echo "$a1.$a2.$a3"
         return 1
      elif [[ $a2 -eq $b2 ]] ; then
         if [[ $a3 -gt $b3 ]] ; then
            echo "$a1.$a2.$a3"
            return 1
         fi
      fi
   fi
   echo "$b1.$b2.$b3"
}

function getNextGitTag() {
   while read line; do
      arrIN=(${line//;/ })
      TAG=${arrIN[0]}

      if [[ $TAG =~ $re ]] ; then
         highestTag=$(versionGreater $TAG $highestTag)
         hasVersionTag=1
      fi
   done

   # Aumenta o último número após o ponto (Patch)
   if [[ "$hasVersionTag" -eq 1 ]] ; then
      array=(${highestTag//./ })
      last=${array[2]}
      last=$((last + 1))
      highestTag=${array[0]}.${array[1]}.${last}
   else
      highestTag="1.0.0"
   fi

   echo $highestTag
}

RESULT=$(git tag | getNextGitTag)
echo "$RESULT"