node {
  stage('TestJVM') {
    checkout scm
    sh 'git submodule update --init --recursive'
    sh "./sbt tortoiseJVM/test:compile"
    sh "./sbt tortoiseJVM/test:fast"
    sh "./sbt tortoiseJVM/test:language"
    sh "./sbt tortoiseJVM/depend"
    junit 'jvm/target/test-reports/*.xml'
  }

  stage('TestJS') {
    checkout scm
    sh 'git submodule update --init --recursive'
    sh "./sbt tortoiseJS/test:compile"
    sh "./sbt tortoiseJS/test:test"
    junit 'js/target/test-reports/*.xml'
  }
}
