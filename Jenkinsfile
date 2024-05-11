
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

        stage('Checks') {
            parallel {
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
            }
        }

    }
}