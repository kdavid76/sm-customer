
pipeline  {
    agent { label 'kubeagent' }
    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    environment {
        GITHUB_PATH = "https://${env.GITHUB_APIKEY}@github.com/kdavid76/sm-customer.git"
        DOCKER_IMAGE_NAME = "davidkrisztian76/sm-customer"
        DOCKER_IMAGE = ""
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
                script {
                    def output = sh (
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    )
                    echo "Last commit message: ${output}"
                    def startsWiths = output.startsWith("[skip ci]")
                    echo "Result: ${startsWiths}"
                    if (startsWiths) {
                        currentBuild.getRawBuild().getExecutor().interrupt(Result.SUCCESS)
                        sleep(1)   // Interrupt is not blocking and does not take effect immediately.
                    }
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    mvn clean package -DskipTests=true
                '''
            }
        }

        stage('Build and deploy Docker Image') {
            environment {
                REGISTRY_CREDENTIALS = 'DockerHub_Credentials'
            }
            steps {
                sh('chmod +x /usr/bin/docker')
                script {
                    DOCKER_IMAGE = docker.build DOCKER_IMAGE_NAME

                    echo "Docker image: ${DOCKER_IMAGE}"

                    docker.withRegistry( 'https://registry.hub.docker.com', REGISTRY_CREDENTIALS ) {
                        dockerImage.push("latest")
                    }
                }
            }
        }

        /*
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
        */
    }
}