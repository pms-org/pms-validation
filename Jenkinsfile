pipeline {
    agent any

    environment {
        DOCKERHUB_REPO = "jaaswanth07/validation-service"
        IMAGE_TAG = "latest"
        EC2_HOST = "ubuntu@3.142.131.169"
    }

    stages {

        stage('Clean Workspace') {
            steps { cleanWs() }
        }

        stage('Git Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/pms-org/pms-validation.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh """
                docker build -f docker/Dockerfile -t ${DOCKERHUB_REPO}:${IMAGE_TAG} .
                """
            }
        }

        stage('Login & Push to DockerHub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push ${DOCKERHUB_REPO}:${IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(['ec2-ssh-key']) {
                    withCredentials([string(credentialsId: 'pms-env-secrets', variable: 'ENV_CONTENT')]) {

                        // Copy compose file
                        sh """
                        scp -o StrictHostKeyChecking=no \
                            docker/prod-docker-compose.yml \
                            ${EC2_HOST}:/home/ubuntu/docker-compose.yml
                        """

                        // Create .env inside EC2 from Jenkins secret
                        sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_HOST} '
                            echo "${ENV_CONTENT}" > /home/ubuntu/.env
                        '
                        """

                        // Deploy containers
                        sh """
                        ssh -o StrictHostKeyChecking=no ${EC2_HOST} '
                            docker pull ${DOCKERHUB_REPO}:${IMAGE_TAG} &&
                            docker compose down &&
                            docker compose up -d
                        '
                        """
                    }
                }
            }
        }
    }

    post {
        success { echo "Deployment Successful" }
        failure { echo "Deployment Failed" }
    }
}