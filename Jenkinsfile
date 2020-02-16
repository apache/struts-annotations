#!groovy

pipeline {
    agent {
        label 'ubuntu'
    }
    tools {
        jdk 'JDK 1.8 (latest)'
        maven 'Maven (latest)'
    }
    environment {
        MAVEN_OPTS = "-Xmx1024m"
    }
    options {
        buildDiscarder logRotator(daysToKeepStr: '14', numToKeepStr: '10')
        timeout(80)
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        quietPeriod(30)
    }
    triggers {
        pollSCM 'H/15 * * * *'
    }
    stages {
        stage('Clean up') {
            steps {
                cleanWs deleteDirs: true, patterns: [[pattern: '**/target/**', type: 'INCLUDE']]
            }
        }
        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B test'
                // step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
            }
            post {
                always {
                    junit(testResults: '**/surefire-reports/*.xml', allowEmptyResults: true)
                    junit(testResults: '**/failsafe-reports/*.xml', allowEmptyResults: true)
                }
            }
        }
        stage('Build Source & JavaDoc') {
            when {
                branch 'master'
            }
            steps {
                dir("local-snapshots-dir/") {
                    deleteDir()
                }
                sh 'mvn -B source:jar javadoc:jar -DskipAssembbly'
            }
        }
        stage('Deploy Snapshot') {
            when {
                branch 'master'
            }
            steps {
                withCredentials([file(credentialsId: 'struts-custom-settings_xml', variable: 'CUSTOM_SETTINGS')]) {
                    sh 'mvn -s \${CUSTOM_SETTINGS} deploy -skipAssembly'
                }
            }
        }
    }
    post {
        always {
            cleanWs deleteDirs: true, patterns: [[pattern: '**/target/**', type: 'INCLUDE']]
        }
    }
    post {
        // If this build failed, send an email to the list.
        failure {
            script {
                emailext(
                        subject: "[BUILD-FAILURE]: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]'",
                        body: """
              BUILD-FAILURE: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]':
               
              Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]</a>"
            """.stripMargin(),
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
            }
        }

        // If this build didn't fail, but there were failing tests, send an email to the list.
        unstable {
            script {
                emailext(
                        subject: "[BUILD-UNSTABLE]: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]'",
                        body: """
              BUILD-UNSTABLE: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]':
               
              Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]</a>"
            """.stripMargin(),
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
            }
        }

        // Send an email, if the last build was not successful and this one is.
        fixed {
            script {
                emailext(
                        subject: "[BUILD-STABLE]: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]'",
                        body: """
              BUILD-STABLE: Job '${env.JOB_NAME} [${env.BRANCH_NAME}] [${env.BUILD_NUMBER}]':
               
              Is back to normal.
            """.stripMargin(),
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
                )
            }
        }
    }
}
