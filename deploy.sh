# !/bin/bash

project_name=${PWD##*/}

local_repo='/Users/gimjeyeon/Project/TDatabase'

mvn -Dmaven.test.skip=true -DaltDeploymentRepository=snapshot-repo::default::file://${local_repo}/snapshots clean deploy

cd ${local_repo}

git status
git add .
git status
git commit -m "Release new version of ${project_name}"
git push origin main