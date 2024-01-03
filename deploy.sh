# !/bin/bash

project_name=${PWD##*/}

local_repo='/Users/gimjeyeon/Project/TDatabase'

cd ${local_repo}

./gradlew clean publishToMavenLocal

git status
git add .
git status
git commit -m "Release new version of ${project_name}"
git push origin main