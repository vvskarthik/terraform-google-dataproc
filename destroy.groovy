pipeline {
    agent { label 'Linux' }

    parameters {
        choice(name: 'APP_NAME', choices: 'abc\napachse\nffmpseg\nostds\nsottm\nposstgres\nsolr\n', description: 'Choose the app to deploy')
    }
    environment {
        DEPLOY_ENV   =   "${env.BRANCH_NAME == 'master' ? 'prod': env.BRANCH_NAME}"
        APP_NAME     =   "${env.APP_NAME}"
        //CREDENTIALS  =   "${env.BRANCH_NAME}-credentials" 
        //GOOGLE_APPLICATION_CREDENTIALS = credentials("project-service-account@lws-cloud-sbx-devops.iam.gserviceaccount.com")
        GOOGLE_APPLICATION_CREDENTIALS = credentials("${env.BRANCH_NAME}-credentials")
    }
    stages {
        stage('Plan') {
            steps {
                script{
                echo "plan: ${WORKSPACE}/$DEPLOY_ENV/${APP_NAME}"
                dir ("${WORKSPACE}/$DEPLOY_ENV/${APP_NAME}") {
                echo 'Terraform planning....for Repo:-' + env.APP_NAME
                sh '''
                 terraform init 
                 terraform state list
                
                '''  
                }
                    
                }
         }
        }   
           

        stage('Approval'){
            steps{
                 script {
                  
          echo 'Terraform Apply waiting for approval'
          emailext body: "Jenkins Build Approval: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}/console",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                subject: "Jenkins Build Approval: Job ${env.JOB_NAME}" , to: 'vvskarthik@gmail.com'
          echo 'email send'
          def userInput = input(id: 'confirm', message: 'Apply Terraform?', submitter: 'rakokkul,deekumar', parameters: [ [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Apply terraform', name: 'confirm'] ])
                //input "Terrform plan?"
            }
            
            }
        }


        stage('Destroy') {
            steps {
                script{

                
                echo "plan: ${WORKSPACE}/$DEPLOY_ENV/${APP_NAME}"
                dir ("${WORKSPACE}/$DEPLOY_ENV/${APP_NAME}") {
                echo 'Terraform apply....for Repo:-' + env.APP_NAME
               sh '''
                 terraform destroy -auto-approve
                
                '''    

                }
                                   
            }
        }
    }
}

    
    // Post describes the steps to take when the pipeline finishes
    
    post {

        always {
            echo "Clearing workspace"
            
            deleteDir() // Clean up the local workspace so we don't leave behind a mess, or sensitive files
            
            emailext body: "${currentBuild.currentResult}: Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\n More info at: ${env.BUILD_URL}",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']],
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}" , to: 'vvskarthik@gmail.com'
        }
    }

}
