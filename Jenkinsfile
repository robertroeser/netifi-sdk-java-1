properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node {
    ansiColor('xterm') {
        withCredentials([
            string(credentialsId: 'artifactory-user', variable: 'secret')
        ]) {
            stage('Clean workspace') {
                deleteDir()
                sh 'ls -lah'
            }
            stage('Checkout Code') {
                checkout scm
            }
            stage('Build Project') {
               sh './gradlew clean build publish --info -P$secret'
            }
        }
    }
}
