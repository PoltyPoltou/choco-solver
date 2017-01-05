#!/bin/sh

function getVersionToRelease() {
    CURRENT_VERSION=`mvn ${MVN_ARGS} org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v "\[INFO\]"`
    echo ${CURRENT_VERSION%%-SNAPSHOT}
}

function getChocoVersion() {
    echo `mvn ${MVN_ARGS} org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=choco.version | grep -v "\[INFO\]"`
}

function guess() {
    v=$1
    echo "${v%.*}.$((${v##*.}+1))-SNAPSHOT"
}

function sedInPlace() {
	if [ $(uname) = "Darwin" ]; then
		sed -i '' "$1" $2
	else
		sed -i'' "$1" $2
	fi
}

VERSION=$(getVersionToRelease)
CHOCO_VERSION=$(getChocoVersion)
NEXT=$(guess $VERSION)
TAG="samples-${VERSION}"

git fetch
git checkout -b release || exit 1

mvn -q dependency:purge-local-repository

echo "New version is ${VERSION}"
YEAR=`LANG=en_US.utf8 date +"%Y"`
sedInPlace "s%Copyright.*.%Copyright (c) $YEAR, IMT Atlantique%"  LICENSE
sedInPlace "s%1. Download.*.%1. Download the source code of [samples](https://github.com/chocoteam/samples/releases/tag/samples-${VERSION})%"  README.md
sedInPlace "s%2. Download.*.%2. Download [choco-solver-${CHOCO_VERSION}.zip](https://github.com/chocoteam/choco-solver/releases/tag/${CHOCO_VERSION}) and unzip it%"  README.md
sedInPlace "s%choco-solver-.*-with-dependencies.jar%choco-solver-${CHOCO_VERSION}-with-dependencies.jar%"  README.md

#Update the poms:wq
mvn versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
mvn license:format

echo "Start release"
#Extract the version
COMMIT=$(git rev-parse HEAD)

#Quit if tag already exists
git ls-remote --exit-code --tags origin ${TAG} && quit "tag ${TAG} already exists"

#Working version ?
# Well, we assume the tests have been run before, and everything is OK for the release
#mvn clean test ||exit 1

git fetch origin master:refs/remotes/origin/master||quit "Unable to fetch master"
#Integrate with master and tag
echo "** Integrate to master **"
git checkout master ||quit "No master branch"
git pull origin master || quit "Unable to pull master"
git merge --no-ff -m "Merge tag '${TAG}' into master" ${COMMIT} ||quit "Unable to integrate to master"

#NOT USED FOR THE MOMENT
##Javadoc
#./bin/push_javadoc apidocs.git ${VERSION}

git tag ${TAG} ||quit "Unable to tag with ${TAG}"
git push --tags ||quit "Unable to push the tag ${TAG}"
git push origin master ||quit "Unable to push master"

mvn versions:set -DnewVersion=${NEXT} -DgenerateBackupPoms=false
git commit -m "Prepare the code for the next version" -a ||quit "Unable to commit to master"
#
##Push changes on develop, with the tag
git push origin master ||quit "Unable to push to develop"

#Clean
git branch --delete release ||quit "Unable to delete release"

git checkout $TAG
mvn clean install -DskipTests
git checkout master