// Uses Declarative syntax to run commands inside a container.
pipeline {
    agent { label 'kubeagent' }
    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    environment {
        GITHUB_PATH = "https://${env.GITHUB_APIKEY}@github.com/kdavid76/sm-customer.git"
        DEVELOPMENT_CONN_URL = "scm:git:https://${env.GITHUB_APIKEY}@github.com/kdavid76/sm-customer.git"
        myVar = "Value of myVvar"
    }
    tools {
        jdk 'oracle-jdk-17'
        maven 'mvn-3.8.7'
        git 'default'
    }
    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                checkout([
                        $class: 'GitSCM',
                        branches: [[name: 'master']],
                        userRemoteConfigs: [[url: env.GITHUB_PATH]]
                ])
            }
        }
        stage('Configure Git') {
            steps {
                sh '''
                        git config --global user.email "mikehammer1902@gmail.com"
                        git config --global user.name "kdavid76"
                        git checkout master
                '''
            }
        }
        stage('Calculate release version') {
            steps {
                script {
                    def nextRelease = calculateNextRelease("${RELEASE_THIS_AS}")
                    echo "CALCULATED NEXT RELEASE: ${nextRelease}"

                    changePomVersion("${nextRelease}")
                }
            }
        }
        stage('Build release artifacts') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Push release to GitHub') {
            steps {
                script {
                    def mavenPom = readMavenPom file: 'pom.xml'
                    def pomVersion = mavenPom.version
                }
                sh '''
                    git add .
                    git commit -m "SM-COMMON: Release ${pomVersion}"
                    git push
                '''
            }
        }
        stage('Push release to artifactory') {
            steps {
                sh 'mvn deploy'
            }
        }
        stage('Calculate next snapshot version') {
            steps {
                script {
                    def nextSnapshot = calculateNextSnapshot("${RELEASE_THIS_AS}", "${NEXT_SNAPSHOT_AS}")
                    echo "CALCULATED NEXT SNAPSHOT: ${nextSnapshot}"

                    changePomVersion("${nextSnapshot}")
                }
            }
        }
        stage('Push snapshot to GitHub') {
            steps {
                script {
                    def mavenPom = readMavenPom file: 'pom.xml'
                    def pomVersion = mavenPom.version
                }
                sh '''
                    git add .
                    git commit -m "SM-COMMON: Next snapshot ${pomVersion}"
                    git push
                '''
            }
        }
        stage('Push snapshot to artifactory') {
            steps {
                sh 'mvn deploy'
            }
        }
    }
}

def calculateNextRelease(inputRelease) {
    def mavenPom = readMavenPom file: 'pom.xml'
    def currentRelease = mavenPom.version.split("-")[0]
    def nextRelease = inputRelease

    echo "NEXT RELEASE (if released from current version): ${currentRelease}"

    if ( !nextRelease ) {
        nextRelease = currentRelease;
    }

    return nextRelease
}

def calculateNextSnapshot(inputRelease, inputSnapshot) {
    def mavenPom = readMavenPom file: 'pom.xml'
    //The current version must be a release version
    def currentRelease = mavenPom.version.split("-")[0]
    def nextSnapshot = inputSnapshot;

    if( inputRelease ) {
        versionParts = inputRelease.split("\\.")
        def incVer = increaseAsInteger("${versionParts[2]}")
        nextSnapshot = getSnapshotVersionString("${versionParts[0]}","${versionParts[1]}", "${incVer}")
    } else {
        if( !nextSnapshot) {
            def versionParts = currentRelease.split("\\.")
            if( inputRelease ) {
                versionParts = inputRelease.split("\\.")
                def incVer = increaseAsInteger("${versionParts[2]}")
                nextSnapshot = getSnapshotVersionString("${versionParts[0]}","${versionParts[1]}", "${incVer}")
            } else if ( "${INCREMENT_FIELD}" == "Build Number" ) {
                def incVer = increaseAsInteger("${versionParts[2]}")
                nextSnapshot = getSnapshotVersionString("${versionParts[0]}","${versionParts[1]}", "${incVer}")
            } else if ( "${INCREMENT_FIELD}" == "Minor Version" ) {
                def incVer = increaseAsInteger("${versionParts[1]}")
                nextSnapshot = getSnapshotVersionString("${versionParts[0]}","${incVer}", "0")
            } else {
                def incVer = increaseAsInteger("${versionParts[0]}")
                nextSnapshot = getSnapshotVersionString("${incVer}","0", "0")
            }
        }
    }

    return nextSnapshot
}

def increaseAsInteger(value) {
    def incVer = "${value}" as Integer
    incVer++

    return incVer
}

def getSnapshotVersionString(partZero, partOne, partTwo) {
    return "${partZero}.${partOne}.${partTwo}-SNAPSHOT"
}

def changePomVersion(newVersion) {
    sh("mvn versions:set -DnewVersion=${newVersion} -P scm-release")
    sh("mvn versions:commit -P scm-release")
}
