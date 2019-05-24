def call(Map params) { 
node('master') {
     try{
    stage('SCM') {
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/mohit939/Web-application.git']]])
    }
	
	stage('SonarQube analysis') {
    withSonarQubeEnv('Sonar') {
      sh 'mvn sonar:sonar'
    } // SonarQube taskId is automatically attached to the pipeline context
  }
  stage("Quality Gate"){
  timeout(time: 60, unit: 'SECONDS') { // Just in case something goes wrong, pipeline will be killed after a timeout
    def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
    if (qg.status != 'OK') {
      error "Pipeline aborted due to quality gate failure: ${qg.status}"
    }
  }
}
    stage('Build') {
        tool name: 'java', type: 'jdk'
	    tool name: 'maven', type: 'maven'
	    def mvnHome = tool 'maven'
	    env.PATH = "${mvnHome}/bin:${env.PATH}"
       rtServer (
        'id': "Jfrog",
        'url': "http://104.43.135.22:8081/artifactory/",
        'username': "admin",
        'password': "password"
) 
 
    rtMavenDeployer (
    id: 'deployer-unique-id',
    serverId: 'Jfrog',
    releaseRepo: 'release/${BUILD_NUMBER}',
    snapshotRepo: "snapshot/${BUILD_NUMBER}"
)

rtMavenRun (
    tool: 'maven',
	type: 'maven',
    pom: 'pom.xml',
    goals: 'clean install',
    opts: '-Xms1024m -Xmx4096m',
    //resolverId: 'resolver-unique-id',
    deployerId: 'deployer-unique-id',
)
} 
 stage('Artifact Download') {
rtDownload (
    serverId: "Jfrog",
    spec:
        """{
          "files": [
            {
              "pattern": "snapshot/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/LoginWebApp-1.0*.war",
              "target": "/var/lib/jenkins/workspace/Project301/"
            }
         ]
        }"""
)
}

stage ('Application Deployment'){
sh 'scp /var/lib/jenkins/workspace/Project301/${BUILD_NUMBER}/com/javawebtutor/LoginWebApp/1.0-SNAPSHOT/LoginWebApp-1.0*.war ubuntu@13.89.226.204:/home/ubuntu/'
sh 'ssh ubuntu@13.89.226.204  \'sudo mv /home/ubuntu/LoginWebApp-1.0*.war /opt/tomcat/webapps/LoginWebApp.war\''
}
        } catch(error) {
		mail body: "${error}", subject: 'Build failure', to: 'mohit.kumar@mindtree.com'
		}
}
}
