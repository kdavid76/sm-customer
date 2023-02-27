
pipeline  {
    agent { label 'kubeagent' }
    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    environment {
        GITHUB_PATH = "https://${env.GITHUB_APIKEY}@github.com/kdavid76/sm-customer.git"
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
                    branches: [[name: env.BRANCH_NAME]],
                    userRemoteConfigs: [[url: env.GITHUB_PATH]]
                ])
            }
        }

        stage('Build') {
            steps {
                sh '''
                    mvn clean package -DskipTests=true
                '''
            }
        }

        stage('Static style check') {
            steps {
                sh '''
                    mvn ktlint:check
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    mvn test
                '''
            }
        }

        stage('Deploy snapshot to artifactory') {
            when {
                branch "develop"
            }
            steps {
                sh('mvn deploy')
            }
        }
    }
}