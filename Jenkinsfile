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
               sh './gradlew -P$secret clean build --info'
            }
        }
    }
}
