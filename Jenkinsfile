node {
  checkout scm
  withEnv(["PATH+MAVEN=${tool 'M3'}/bin"]) {
    sh 'mvn -Dmaven.repo.local=.repository -B -pl willow-maven-plugin -am clean install'
    sh 'mvn -Dmaven.repo.local=.repository -B -e clean verify'
  }
}
