pipeline {
      agent any

      stages {
          stage('Build') {
              steps {
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
              echo 'CI passed'
          }
          failure {
              echo 'CI failed'
          }
      }
  }