def AWS_SSM_PARAM(param_name){
    def OUTPUT = sh ( script: "aws ssm get-parameter --name ${param_name} --with-decryption --query 'Parameter.Value' --output text", reurnStdout: true).trim()
    return OUTPUT
}

def call(){
    node(workstation){

        common.codeCheckout()

        if(app_type == "nodejs"){
            stage('Download Dependencies'){
                sh 'npm install'
            }
        }


        if(env.JOB_BASE_NAME ==~ "PR.*") {
            sh 'echo PR'
            stage('Test Cases'){
                //sh 'npm test'
            }

            stage('Code Quality') {
                env.SONAR_TOKEN = AWS_SSM_PARAM('sonar.token')
                wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${SONAR_TOKEN}", var: 'PASSWORD']]]) {
                    sh 'sonar-scanner -Dsonar.host.urlhttp://localhost:9000 -Dsonar.login=${SONAR_TOKEN} -Dsonar.projectKey=${repo_name} -Dsonar.qualitygate.wait=true -Dsonar.exclusions=node_modules/**'
                }
            }
            }else if(env.BRANCH_NAME == "main") {
                sh 'echo main'
                stage('Build'){}
            }else if(env.TAG_NAME == "PR.*") {
                sh 'echo TAG'
                stage('Build'){
                    sh 'echo ${TAG_NAME} > VERSION'
                    sh 'zip -r ${repo_name}-${TAG_NAME}.zip *'
                }
                stage('Release App'){
                    env.ARTIFACTORY_PASSWORD = AWS_SSM_PARAM('artifactory.password')
                    wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${ARTIFACTORY_PASSWORD}", var: 'PASSWORD']]]) {
                        sh 'curl -sSf -u "admin:${ARTIFACTORY_PASSWORD}" -X PUT  -T ${repo_name}-${TAG_NAME}.zip http://artifactory.rdevopsb72.online:8081/artifactory/${repo_name}/${repo_name}-${TAG_NAME}.zip'
                }
            }
        }else{
            sh 'echo branch'
            stage('Test Cases') {
                //sh 'npm test'
                //These test cases in organization is ideally written and we dont skip this step. Since it is LAB, So we are moving further ahead.
            }
        }
    }
}