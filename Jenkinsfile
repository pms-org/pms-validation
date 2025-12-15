pipeline {
    agent any

    environment {
        DOCKERHUB_REPO = "jaaswanth07/validation-service"
        IMAGE_TAG = "latest"
        EC2_IP="3.142.131.169"
        SERVER_URL="http://3.142.131.169:8085/swagger-ui/index.html"
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
                docker build -f docker/Dockerfile -t $DOCKERHUB_REPO:$IMAGE_TAG .
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
                    sh '''
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push $DOCKERHUB_REPO:$IMAGE_TAG
                    '''
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(['ec2-ssh-key']) {
                    withCredentials([file(credentialsId: 'pms-env-file', variable: 'ENV_FILE')]) {

                        // Copy compose file
                        sh '''
                        scp -o StrictHostKeyChecking=no \
                            docker/prod-docker-compose.yml \
                            $EC2_HOST:/home/ubuntu/docker-compose.yml
                        '''

                        // Copy .env inside EC2 from Jenkins secret file
                        sh '''
                        scp -o StrictHostKeyChecking=no "$ENV_FILE" "$EC2_HOST:/home/ubuntu/.env"
                        '''

                        // Deploy containers
                        sh '''
                        ssh -o StrictHostKeyChecking=no $EC2_HOST '
                            docker pull $DOCKERHUB_REPO:$IMAGE_TAG &&
                            docker compose down &&
                            docker compose up -d &&
                            docker ps
                        '
                        '''
                    }
                }
            }
        }
    }

    post {
        success { 
            echo "Deployment Successful" 
            echo "Deployed EC2 Host: $EC2_HOST"
        }
        failure { echo "Deployment Failed" }
    }
}