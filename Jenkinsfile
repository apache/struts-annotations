#!groovy

pipeline {
    agent {
        label 'ubuntu'
    }
    tools {
        jdk 'jdk_17_latest'
        maven 'maven_3_latest'
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
                withCredentials([file(credentialsId: 'lukaszlenart-repository-access-token', variable: 'CUSTOM_SETTINGS')]) {
                    sh 'mvn -s \${CUSTOM_SETTINGS} deploy -skipAssembly'
                }
            }
        }
    }
    post {
        always {
            cleanWs deleteDirs: true, patterns: [[pattern: '**/target/**', type: 'INCLUDE']]
        }
        // If this build failed, send an email to the list.
        failure {
            script {
                emailext(
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                        from: "Mr. Jenkins <jenkins@builds.apache.org>",
                        subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} failed",
                        body: """
There is a build failure in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
                )
            }
        }

        // If this build didn't fail, but there were failing tests, send an email to the list.
        unstable {
            script {
                emailext(
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                        from: "Mr. Jenkins <jenkins@builds.apache.org>",
                        subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} unstable",
                        body: """
Some tests have failed in ${env.JOB_NAME}.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
                )
            }
        }

        // Send an email, if the last build was not successful and this one is.
        fixed {
            script {
                emailext(
                        to: "dev@struts.apache.org",
                        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                        from: 'Mr. Jenkins <jenkins@builds.apache.org>',
                        subject: "Jenkins job ${env.JOB_NAME}#${env.BUILD_NUMBER} back to normal",
                        body: """
The build for ${env.JOB_NAME} completed successfully and is back to normal.

Build: ${env.BUILD_URL}
Logs: ${env.BUILD_URL}console
Changes: ${env.BUILD_URL}changes

--
Mr. Jenkins
Director of Continuous Integration
"""
                )
            }
        }
    }
}
