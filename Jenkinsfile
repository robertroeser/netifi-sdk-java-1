properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node {
    ansiColor('xterm') {
        withCredentials([
            string(credentialsId: 'artifactory-user', variable: 'secret')
        ]) {
            stage('Checkout') {
                checkout scm
            }
            stage('Build') {
               sh './gradlew clean build --info'
            }
            stage('Publish') {
               sh './gradlew publish -P$secret --info'
            }
        }
    }
}
