pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build -x test --no-daemon'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
        }
    }

    post {
        success {
            githubNotify credentialsId: 'github-token',
                        status: 'SUCCESS',
                        description: 'Build and tests passed',
                        context: 'Jenkins CI'
        }
        failure {
            githubNotify credentialsId: 'github-token',
                        status: 'FAILURE',
                        description: 'Build or tests failed',
                        context: 'Jenkins CI'
        }
    }
}
