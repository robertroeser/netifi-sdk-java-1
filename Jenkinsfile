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
              jdk = tool name: 'JDK18'
              env.JAVA_HOME = "${jdk}"

               sh './gradlew -P$secret clean build --info'
            }
        }
    }
}
