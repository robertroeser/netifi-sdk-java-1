properties([
    buildDiscarder(logRotator(numToKeepStr: '5')),
    disableConcurrentBuilds()
])

node {
    ansiColor('xterm') {
        withCredentials([
            file(credentialsId: 'azure_mgmt_secrets', variable: 'secret_file')
        ]) {
            stage('Clean workspace') {
                deleteDir()
                sh 'ls -lah'
            }
            stage('Checkout Code') {
                checkout scm
            }
        }
    }
}
