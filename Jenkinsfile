pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "souptiktat/springboot-app"
        VERSION = "${BUILD_NUMBER}"
        K8S_NAMESPACE = "springboot-app"
    }

    tools {
        maven "Maven3"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t $DOCKER_IMAGE:$VERSION ."
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {

                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker push $DOCKER_IMAGE:$VERSION
                    """
                }
            }
        }

        stage('Deploy Green Version') {
            steps {
                sh """
                kubectl set image deployment/springboot-green \
                springboot-app=$DOCKER_IMAGE:$VERSION \
                -n $K8S_NAMESPACE
                """
            }
        }

        stage('Wait for Green Ready') {
            steps {
                sh """
                kubectl rollout status deployment/springboot-green \
                -n $K8S_NAMESPACE
                """
            }
        }

        stage('Switch Traffic to Green') {
            steps {
                sh """
                kubectl patch service springboot-service \
                -n $K8S_NAMESPACE \
                -p '{"spec":{"selector":{"app":"springboot-app","version":"green"}}}'
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    sleep 20
                    def status = sh(
                        script: "kubectl get pods -n $K8S_NAMESPACE",
                        returnStatus: true
                    )

                    if (status != 0) {
                        error("Deployment verification failed")
                    }
                }
            }
        }

        stage('Cleanup Old Blue (Optional)') {
            steps {
                sh "kubectl scale deployment springboot-blue --replicas=0 -n $K8S_NAMESPACE"
            }
        }
    }

    post {
        failure {
            echo "Rolling back to Blue..."

            sh """
            kubectl patch service springboot-service \
            -n $K8S_NAMESPACE \
            -p '{"spec":{"selector":{"app":"springboot-app","version":"blue"}}}'
            """
        }

        always {
            cleanWs()
        }
    }
}