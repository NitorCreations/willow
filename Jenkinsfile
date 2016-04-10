node {
  checkout scm
  withEnv(["PATH+MAVEN=${tool 'M3'}/bin"]) {
    sh 'mvn -B -pl willow-maven-plugin -am clean install'
    sh 'mvn -B -Pintegrationtests -e clean verify'
  }
}
